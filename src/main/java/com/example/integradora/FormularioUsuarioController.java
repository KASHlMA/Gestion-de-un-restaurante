package com.example.integradora;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class FormularioUsuarioController {

    @FXML private Label tituloLabel;
    @FXML private TextField txtNombre;
    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtContrasena;
    @FXML private ComboBox<String> comboRol;
    @FXML private ComboBox<String> comboEstado;

    private UsuariosAdminController.Usuario usuarioEditar;
    private Runnable callbackRefrescar;

    @FXML
    public void initialize() {
        comboRol.getItems().setAll("ADMIN", "LIDER", "MESERO");
        comboEstado.getItems().setAll("ACTIVO", "INACTIVO");
    }

    // Para editar o agregar (desde UsuariosAdminController)
    public void setDatos(UsuariosAdminController.Usuario usuario, Runnable refrescar) {
        this.usuarioEditar = usuario;
        this.callbackRefrescar = refrescar;

        if (usuario != null) {
            tituloLabel.setText("Editar Usuario");
            txtNombre.setText(usuario.getNombre());
            txtUsuario.setText(usuario.getUsuario());
            comboRol.setValue(usuario.getRol());
            comboEstado.setValue(usuario.getEstado());
            txtContrasena.setPromptText("Cambiar contraseña (opcional)");
        } else {
            tituloLabel.setText("Añadir Usuario");
            comboRol.setValue("MESERO");
            comboEstado.setValue("ACTIVO");
        }
    }

    @FXML
    private void guardar() {
        String nombre = txtNombre.getText().trim();
        String usuario = txtUsuario.getText().trim();
        String contrasena = txtContrasena.getText();
        String rol = comboRol.getValue();
        String estado = comboEstado.getValue();

        if (nombre.isEmpty() || usuario.isEmpty() || rol == null || estado == null ||
                (usuarioEditar == null && contrasena.isEmpty())) {
            mostrarAlerta("Campos incompletos", "Completa todos los campos.");
            return;
        }

        try (Connection con = Conexion.conectar()) {
            if (usuarioEditar == null) {
                // Validar que usuario sea único
                String check = "SELECT 1 FROM USUARIOS WHERE USUARIO = ?";
                try (PreparedStatement stmt = con.prepareStatement(check)) {
                    stmt.setString(1, usuario);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        mostrarAlerta("Usuario existente", "Ese nombre de usuario ya está registrado.");
                        return;
                    }
                }
                // Insertar nuevo usuario
                String sql = "INSERT INTO USUARIOS (NOMBRE, USUARIO, CONTRASENA, ROL, ESTADO) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement stmt = con.prepareStatement(sql)) {
                    stmt.setString(1, nombre);
                    stmt.setString(2, usuario);
                    stmt.setString(3, contrasena);
                    stmt.setString(4, rol);
                    stmt.setString(5, estado);
                    stmt.executeUpdate();
                }
            } else {
                // Editar usuario (opcional: cambia contraseña solo si se escribe)
                String sql = contrasena.isEmpty()
                        ? "UPDATE USUARIOS SET NOMBRE=?, USUARIO=?, ROL=?, ESTADO=? WHERE ID=?"
                        : "UPDATE USUARIOS SET NOMBRE=?, USUARIO=?, CONTRASENA=?, ROL=?, ESTADO=? WHERE ID=?";
                try (PreparedStatement stmt = con.prepareStatement(sql)) {
                    stmt.setString(1, nombre);
                    stmt.setString(2, usuario);
                    int idx = 3;
                    if (!contrasena.isEmpty()) {
                        stmt.setString(3, contrasena);
                        stmt.setString(4, rol);
                        stmt.setString(5, estado);
                        stmt.setInt(6, usuarioEditar.getId());
                    } else {
                        stmt.setString(3, rol);
                        stmt.setString(4, estado);
                        stmt.setInt(5, usuarioEditar.getId());
                    }
                    stmt.executeUpdate();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo guardar el usuario.");
            return;
        }
        cerrar();
        if (callbackRefrescar != null) callbackRefrescar.run();
    }

    @FXML
    private void cancelar() {
        cerrar();
    }

    private void cerrar() {
        Stage stage = (Stage) txtNombre.getScene().getWindow();
        stage.close();
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, mensaje);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
