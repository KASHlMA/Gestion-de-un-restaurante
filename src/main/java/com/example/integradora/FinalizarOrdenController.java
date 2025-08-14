package com.example.integradora;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;

import java.sql.*;

public class FinalizarOrdenController {

    @FXML private Label labelNombreMesero;
    @FXML private Label labelRuta;
    @FXML private TableView<ResumenCuenta> tablaResumen;
    @FXML private TableColumn<ResumenCuenta, String> colPlatillo;
    @FXML private TableColumn<ResumenCuenta, String> colCantidad;
    @FXML private Label labelTotal;
    @FXML private Button btnFinalizar;

    private int idOrden;
    private int idMesero;
    private String nombreMesero;
    private String ruta;

    private final ObservableList<ResumenCuenta> resumen = FXCollections.observableArrayList();

    /** Llamar justo después de cargar el FXML */
    public void setDatosOrden(int idOrden, int idMesero, String nombreMesero, String ruta) {
        this.idOrden = idOrden;
        this.idMesero = idMesero;
        this.nombreMesero = nombreMesero;
        this.ruta = ruta;

        if (labelNombreMesero != null) labelNombreMesero.setText(nombreMesero);
        if (labelRuta != null) labelRuta.setText(ruta);

        cargarResumen();
    }

    @FXML
    public void initialize() {
        if (colPlatillo != null) colPlatillo.setCellValueFactory(c -> c.getValue().platilloProperty());
        if (colCantidad != null) colCantidad.setCellValueFactory(c -> c.getValue().cantidadProperty());
        if (tablaResumen != null) tablaResumen.setItems(resumen);
    }

    private void cargarResumen() {
        resumen.clear();
        double total = 0.0;
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
                resumen.add(new ResumenCuenta(nombre, cantidad));
                total += cantidad * precio;
            }
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo cargar el resumen de la orden.");
        }
        if (labelTotal != null) labelTotal.setText(String.format("$%.2f", total));
    }

    // --------------- Botones ---------------

    /** Cerrar definitivamente: marca CERRADA y pasa a calificar */
    @FXML
    private void finalizar() {
        try (Connection con = Conexion.conectar()) {
            String sqlCerrar = "UPDATE ORDENES SET ESTADO = 'CERRADA' WHERE ID = ?";
            try (PreparedStatement stmt = con.prepareStatement(sqlCerrar)) {
                stmt.setInt(1, idOrden);
                stmt.executeUpdate();
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/integradora/calificar_mesero.fxml"));
            Parent root = loader.load();

            CalificarMeseroController controller = loader.getController();
            controller.setDatos(idMesero, idOrden);

            Stage stage = (Stage) btnFinalizar.getScene().getWindow();
            stage.getScene().setRoot(root);            stage.setMaximized(true);

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo finalizar la orden.");
        }
    }

    /** Volver al menú del mesero SIN cerrar la orden */
    @FXML
    private void volver() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/integradora/mesero.fxml"));
            Parent root = loader.load();

            MeseroController meseroController = loader.getController();
            meseroController.setIdMesero(idMesero, nombreMesero);

            Stage stage = (Stage) ((Node)btnFinalizar).getScene().getWindow();
            stage.getScene().setRoot(root);            stage.setMaximized(true);

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo volver al menú del mesero.");
        }
    }

    @FXML
    private void cerrarSesion(javafx.event.ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/integradora/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);            stage.setMaximized(true);
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo cerrar la sesión.");
        }
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, mensaje);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    // Modelo tabla
    public static class ResumenCuenta {
        private final SimpleStringProperty platillo;
        private final SimpleStringProperty cantidad;

        public ResumenCuenta(String platillo, int cantidad) {
            this.platillo = new SimpleStringProperty(platillo);
            this.cantidad = new SimpleStringProperty("x" + cantidad);
        }
        public String getPlatillo() { return platillo.get(); }
        public SimpleStringProperty platilloProperty() { return platillo; }
        public String getCantidad() { return cantidad.get(); }
        public SimpleStringProperty cantidadProperty() { return cantidad; }
    }
}
