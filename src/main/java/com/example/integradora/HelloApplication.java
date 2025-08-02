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
        // Carga tu FXML principal
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/integradora/Login.fxml"));
        Parent root = loader.load();

        // Crea la escena
        Scene scene = new Scene(root);

        // Carga el icono y agrégalo a la ventana
        Image icon = new Image(getClass().getResourceAsStream("/images/icon.png"));
        stage.getIcons().add(icon);

        // Título, escena, mostrar
        stage.setTitle("FastFood");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
