package com.example.integradora;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class OrdenController {

    @FXML private Label labelMesaYHorario;
    @FXML private TableView<FilaOrden> tablaOrden;
    @FXML private TableColumn<FilaOrden, String> colCategoria;
    @FXML private TableColumn<FilaOrden, String> colPlatillo;
    @FXML private TableColumn<FilaOrden, String> colCantidad;
    @FXML private TableColumn<FilaOrden, Void> colAcciones;

    // Listas para las opciones de combos
    private ObservableList<String> categorias = FXCollections.observableArrayList();
    private ObservableList<String> platillos = FXCollections.observableArrayList();

    // Guarda datos recibidos
    private int idMesero;
    private String nombreMesero;
    private String nombreMesa;
    private String horario;

    // Lista de las filas de la orden
    private final ObservableList<FilaOrden> filas = FXCollections.observableArrayList();

    // Llamar después de cargar el FXML
    public void setDatosMesa(int idMesero, String nombreMesero, String nombreMesa, String horario) {
        this.idMesero = idMesero;
        this.nombreMesero = nombreMesero;
        this.nombreMesa = nombreMesa;
        this.horario = horario;
        labelMesaYHorario.setText("Mesa Asignada: " + nombreMesa + " - " + horario);
    }

    @FXML
    public void initialize() {
        // Llena los combos de categorías y platillos
        cargarCategorias();

        // Configura las columnas
        colCategoria.setCellValueFactory(cell -> cell.getValue().categoriaProperty());
        colPlatillo.setCellValueFactory(cell -> cell.getValue().platilloProperty());
        colCantidad.setCellValueFactory(cell -> cell.getValue().cantidadProperty());

        // Botón eliminar para cada fila
        colAcciones.setCellFactory(param -> new TableCell<>() {
            private final Button btnEliminar = new Button("🗑");

            {
                btnEliminar.setOnAction(event -> {
                    FilaOrden fila = getTableView().getItems().get(getIndex());
                    filas.remove(fila);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnEliminar);
            }
        });

        // Inicia con una fila
        tablaOrden.setItems(filas);
        agregarFilaOrden();
    }

    /**
     * Llena la lista de categorías desde la base.
     */
    private void cargarCategorias() {
        categorias.clear();
        String sql = "SELECT NOMBRE FROM CATEGORIAS";
        try (Connection con = Conexion.conectar();
             PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                categorias.add(rs.getString("NOMBRE"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Cuando el usuario da clic en "+ Añadir platillo", agrega una fila nueva a la tabla.
     */
    @FXML
    private void agregarFilaOrden() {
        FilaOrden nueva = new FilaOrden();
        filas.add(nueva);
    }

    /**
     * Cuando el usuario da clic en "Realizar la orden"
     */
    @FXML
    private void realizarOrden() {
        // Validación: que todas las filas tengan categoría, platillo y cantidad
        for (FilaOrden fila : filas) {
            if (fila.getCategoria() == null || fila.getPlatillo() == null || fila.getCantidad() == null) {
                mostrarAlerta("Faltan datos", "Completa todos los campos antes de realizar la orden.");
                return;
            }
        }

        // TODO: Inserta la orden en tu base de datos aquí (una fila por platillo)
        mostrarAlerta("¡Orden realizada!", "La orden ha sido registrada (simulado).");

        // Limpia la orden
        filas.clear();
        agregarFilaOrden();
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, mensaje);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    /**
     * Clase modelo para cada fila de la orden
     */
    public class FilaOrden {
        private final SimpleStringProperty categoria = new SimpleStringProperty();
        private final SimpleStringProperty platillo = new SimpleStringProperty();
        private final SimpleStringProperty cantidad = new SimpleStringProperty();

        public FilaOrden() {
            // Puedes inicializar combos aquí si los vas a poner como celdas personalizadas
        }

        public String getCategoria() { return categoria.get(); }
        public void setCategoria(String c) { categoria.set(c); }
        public SimpleStringProperty categoriaProperty() { return categoria; }

        public String getPlatillo() { return platillo.get(); }
        public void setPlatillo(String p) { platillo.set(p); }
        public SimpleStringProperty platilloProperty() { return platillo; }

        public String getCantidad() { return cantidad.get(); }
        public void setCantidad(String q) { cantidad.set(q); }
        public SimpleStringProperty cantidadProperty() { return cantidad; }
    }
}
