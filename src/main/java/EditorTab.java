import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;

import java.util.Collection;

public class EditorTab extends Tab {

    private final CodeArea codeArea;
    private final TextArea outputArea;
    private final TextField inputField;

    public EditorTab(String title, String initialText) {
        super(title);

        codeArea = new CodeArea();
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.replaceText(initialText);

        // Placeholder label behavior
        Label placeholder = new Label("Write your Java code here...");
        placeholder.setStyle("-fx-text-fill: gray; -fx-opacity: 0.6;");
        placeholder.visibleProperty().bind(
                Bindings.createBooleanBinding(
                        () -> codeArea.getText().isEmpty(),
                        codeArea.textProperty()
                )
        );

        // Syntax highlighting
        codeArea.textProperty().addListener((obs, oldText, newText) -> {
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

        VBox centerBox = new VBox(6, codeArea, outputArea, inputField);
        centerBox.setPadding(new Insets(8));
        BorderPane content = new BorderPane(centerBox);
        content.setPadding(new Insets(4));

        setContent(content);
    }

    public CodeArea getCodeArea() {
        return codeArea;
    }

    public void clearOutput() {
        outputArea.clear();
    }

    public void runCode(Runnable onFinished) {
        outputArea.clear();

        Compiler.compile(codeArea.getText(), (className) -> {
            Platform.runLater(() -> {
                outputArea.appendText("Program Started...\n> ");
            });

            new Thread(() -> {
                Executor.run(className, text -> {
                    Platform.runLater(() -> outputArea.appendText(text));
                });
                Platform.runLater(onFinished);
            }).start();

        }, error -> {
            Platform.runLater(() -> {
                outputArea.appendText(error + "\n");
                onFinished.run();
            });
        });
    }
}