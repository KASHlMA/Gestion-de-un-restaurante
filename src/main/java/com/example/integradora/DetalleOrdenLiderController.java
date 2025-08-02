package com.example.integradora;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.sql.*;

public class DetalleOrdenLiderController {

    @FXML private Label labelInfo;
    @FXML private TableView<FilaDetalle> tablaDetalles;
    @FXML private TableColumn<FilaDetalle, String> colPlatillo;
    @FXML private TableColumn<FilaDetalle, String> colCantidad;
    @FXML private TableColumn<FilaDetalle, String> colPrecio;
    @FXML private TableColumn<FilaDetalle, String> colTotal;
    @FXML private Label labelTotalGeneral;

    private final ObservableList<FilaDetalle> filas = FXCollections.observableArrayList();

    private int idOrden;

    // Llama esto al abrir la ventana:
    public void setDatos(int idOrden, String nombreMesa, String horario) {
        this.idOrden = idOrden;
        labelInfo.setText("Orden: " + nombreMesa + " (" + horario + ")");
        cargarDetalleOrden();
    }

    private void cargarDetalleOrden() {
        filas.clear();
        double totalGeneral = 0.0;
        String sql = "SELECT p.NOMBRE AS platillo, d.CANTIDAD, p.PRECIO " +
                "FROM DETALLE_ORDEN d " +
                "JOIN PLATILLOS p ON d.PLATILLO_ID = p.ID " +
                "WHERE d.ORDEN_ID = ?";
        try (Connection con = Conexion.conectar();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, idOrden);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String nombre = rs.getString("platillo");
                int cantidad = rs.getInt("cantidad");
                double precio = rs.getDouble("precio");
                double total = cantidad * precio;
                totalGeneral += total;
                filas.add(new FilaDetalle(nombre, cantidad, precio, total));
            }
            tablaDetalles.setItems(filas);
            labelTotalGeneral.setText("Total de la cuenta: $" + String.format("%.2f", totalGeneral));
        } catch (Exception e) {
            e.printStackTrace();
            labelTotalGeneral.setText("Error al obtener detalles.");
        }
    }

    @FXML
    private void initialize() {
        colPlatillo.setCellValueFactory(cell -> cell.getValue().platilloProperty());
        colCantidad.setCellValueFactory(cell -> cell.getValue().cantidadProperty());
        colPrecio.setCellValueFactory(cell -> cell.getValue().precioProperty());
        colTotal.setCellValueFactory(cell -> cell.getValue().totalProperty());
    }

    @FXML
    private void cerrarVentana() {
        Stage stage = (Stage) labelInfo.getScene().getWindow();
        stage.close();
    }

    // --- Clase modelo ---
    public static class FilaDetalle {
        private final javafx.beans.property.SimpleStringProperty platillo;
        private final javafx.beans.property.SimpleStringProperty cantidad;
        private final javafx.beans.property.SimpleStringProperty precio;
        private final javafx.beans.property.SimpleStringProperty total;

        public FilaDetalle(String platillo, int cantidad, double precio, double total) {
            this.platillo = new javafx.beans.property.SimpleStringProperty(platillo);
            this.cantidad = new javafx.beans.property.SimpleStringProperty("x" + cantidad);
            this.precio = new javafx.beans.property.SimpleStringProperty("$" + String.format("%.2f", precio));
            this.total = new javafx.beans.property.SimpleStringProperty("$" + String.format("%.2f", total));
        }
        public javafx.beans.property.SimpleStringProperty platilloProperty() { return platillo; }
        public javafx.beans.property.SimpleStringProperty cantidadProperty() { return cantidad; }
        public javafx.beans.property.SimpleStringProperty precioProperty() { return precio; }
        public javafx.beans.property.SimpleStringProperty totalProperty() { return total; }
    }
}
