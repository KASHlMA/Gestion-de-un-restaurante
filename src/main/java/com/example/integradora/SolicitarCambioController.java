package com.example.integradora;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class SolicitarCambioController {

    @FXML private Label labelMesa;
    @FXML private TextArea txtDescripcion;
    @FXML private Button btnEnviar;

    private int idMesero;
    private int idMesa;

    // Llámalo antes de mostrar la ventana para saber quién es el mesero y la mesa
    public void setDatos(int idMesero, int idMesa, String nombreMesa) {
        this.idMesero = idMesero;
        this.idMesa = idMesa;
        labelMesa.setText(nombreMesa);
    }

    @FXML
    private void enviarSolicitud() {
        String descripcion = txtDescripcion.getText().trim();
        if (descripcion.isEmpty()) {
            mostrarAlerta("Debes escribir una descripción/motivo.");
            return;
        }

        String sql = "INSERT INTO SOLICITUDES_CAMBIO (MESERO_ID, MESA_ID, DESCRIPCION, FECHA_SOLICITUD, ESTADO) VALUES (?, ?, ?, SYSDATE, 'PENDIENTE')";
        try (Connection con = Conexion.conectar();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, idMesero);
            stmt.setInt(2, idMesa);
            stmt.setString(3, descripcion);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("No se pudo registrar la solicitud.");
            return;
        }

        mostrarAlerta("¡Solicitud enviada! El líder la revisará.");
        ((Stage) btnEnviar.getScene().getWindow()).close(); // Cierra la ventana
    }

    private void mostrarAlerta(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, mensaje);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
