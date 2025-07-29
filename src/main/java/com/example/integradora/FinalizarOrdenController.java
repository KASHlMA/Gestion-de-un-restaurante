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
    private String nombreMesero;
    private String ruta;
    private int idMesero;


    private ObservableList<ResumenCuenta> resumen = FXCollections.observableArrayList();

    public void setDatosOrden(int idOrden, int idMesero, String nombreMesero, String ruta) {
        this.idOrden = idOrden;
        this.idMesero = idMesero; // <-- GUÁRDALO AQUÍ
        this.nombreMesero = nombreMesero;
        this.ruta = ruta;
        labelNombreMesero.setText(nombreMesero);
        labelRuta.setText(ruta);
        cargarResumen();
    }



    @FXML
    public void initialize() {
        colPlatillo.setCellValueFactory(cell -> cell.getValue().platilloProperty());
        colCantidad.setCellValueFactory(cell -> cell.getValue().cantidadProperty());
        tablaResumen.setItems(resumen);
    }

    // Cargar platillos, cantidad y precios para calcular total
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
        labelTotal.setText(String.format("$%.2f", total));
    }

    @FXML
    private void finalizar() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/integradora/calificar_mesero.fxml"));
            Parent root = loader.load();

            // Pasa los datos al controller de calificación
            CalificarMeseroController controller = loader.getController();
            controller.setDatos(idMesero, idOrden);

            Stage stage = (Stage) btnFinalizar.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo mostrar la pantalla de calificación.");
        }
    }


    @FXML
    private void cerrarSesion(javafx.event.ActionEvent event) {
        finalizar(); // Puedes reutilizar
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, mensaje);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    // Modelo para la tabla
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
