package com.example.integradora;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class FormularioCategoriaController {
    @FXML private Label tituloLabel;
    @FXML private TextField txtNombreCategoria;
    @FXML private ComboBox<String> comboEstado;

    private ComidaAdminController.Categoria categoriaEditar;
    private Runnable callbackRefrescar;

    @FXML
    public void initialize() {
        comboEstado.setItems(FXCollections.observableArrayList("ACTIVO", "INACTIVO"));
    }

    public void setDatos(ComidaAdminController.Categoria categoria, Runnable refrescar) {
        this.categoriaEditar = categoria;
        this.callbackRefrescar = refrescar;
        if (categoria != null) {
            tituloLabel.setText("Editar Categoría");
            txtNombreCategoria.setText(categoria.getNombre());
            comboEstado.setValue(categoria.getEstado());
        } else {
            tituloLabel.setText("Añadir Categoría");
            txtNombreCategoria.clear();
            comboEstado.setValue("ACTIVO");
        }
    }

    @FXML
    private void guardar() {
        String nombre = txtNombreCategoria.getText().trim();
        String estado = comboEstado.getValue();

        if (nombre.isEmpty() || estado == null) {
            mostrarAlerta("Faltan datos", "Completa todos los campos.");
            return;
        }

        try (Connection con = Conexion.conectar()) {
            if (categoriaEditar == null) {
                String sql = "INSERT INTO CATEGORIAS (NOMBRE, ESTADO) VALUES (?, ?)";
                try (PreparedStatement stmt = con.prepareStatement(sql)) {
                    stmt.setString(1, nombre);
                    stmt.setString(2, estado);
                    stmt.executeUpdate();
                }
                mostrarAlerta("Éxito", "Categoría añadida correctamente.");
            } else {
                String sql = "UPDATE CATEGORIAS SET NOMBRE = ?, ESTADO = ? WHERE ID = ?";
                try (PreparedStatement stmt = con.prepareStatement(sql)) {
                    stmt.setString(1, nombre);
                    stmt.setString(2, estado);
                    stmt.setInt(3, categoriaEditar.getId());
                    stmt.executeUpdate();
                }
                mostrarAlerta("Éxito", "Categoría editada correctamente.");
            }
            if (callbackRefrescar != null) callbackRefrescar.run();
            cerrar();
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo guardar la categoría.");
        }
    }

    @FXML
    private void cancelar() {
        cerrar();
    }

    private void cerrar() {
        Stage stage = (Stage) txtNombreCategoria.getScene().getWindow();
        stage.close();
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, mensaje);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
