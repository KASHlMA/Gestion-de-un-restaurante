package com.example.integradora;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class LiderController {

    // Tabla principal (todas)
    @FXML private TableView<AsignacionMesa> tablaMesas;
    @FXML private TableColumn<AsignacionMesa, String> colMesas;
    @FXML private TableColumn<AsignacionMesa, String> colMesero;
    @FXML private TableColumn<AsignacionMesa, String> colHorario;
    @FXML private TableColumn<AsignacionMesa, String> colFecha;     // NUEVA
    @FXML private TableColumn<AsignacionMesa, String> colEstado;
    @FXML private TableColumn<AsignacionMesa, Void>   colDetalle;

    // Contenido central de la vista
    @FXML private VBox contenidoCentral;
    @FXML private Label tituloLabel;

    // Segunda tabla: SOLO HOY
    @FXML private TableView<AsignacionMesa> tablaHoy;
    @FXML private TableColumn<AsignacionMesa, String> colMesasHoy;
    @FXML private TableColumn<AsignacionMesa, String> colMeseroHoy;
    @FXML private TableColumn<AsignacionMesa, String> colHorarioHoy;
    @FXML private TableColumn<AsignacionMesa, String> colFechaHoy;
    @FXML private TableColumn<AsignacionMesa, String> colEstadoHoy;
    @FXML private TableColumn<AsignacionMesa, Void>   colDetalleHoy;

    private final ObservableList<AsignacionMesa> datos    = FXCollections.observableArrayList();
    private final ObservableList<AsignacionMesa> datosHoy = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // ----- Tabla principal -----
        colMesas.setCellValueFactory(new PropertyValueFactory<>("mesas"));
        colMesero.setCellValueFactory(new PropertyValueFactory<>("mesero"));
        colHorario.setCellValueFactory(new PropertyValueFactory<>("horario"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estadoCuenta"));
        colDetalle.setCellFactory(param -> crearCeldaDetalle(tablaMesas));

        // ----- Tabla HOY -----
        if (tablaHoy != null) { // por si el FXML no la incluyó aún
            colMesasHoy.setCellValueFactory(new PropertyValueFactory<>("mesas"));
            colMeseroHoy.setCellValueFactory(new PropertyValueFactory<>("mesero"));
            colHorarioHoy.setCellValueFactory(new PropertyValueFactory<>("horario"));
            colFechaHoy.setCellValueFactory(new PropertyValueFactory<>("fecha"));
            colEstadoHoy.setCellValueFactory(new PropertyValueFactory<>("estadoCuenta"));
            colDetalleHoy.setCellFactory(param -> crearCeldaDetalle(tablaHoy));
        }

        // Cargar datos
        cargarDatosTabla();      // todas
        cargarAsignacionesDeHoy(); // solo hoy
    }

    /** Celda con botón "Ver Detalle" reutilizable para cualquier TableView */
    private TableCell<AsignacionMesa, Void> crearCeldaDetalle(TableView<AsignacionMesa> tabla) {
        return new TableCell<>() {
            private final Button btn = new Button("Ver Detalle");
            {
                btn.getStyleClass().add("boton-accion");
                btn.setOnAction(event -> {
                    AsignacionMesa a = tabla.getItems().get(getIndex());
                    int idOrden = obtenerIdOrdenPorAsignacion(a.getAsignacionId());
                    if (idOrden != -1) {
                        mostrarDetalleOrden(idOrden, a.getMesas(), a.getHorario());
                    } else {
                        mostrarAlerta("SIN ORDEN", "Esta mesa aún no tiene orden registrada.");
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        };
    }

    /** Llena la tabla principal con TODAS las asignaciones (incluye FECHA) */
    private void cargarDatosTabla() {
        datos.clear();
        String sql =
                "SELECT am.ID AS asignacion_id, m.NOMBRE AS mesa, u.NOMBRE AS mesero, " +
                        "       TO_CHAR(am.HORARIO_INICIO, 'HH12:MI AM') || ' a ' || TO_CHAR(am.HORARIO_FIN, 'HH12:MI AM') AS horario, " +
                        "       TO_CHAR(NVL(am.FECHA_ASIGNACION, TRUNC(am.HORARIO_INICIO)), 'DD/MM/YYYY') AS fecha, " +
                        "       (SELECT o.ESTADO FROM ORDENES o " +
                        "         WHERE o.ASIGNACION_ID = am.ID ORDER BY o.FECHA DESC FETCH FIRST 1 ROWS ONLY) AS estado " +
                        "FROM ASIGNACIONES_MESAS am " +
                        "JOIN MESAS m  ON am.MESA_ID = m.ID " +
                        "JOIN USUARIOS u ON am.MESERO_ID = u.ID " +
                        "ORDER BY NVL(am.FECHA_ASIGNACION, TRUNC(am.HORARIO_INICIO)) DESC, m.NOMBRE";

        try (Connection con = Conexion.conectar();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                datos.add(new AsignacionMesa(
                        rs.getInt("asignacion_id"),
                        rs.getString("mesa"),
                        rs.getString("mesero"),
                        rs.getString("horario"),
                        rs.getString("estado") == null ? "Sin Orden" : rs.getString("estado"),
                        rs.getString("fecha")
                ));
            }
            tablaMesas.setItems(datos);
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error al cargar datos", "No se pudieron obtener las asignaciones de mesas.");
        }
    }


    private void cargarAsignacionesDeHoy() {
        datosHoy.clear();
        String sql =
                "SELECT am.ID AS asignacion_id, m.NOMBRE AS mesa, u.NOMBRE AS mesero, " +
                        "       TO_CHAR(am.HORARIO_INICIO, 'HH12:MI AM') || ' a ' || TO_CHAR(am.HORARIO_FIN, 'HH12:MI AM') AS horario, " +
                        "       TO_CHAR(NVL(am.FECHA_ASIGNACION, TRUNC(am.HORARIO_INICIO)), 'DD/MM/YYYY') AS fecha, " +
                        "       (SELECT o.ESTADO FROM ORDENES o " +
                        "         WHERE o.ASIGNACION_ID = am.ID ORDER BY o.FECHA DESC FETCH FIRST 1 ROWS ONLY) AS estado " +
                        "FROM ASIGNACIONES_MESAS am " +
                        "JOIN MESAS m  ON am.MESA_ID = m.ID " +
                        "JOIN USUARIOS u ON am.MESERO_ID = u.ID " +
                        "WHERE NVL(am.FECHA_ASIGNACION, TRUNC(am.HORARIO_INICIO)) = TRUNC(CURRENT_DATE) " +
                        "ORDER BY m.NOMBRE";

        try (Connection con = Conexion.conectar();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                datosHoy.add(new AsignacionMesa(
                        rs.getInt("asignacion_id"),
                        rs.getString("mesa"),
                        rs.getString("mesero"),
                        rs.getString("horario"),
                        rs.getString("estado") == null ? "Sin Orden" : rs.getString("estado"),
                        rs.getString("fecha")
                ));
            }
            tablaHoy.setItems(datosHoy);
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error al cargar datos (Hoy)", "No se pudieron obtener las asignaciones de hoy.");
        }
    }


    // -------- Navegación/menú --------

    @FXML
    private void cerrarSesion(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/integradora/Login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true); // <-- Aquí se maximiza la ventana

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo volver al login.");
        }
    }


    private void setContenidoCentral(String rutaFXML) {
        try {
            Parent nodo = FXMLLoader.load(getClass().getResource(rutaFXML));
            contenidoCentral.getChildren().setAll(nodo);
        } catch (Exception e) {
            e.printStackTrace();
            contenidoCentral.getChildren().setAll(new Label("No se pudo cargar la pantalla."));
        }
    }

    @FXML
    private void mostrarMesas() {
        // refrescar datos
        cargarAsignacionesDeHoy();
        cargarDatosTabla();

        Label subtituloHoy   = new Label("Asignadas Hoy");
        subtituloHoy.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        Label subtituloTodo  = new Label("Lista de asignaciones completa");
        subtituloTodo.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        tituloLabel.setVisible(true);

        // primero HOY, luego TODAS
        contenidoCentral.getChildren().setAll(
                tituloLabel,
                subtituloHoy,
                tablaHoy,
                subtituloTodo,
                tablaMesas
        );
    }


    @FXML
    private void mostrarAsignacion() {
        setContenidoCentral("/com/example/integradora/asignacion.fxml");
    }

    @FXML
    private void mostrarSolicitudes() {
        setContenidoCentral("/com/example/integradora/solicitudes.fxml");
    }

    // -------- Utilidades --------

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR, mensaje);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    @FXML
    private void mostrarDetalleOrden(int idOrden, String nombreMesa, String horario) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/integradora/detalle_orden_lider.fxml"));
            Parent root = loader.load();
            DetalleOrdenLiderController controller = loader.getController();
            controller.setDatos(idOrden, nombreMesa, horario);

            Stage stage = new Stage();
            stage.setTitle("Detalle de la Orden");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int obtenerIdOrdenPorAsignacion(int asignacionId) {
        String sql = "SELECT ID FROM ORDENES " +
                "WHERE ASIGNACION_ID = ? AND (ESTADO = 'ENVIADA' OR ESTADO = 'CERRADA') " +
                "ORDER BY FECHA DESC FETCH FIRST 1 ROWS ONLY";
        try (Connection con = Conexion.conectar();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, asignacionId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt("ID");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    // -------- Modelo --------
    public static class AsignacionMesa {
        private final int asignacionId;
        private final String mesas;
        private final String mesero;
        private final String horario;
        private final String fecha;         // NUEVO
        private final String estadoCuenta;

        public AsignacionMesa(int asignacionId, String mesas, String mesero,
                              String horario, String fecha, String estadoCuenta) {
            this.asignacionId = asignacionId;
            this.mesas = mesas;
            this.mesero = mesero;
            this.horario = horario;
            this.fecha = fecha;
            this.estadoCuenta = estadoCuenta;
        }

        public int getAsignacionId() { return asignacionId; }
        public String getMesas() { return mesas; }
        public String getMesero() { return mesero; }
        public String getHorario() { return horario; }
        public String getFecha() { return fecha; }
        public String getEstadoCuenta() { return estadoCuenta; }
    }
}
