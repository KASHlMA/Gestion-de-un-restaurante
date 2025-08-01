package com.example.integradora;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class SolicitarCambioController {

    @FXML private Label labelMesa;
    @FXML private TextArea txtDescripcion;
    @FXML private Button btnEnviar;

    private int idMesero;
    private int idMesa;

    private int asignacionId;

    // Recibe el id de la asignación
    public void setDatos(int idMesero, int idMesa, String nombreMesa, int asignacionId) {
        this.idMesero = idMesero;
        this.idMesa = idMesa;
        this.asignacionId = asignacionId;
        labelMesa.setText("Mesa: " + nombreMesa);

        Platform.runLater(this::verificarSolicitudPendiente);
    }



    // Verifica si ya hay una solicitud PENDIENTE
    private void verificarSolicitudPendiente() {
        String sql = "SELECT ESTADO FROM SOLICITUDES_CAMBIO WHERE ASIGNACION_ID = ? AND ESTADO = 'PENDIENTE'";
        try (Connection con = Conexion.conectar();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, asignacionId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                txtDescripcion.setDisable(true);
                btnEnviar.setDisable(true);
                Alert alerta = new Alert(Alert.AlertType.INFORMATION,
                        "Ya tienes una solicitud pendiente para esta mesa y horario. Espera la respuesta del líder.");
                alerta.showAndWait();
                if (btnEnviar.getScene() != null && btnEnviar.getScene().getWindow() != null) {
                    Stage stage = (Stage) btnEnviar.getScene().getWindow();
                    stage.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @FXML
    private void enviarSolicitud() {
        String motivo = txtDescripcion.getText().trim();
        if (motivo.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Por favor, explica el motivo.").showAndWait();
            return;
        }
        String sql = "INSERT INTO SOLICITUDES_CAMBIO (ASIGNACION_ID, MESERO_ID, MESA_ID, TIPO, DESCRIPCION, FECHA_SOLICITUD, ESTADO) VALUES (?, ?, ?, 'MESA', ?, SYSTIMESTAMP, 'PENDIENTE')";
        try (Connection con = Conexion.conectar();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, asignacionId);
            stmt.setInt(2, idMesero);
            stmt.setInt(3, idMesa);
            stmt.setString(4, motivo);
            stmt.executeUpdate();
            new Alert(Alert.AlertType.INFORMATION, "¡Solicitud enviada correctamente!").showAndWait();
            if (btnEnviar.getScene() != null && btnEnviar.getScene().getWindow() != null) {
                Stage stage = (Stage) btnEnviar.getScene().getWindow();
                stage.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "No se pudo enviar la solicitud.").showAndWait();
        }
    }

}
