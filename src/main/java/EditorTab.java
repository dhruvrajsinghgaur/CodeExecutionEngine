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

    private final Executor executor;
    private final File tempDir;

    private File currentFile;
    private boolean modified = false;
    private Thread runThread;

    public EditorTab(String title, String initialText) {
        super(title);

        tempDir = new File(System.getProperty("user.dir"), "temp/" + UUID.randomUUID());
        tempDir.mkdirs();

        executor = new Executor();

        codeArea = new CodeArea();
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.replaceText(initialText);
        codeArea.setWrapText(false);
        codeArea.getStyleClass().add("code-area");

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

        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setPromptText("Output will appear here...");
        outputArea.getStyleClass().add("output-area");

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

        SplitPane mainSplit = new SplitPane(codeArea, outputArea);
        mainSplit.setOrientation(Orientation.VERTICAL);
        mainSplit.setDividerPositions(0.7); // 70% editor, 30% output

        VBox layout = new VBox(6, mainSplit, inputField);
        VBox.setVgrow(mainSplit, Priority.ALWAYS);
        layout.setPadding(new Insets(8));

        setContent(layout);

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
            executor.stop();
            deleteTempDir();
        });
    }

    public String getFileName() {
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

    public void runCode(Runnable onFinished) {
        outputArea.clear();

        final String code = codeArea.getText();

        runThread = new Thread(() -> {
            Compiler.compile(code, tempDir,

                    (className) -> {
                        Platform.runLater(() ->
                                outputArea.appendText("▶  Running " + className + "...\n\n"));

                        executor.run(className, tempDir,
                                text -> outputArea.appendText(text));

                        Platform.runLater(() -> {
                            outputArea.appendText("\n\n✓  Process finished.\n");
                            onFinished.run();
                        });
                    },

                    (error) -> Platform.runLater(() -> {
                        outputArea.appendText("✗  Compile error:\n\n" + error);
                        onFinished.run();
                    })
            );
        });

        runThread.setDaemon(true);
        runThread.start();
    }

    public void stopCode() {
        executor.stop();
        if (runThread != null && runThread.isAlive()) {
            runThread.interrupt();
        }
    }

    public boolean isRunning() {
        return executor.isRunning();
    }

    public CodeArea getCodeArea() {
        return codeArea;
    }

    public void clearOutput() {
        outputArea.clear();
    }

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