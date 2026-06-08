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

        // ── Toolbar buttons ───────────────────────────────────────────────────────
        Button newTabBtn   = new Button("New");
        Button openBtn     = new Button("Open");
        Button saveBtn     = new Button("Save");
        Button saveAsBtn   = new Button("Save As");
        Button closeTabBtn = new Button("Close Tab");
        Button runBtn      = new Button("▶  Run");
        Button stopBtn     = new Button("■  Stop");
        Button clearBtn    = new Button("Clear");
        Label  status      = new Label("Ready");

        // Stop is only enabled while code is running
        stopBtn.setDisable(true);
        stopBtn.setStyle("-fx-text-fill: #cc3333;");
        runBtn.setStyle("-fx-text-fill: #2ecc71;");

        // Visual separator between file ops and run controls
        Separator sep = new Separator();
        sep.setOrientation(javafx.geometry.Orientation.VERTICAL);

        HBox topBar = new HBox(8,
                newTabBtn, openBtn, saveBtn, saveAsBtn, closeTabBtn,
                sep,
                runBtn, stopBtn, clearBtn,
                status);
        topBar.setPadding(new Insets(8));

        // ── TabPane ───────────────────────────────────────────────────────────────
        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        tabPane.getTabs().add(createEditorTab("Untitled1.java", ""));

        // ── Button actions ────────────────────────────────────────────────────────

        newTabBtn.setOnAction(e -> {
            int n = tabPane.getTabs().size() + 1;
            tabPane.getTabs().add(createEditorTab("Untitled" + n + ".java", ""));
            tabPane.getSelectionModel().selectLast();
        });

        openBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Open Java File");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Java Files", "*.java"));
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
            if (et != null && et.saveWithChooserIfNeeded(stage)) {
                status.setText("Saved: " + et.getFileName());
            }
        });

        saveAsBtn.setOnAction(e -> {
            EditorTab et = getSelectedEditorTab();
            if (et == null) return;
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Save As");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Java Files", "*.java"));
            File file = chooser.showSaveDialog(stage);
            if (file != null && et.saveAs(file)) {
                status.setText("Saved As: " + file.getName());
            }
        });

        closeTabBtn.setOnAction(e -> {
            Tab selected = tabPane.getSelectionModel().getSelectedItem();
            if (selected != null) tabPane.getTabs().remove(selected);
        });

        runBtn.setOnAction(e -> {
            EditorTab et = getSelectedEditorTab();
            if (et == null) return;

            // Auto-save before running so the file is always up to date
            et.saveWithChooserIfNeeded(stage);

            // Disable Run, enable Stop
            runBtn.setDisable(true);
            stopBtn.setDisable(false);
            status.setText("Compiling...");

            et.runCode(() -> {
                // Restore button state once execution finishes
                runBtn.setDisable(false);
                stopBtn.setDisable(true);
                status.setText("Ready");
            });
        });

        stopBtn.setOnAction(e -> {
            EditorTab et = getSelectedEditorTab();
            if (et != null) {
                et.stopCode();
                status.setText("Stopped");
                runBtn.setDisable(false);
                stopBtn.setDisable(true);
            }
        });

        clearBtn.setOnAction(e -> {
            EditorTab et = getSelectedEditorTab();
            if (et != null) et.clearOutput();
        });

        // ── Scene ─────────────────────────────────────────────────────────────────
        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(tabPane);

        Scene scene = new Scene(root, 1100, 750);
        try {
            scene.getStylesheets().add(
                    getClass().getResource("/style.css").toExternalForm());
        } catch (Exception ex) {
            // Continue without stylesheet if not found
        }

        stage.setScene(scene);
        stage.setTitle("Code Execution Engine");
        stage.show();
    }

    private EditorTab createEditorTab(String title, String initialText) {
        return new EditorTab(title, initialText);
    }

    private EditorTab getSelectedEditorTab() {
        Tab t = tabPane.getSelectionModel().getSelectedItem();
        return t instanceof EditorTab ? (EditorTab) t : null;
    }

    public static void main(String[] args) {
        launch(args);

    }
}