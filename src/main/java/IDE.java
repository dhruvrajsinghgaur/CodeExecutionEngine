import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class IDE extends Application {

    private TabPane tabPane;

    @Override
    public void start(Stage stage) {
        // Top bar buttons
        Button newTabBtn = new Button("New");
        Button openBtn = new Button("Open");
        Button saveBtn = new Button("Save");
        Button saveAsBtn = new Button("Save As");
        Button closeTabBtn = new Button("Close");
        Button runBtn = new Button("Run");
        Button clearBtn = new Button("Clear");
        Label status = new Label("Ready");

        HBox topBar = new HBox(8, newTabBtn, openBtn, saveBtn, saveAsBtn, closeTabBtn, runBtn, clearBtn, status);
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

        openBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Open Java File");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Java Files", "*.java"));
            File file = chooser.showOpenDialog(stage);
            if (file != null) {
                EditorTab tab = createEditorTab(file.getName(), "");
                if (tab.open(file)) {
                    tabPane.getTabs().add(tab);
                    tabPane.getSelectionModel().select(tab);
                    status.setText("Opened: " + file.getName());
                }
            }
        });

        saveBtn.setOnAction(e -> {
            EditorTab et = getSelectedEditorTab();
            if (et != null) {
                boolean ok = et.saveWithChooserIfNeeded(stage);
                if (ok) status.setText("Saved: " + et.getFileName());
            }
        });

        saveAsBtn.setOnAction(e -> {
            EditorTab et = getSelectedEditorTab();
            if (et != null) {
                FileChooser chooser = new FileChooser();
                chooser.setTitle("Save As");
                chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Java Files", "*.java"));
                File file = chooser.showSaveDialog(stage);
                if (file != null) {
                    boolean ok = et.saveAs(file);
                    if (ok) status.setText("Saved As: " + file.getName());
                }
            }
        });

        closeTabBtn.setOnAction(e -> {
            Tab selected = tabPane.getSelectionModel().getSelectedItem();
            if (selected != null) tabPane.getTabs().remove(selected);
        });

        runBtn.setOnAction(e -> {
            EditorTab et = getSelectedEditorTab();
            if (et == null) return;
            status.setText("Running: " + et.getText());
            // auto-save before run
            et.saveWithChooserIfNeeded(stage);
            et.runCode(() -> status.setText("Ready"));
        });

        clearBtn.setOnAction(e -> {
            EditorTab et = getSelectedEditorTab();
            if (et != null) et.clearOutput();
        });

        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(tabPane);

        Scene scene = new Scene(root, 1000, 700);
        try {
            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        } catch (Exception ex) {
            // stylesheet not found; continue without it
        }

        stage.setScene(scene);
        stage.getIcons();
        stage.setTitle("Code Execution Engine");
        stage.show();
    }

    private EditorTab createEditorTab(String title, String initialText) {
        return new EditorTab(title, initialText);
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