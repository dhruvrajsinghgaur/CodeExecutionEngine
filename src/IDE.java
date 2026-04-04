import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class IDE extends Application {

    private TextArea codeArea;
    private TextArea outputArea;
    private TextField inputField;

    @Override
    public void start(Stage stage) {

        Button runBtn = new Button("Run");
        Button clearBtn = new Button("Clear");
        Label status = new Label("Ready");

        HBox topBar = new HBox(10, runBtn, clearBtn, status);
        topBar.setPadding(new Insets(10));

        codeArea = new TextArea();
        codeArea.setPromptText("Write your Java code here...");

        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setPromptText("Output...");

        inputField = new TextField();
        inputField.setPromptText("Enter input...");

        VBox root = new VBox(10, topBar, codeArea, outputArea, inputField);
        root.setPadding(new Insets(10));

        runBtn.setOnAction(e -> runCode());
        clearBtn.setOnAction(e -> outputArea.clear());
        inputField.setOnAction(e -> sendInput());

        stage.setScene(new Scene(root, 800, 600));
        stage.setTitle("Code Execution Engine");
        stage.show();
    }

    private void runCode() {
        outputArea.clear();

        Compiler.compile(codeArea.getText(), (className) -> {

            outputArea.appendText("Program Started...\n> ");

            // ✅ RUN IN BACKGROUND THREAD
            new Thread(() -> {
                Executor.run(className, text -> {
                    outputArea.appendText(text);
                });
            }).start();

        }, error -> {
            outputArea.appendText(error + "\n");
        });
    }

    private void sendInput() {
        String input = inputField.getText();
        inputField.clear();

        outputArea.appendText(input + "\n");

        Executor.sendInput(input);

        // show next prompt
        outputArea.appendText("> ");
    }

    public static void main(String[] args) {
        launch(args);
    }
}

