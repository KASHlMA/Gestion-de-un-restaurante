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
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class MeseroController {

    @FXML private Label labelNombreMesero;

    // ===== TABLA: HOY =====
    @FXML private TableView<MesaAsignada> tablaHoyMesero;
    @FXML private TableColumn<MesaAsignada, String> colMesaHoy;
    @FXML private TableColumn<MesaAsignada, String> colHorarioHoy;
    @FXML private TableColumn<MesaAsignada, String> colFechaHoy;
    @FXML private TableColumn<MesaAsignada, Void>   colTomarOrdenHoy;
    @FXML private TableColumn<MesaAsignada, Void>   colCambiosHoy;
    @FXML private TableColumn<MesaAsignada, Void>   colCerrarHoy;

    // ===== TABLA: PASADAS =====
    @FXML private TableView<MesaAsignada> tablaPasadasMesero;
    @FXML private TableColumn<MesaAsignada, String> colMesaPas;
    @FXML private TableColumn<MesaAsignada, String> colHorarioPas;
    @FXML private TableColumn<MesaAsignada, String> colFechaPas;
    @FXML private TableColumn<MesaAsignada, Void>   colTomarOrdenPas;
    @FXML private TableColumn<MesaAsignada, Void>   colCambiosPas;
    @FXML private TableColumn<MesaAsignada, Void>   colCerrarPas;

    private int idMesero;
    private final ObservableList<MesaAsignada> datosHoy     = FXCollections.observableArrayList();
    private final ObservableList<MesaAsignada> datosPasadas = FXCollections.observableArrayList();

    // Se llama después del login
    public void setIdMesero(int id, String nombre) {
        this.idMesero = id;
        labelNombreMesero.setText(nombre);
        configurarTablas();
        cargarAsignacionesHoy();
        cargarAsignacionesPasadas();
    }

    @FXML
    public void initialize() {
        // Puede llamarse antes que setIdMesero; es idempotente
        configurarTablas();
    }

    private void configurarTablas() {
        // ---- HOY ----
        if (colMesaHoy != null) {
            colMesaHoy.setCellValueFactory(new PropertyValueFactory<>("mesa"));
            colHorarioHoy.setCellValueFactory(new PropertyValueFactory<>("horario"));
            colFechaHoy.setCellValueFactory(new PropertyValueFactory<>("fecha"));

            configurarBoton(colTomarOrdenHoy, "Tomar Orden",
                    this::tomarOrden,
                    a -> !(a.ordenEstado != null && !a.ordenEstado.equals("ABIERTA")));

            configurarBoton(colCambiosHoy, "Realizar Cambios",
                    this::solicitarCambio,
                    a -> !(a.ordenEstado == null || !a.ordenEstado.equals("ENVIADA")));

            configurarBoton(colCerrarHoy, "Cerrar Cuenta",
                    this::cerrarCuenta,
                    a -> !(a.ordenEstado == null || !a.ordenEstado.equals("ENVIADA")));
        }

        // ---- PASADAS ---- (sin acciones)
        if (colMesaPas != null) {
            colMesaPas.setCellValueFactory(new PropertyValueFactory<>("mesa"));
            colHorarioPas.setCellValueFactory(new PropertyValueFactory<>("horario"));
            colFechaPas.setCellValueFactory(new PropertyValueFactory<>("fecha"));

            configurarBoton(colTomarOrdenPas, "Tomar Orden", this::tomarOrden, a -> false);
            configurarBoton(colCambiosPas, "Realizar Cambios", this::solicitarCambio, a -> false);
            configurarBoton(colCerrarPas, "Cerrar Cuenta", this::cerrarCuenta, a -> false);
        }
    }

    /** Fabrica una columna de botones con regla para habilitar/deshabilitar */
    private void configurarBoton(TableColumn<MesaAsignada, Void> col, String texto,
                                 Consumer<MesaAsignada> handler,
                                 Predicate<MesaAsignada> enablePredicate) {
        if (col == null) return;
        col.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button(texto);
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                MesaAsignada a = getTableView().getItems().get(getIndex());
                btn.setDisable(!enablePredicate.test(a));
                btn.setOnAction(e -> handler.accept(a));
                setGraphic(btn);
            }
        });
    }

    private void cargarAsignacionesHoy() {
        datosHoy.clear();
        String sql =
                "SELECT am.ID AS asignacion_id, m.NOMBRE AS mesa, " +
                        "       TO_CHAR(am.HORARIO_INICIO, 'HH12:MI AM') || ' a ' || TO_CHAR(am.HORARIO_FIN, 'HH12:MI AM') AS horario, " +
                        "       TO_CHAR(NVL(am.FECHA_ASIGNACION, TRUNC(am.HORARIO_INICIO)), 'DD/MM/YYYY') AS fecha, " +
                        "       (SELECT o.ESTADO FROM ORDENES o WHERE o.ASIGNACION_ID = am.ID " +
                        "         ORDER BY o.FECHA DESC FETCH FIRST 1 ROWS ONLY) AS ORDEN_ESTADO " +
                        "FROM ASIGNACIONES_MESAS am " +
                        "JOIN MESAS m ON am.MESA_ID = m.ID " +
                        "WHERE am.MESERO_ID = ? " +
                        "  AND m.ESTADO = 'ACTIVO' " +
                        "  AND NVL(am.FECHA_ASIGNACION, TRUNC(am.HORARIO_INICIO)) = TRUNC(CURRENT_DATE) " +
                        "ORDER BY m.NOMBRE";
        try (Connection con = Conexion.conectar();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, idMesero);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                datosHoy.add(new MesaAsignada(
                        rs.getInt("asignacion_id"),
                        rs.getString("mesa"),
                        rs.getString("horario"),
                        rs.getString("fecha"),
                        rs.getString("ORDEN_ESTADO")
                ));
            }
            if (tablaHoyMesero != null) tablaHoyMesero.setItems(datosHoy);
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudieron cargar tus mesas de hoy.");
        }
    }

    private void cargarAsignacionesPasadas() {
        datosPasadas.clear();
        String sql =
                "SELECT am.ID AS asignacion_id, m.NOMBRE AS mesa, " +
                        "       TO_CHAR(am.HORARIO_INICIO, 'HH12:MI AM') || ' a ' || TO_CHAR(am.HORARIO_FIN, 'HH12:MI AM') AS horario, " +
                        "       TO_CHAR(NVL(am.FECHA_ASIGNACION, TRUNC(am.HORARIO_INICIO)), 'DD/MM/YYYY') AS fecha, " +
                        "       (SELECT o.ESTADO FROM ORDENES o WHERE o.ASIGNACION_ID = am.ID " +
                        "         ORDER BY o.FECHA DESC FETCH FIRST 1 ROWS ONLY) AS ORDEN_ESTADO " +
                        "FROM ASIGNACIONES_MESAS am " +
                        "JOIN MESAS m ON am.MESA_ID = m.ID " +
                        "WHERE am.MESERO_ID = ? " +
                        "  AND m.ESTADO = 'ACTIVO' " +
                        "  AND NVL(am.FECHA_ASIGNACION, TRUNC(am.HORARIO_INICIO)) < TRUNC(CURRENT_DATE) " +
                        "ORDER BY NVL(am.FECHA_ASIGNACION, TRUNC(am.HORARIO_INICIO)) DESC, m.NOMBRE";
        try (Connection con = Conexion.conectar();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, idMesero);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                datosPasadas.add(new MesaAsignada(
                        rs.getInt("asignacion_id"),
                        rs.getString("mesa"),
                        rs.getString("horario"),
                        rs.getString("fecha"),
                        rs.getString("ORDEN_ESTADO")
                ));
            }
            if (tablaPasadasMesero != null) tablaPasadasMesero.setItems(datosPasadas);
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudieron cargar tus mesas pasadas.");
        }
    }

    // ===== Acciones =====

    private void tomarOrden(MesaAsignada asignacion) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/integradora/orden.fxml"));
            Parent root = loader.load();
            OrdenController ordenController = loader.getController();
            ordenController.setDatosMesa(idMesero, labelNombreMesero.getText(),
                    asignacion.getMesa(), asignacion.getHorario(), asignacion.getAsignacionId());

            Stage stage = (Stage) labelNombreMesero.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo abrir la pantalla de orden.");
        }
    }

    private void solicitarCambio(MesaAsignada asignacion) {
        try (Connection con = Conexion.conectar()) {
            int idMesa = obtenerIdMesaPorNombre(asignacion.getMesa());

            String sql = "SELECT ESTADO FROM SOLICITUDES_CAMBIO WHERE ASIGNACION_ID = ? " +
                    "ORDER BY FECHA_SOLICITUD DESC FETCH FIRST 1 ROWS ONLY";
            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setInt(1, asignacion.getAsignacionId());
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    String estado = rs.getString("ESTADO");
                    if ("PENDIENTE".equals(estado)) {
                        mostrarAlerta("Solicitud en espera", "Ya tienes una solicitud pendiente para esta mesa y horario.");
                        return;
                    } else if ("APROBADO".equals(estado)) {
                        abrirPantallaModificarOrden(asignacion.getAsignacionId(), asignacion.getMesa(), asignacion.getHorario());
                        return;
                    } else if ("DENEGADO".equals(estado)) {
                        mostrarAlerta("Solicitud denegada", "Tu última solicitud fue denegada. Debes modificar la orden.");
                        return;
                    }
                }
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/integradora/solicitar_cambio.fxml"));
            Parent root = loader.load();
            SolicitarCambioController controller = loader.getController();
            controller.setDatos(idMesero, idMesa, asignacion.getMesa(), asignacion.getAsignacionId());

            Stage dialog = new Stage();
            dialog.setTitle("Solicitar Cambio");
            dialog.setScene(new Scene(root));
            dialog.initOwner(labelNombreMesero.getScene().getWindow());
            dialog.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo abrir la ventana de solicitud de cambio.");
        }
    }

    /** Abre la pantalla de edición y PRELLENA usando el id de la orden ENVIADA */
    private void abrirPantallaModificarOrden(int asignacionId, String nombreMesa, String horario) {
        try {
            int idOrden = -1;
            String sql = "SELECT ID FROM ORDENES WHERE ASIGNACION_ID = ? AND ESTADO = 'ENVIADA' " +
                    "ORDER BY FECHA DESC FETCH FIRST 1 ROWS ONLY";
            try (Connection con = Conexion.conectar();
                 PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setInt(1, asignacionId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) idOrden = rs.getInt("ID");
            }
            if (idOrden == -1) {
                mostrarAlerta("No se encontró orden", "No hay una orden enviada para modificar.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/integradora/orden.fxml"));
            Parent root = loader.load();
            OrdenController ordenController = loader.getController();

            ordenController.setDatosMesaParaEditar(
                    idMesero, labelNombreMesero.getText(),
                    nombreMesa, horario, asignacionId, idOrden
            );

            Stage stage = (Stage) labelNombreMesero.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo abrir la pantalla de modificación de orden.");
        }
    }

    // YA NO cierra la orden aquí. Solo busca la ENVIADA y muestra el resumen.
    private void cerrarCuenta(MesaAsignada asignacion) {
        try (Connection con = Conexion.conectar()) {
            int idOrden = -1;
            String sqlOrden = "SELECT ID FROM ORDENES WHERE ASIGNACION_ID = ? AND ESTADO = 'ENVIADA'";
            try (PreparedStatement stmt = con.prepareStatement(sqlOrden)) {
                stmt.setInt(1, asignacion.getAsignacionId());
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) idOrden = rs.getInt("ID");
            }
            if (idOrden == -1) {
                mostrarAlerta("Error", "No se encontró una orden enviada para esta mesa.");
                return;
            }

            // MOSTRAR resumen (sin cerrar)
            mostrarResumenFinal(idOrden);

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo abrir el resumen.");
        }
    }

    /** Muestra el resumen (la orden aún NO se cierra) */
    private void mostrarResumenFinal(int idOrden) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/integradora/finalizar_orden.fxml"));
            Parent root = loader.load();

            FinalizarOrdenController controller = loader.getController();
            controller.setDatosOrden(idOrden, idMesero, labelNombreMesero.getText(), "Mesas Asignadas/Resumen Final");

            Stage stage = (Stage) labelNombreMesero.getScene().getWindow();
            stage.getScene().setRoot(root);            stage.setMaximized(true); // <- pantalla maximizada
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo mostrar el resumen final.");
        }
    }

    private int obtenerIdMesaPorNombre(String nombreMesa) throws Exception {
        String sql = "SELECT ID FROM MESAS WHERE NOMBRE = ?";
        try (Connection con = Conexion.conectar();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, nombreMesa);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt("ID");
            else throw new Exception("No se encontró la mesa: " + nombreMesa);
        }
    }

    @FXML
    private void cerrarSesion(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/integradora/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo volver al login.");
        }
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, mensaje);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    // ===== Modelo con FECHA y estado =====
    public static class MesaAsignada {
        private final int asignacionId;
        private final String mesa;
        private final String horario;
        private final String fecha;
        private final String ordenEstado;

        public MesaAsignada(int asignacionId, String mesa, String horario, String fecha, String ordenEstado) {
            this.asignacionId = asignacionId;
            this.mesa = mesa;
            this.horario = horario;
            this.fecha = fecha;
            this.ordenEstado = ordenEstado;
        }
        public int getAsignacionId() { return asignacionId; }
        public String getMesa() { return mesa; }
        public String getHorario() { return horario; }
        public String getFecha() { return fecha; }
        public String getOrdenEstado() { return ordenEstado; }
    }
}
