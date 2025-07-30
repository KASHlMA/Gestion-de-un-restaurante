package com.example.integradora;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class FormularioMesaController {
    @FXML private Label tituloLabel;
    @FXML private TextField txtNombreMesa;
    @FXML private ComboBox<String> comboEstado;

    private MesasAdminController.Mesa mesaEditar;
    private Runnable callbackRefrescar; // Para refrescar la tabla después de guardar

    @FXML
    public void initialize() {
        comboEstado.getItems().setAll("ACTIVO", "INACTIVO");
    }

    public void setDatos(MesasAdminController.Mesa mesa, Runnable refrescar) {
        this.mesaEditar = mesa;
        this.callbackRefrescar = refrescar;
        comboEstado.getItems().setAll("ACTIVO", "INACTIVO");
        if (mesa != null) {
            tituloLabel.setText("Editar Mesa");
            txtNombreMesa.setText(mesa.getNombre());
            comboEstado.setValue(mesa.getEstado());
        } else {
            tituloLabel.setText("Añadir Mesa");
            comboEstado.setValue("ACTIVO");
        }
    }

    @FXML
    private void guardar() {
        String nombre = txtNombreMesa.getText().trim();
        String estado = comboEstado.getValue();
        if (nombre.isEmpty()) {
            mostrarAlerta("Faltan datos", "Escribe el nombre de la mesa.");
            return;
        }
        try (var con = Conexion.conectar()) {
            if (mesaEditar == null) {
                var stmt = con.prepareStatement("INSERT INTO MESAS (NOMBRE, ESTADO) VALUES (?, ?)");
                stmt.setString(1, nombre);
                stmt.setString(2, estado);
                stmt.executeUpdate();
            } else {
                var stmt = con.prepareStatement("UPDATE MESAS SET NOMBRE = ?, ESTADO = ? WHERE ID = ?");
                stmt.setString(1, nombre);
                stmt.setString(2, estado);
                stmt.setInt(3, mesaEditar.getId());
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo guardar.");
            return;
        }
        cerrar();
        if (callbackRefrescar != null) callbackRefrescar.run();
    }

    // Si tienes un botón "Eliminar" en tu formulario, llama a este método y pásale el id:
    @FXML
    private void eliminar() {
        if (mesaEditar == null) {
            mostrarAlerta("Error", "No hay mesa seleccionada para eliminar.");
            return;
        }
        eliminarMesa(mesaEditar.getId());
    }

    private void eliminarMesa(int mesaId) {
        if (tieneRelacion(mesaId)) {
            mostrarAlerta("No puedes eliminar la mesa", "Esta mesa tiene registros relacionados (órdenes o asignaciones).");
            return;
        }
        String sql = "DELETE FROM MESAS WHERE ID = ?";
        try (Connection con = Conexion.conectar();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, mesaId);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                mostrarAlerta("Mesa eliminada", "La mesa fue eliminada exitosamente.");
                cerrar();
                if (callbackRefrescar != null) callbackRefrescar.run();
            }
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo eliminar la mesa.");
        }
    }

    private boolean tieneRelacion(int mesaId) {
        String[] tablas = {"ORDENES", "ASIGNACIONES_MESAS"};
        for (String tabla : tablas) {
            String sql = "SELECT 1 FROM " + tabla + " WHERE MESA_ID = ?";
            try (Connection con = Conexion.conectar();
                 PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setInt(1, mesaId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return true; // Encontró una relación
                }
            } catch (Exception e) {
                e.printStackTrace();
                // Si hay error, por seguridad no dejes borrar
                return true;
            }
        }
        return false; // No tiene relaciones
    }

    @FXML
    private void cancelar() {
        cerrar();
    }

    private void cerrar() {
        Stage stage = (Stage) txtNombreMesa.getScene().getWindow();
        stage.close();
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, mensaje);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
