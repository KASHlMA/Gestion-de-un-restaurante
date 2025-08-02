package com.example.integradora;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class LiderController {

    @FXML private TableView<AsignacionMesa> tablaMesas;
    @FXML private TableColumn<AsignacionMesa, String> colMesas;
    @FXML private TableColumn<AsignacionMesa, String> colMesero;
    @FXML private TableColumn<AsignacionMesa, String> colHorario;
    @FXML private VBox contenidoCentral;
    @FXML private TableColumn<AsignacionMesa, String> colEstado;
    @FXML private TableColumn<AsignacionMesa, Void> colDetalle;
    @FXML private Label tituloLabel;



    private ObservableList<AsignacionMesa> datos = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colMesas.setCellValueFactory(new PropertyValueFactory<>("mesas"));
        colMesero.setCellValueFactory(new PropertyValueFactory<>("mesero"));
        colHorario.setCellValueFactory(new PropertyValueFactory<>("horario"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estadoCuenta"));

        // Botón Detalle
        colDetalle.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("Ver Detalle");
            {
                btn.setOnAction(event -> {
                    AsignacionMesa asignacion = getTableView().getItems().get(getIndex());
                    int idOrden = obtenerIdOrdenPorAsignacion(asignacion.getAsignacionId());
                    if (idOrden != -1) {
                        mostrarDetalleOrden(idOrden, asignacion.getMesas(), asignacion.getHorario());
                    } else {
                        mostrarAlerta("Sin Orden", "Esta mesa aún no tiene orden registrada.");
                    }
                });


            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        cargarDatosTabla();
    }


    /**
     * Llena la tabla con datos de la base de datos.
     */
    private void cargarDatosTabla() {
        datos.clear();
        String sql = "SELECT am.ID AS asignacion_id, m.NOMBRE AS mesa, u.NOMBRE AS mesero, " +
                "TO_CHAR(am.HORARIO_INICIO, 'HH12:MI AM') || ' a ' || TO_CHAR(am.HORARIO_FIN, 'HH12:MI AM') AS horario, " +
                "(SELECT o.ESTADO FROM ORDENES o WHERE o.ASIGNACION_ID = am.ID ORDER BY o.FECHA DESC FETCH FIRST 1 ROWS ONLY) AS estado " +
                "FROM ASIGNACIONES_MESAS am " +
                "JOIN MESAS m ON am.MESA_ID = m.ID " +
                "JOIN USUARIOS u ON am.MESERO_ID = u.ID";

        try (Connection con = Conexion.conectar();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int asignacionId = rs.getInt("asignacion_id");
                String mesas = rs.getString("mesa");
                String mesero = rs.getString("mesero");
                String horario = rs.getString("horario");
                String estado = rs.getString("estado");
                if (estado == null) estado = "Sin Orden";
                datos.add(new AsignacionMesa(asignacionId, mesas, mesero, horario, estado));
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

        cargarDatosTabla();
        tituloLabel.setVisible(true);
        contenidoCentral.getChildren().setAll(
                tituloLabel,
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
        private final int asignacionId;        // <--- NUEVO
        private final String mesas;
        private final String mesero;
        private final String horario;
        private final String estadoCuenta;     // <--- NUEVO

        // Constructor actualizado
        public AsignacionMesa(int asignacionId, String mesas, String mesero, String horario, String estadoCuenta) {
            this.asignacionId = asignacionId;
            this.mesas = mesas;
            this.mesero = mesero;
            this.horario = horario;
            this.estadoCuenta = estadoCuenta;
        }
        public int getAsignacionId() { return asignacionId; }
        public String getMesas() { return mesas; }
        public String getMesero() { return mesero; }
        public String getHorario() { return horario; }
        public String getEstadoCuenta() { return estadoCuenta; }
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
            // Opcional: alerta de error
        }
    }

    // Devuelve el ID de la orden (ENVIADA o CERRADA) de la asignación, o -1 si no hay
    private int obtenerIdOrdenPorAsignacion(int asignacionId) {
        String sql = "SELECT ID FROM ORDENES WHERE ASIGNACION_ID = ? AND (ESTADO = 'ENVIADA' OR ESTADO = 'CERRADA') ORDER BY FECHA DESC FETCH FIRST 1 ROWS ONLY";
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




}
