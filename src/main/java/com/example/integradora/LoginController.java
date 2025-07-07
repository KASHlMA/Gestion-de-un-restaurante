package com.example.integradora;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.event.ActionEvent;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ImageView userIcon;

    @FXML
    public void initialize() {
        Image img = new Image(getClass().getResource("/images/iconUser.jpg").toExternalForm());
        userIcon.setImage(img);
    }

    @FXML
    private void login(ActionEvent event) {
        String user = usernameField.getText();
        String pass = passwordField.getText();

        try {
            FXMLLoader loader = new FXMLLoader();
            Scene nuevaEscena = null;

            if (user.equals("admin") && pass.equals("1234")) {
                loader.setLocation(getClass().getResource("/com/example/integradora/Admin.fxml"));
                nuevaEscena = new Scene(loader.load());

            } else if (user.equals("lider1") && pass.equals("abcd")) {
                loader.setLocation(getClass().getResource("/com/example/integradora/LiderMesero.fxml"));
                nuevaEscena = new Scene(loader.load());

            } else {
                System.out.println("Credenciales incorrectas.");
                return;
            }

            // Obtener el Stage y cambiar escena
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(nuevaEscena);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
