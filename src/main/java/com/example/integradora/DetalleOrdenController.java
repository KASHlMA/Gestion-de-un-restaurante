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

public class DetalleOrdenController {

    @FXML private Label labelNombreMesero;
    @FXML private Label labelRuta;
    @FXML private TableView<DetalleOrden> tablaDetalle;
    @FXML private TableColumn<DetalleOrden, String> colPlatillo;
    @FXML private TableColumn<DetalleOrden, String> colCantidad;
    @FXML private Button btnModificar;
    @FXML private Button btnEnviarCocina;

    private int idOrden;
    private int idMesero;
    private String nombreMesero;

    private ObservableList<DetalleOrden> detalles = FXCollections.observableArrayList();

    /**
     * Llama esto justo después de cargar el FXML, así:
     *  controller.setDatosOrden(idOrden, idMesero, nombreMesero, ruta);
     */
    public void setDatosOrden(int idOrden, int idMesero, String nombreMesero, String ruta) {
        this.idOrden = idOrden;
        this.idMesero = idMesero;
        this.nombreMesero = nombreMesero;
        labelNombreMesero.setText(nombreMesero);
        labelRuta.setText(ruta);
        cargarDetalleOrden();
        actualizarEstadoBotones();
    }

    /**
     * Inicializa las columnas y la tabla
     */
    @FXML
    public void initialize() {
        colPlatillo.setCellValueFactory(cell -> cell.getValue().platilloProperty());
        colCantidad.setCellValueFactory(cell -> cell.getValue().cantidadProperty());
        tablaDetalle.setItems(detalles);
    }

    /**
     * Consulta los detalles de la orden en la BD y los muestra en la tabla.
     */
    private void cargarDetalleOrden() {
        detalles.clear();
        String sql = "SELECT p.NOMBRE AS platillo, d.CANTIDAD " +
                "FROM DETALLE_ORDEN d JOIN PLATILLOS p ON d.PLATILLO_ID = p.ID " +
                "WHERE d.ORDEN_ID = ?";
        try (Connection con = Conexion.conectar();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, idOrden);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                detalles.add(new DetalleOrden(rs.getString("platillo"), rs.getInt("cantidad")));
            }
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudieron cargar los detalles de la orden.");
        }
    }

    /**
     * Actualiza los botones dependiendo del estado de la orden
     */
    private void actualizarEstadoBotones() {
        String estado = obtenerEstadoOrden();
        if ("ENVIADA".equalsIgnoreCase(estado)) {
            btnModificar.setVisible(false);
            btnEnviarCocina.setVisible(false);
        } else {
            btnModificar.setVisible(true);
            btnEnviarCocina.setVisible(true);
        }
    }


    /**
     * Consulta el estado de la orden (ABIERTA, ENVIADA, CERRADA...)
     */
    private String obtenerEstadoOrden() {
        String estado = "";
        String sql = "SELECT ESTADO FROM ORDENES WHERE ID = ?";
        try (Connection con = Conexion.conectar();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, idOrden);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) estado = rs.getString("ESTADO");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return estado;
    }

    /**
     * Botón para modificar la orden (redirige a la pantalla de edición)
     */
    @FXML
    private void modificarOrden() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/integradora/orden.fxml"));
            Parent root = loader.load();
            OrdenController ordenController = loader.getController();
            int asignacionId = obtenerAsignacionIdPorOrden(idOrden);
            ordenController.setDatosMesa(
                    idMesero,
                    nombreMesero,
                    obtenerNombreMesaPorOrden(idOrden),
                    obtenerHorarioPorOrden(idOrden),
                    asignacionId
            );
            Stage stage = (Stage) tablaDetalle.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo abrir la edición de la orden.");
        }
    }

    private int obtenerAsignacionIdPorOrden(int idOrden) throws Exception {
        String sql = "SELECT ASIGNACION_ID FROM ORDENES WHERE ID = ?";
        try (Connection con = Conexion.conectar();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, idOrden);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt("ASIGNACION_ID");
            else throw new Exception("No se encontró la asignación para la orden: " + idOrden);
        }
    }


    /**
     * Botón para enviar la orden a cocina (cambia el estado y deshabilita edición)
     */
    @FXML
    private void enviarCocina() {
        String sql = "UPDATE ORDENES SET ESTADO='ENVIADA' WHERE ID = ?";
        try (Connection con = Conexion.conectar();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, idOrden);
            stmt.executeUpdate();
            mostrarAlerta("¡Listo!", "La orden se ha enviado a cocina.");

            // --- REGRESAR A MENÚ DE MESAS ASIGNADAS DEL MESERO ---
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/integradora/mesero.fxml"));
            Parent root = loader.load();
            MeseroController meseroController = loader.getController();
            meseroController.setIdMesero(idMesero, nombreMesero);
            Stage stage = (Stage) tablaDetalle.getScene().getWindow();
            stage.getScene().setRoot(root);
            // No hace falta llamar a actualizarEstadoBotones() aquí, ya que sales de esta pantalla.
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo enviar a cocina.");
        }
    }


    /**
     * Botón para solicitar cambios (envía notificación al líder de meseros)
     */

    /**
     * Cierra la sesión y vuelve al login.
     */
    @FXML
    private void cerrarSesion(javafx.event.ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/integradora/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo cerrar la sesión.");
        }
    }

    // Métodos auxiliares para obtener datos de la orden
    private String obtenerNombreMesaPorOrden(int idOrden) {
        String nombre = "";
        String sql = "SELECT m.NOMBRE FROM ORDENES o JOIN MESAS m ON o.MESA_ID = m.ID WHERE o.ID = ?";
        try (Connection con = Conexion.conectar();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, idOrden);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) nombre = rs.getString("NOMBRE");
        } catch (Exception e) { e.printStackTrace(); }
        return nombre;
    }
    private String obtenerHorarioPorOrden(int idOrden) {
        String horario = "";
        String sql = "SELECT TO_CHAR(o.FECHA, 'HH12:MI AM') AS HORA FROM ORDENES o WHERE o.ID = ?";
        try (Connection con = Conexion.conectar();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, idOrden);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) horario = rs.getString("HORA");
        } catch (Exception e) { e.printStackTrace(); }
        return horario;
    }

    // Modelo para la tabla
    public static class DetalleOrden {
        private final SimpleStringProperty platillo;
        private final SimpleStringProperty cantidad;
        public DetalleOrden(String platillo, int cantidad) {
            this.platillo = new SimpleStringProperty(platillo);
            this.cantidad = new SimpleStringProperty("x" + cantidad);
        }
        public String getPlatillo() { return platillo.get(); }
        public SimpleStringProperty platilloProperty() { return platillo; }
        public String getCantidad() { return cantidad.get(); }
        public SimpleStringProperty cantidadProperty() { return cantidad; }
    }

    // Utilidad: muestra alerta informativa
    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, mensaje);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

}
