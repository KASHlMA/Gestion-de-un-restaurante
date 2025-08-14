package com.example.integradora;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

public class UsuariosAdminController {

    @FXML private TableView<Usuario> tablaUsuarios;
    @FXML private TableColumn<Usuario, String> colNombre;
    @FXML private TableColumn<Usuario, String> colUsuario;
    @FXML private TableColumn<Usuario, String> colRol;
    @FXML private TableColumn<Usuario, String> colEstado;
    @FXML private TableColumn<Usuario, Void>   colAcciones;
    @FXML private Button btnAgregarUsuario;

    private final ObservableList<Usuario> datos = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colUsuario.setCellValueFactory(new PropertyValueFactory<>("usuario"));
        colRol.setCellValueFactory(new PropertyValueFactory<>("rol"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        cargarUsuarios();

        // Columna Acciones: Editar / Eliminar (con confirmación)
        colAcciones.setCellFactory(param -> new TableCell<>() {
            private final Button btnEditar   = new Button("Editar");
            private final Button btnEliminar = new Button("Eliminar");
            private final HBox box = new HBox(10, btnEditar, btnEliminar);

            {
                btnEditar.setOnAction(e -> {
                    Usuario u = getTableView().getItems().get(getIndex());
                    abrirEditarUsuario(u);
                });

                btnEliminar.setOnAction(e -> {
                    Usuario u = getTableView().getItems().get(getIndex());
                    // CONFIRMAR antes de eliminar
                    if (confirmar("Eliminar usuario",
                            "¿Seguro que deseas eliminar a \"" + u.getNombre() + "\"?")) {
                        eliminarUsuario(u.getId()); // usa tu lógica existente
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void cargarUsuarios() {
        datos.clear();
        String sql = "SELECT ID, NOMBRE, USUARIO, ROL, ESTADO FROM USUARIOS";
        try (Connection con = Conexion.conectar();
             PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                datos.add(new Usuario(
                        rs.getInt("ID"),
                        rs.getString("NOMBRE"),
                        rs.getString("USUARIO"),
                        rs.getString("ROL"),
                        rs.getString("ESTADO")
                ));
            }
            tablaUsuarios.setItems(datos);
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudieron cargar los usuarios.");
        }
    }

    @FXML
    private void abrirAgregarUsuario() {
        abrirFormularioUsuario(null);
    }

    private void abrirEditarUsuario(Usuario usuario) {
        abrirFormularioUsuario(usuario);
    }

    private void abrirFormularioUsuario(Usuario usuarioEditar) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/integradora/formulario_usuario.fxml"));
            Parent root = loader.load();
            FormularioUsuarioController controller = loader.getController();
            controller.setDatos(usuarioEditar, this::cargarUsuarios); // refresca al guardar

            Stage stage = new Stage();
            stage.setTitle(usuarioEditar == null ? "Agregar Usuario" : "Editar Usuario");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo abrir el formulario de usuario.");
        }
    }

    /** Elimina al usuario sólo si no tiene relaciones. Luego refresca la tabla. */
    private void eliminarUsuario(int usuarioId) {
        if (tieneRelacion(usuarioId)) {
            mostrarAlerta("No se puede eliminar",
                    "Este usuario tiene registros relacionados (órdenes, asignaciones, calificaciones, etc.).");
            return;
        }

        String sql = "DELETE FROM USUARIOS WHERE ID = ?";
        try (Connection con = Conexion.conectar();
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setInt(1, usuarioId);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                mostrarAlerta("Usuario eliminado", "El usuario fue eliminado exitosamente.");
                cargarUsuarios(); // refresca
            }
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo eliminar el usuario.");
        }
    }

    /** Confirm genérico Sí/No. */
    private boolean confirmar(String titulo, String mensaje) {
        ButtonType si = new ButtonType("Sí", ButtonBar.ButtonData.OK_DONE);
        ButtonType no = new ButtonType("No", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, mensaje, si, no);
        alert.setTitle(titulo);
        alert.setHeaderText(null);

        // Dueño del diálogo: la ventana de esta tabla
        Stage owner = (Stage) (tablaUsuarios != null ? tablaUsuarios.getScene().getWindow() : null);
        if (owner != null) alert.initOwner(owner);

        Optional<ButtonType> r = alert.showAndWait();
        return r.isPresent() && r.get() == si;
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, mensaje);
        alert.setTitle(titulo);
        alert.setHeaderText(null);

        Stage owner = (Stage) (tablaUsuarios != null ? tablaUsuarios.getScene().getWindow() : null);
        if (owner != null) alert.initOwner(owner);

        alert.showAndWait();
    }

    // Verificación de relaciones (ajusta tablas/columnas si aplica)
    private boolean tieneRelacion(int usuarioId) {
        String[] tablas   = {"ORDENES", "ASIGNACIONES_MESAS", "CALIFICACIONES", "SOLICITUDES_CAMBIO"};
        String[] columnas = {"MESERO_ID", "MESERO_ID", "MESERO_ID", "MESERO_ID"};

        for (int i = 0; i < tablas.length; i++) {
            String sql = "SELECT 1 FROM " + tablas[i] + " WHERE " + columnas[i] + " = ?";
            try (Connection con = Conexion.conectar();
                 PreparedStatement stmt = con.prepareStatement(sql)) {

                stmt.setInt(1, usuarioId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) return true; // Hay relación
            } catch (Exception e) {
                e.printStackTrace();
                return true; // Por seguridad, no permitimos borrar si hay error
            }
        }
        return false;
    }

    // Modelo
    public static class Usuario {
        private final int id;
        private final String nombre;
        private final String usuario;
        private final String rol;
        private final String estado;

        public Usuario(int id, String nombre, String usuario, String rol, String estado) {
            this.id = id;
            this.nombre = nombre;
            this.usuario = usuario;
            this.rol = rol;
            this.estado = estado;
        }
        public int getId() { return id; }
        public String getNombre() { return nombre; }
        public String getUsuario() { return usuario; }
        public String getRol() { return rol; }
        public String getEstado() { return estado; }
    }
}
