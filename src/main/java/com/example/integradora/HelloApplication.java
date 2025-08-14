package com.example.integradora;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;   // <--- Importa esta clase
import javafx.stage.Stage;
public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/integradora/Login.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);

        stage.setTitle("FastFood");
        stage.setScene(scene);
        stage.setMaximized(true);

        // 🔹 Cada vez que cambies la escena, se remaximiza automáticamente
        stage.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                stage.setMaximized(true);
                stage.sizeToScene();
                stage.centerOnScreen();
            }
        });

        stage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
