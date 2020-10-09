package com.viasat.ksqlstudio.view;

import com.viasat.ksqlstudio.App;
import com.viasat.ksqlstudio.service.InformationService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import javax.swing.event.ChangeListener;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConnectController {

    @FXML
    private Label loadingLabel;

    @FXML
    private TextField hostnameField;

    @FXML
    private Button saveButton;

    @FXML
    private Label titleLabel;

    private Stage stage;
    private ExecutorService executor;

    @FXML
    public void initialize() {
        executor = Executors.newSingleThreadExecutor();
        hostnameField.textProperty().addListener(
                (observable, oldVal, newVal) -> saveButton.setDisable(true));
    }

    public void initialize(Stage stage) {
        this.stage = stage;
        stage.getScene().getRoot().setStyle(String.format("-fx-font-size: %dpx;",
                App.getSettings().getBaseFontSize()));
        titleLabel.setStyle(String.format("-fx-font-size: %dpx;",
                (int)(App.getSettings().getBaseFontSize() * 1.5)));
    }

    public void onTest() {
        if (!hostnameField.getText().startsWith("http://")) {
            hostnameField.setText("http://" + hostnameField.getText());
        }
        loadingLabel.setVisible(true);
        loadingLabel.setStyle("-fx-text-fill: black;");
        loadingLabel.setText("Testing connection...");
        executor.submit(this::testConnection);
    }

    public void onSave() {
        App.getSettings().setKsqlHost(hostnameField.getText());
        stage.close();
    }

    public void onCancel() {
        this.executor.shutdownNow();
        stage.close();
    }

    public void testConnection() {
        if (InformationService.checkConnection(hostnameField.getText())) {
            Platform.runLater(() -> {
                loadingLabel.setStyle("-fx-text-fill: green;");
                loadingLabel.setText("Connection verified");
                saveButton.setDisable(false);
            });
        }
        else {
            Platform.runLater(() -> {
                loadingLabel.setStyle("-fx-text-fill: red;");
                loadingLabel.setText("Failed to connect");
            });
        }
    }

}
