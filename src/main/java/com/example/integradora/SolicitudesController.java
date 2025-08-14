package com.example.integradora;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.sql.*;
import java.util.Optional;

public class SolicitudesController {

    @FXML private TableView<SolicitudCambio> tablaSolicitudes;
    @FXML private TableColumn<SolicitudCambio, String> colMesa;
    @FXML private TableColumn<SolicitudCambio, String> colMesero;
    @FXML private TableColumn<SolicitudCambio, String> colDescripcion;
    @FXML private TableColumn<SolicitudCambio, String> colEstado;
    @FXML private TableColumn<SolicitudCambio, String> colHorario;
    @FXML private TableColumn<SolicitudCambio, Void>   colAcciones;

    private final ObservableList<SolicitudCambio> solicitudes = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colMesa.setCellValueFactory(new PropertyValueFactory<>("mesa"));
        colHorario.setCellValueFactory(new PropertyValueFactory<>("horario"));
        colMesero.setCellValueFactory(new PropertyValueFactory<>("mesero"));
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        // Botones de acción por fila con confirmación
        colAcciones.setCellFactory(param -> new TableCell<>() {
            private final Button btnAceptar  = new Button("Aceptar");
            private final Button btnDenegar  = new Button("Denegar");
            private final Button btnEliminar = new Button("Eliminar");
            private final HBox box = new HBox(8, btnAceptar, btnDenegar, btnEliminar);

            {
                btnAceptar.setOnAction(e -> actualizarEstadoConConfirmacion("APROBADO",
                        "¿Seguro que deseas APROBAR esta solicitud?"));
                btnDenegar.setOnAction(e -> actualizarEstadoConConfirmacion("DENEGADO",
                        "¿Seguro que deseas DENEGAR esta solicitud?"));
                btnEliminar.setOnAction(e -> eliminarConConfirmacion());
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    // Opcional: deshabilitar acciones si ya no está pendiente
                    SolicitudCambio s = getTableView().getItems().get(getIndex());
                    boolean pendiente = "PENDIENTE".equalsIgnoreCase(s.getEstado());
                    btnAceptar.setDisable(!pendiente);
                    btnDenegar.setDisable(!pendiente);
                    setGraphic(box);
                }
            }

            private void actualizarEstadoConConfirmacion(String nuevoEstado, String mensajeConfirmacion) {
                SolicitudCambio sol = getTableView().getItems().get(getIndex());
                if (!confirmar("Confirmar", mensajeConfirmacion)) return;

                try (Connection con = Conexion.conectar()) {
                    String sql = "UPDATE SOLICITUDES_CAMBIO SET ESTADO = ? WHERE ID = ?";
                    try (PreparedStatement stmt = con.prepareStatement(sql)) {
                        stmt.setString(1, nuevoEstado);
                        stmt.setInt(2, sol.getId());
                        stmt.executeUpdate();
                    }

                    // Si deseas liberar la orden para edición al aprobar, descomenta este bloque:
                    /*
                    if ("APROBADO".equals(nuevoEstado)) {
                        // Ejemplo: nada que hacer si tu flujo abre directamente la pantalla de edición
                        // tras detectar 'APROBADO' en el MeseroController.
                    }
                    */

                    mostrarInfo("Éxito", "La solicitud se actualizó a: " + nuevoEstado + ".");
                    cargarSolicitudes();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    mostrarInfo("Error", "No se pudo actualizar la solicitud.");
                }
            }

            private void eliminarConConfirmacion() {
                SolicitudCambio sol = getTableView().getItems().get(getIndex());
                if (!confirmar("Eliminar solicitud",
                        "¿Seguro que deseas ELIMINAR la solicitud de \"" + sol.getMesa() + "\"?")) return;

                try (Connection con = Conexion.conectar()) {
                    String sql = "DELETE FROM SOLICITUDES_CAMBIO WHERE ID = ?";
                    try (PreparedStatement stmt = con.prepareStatement(sql)) {
                        stmt.setInt(1, sol.getId());
                        stmt.executeUpdate();
                    }
                    mostrarInfo("Eliminada", "La solicitud fue eliminada correctamente.");
                    cargarSolicitudes();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    mostrarInfo("Error", "No se pudo eliminar la solicitud.");
                }
            }
        });

        cargarSolicitudes();
    }

    private void cargarSolicitudes() {
        solicitudes.clear();
        String sql =
                "SELECT s.ID, s.ASIGNACION_ID, m.NOMBRE AS mesa, " +
                        "       TO_CHAR(am.HORARIO_INICIO, 'HH12:MI AM') || ' a ' || TO_CHAR(am.HORARIO_FIN, 'HH12:MI AM') AS horario, " +
                        "       u.NOMBRE AS mesero, s.DESCRIPCION, s.ESTADO " +
                        "FROM SOLICITUDES_CAMBIO s " +
                        "LEFT JOIN ASIGNACIONES_MESAS am ON s.ASIGNACION_ID = am.ID " +
                        "LEFT JOIN MESAS m ON am.MESA_ID = m.ID " +
                        "LEFT JOIN USUARIOS u ON s.MESERO_ID = u.ID " +
                        "ORDER BY s.FECHA_SOLICITUD DESC";
        try (Connection con = Conexion.conectar();
             PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                solicitudes.add(new SolicitudCambio(
                        rs.getInt("ID"),
                        rs.getInt("ASIGNACION_ID"),
                        rs.getString("mesa"),
                        rs.getString("horario"),
                        rs.getString("mesero"),
                        rs.getString("descripcion"),
                        rs.getString("estado")
                ));
            }
            tablaSolicitudes.setItems(solicitudes);
        } catch (Exception e) {
            e.printStackTrace();
            mostrarInfo("Error", "No se pudieron cargar las solicitudes.");
        }
    }

    // ---------- Utilidades de diálogo ----------
    private boolean confirmar(String titulo, String mensaje) {
        ButtonType si = new ButtonType("Sí", ButtonBar.ButtonData.OK_DONE);
        ButtonType no = new ButtonType("No", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, mensaje, si, no);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        Stage owner = ownerWindow();
        if (owner != null) alert.initOwner(owner);
        Optional<ButtonType> r = alert.showAndWait();
        return r.isPresent() && r.get() == si;
    }

    private void mostrarInfo(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, mensaje);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        Stage owner = ownerWindow();
        if (owner != null) alert.initOwner(owner);
        alert.showAndWait();
    }

    private Stage ownerWindow() {
        return (tablaSolicitudes != null && tablaSolicitudes.getScene() != null)
                ? (Stage) tablaSolicitudes.getScene().getWindow()
                : null;
    }

    // ---------- Modelo ----------
    public static class SolicitudCambio {
        private final int id;
        private final int asignacionId;
        private final String mesa;
        private final String horario;
        private final String mesero;
        private final String descripcion;
        private final String estado;

        public SolicitudCambio(int id, int asignacionId, String mesa, String horario,
                               String mesero, String descripcion, String estado) {
            this.id = id;
            this.asignacionId = asignacionId;
            this.mesa = mesa;
            this.horario = horario;
            this.mesero = mesero;
            this.descripcion = descripcion;
            this.estado = estado;
        }
        public int getId() { return id; }
        public int getAsignacionId() { return asignacionId; }
        public String getMesa() { return mesa; }
        public String getHorario() { return horario; }
        public String getMesero() { return mesero; }
        public String getDescripcion() { return descripcion; }
        public String getEstado() { return estado; }
    }
}
