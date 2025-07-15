package com.example.integradora;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class LiderController {

    @FXML private TableView<AsignacionMesa> tablaMesas;
    @FXML private TableColumn<AsignacionMesa, String> colMesas;
    @FXML private TableColumn<AsignacionMesa, String> colMesero;
    @FXML private TableColumn<AsignacionMesa, String> colHorario;
    @FXML private VBox contenidoCentral;

    private ObservableList<AsignacionMesa> datos = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Asocia columnas con las propiedades de la clase AsignacionMesa
        colMesas.setCellValueFactory(new PropertyValueFactory<>("mesas"));
        colMesero.setCellValueFactory(new PropertyValueFactory<>("mesero"));
        colHorario.setCellValueFactory(new PropertyValueFactory<>("horario"));

        cargarDatosTabla();
    }

    /**
     * Llena la tabla con datos de la base de datos.
     */
    private void cargarDatosTabla() {
        datos.clear();
        String sql = "SELECT m.NOMBRE AS mesa, u.NOMBRE AS mesero, " +
                "TO_CHAR(am.HORARIO_INICIO, 'HH12:MI AM') || ' a ' || TO_CHAR(am.HORARIO_FIN, 'HH12:MI AM') AS horario " +
                "FROM ASIGNACIONES_MESAS am " +
                "JOIN MESAS m ON am.MESA_ID = m.ID " +
                "JOIN USUARIOS u ON am.MESERO_ID = u.ID";

        try (Connection con = Conexion.conectar();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String mesas = rs.getString("mesa");
                String mesero = rs.getString("mesero");
                String horario = rs.getString("horario");
                datos.add(new AsignacionMesa(mesas, mesero, horario));
            }
            tablaMesas.setItems(datos);
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error al cargar datos", "No se pudieron obtener las asignaciones de mesas.");
        }
    }

    /**
     * Cierra sesión y regresa al login.
     */
    @FXML
    private void cerrarSesion(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/integradora/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo volver al login.");
        }
    }

    /**
     * Cambia el contenido central por el FXML indicado.
     */
    private void setContenidoCentral(String rutaFXML) {
        try {
            Parent nodo = FXMLLoader.load(getClass().getResource(rutaFXML));
            contenidoCentral.getChildren().setAll(nodo);
        } catch (Exception e) {
            e.printStackTrace();
            contenidoCentral.getChildren().setAll(new Label("No se pudo cargar la pantalla."));
        }
    }

    /**
     * Botón menú: mostrar la tabla de mesas (contenido inicial)
     */
    @FXML
    private void mostrarMesas() {
        // Puedes reconstruir el VBox inicial aquí, si necesitas limpiar la pantalla central
        contenidoCentral.getChildren().setAll(
                new Label("¡Bienvenido!"), // Si tienes un label de bienvenida aparte
                tablaMesas
        );
        // O vuelve a cargar los datos si lo deseas:
        // cargarDatosTabla();
    }

    /**
     * Botón menú: cargar la pantalla de Asignación de Plan
     */
    @FXML
    private void mostrarAsignacion() {
        setContenidoCentral("/com/example/integradora/asignacion.fxml");
    }

    /**
     * Botón menú: cargar la pantalla de Solicitudes de Cambios
     */
    @FXML
    private void mostrarSolicitudes() {
        setContenidoCentral("/com/example/integradora/solicitudes.fxml");
    }

    /**
     * Muestra una alerta de error.
     */
    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR, mensaje);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    // Clase interna solo para el modelo de la tabla (NO pongas métodos de controller aquí)
    public static class AsignacionMesa {
        private final String mesas;
        private final String mesero;
        private final String horario;

        public AsignacionMesa(String mesas, String mesero, String horario) {
            this.mesas = mesas;
            this.mesero = mesero;
            this.horario = horario;
        }
        public String getMesas() { return mesas; }
        public String getMesero() { return mesero; }
        public String getHorario() { return horario; }
    }
}
