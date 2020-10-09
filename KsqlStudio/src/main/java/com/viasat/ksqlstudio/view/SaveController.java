package com.viasat.ksqlstudio.view;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.stage.Stage;


public class SaveController {

    @FXML
    private ListView<String> filesList;
    private final ObservableList<String> items = FXCollections.observableArrayList();
    private Stage stage;

    private int result = CANCEL;

    public static int CANCEL = 0;
    public static int SAVE = 1;
    public static int CONTINUE = 2;

    @FXML
    public void initialize() {
        filesList.setItems(items);
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void addFile(String file) {
        items.add(file);
    }

    public void onSave() {
        this.result = SAVE;
        this.stage.close();
    }

    public void onClose() {
        this.result = CONTINUE;
        this.stage.close();
    }

    public void onCancel() {
        this.result = CANCEL;
        this.stage.close();
    }


    public int getResult() {
        return result;
    }
}
