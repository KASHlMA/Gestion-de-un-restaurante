package com.example.integradora;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class FormularioPlatilloController {
    @FXML private Label tituloLabel;
    @FXML private ComboBox<String> comboCategoria;
    @FXML private TextField txtNombrePlatillo;
    @FXML private TextField txtPrecio;
    @FXML private ComboBox<String> comboEstado;

    private ComidaAdminController.Platillo platilloEditar;
    private Runnable callbackRefrescar;

    @FXML
    public void initialize() {
        comboEstado.setItems(FXCollections.observableArrayList("ACTIVO", "INACTIVO"));
        cargarCategorias();
    }

    private void cargarCategorias() {
        ObservableList<String> categorias = FXCollections.observableArrayList();
        String sql = "SELECT NOMBRE FROM CATEGORIAS WHERE ESTADO = 'ACTIVO'";
        try (Connection con = Conexion.conectar();
             PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                categorias.add(rs.getString("NOMBRE"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        comboCategoria.setItems(categorias);
    }

    public void setDatos(ComidaAdminController.Platillo platillo, Runnable refrescar) {
        this.platilloEditar = platillo;
        this.callbackRefrescar = refrescar;
        if (platillo != null) {
            tituloLabel.setText("Editar Platillo");
            comboCategoria.setValue(platillo.getCategoria());
            txtNombrePlatillo.setText(platillo.getNombre());
            txtPrecio.setText(platillo.getPrecio());
            comboEstado.setValue(platillo.getEstado());
        } else {
            tituloLabel.setText("Añadir Platillo");
            comboCategoria.getSelectionModel().clearSelection();
            txtNombrePlatillo.clear();
            txtPrecio.clear();
            comboEstado.setValue("ACTIVO");
        }
    }

    @FXML
    private void guardar() {
        String categoria = comboCategoria.getValue();
        String nombre = txtNombrePlatillo.getText().trim();
        String precioStr = txtPrecio.getText().trim();
        String estado = comboEstado.getValue();

        if (categoria == null || nombre.isEmpty() || precioStr.isEmpty() || estado == null) {
            mostrarAlerta("Faltan datos", "Completa todos los campos.");
            return;
        }

        double precio;
        try {
            precio = Double.parseDouble(precioStr);
            if (precio < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            mostrarAlerta("Precio inválido", "El precio debe ser un número positivo.");
            return;
        }

        try (Connection con = Conexion.conectar()) {
            // Obtener ID de categoría
            int idCategoria = -1;
            String sqlCat = "SELECT ID FROM CATEGORIAS WHERE NOMBRE = ?";
            try (PreparedStatement stmt = con.prepareStatement(sqlCat)) {
                stmt.setString(1, categoria);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) idCategoria = rs.getInt("ID");
            }
            if (idCategoria == -1) {
                mostrarAlerta("Error", "Categoría no encontrada.");
                return;
            }

            if (platilloEditar == null) {
                // Nuevo platillo
                String sql = "INSERT INTO PLATILLOS (NOMBRE, PRECIO, ESTADO, CATEGORIA_ID) VALUES (?, ?, ?, ?)";
                try (PreparedStatement stmt = con.prepareStatement(sql)) {
                    stmt.setString(1, nombre);
                    stmt.setDouble(2, precio);
                    stmt.setString(3, estado);
                    stmt.setInt(4, idCategoria);
                    stmt.executeUpdate();
                }
                mostrarAlerta("Éxito", "Platillo añadido correctamente.");
            } else {
                // Editar platillo
                String sql = "UPDATE PLATILLOS SET NOMBRE = ?, PRECIO = ?, ESTADO = ?, CATEGORIA_ID = ? WHERE ID = ?";
                try (PreparedStatement stmt = con.prepareStatement(sql)) {
                    stmt.setString(1, nombre);
                    stmt.setDouble(2, precio);
                    stmt.setString(3, estado);
                    stmt.setInt(4, idCategoria);
                    stmt.setInt(5, platilloEditar.getId());
                    stmt.executeUpdate();
                }
                mostrarAlerta("Éxito", "Platillo editado correctamente.");
            }
            if (callbackRefrescar != null) callbackRefrescar.run();
            cerrar();
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo guardar el platillo.");
        }
    }

    @FXML
    private void cancelar() {
        cerrar();
    }

    private void cerrar() {
        Stage stage = (Stage) txtNombrePlatillo.getScene().getWindow();
        stage.close();
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, mensaje);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
