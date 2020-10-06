package com.viasat.ksqlstudio;

import com.viasat.ksqlstudio.view.Controller;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


/**
 * Entrypoint for the application. The base UI layout is loaded from the
 * layout FXML file, the settings are loaded, and the application is started.
 *
 */
public class App extends Application
{
    private  static AppSettings settings;

    public static AppSettings getSettings() {
        return settings;
    }

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("/layout.fxml"));
        Parent root = loader.load();
        primaryStage.setTitle("ksqlDB Studio");
        primaryStage.setScene(new Scene(root, 900, 900));
        primaryStage.show();
        final Controller controller = loader.getController();
        primaryStage.setOnCloseRequest( ev -> {
            controller.destroy();
            AppSettings.save(settings);
        });
    }

    public static void main(String[] args) {
        settings = AppSettings.load();
        launch(args);
    }
}
