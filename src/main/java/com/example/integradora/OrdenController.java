package com.example.integradora;

import javafx.beans.property.SimpleStringProperty;
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

import java.sql.*;
import java.util.*;

public class OrdenController {
    @FXML private Label labelMesero;
    @FXML private Label labelMesaYHorario;
    @FXML private TableView<FilaOrden> tablaOrden;
    @FXML private TableColumn<FilaOrden, String> colCategoria;
    @FXML private TableColumn<FilaOrden, String> colPlatillo;
    @FXML private TableColumn<FilaOrden, String> colCantidad;
    @FXML private TableColumn<FilaOrden, Void> colAcciones;

    // Listas para combos y mapeos
    private ObservableList<String> categorias = FXCollections.observableArrayList();
    private Map<String, ObservableList<String>> platillosPorCategoria = new HashMap<>();

    // Datos de la sesión
    private int idMesero;
    private String nombreMesero;
    private String nombreMesa;
    private String horario;

    // Lista de filas de la orden
    private final ObservableList<FilaOrden> filas = FXCollections.observableArrayList();

    // Método llamado desde el MeseroController para inicializar la pantalla
    public void setDatosMesa(int idMesero, String nombreMesero, String nombreMesa, String horario) {
        this.idMesero = idMesero;
        this.nombreMesero = nombreMesero;
        this.nombreMesa = nombreMesa;
        this.horario = horario;
        labelMesaYHorario.setText("Mesa Asignada: " + nombreMesa + " - " + horario);
        labelMesero.setText(nombreMesero);
    }

    @FXML
    public void initialize() {
        // Cargar combos desde la base de datos
        cargarCategoriasYPlatillos();

        // Configurar las columnas de la tabla
        colCategoria.setCellValueFactory(cell -> cell.getValue().categoriaProperty());
        colPlatillo.setCellValueFactory(cell -> cell.getValue().platilloProperty());
        colCantidad.setCellValueFactory(cell -> cell.getValue().cantidadProperty());

        // CellFactory para columna de categoría (ComboBox)
        colCategoria.setCellFactory(param -> {
            final ComboBox<String> combo = new ComboBox<>(categorias);
            return new TableCell<>() {
                {
                    combo.setOnAction(e -> {
                        FilaOrden fila = getTableView().getItems().get(getIndex());
                        String cat = combo.getValue();
                        fila.setCategoria(cat);
                        fila.setPlatillo(null);
                        getTableView().refresh();
                    });
                }
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) setGraphic(null);
                    else {
                        combo.setValue(item);
                        setGraphic(combo);
                    }
                }
            };
        });

        // CellFactory para columna de platillo (ComboBox dependiente)
        colPlatillo.setCellFactory(param -> {
            final ComboBox<String> combo = new ComboBox<>();
            return new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) setGraphic(null);
                    else {
                        FilaOrden fila = getTableView().getItems().get(getIndex());
                        String categoriaSeleccionada = fila.getCategoria();
                        if (categoriaSeleccionada != null && platillosPorCategoria.containsKey(categoriaSeleccionada)) {
                            combo.setItems(platillosPorCategoria.get(categoriaSeleccionada));
                        } else {
                            combo.setItems(FXCollections.observableArrayList());
                        }
                        combo.setValue(item);
                        combo.setOnAction(e -> fila.setPlatillo(combo.getValue()));
                        setGraphic(combo);
                    }
                }
            };
        });

        // CellFactory para columna de cantidad (TextField editable)
        colCantidad.setCellFactory(param -> {
            final TextField textField = new TextField();
            return new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) setGraphic(null);
                    else {
                        FilaOrden fila = getTableView().getItems().get(getIndex());
                        textField.setText(item != null ? item : "");
                        textField.textProperty().addListener((obs, oldVal, newVal) -> fila.setCantidad(newVal));
                        setGraphic(textField);
                    }
                }
            };
        });

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
     * Carga categorías y platillos agrupados desde la base.
     */
    private void cargarCategoriasYPlatillos() {
        categorias.clear();
        platillosPorCategoria.clear();
        String sql = "SELECT c.NOMBRE AS categoria, p.NOMBRE AS platillo " +
                "FROM CATEGORIAS c LEFT JOIN PLATILLOS p ON c.ID = p.CATEGORIA_ID";
        try (Connection con = Conexion.conectar();
             PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            Map<String, Set<String>> temp = new HashMap<>();
            while (rs.next()) {
                String cat = rs.getString("categoria");
                String plat = rs.getString("platillo");
                temp.putIfAbsent(cat, new HashSet<>());
                if (plat != null) temp.get(cat).add(plat);
            }
            for (String cat : temp.keySet()) {
                categorias.add(cat);
                platillosPorCategoria.put(cat, FXCollections.observableArrayList(temp.get(cat)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Agrega una nueva fila vacía para ordenar un platillo.
     */
    @FXML
    private void agregarFilaOrden() {
        filas.add(new FilaOrden());
    }

    /**
     * Al presionar "Realizar la orden", guarda en la base de datos.
     */
    @FXML
    private void realizarOrden() {
        // 1. Validación de campos
        for (FilaOrden fila : filas) {
            if (fila.getCategoria() == null || fila.getPlatillo() == null || fila.getCantidad() == null ||
                    fila.getCategoria().isEmpty() || fila.getPlatillo().isEmpty() || fila.getCantidad().isEmpty()) {
                mostrarAlerta("Faltan datos", "Completa todos los campos antes de realizar la orden.");
                return;
            }
            try {
                int cant = Integer.parseInt(fila.getCantidad());
                if (cant <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                mostrarAlerta("Cantidad inválida", "La cantidad debe ser un número mayor a cero.");
                return;
            }
        }

        try (Connection con = Conexion.conectar()) {
            con.setAutoCommit(false);

            int mesaId = obtenerIdMesaPorNombre(nombreMesa);

            // 👉 Primero busca si ya hay una orden ABIERTA para esta mesa y este mesero
            int idOrden = -1;
            String sqlCheck = "SELECT ID FROM ORDENES WHERE MESA_ID = ? AND MESERO_ID = ? AND ESTADO = 'ABIERTA'";
            try (PreparedStatement stmt = con.prepareStatement(sqlCheck)) {
                stmt.setInt(1, mesaId);
                stmt.setInt(2, idMesero);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) idOrden = rs.getInt("ID");
            }

            // Si no existe, crea una nueva
            if (idOrden == -1) {
                String sqlOrden = "INSERT INTO ORDENES (MESA_ID, MESERO_ID, FECHA, ESTADO) VALUES (?, ?, SYSTIMESTAMP, 'ABIERTA')";
                try (PreparedStatement stmt = con.prepareStatement(sqlOrden, new String[]{"ID"})) {
                    stmt.setInt(1, mesaId);
                    stmt.setInt(2, idMesero);
                    stmt.executeUpdate();
                    ResultSet rs = stmt.getGeneratedKeys();
                    if (rs.next()) idOrden = rs.getInt(1);
                }
            } else {
                // Si existe, elimina los detalles anteriores para actualizar la orden (opcional)
                String sqlDel = "DELETE FROM DETALLE_ORDEN WHERE ORDEN_ID = ?";
                try (PreparedStatement stmt = con.prepareStatement(sqlDel)) {
                    stmt.setInt(1, idOrden);
                    stmt.executeUpdate();
                }
            }

            // --- Inserta los detalles nuevos ---
            String sqlDetalle = "INSERT INTO DETALLE_ORDEN (ORDEN_ID, PLATILLO_ID, CANTIDAD) VALUES (?, ?, ?)";
            try (PreparedStatement stmtDetalle = con.prepareStatement(sqlDetalle)) {
                for (FilaOrden fila : filas) {
                    int platilloId = obtenerIdPlatilloPorNombre(fila.getPlatillo());
                    int cantidad = Integer.parseInt(fila.getCantidad());
                    stmtDetalle.setInt(1, idOrden);
                    stmtDetalle.setInt(2, platilloId);
                    stmtDetalle.setInt(3, cantidad);
                    stmtDetalle.addBatch();
                }
                stmtDetalle.executeBatch();
            }

            con.commit();

            // --- Navega al detalle ---
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/integradora/detalle_orden.fxml"));
            Parent root = loader.load();
            DetalleOrdenController controller = loader.getController();
            String ruta = "Mesas Asignadas/Orden " + nombreMesa;
            controller.setDatosOrden(idOrden, idMesero, nombreMesero, ruta);

            Stage stage = (Stage) tablaOrden.getScene().getWindow();
            stage.setScene(new Scene(root));

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error al guardar", "No se pudo registrar la orden.\n" + e.getMessage());
        }
    }





    /**
     * Busca el ID de la mesa por nombre
     */
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

    /**
     * Busca el ID del platillo por nombre
     */
    private int obtenerIdPlatilloPorNombre(String nombrePlatillo) throws Exception {
        String sql = "SELECT ID FROM PLATILLOS WHERE NOMBRE = ?";
        try (Connection con = Conexion.conectar();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, nombrePlatillo);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt("ID");
            else throw new Exception("No se encontró el platillo: " + nombrePlatillo);
        }
    }

    /**
     * Muestra una alerta informativa
     */
    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, mensaje);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    /**
     * Clase modelo para cada fila de la orden
     */
    public static class FilaOrden {
        private final SimpleStringProperty categoria = new SimpleStringProperty();
        private final SimpleStringProperty platillo = new SimpleStringProperty();
        private final SimpleStringProperty cantidad = new SimpleStringProperty();

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

    // Botón "Volver Atrás"
    @FXML
    private void volverAtras(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/integradora/mesero.fxml"));
            Parent root = loader.load();
            MeseroController meseroController = loader.getController();
            meseroController.setIdMesero(idMesero, nombreMesero);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo volver atrás.");
        }
    }

    // Botón "Cerrar Sesión"
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
}
