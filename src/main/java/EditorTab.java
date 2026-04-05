import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

public class EditorTab extends Tab {

    private final CodeArea codeArea;
    private final TextArea outputArea;
    private final TextField inputField;

    private File currentFile;          // file backing this tab, null if untitled
    private boolean modified = false;  // true when editor content differs from saved file

    public EditorTab(String title, String initialText) {
        super(title);

        codeArea = new CodeArea();
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.replaceText(initialText);

        // visual tweaks
        codeArea.setWrapText(false);
        codeArea.setStyle("-fx-font-size: 13px; -fx-font-family: 'Consolas';"); // fallback inline style


        // after creating codeArea and other controls
        codeArea.setPrefHeight(Region.USE_COMPUTED_SIZE);
        codeArea.setMaxHeight(Double.MAX_VALUE);

        // Placeholder label behavior (compatible binding)
        Label placeholder = new Label("Write your Java code here...");
        placeholder.getStyleClass().add("placeholder");
        placeholder.visibleProperty().bind(
                Bindings.createBooleanBinding(() -> codeArea.getText().isEmpty(), codeArea.textProperty())
        );

        // Track modifications and syntax highlighting
        codeArea.textProperty().addListener((obs, oldText, newText) -> {
            setModified(true);
            StyleSpans<Collection<String>> spans = SyntaxHighlighter.computeHighlighting(newText);
            codeArea.setStyleSpans(0, spans);
        });

        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setPromptText("Output...");

        inputField = new TextField();
        inputField.setPromptText("Enter input...");

        inputField.setOnAction(e -> {
            String input = inputField.getText();
            inputField.clear();
            outputArea.appendText(input + "\n");
            Executor.sendInput(input);
            outputArea.appendText("> ");
        });

        // ensure the parent VBox lets the codeArea grow
        VBox centerBox = new VBox(6, codeArea, outputArea, inputField);
        VBox.setVgrow(codeArea, Priority.ALWAYS);
        VBox.setVgrow(outputArea, Priority.NEVER); // console stays compact
        centerBox.setPadding(new Insets(8));
        BorderPane content = new BorderPane(centerBox);
        content.setPadding(new Insets(4));

        setContent(content);

        // When tab is closed, prompt to save if modified
        setOnCloseRequest(evt -> {
            if (isModified()) {
                Alert a = new Alert(Alert.AlertType.CONFIRMATION);
                a.setTitle("Unsaved Changes");
                a.setHeaderText("Save changes to " + getFileName() + " before closing?");
                a.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
                ButtonType res = a.showAndWait().orElse(ButtonType.CANCEL);
                if (res == ButtonType.YES) {
                    boolean ok = saveWithChooserIfNeeded(getTabPane() != null ? getTabPane().getScene().getWindow() : null);
                    if (!ok) evt.consume(); // cancel close if save failed or cancelled
                } else if (res == ButtonType.CANCEL) {
                    evt.consume();
                }
            }
        });
    }

    // ---------- File operations (public so IDE can call them) ----------

    public String getFileName() {
        return currentFile != null ? currentFile.getName() : getText();
    }

    public boolean isModified() {
        return modified;
    }

    private void setModified(boolean value) {
        modified = value;
        updateTabTitle();
    }

    private void updateTabTitle() {
        String base = currentFile != null ? currentFile.getName() : getText();
        if (base.endsWith("*")) base = base.substring(0, base.length() - 1);
        if (modified) {
            setText(base + "*");
        } else {
            setText(base);
        }
    }

    /**
     * Save using chooser if no current file. Returns true if saved.
     * If ownerWindow is null, chooser will show without owner.
     */
    public boolean saveWithChooserIfNeeded(Window ownerWindow) {
        if (currentFile == null) {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Save As");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Java Files", "*.java"));
            File chosen = chooser.showSaveDialog(ownerWindow);
            if (chosen == null) return false;
            return saveAs(chosen);
        } else {
            return saveAs(currentFile);
        }
    }

    public boolean saveAs(File file) {
        try (Writer w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            w.write(codeArea.getText());
            currentFile = file;
            setModified(false);
            updateTabTitle();
            return true;
        } catch (IOException e) {
            showError("Save failed", e.getMessage());
            return false;
        }
    }

    public boolean open(File file) {
        try {
            byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
            String content = new String(bytes, StandardCharsets.UTF_8);
            codeArea.replaceText(content);
            currentFile = file;
            setModified(false);
            updateTabTitle();
            return true;
        } catch (IOException e) {
            showError("Open failed", e.getMessage());
            return false;
        }
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

    // ---------- Editor helpers (public) ----------

    public CodeArea getCodeArea() {
        return codeArea;
    }

    public void clearOutput() {
        outputArea.clear();
    }

    public void runCode(Runnable onFinished) {
        outputArea.clear();

        Compiler.compile(codeArea.getText(), (className) -> {
            Platform.runLater(() -> outputArea.appendText("Program Started...\n> "));

            new Thread(() -> {
                Executor.run(className, text -> Platform.runLater(() -> outputArea.appendText(text)));
                Platform.runLater(onFinished);
            }).start();

        }, error -> Platform.runLater(() -> {
            outputArea.appendText(error + "\n");
            onFinished.run();
        }));
    }
}