import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class IDE extends Application {

    private TabPane tabPane;

    @Override
    public void start(Stage stage) {
        // Top bar buttons
        Button newTabBtn = new Button("New");
        Button closeTabBtn = new Button("Close");
        Button runBtn = new Button("Run");
        Button clearBtn = new Button("Clear");
        Label status = new Label("Ready");

        HBox topBar = new HBox(8, newTabBtn, closeTabBtn, runBtn, clearBtn, status);
        topBar.setPadding(new Insets(8));

        // TabPane
        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);

        // Start with one tab
        tabPane.getTabs().add(createEditorTab("Untitled1.java", ""));

        // Button actions
        newTabBtn.setOnAction(e -> {
            int n = tabPane.getTabs().size() + 1;
            tabPane.getTabs().add(createEditorTab("Untitled" + n + ".java", ""));
            tabPane.getSelectionModel().selectLast();
        });

        closeTabBtn.setOnAction(e -> {
            Tab selected = tabPane.getSelectionModel().getSelectedItem();
            if (selected != null) tabPane.getTabs().remove(selected);
        });

        runBtn.setOnAction(e -> {
            EditorTab et = getSelectedEditorTab();
            if (et != null) {
                status.setText("Running: " + et.getText());
                et.runCode(() -> status.setText("Ready"));
            }
        });

        clearBtn.setOnAction(e -> {
            EditorTab et = getSelectedEditorTab();
            if (et != null) et.clearOutput();
        });

        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(tabPane);

        Scene scene = new Scene(root, 1000, 700);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        stage.setScene(scene);
        stage.setTitle("Multi-Tab Code Execution Engine");
        stage.show();
    }

    private EditorTab createEditorTab(String title, String initialText) {
        EditorTab tab = new EditorTab(title, initialText);
        return tab;
    }

    private EditorTab getSelectedEditorTab() {
        Tab t = tabPane.getSelectionModel().getSelectedItem();
        if (t instanceof EditorTab) return (EditorTab) t;
        return null;
    }

    public static void main(String[] args) {
        launch(args);
    }
}