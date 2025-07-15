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

        // Consulta SQL para buscar usuario por nombre, contraseña y obtener su ROL y ESTADO
        String sql = "SELECT ROL, ESTADO FROM USUARIOS WHERE USUARIO = ? AND CONTRASENA = ?";
        try (Connection con = Conexion.conectar();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, usuario); // Asigna el nombre de usuario al primer ?
            stmt.setString(2, contrasena); // Asigna la contraseña al segundo ?

            // Ejecuta la consulta y obtiene los resultados
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) { // Si encontró un usuario
                String rol = rs.getString("ROL");
                String estado = rs.getString("ESTADO");

                // Verifica si el usuario está activo
                if (!"ACTIVO".equalsIgnoreCase(estado)) {
                    mostrarAlerta("Usuario inactivo", "Este usuario está inactivo.", Alert.AlertType.ERROR);
                    return; // Termina el método si no está activo
                }

                // Según el ROL, navega a la pantalla correspondiente
                switch (rol.toUpperCase()) {
                    case "ADMIN":
                        // Aquí puedes poner abrirPantalla("/com/example/integradora/admin.fxml");
                        mostrarAlerta("Bienvenido", "¡Acceso como ADMIN!", Alert.AlertType.INFORMATION);
                        break;
                    case "LIDER_MESEROS":
                    case "LIDER DE MESEROS":
                        // Navega a la pantalla de líder de mesero
                        abrirPantalla("/com/example/integradora/lider.fxml");
                        break;
                    case "MESERO":
                        // Aquí puedes poner abrirPantalla("/com/example/integradora/mesero.fxml");
                        mostrarAlerta("Bienvenido", "¡Acceso como MESERO!", Alert.AlertType.INFORMATION);
                        break;
                    default:
                        // Rol no reconocido
                        mostrarAlerta("Rol desconocido", "El rol no es válido.", Alert.AlertType.ERROR);
                }
            } else {
                // Si no encuentra usuario, muestra mensaje de error
                mostrarAlerta("Error de login", "Usuario o contraseña incorrectos.", Alert.AlertType.ERROR);
            }
        } catch (Exception e) {
            // Muestra un error si hay una excepción (por ejemplo, de conexión)
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
