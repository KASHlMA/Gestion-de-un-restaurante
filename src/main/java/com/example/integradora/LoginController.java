package com.example.integradora;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginController {

    // Campos del formulario de login mapeados desde el FXML
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ImageView userIcon;

    /**
     * Método que se ejecuta cuando se presiona el botón de "Inicio de sesión".
     * Realiza la validación de usuario, contraseña y navega según el rol.
     */
    @FXML
    private void login() {
        // Obtiene el texto ingresado por el usuario en los campos
        String usuario = usernameField.getText();
        String contrasena = passwordField.getText();

        // Ahora tu consulta también trae el ID y el nombre real
        String sql = "SELECT ID, NOMBRE, ROL, ESTADO FROM USUARIOS WHERE USUARIO = ? AND CONTRASENA = ?";

        try (Connection con = Conexion.conectar();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, usuario);
            stmt.setString(2, contrasena);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String rol = rs.getString("ROL");
                String estado = rs.getString("ESTADO");
                int idUsuario = rs.getInt("ID");
                String nombreUsuario = rs.getString("NOMBRE");

                if (!"ACTIVO".equalsIgnoreCase(estado)) {
                    mostrarAlerta("Usuario inactivo", "Este usuario está inactivo.", Alert.AlertType.ERROR);
                    return;
                }

                switch (rol.toUpperCase()) {
                    case "ADMIN":
                        abrirPantalla("/com/example/integradora/admin.fxml");
                        break;
                    case "LIDER_MESEROS":
                    case "LIDER DE MESEROS":
                        abrirPantalla("/com/example/integradora/lider.fxml");
                        break;
                    case "MESERO":
                        // --- AQUÍ SE HACE LA REDIRECCIÓN ---
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/integradora/mesero.fxml"));
                        Parent root = loader.load();
                        MeseroController meseroController = loader.getController();
                        meseroController.setIdMesero(idUsuario, nombreUsuario);
                        Stage stage = (Stage) usernameField.getScene().getWindow();
                        stage.setScene(new Scene(root));
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

    /**
     * Muestra una alerta con título, mensaje y tipo.
     * Puedes usar esto para mostrar mensajes de error o información.
     */
    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    /**
     * Cambia la pantalla (scene) actual por la indicada en rutaFXML.
     * Usado para navegar a la pantalla de líder, admin, mesero, etc.
     * @param rutaFXML Ruta al archivo FXML de la pantalla que quieres cargar.
     */
    private void abrirPantalla(String rutaFXML) {
        try {
            // Carga el nuevo FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource(rutaFXML));
            Parent root = loader.load();

            // Obtiene la ventana (stage) actual y cambia la escena
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo cargar la pantalla.", Alert.AlertType.ERROR);
        }
    }
}
