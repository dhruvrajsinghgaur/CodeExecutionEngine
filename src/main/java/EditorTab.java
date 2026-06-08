import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Duration;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.UUID;

public class EditorTab extends Tab {

    private final CodeArea codeArea;
    private final TextArea outputArea;
    private final TextField inputField;

    // Each tab owns its Executor and temp directory — no more static sharing
    private final Executor executor;
    private final File tempDir;

    private File currentFile;
    private boolean modified = false;
    private Thread runThread; // tracks the compile+run background thread

    public EditorTab(String title, String initialText) {
        super(title);

        // Unique temp directory per tab eliminates compilation collisions
        tempDir = new File(System.getProperty("user.dir"), "temp/" + UUID.randomUUID());
        tempDir.mkdirs();

        executor = new Executor();

        // ── Code editor ──────────────────────────────────────────────────────────
        codeArea = new CodeArea();
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.replaceText(initialText);
        codeArea.setWrapText(false);
        codeArea.getStyleClass().add("code-area");

        // Debounced syntax highlighting — fires 150 ms after the last keystroke.
        // Without this, every single keypress triggers a full regex pass (laggy on big files).
        PauseTransition highlightPause = new PauseTransition(Duration.millis(150));
        codeArea.textProperty().addListener((obs, oldText, newText) -> {
            setModified(true);
            highlightPause.setOnFinished(e -> {
                StyleSpans<Collection<String>> spans =
                        SyntaxHighlighter.computeHighlighting(newText);
                codeArea.setStyleSpans(0, spans);
            });
            highlightPause.playFromStart();
        });

        // ── Output console ───────────────────────────────────────────────────────
        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setPromptText("Output will appear here...");
        outputArea.getStyleClass().add("output-area");

        // ── Stdin input field ────────────────────────────────────────────────────
        inputField = new TextField();
        inputField.setPromptText("Type input and press Enter...");
        inputField.getStyleClass().add("input-field");
        inputField.setOnAction(e -> {
            String input = inputField.getText();
            if (input.isEmpty()) return;
            inputField.clear();
            outputArea.appendText(input + "\n");
            executor.sendInput(input);
        });

        // ── Layout: resizable split between editor and output ────────────────────
        SplitPane mainSplit = new SplitPane(codeArea, outputArea);
        mainSplit.setOrientation(Orientation.VERTICAL);
        mainSplit.setDividerPositions(0.7); // 70% editor, 30% output

        VBox layout = new VBox(6, mainSplit, inputField);
        VBox.setVgrow(mainSplit, Priority.ALWAYS);
        layout.setPadding(new Insets(8));

        setContent(layout);

        // ── On tab close: prompt to save if modified, then clean up ──────────────
        setOnCloseRequest(evt -> {
            if (isModified()) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Unsaved Changes");
                alert.setHeaderText("Save \"" + getFileName() + "\" before closing?");
                alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
                ButtonType result = alert.showAndWait().orElse(ButtonType.CANCEL);

                if (result == ButtonType.YES) {
                    boolean saved = saveWithChooserIfNeeded(
                            getTabPane() != null ? getTabPane().getScene().getWindow() : null);
                    if (!saved) { evt.consume(); return; } // save cancelled → keep tab open
                } else if (result == ButtonType.CANCEL) {
                    evt.consume(); return;
                }
            }
            executor.stop();    // kill any running process
            deleteTempDir();    // clean up compiled class files
        });
    }

    // ── File operations ───────────────────────────────────────────────────────────

    public String getFileName() {
        // Strip the "modified" asterisk when returning the plain name
        String t = getText();
        return t.endsWith("*") ? t.substring(0, t.length() - 1) : t;
    }

    public boolean isModified() {
        return modified;
    }

    private void setModified(boolean value) {
        modified = value;
        updateTabTitle();
    }

    private void updateTabTitle() {
        String base = currentFile != null ? currentFile.getName() : getFileName();
        setText(modified ? base + "*" : base);
    }

    /**
     * Saves to the current file if one exists, otherwise opens a Save As dialog.
     */
    public boolean saveWithChooserIfNeeded(Window ownerWindow) {
        if (currentFile == null) {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Save As");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Java Files", "*.java"));
            File chosen = chooser.showSaveDialog(ownerWindow);
            if (chosen == null) return false;
            return saveAs(chosen);
        }
        return saveAs(currentFile);
    }

    public boolean saveAs(File file) {
        try (Writer w = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            w.write(codeArea.getText());
            currentFile = file;
            setModified(false);
            return true;
        } catch (IOException e) {
            showError("Save Failed", e.getMessage());
            return false;
        }
    }

    public boolean open(File file) {
        try {
            String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            codeArea.replaceText(content);
            currentFile = file;
            setModified(false);
            updateTabTitle();
            return true;
        } catch (IOException e) {
            showError("Open Failed", e.getMessage());
            return false;
        }
    }

    // ── Run / Stop ────────────────────────────────────────────────────────────────

    /**
     * Compiles and runs the code in a background thread.
     * onFinished is always called on the JavaFX thread when execution ends.
     *
     * FIX: previously this called Compiler.compile() directly on the FX thread,
     * which blocked the UI during compilation. Now it's fully off-thread.
     */
    public void runCode(Runnable onFinished) {
        outputArea.clear();

        // Capture text on the FX thread before going to the background thread
        final String code = codeArea.getText();

        runThread = new Thread(() -> {
            Compiler.compile(code, tempDir,

                    // ── onSuccess ───────────────────────────────────────────────────
                    (className) -> {
                        Platform.runLater(() ->
                                outputArea.appendText("▶  Running " + className + "...\n\n"));

                        // executor.run() blocks until the process exits + all output is flushed
                        executor.run(className, tempDir,
                                text -> outputArea.appendText(text));

                        Platform.runLater(() -> {
                            outputArea.appendText("\n\n✓  Process finished.\n");
                            onFinished.run();
                        });
                    },

                    // ── onError ─────────────────────────────────────────────────────
                    (error) -> Platform.runLater(() -> {
                        outputArea.appendText("✗  Compile error:\n\n" + error);
                        onFinished.run();
                    })
            );
        });

        runThread.setDaemon(true);
        runThread.start();
    }

    /**
     * Kills the running process and interrupts the compile/run thread.
     */
    public void stopCode() {
        executor.stop();
        if (runThread != null && runThread.isAlive()) {
            runThread.interrupt();
        }
    }

    public boolean isRunning() {
        return executor.isRunning();
    }

    // ── Editor helpers ────────────────────────────────────────────────────────────

    public CodeArea getCodeArea() {
        return codeArea;
    }

    public void clearOutput() {
        outputArea.clear();
    }

    // ── Private helpers ───────────────────────────────────────────────────────────

    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle(title);
            a.setHeaderText(null);
            a.setContentText(message);
            a.showAndWait();
        });
    }

    private void deleteTempDir() {
        try {
            if (tempDir.exists()) {
                File[] files = tempDir.listFiles();
                if (files != null) for (File f : files) f.delete();
                tempDir.delete();
            }
        } catch (Exception ignored) {}
    }
}