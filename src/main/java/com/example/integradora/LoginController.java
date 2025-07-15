package com.example.integradora;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ImageView userIcon;

    // Botón en el FXML llama a este método
    @FXML
    private void login() {
        String usuario = usernameField.getText();
        String contrasena = passwordField.getText();

        String sql = "SELECT ROL, ESTADO FROM USUARIOS WHERE USUARIO = ? AND CONTRASENA = ?";
        try (Connection con = Conexion.conectar();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, usuario);
            stmt.setString(2, contrasena);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String rol = rs.getString("ROL");
                String estado = rs.getString("ESTADO");
                if (!"ACTIVO".equalsIgnoreCase(estado)) {
                    mostrarAlerta("Usuario inactivo", "Este usuario está inactivo.", Alert.AlertType.ERROR);
                    return;
                }
                // Aquí navegas según rol (puedes cambiar a cargar FXML)
                switch (rol.toUpperCase()) {
                    case "ADMIN":
                        mostrarAlerta("Bienvenido", "¡Acceso como ADMIN!", Alert.AlertType.INFORMATION);
                        //abrirPantalla("/com/example/integradora/admin.fxml");
                        break;
                    case "LIDER_MESEROS":
                    case "LIDER DE MESEROS":
                        mostrarAlerta("Bienvenido", "¡Acceso como LIDER DE MESEROS!", Alert.AlertType.INFORMATION);
                        //abrirPantalla("/com/example/integradora/lider.fxml");
                        break;
                    case "MESERO":
                        mostrarAlerta("Bienvenido", "¡Acceso como MESERO!", Alert.AlertType.INFORMATION);
                        //abrirPantalla("/com/example/integradora/mesero.fxml");
                        break;
                    default:
                        mostrarAlerta("Rol desconocido", "El rol no es válido.", Alert.AlertType.ERROR);
                }
            } else {
                mostrarAlerta("Error de login", "Usuario o contraseña incorrectos.", Alert.AlertType.ERROR);
            }
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error de sistema", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    // Cuando tengas los otros FXML, descomenta y usa este método para navegar:
    /*
    private void abrirPantalla(String rutaFXML) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(rutaFXML));
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo cargar la pantalla.", Alert.AlertType.ERROR);
        }
    }
    */
}
