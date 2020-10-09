package com.viasat.ksqlstudio.view;

import com.viasat.ksqlstudio.App;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.fxmisc.richtext.model.PlainTextChange;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FileEditor {
    private final TabPane view;
    private final List<CodeEditor> editors = new ArrayList<>();

    private int fontSize = 25;

    public FileEditor(TabPane view){
        this.view = view;
        App.getSettings().getOpenFiles().removeIf(f -> !(new File(f)).exists());
        for (String f : App.getSettings().getOpenFiles()) {
            openFile(new File(f));
        }
    }


    private void addTab(File file) {
        // Create new tab
        Tab defaultTab = new Tab(file.getName());
        view.getTabs().add(defaultTab);

        // Add code editor to tab
        AnchorPane defaultPane = new AnchorPane();
        CodeEditor editor = new CodeEditor(file);
        editors.add(editor);
        defaultPane.getChildren().add(editor);

        // Position editor
        AnchorPane.setBottomAnchor(editor, 0D);
        AnchorPane.setTopAnchor(editor, 0D);
        AnchorPane.setLeftAnchor(editor, 0D);
        AnchorPane.setRightAnchor(editor, 0D);
        defaultTab.setClosable(true);
        defaultTab.setContent(defaultPane);

        ContextMenu menu = new ContextMenu();
        // Add close tab context menu item
        MenuItem closeItem = new MenuItem("Close");
        closeItem.setOnAction(e -> {
            int index = view.getTabs().indexOf(defaultTab);
            editors.remove(index);
            view.getTabs().remove(defaultTab);
            App.getSettings().getOpenFiles().remove(editor.getId());
        });
        menu.getItems().add(closeItem);
        defaultTab.setContextMenu(menu);

        if (file.exists()) {
            editor.loadFromFile(file);
        }
        /*
        if (view.getTabs().size() == 1) {
            runButton.setVisible(true);
            cbxOffset.setVisible(true);
        }*/
        editor.setStyle(String.format("-fx-font-size: %dpx;", (int)(this.fontSize * 1.25)));
        editor.plainTextChanges().subscribe(this::codeTyped);
    }

    public void codeTyped(PlainTextChange change) {
        String tabText  = view.getSelectionModel().getSelectedItem().getText();
        if (!tabText.endsWith("*")) {
            view.getSelectionModel().getSelectedItem().setText(tabText + "*");
        }
    }

    public void onNew() {
        String name = "query1";

        // Compute tab name (harder than it sounds)
        Optional<Tab> maxQuery = view.getTabs().stream()
                .filter(t -> t.getText().startsWith("query"))
                .reduce((max, t) -> max.getText().compareTo(t.getText()) > 0 ? max : t);

        if (maxQuery.isPresent()) {
            try {
                int num = Integer.parseInt(maxQuery.get().getText().substring(5)) + 1;
                name = "query" + num;
            } catch (NumberFormatException e) {
            }
        }
        // Add tab to editor
        addTab(new File(name));
        this.view.getSelectionModel().selectLast();
        this.editors.get(this.editors.size() - 1).requestFocus();
    }

    public void onOpen() {
        FileChooser fileChooser = new FileChooser();
        List<File> files = fileChooser.showOpenMultipleDialog(null);
        for (File file : files) {
            if (file != null && file.exists()) {
                openFile(file);
            }
        }
    }

    private void openFile(File file) {
        this.addTab(file);
        this.view.getSelectionModel().selectLast();
        App.getSettings().getOpenFiles().add(file.getAbsolutePath());
    }

    public void onSave() {
        CodeEditor editor = editors.get(view.getSelectionModel().getSelectedIndex());
        editor.save();
        view.getSelectionModel().getSelectedItem().setText(editor.getFile().getName());
        App.getSettings().getOpenFiles().add(editor.getFile().getAbsolutePath());
    }


    public void onClose(WindowEvent event) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader();
            fxmlLoader.setLocation(getClass().getResource("/save_dialog.fxml"));
            Parent parent = fxmlLoader.load();
            SaveController controller = fxmlLoader.getController();

            List<CodeEditor> unsaved = new ArrayList<>();
            for (int i = 0; i < view.getTabs().size(); i++) {
                if (view.getTabs().get(i).getText().endsWith("*")) {
                    unsaved.add(editors.get(i));
                    controller.addFile(editors.get(i).getFile().getName());
                }
            }
            if (unsaved.size() < 1) return;
            Scene scene = new Scene(parent, fontSize * 30, fontSize * 20);
            scene.getRoot().setStyle(String.format("-fx-font-size: %dpx;",
                    fontSize));
            Stage stage = new Stage();
            stage.setTitle("Confirm close");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(scene);
            controller.setStage(stage);
            stage.showAndWait();

            int choice = controller.getResult();
            if (choice == SaveController.SAVE) {
                for (CodeEditor e : unsaved) {
                    e.save();
                }
            }
            else if (choice == SaveController.CANCEL) {
                event.consume();
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
        for (CodeEditor area : editors) {
            area.destroy();
        }
    }

    public String getText() {
        return editors.get(view.getSelectionModel().getSelectedIndex()).getText();
    }

    public String getSelectedText() {
        return editors.get(view.getSelectionModel().getSelectedIndex()).getSelectedText();
    }

    public void scale(int fontSize) {
        this.fontSize = fontSize;
        this.editors.forEach(editor -> editor.setStyle(String.format("-fx-font-size: %dpx;",
                (int) (App.getSettings().getBaseFontSize() * 1.25))));
    }

}
