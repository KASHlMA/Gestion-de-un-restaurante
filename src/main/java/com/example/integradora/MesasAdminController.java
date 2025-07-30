package com.example.integradora;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class MesasAdminController {

    @FXML private TableView<Mesa> tablaMesas;
    @FXML private TableColumn<Mesa, String> colNombreMesa;
    @FXML private TableColumn<Mesa, String> colEstado;
    @FXML private TableColumn<Mesa, Void> colAcciones;
    @FXML private Button btnAnadirMesa;

    private ObservableList<Mesa> datos = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colNombreMesa.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        cargarMesas();

        // Botón editar/eliminar en cada fila
        colAcciones.setCellFactory(param -> new TableCell<>() {
            private final Button btnEditar = new Button("✏️");
            private final Button btnEliminar = new Button("🗑");
            private final HBox box = new HBox(10, btnEditar, btnEliminar);
            {
                btnEditar.setOnAction(e -> abrirEditarMesa(getTableView().getItems().get(getIndex())));
                btnEliminar.setOnAction(e -> eliminarMesa(getTableView().getItems().get(getIndex()).getId()));
                btnEditar.setStyle("-fx-background-color: #D0E9F7; -fx-border-radius: 6; -fx-cursor: hand;");
                btnEliminar.setStyle("-fx-background-color: #FFD6D6; -fx-border-radius: 6; -fx-cursor: hand;");
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

    }

    private void cargarMesas() {
        datos.clear();
        String sql = "SELECT ID, NOMBRE, ESTADO FROM MESAS";
        try (Connection con = Conexion.conectar();
             PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                datos.add(new Mesa(rs.getInt("ID"), rs.getString("NOMBRE"), rs.getString("ESTADO")));
            }
            tablaMesas.setItems(datos);
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudieron cargar las mesas.");
        }
    }

    @FXML
    private void abrirAgregarMesa() {
        abrirFormularioMesa(null);
    }

    private void abrirEditarMesa(Mesa mesa) {
        abrirFormularioMesa(mesa);
    }

    // Abre el formulario para añadir o editar mesa
    private void abrirFormularioMesa(Mesa mesaEditar) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/integradora/formulario_mesa.fxml"));
            Parent root = loader.load();
            FormularioMesaController controller = loader.getController();
            controller.setDatos(mesaEditar, this::cargarMesas); // refresca al guardar
            Stage stage = new Stage();
            stage.setTitle(mesaEditar == null ? "Añadir Mesa" : "Editar Mesa");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo abrir el formulario.");
        }
    }

    private void eliminarMesa(int mesaId) {
        // Verifica si hay relaciones
        if (tieneRelacion(mesaId)) {
            mostrarAlerta("No puedes eliminar la mesa", "Esta mesa tiene registros relacionados (órdenes o asignaciones).");
            return;
        }

        String sql = "DELETE FROM MESAS WHERE ID = ?";
        try (Connection con = Conexion.conectar();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, mesaId);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                mostrarAlerta("Mesa eliminada", "La mesa fue eliminada exitosamente.");
                cargarMesas(); // refresca la tabla
            }
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo eliminar la mesa.");
        }
    }


    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, mensaje);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    // Modelo Mesa
    public static class Mesa {
        private final int id;
        private final String nombre;
        private final String estado;
        public Mesa(int id, String nombre, String estado) {
            this.id = id;
            this.nombre = nombre;
            this.estado = estado;
        }
        public int getId() { return id; }
        public String getNombre() { return nombre; }
        public String getEstado() { return estado; }
    }

    private boolean tieneRelacion(int mesaId) {
        String[] tablas = {"ORDENES", "ASIGNACIONES_MESAS"};
        for (String tabla : tablas) {
            String sql = "SELECT 1 FROM " + tabla + " WHERE MESA_ID = ?";
            try (Connection con = Conexion.conectar();
                 PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setInt(1, mesaId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return true; // Tiene relación
                }
            } catch (Exception e) {
                e.printStackTrace();
                // Por seguridad, si hay error asumimos que tiene relación y no borramos
                return true;
            }
        }
        return false; // No hay relación
    }

}
