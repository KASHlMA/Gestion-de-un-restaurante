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

    private ObservableList<String> categorias = FXCollections.observableArrayList();
    private Map<String, ObservableList<String>> platillosPorCategoria = new HashMap<>();

    private int idMesero;
    private String nombreMesero;
    private String nombreMesa;
    private String horario;
    private int idAsignacion;

    // Si no es null: estamos EDITANDO esta orden
    private Integer idOrdenEnEdicion = null;

    private final ObservableList<FilaOrden> filas = FXCollections.observableArrayList();

    // ===== Setters =====

    // Tomar orden (sin prefill)
    public void setDatosMesa(int idMesero, String nombreMesero, String nombreMesa, String horario, int idAsignacion) {
        this.idMesero = idMesero;
        this.nombreMesero = nombreMesero;
        this.nombreMesa = nombreMesa;
        this.horario = horario;
        this.idAsignacion = idAsignacion;
        this.idOrdenEnEdicion = null;

        labelMesaYHorario.setText("Mesa Asignada: " + nombreMesa + " - " + horario);
        labelMesero.setText(nombreMesero);

        if (filas.isEmpty()) {
            agregarFilaOrden();
        }
    }

    // Editar orden (con prefill)
    public void setDatosMesaParaEditar(int idMesero, String nombreMesero, String nombreMesa,
                                       String horario, int idAsignacion, int idOrdenEditar) {
        setDatosMesa(idMesero, nombreMesero, nombreMesa, horario, idAsignacion);
        this.idOrdenEnEdicion = idOrdenEditar;
        prellenarDesdeOrden(idOrdenEditar);
    }

    @FXML
    public void initialize() {
        cargarCategoriasYPlatillos();

        colCategoria.setCellValueFactory(cell -> cell.getValue().categoriaProperty());
        colPlatillo.setCellValueFactory(cell -> cell.getValue().platilloProperty());
        colCantidad.setCellValueFactory(cell -> cell.getValue().cantidadProperty());

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
                    if (empty) {
                        setGraphic(null);
                    } else {
                        combo.setValue(item);
                        setGraphic(combo);
                    }
                }
            };
        });

        colPlatillo.setCellFactory(param -> {
            final ComboBox<String> combo = new ComboBox<>();
            return new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
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

        colCantidad.setCellFactory(param -> {
            final TextField textField = new TextField();
            return new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        FilaOrden fila = getTableView().getItems().get(getIndex());
                        textField.setText(item != null ? item : "");
                        textField.textProperty().addListener((obs, oldVal, newVal) -> fila.setCantidad(newVal));
                        setGraphic(textField);
                    }
                }
            };
        });

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

        tablaOrden.setItems(filas);
    }

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

    private void prellenarDesdeOrden(int idOrden) {
        filas.clear();
        String sql = "SELECT c.NOMBRE AS categoria, p.NOMBRE AS platillo, d.CANTIDAD " +
                "FROM DETALLE_ORDEN d " +
                "JOIN PLATILLOS p ON p.ID = d.PLATILLO_ID " +
                "JOIN CATEGORIAS c ON c.ID = p.CATEGORIA_ID " +
                "WHERE d.ORDEN_ID = ? ORDER BY d.ID";
        try (Connection con = Conexion.conectar();
             PreparedStatement st = con.prepareStatement(sql)) {
            st.setInt(1, idOrden);
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                FilaOrden f = new FilaOrden();
                f.setCategoria(rs.getString("categoria"));
                f.setPlatillo(rs.getString("platillo"));
                f.setCantidad(String.valueOf(rs.getInt("CANTIDAD")));
                filas.add(f);
            }
            if (filas.isEmpty()) {
                agregarFilaOrden();
            }
            tablaOrden.refresh();
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo prellenar la orden.");
        }
    }

    @FXML
    private void agregarFilaOrden() {
        filas.add(new FilaOrden());
    }

    @FXML
    private void realizarOrden() {
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
            int idOrden;

            if (idOrdenEnEdicion != null) {
                // === EDITANDO una orden existente ===
                idOrden = idOrdenEnEdicion;

                try (PreparedStatement del = con.prepareStatement(
                        "DELETE FROM DETALLE_ORDEN WHERE ORDEN_ID = ?")) {
                    del.setInt(1, idOrden);
                    del.executeUpdate();
                }

            } else {
                // === CREANDO / ACTUALIZANDO una ABIERTA de esta asignación ===
                idOrden = -1;
                String sqlCheck = "SELECT ID FROM ORDENES WHERE ASIGNACION_ID = ? AND ESTADO = 'ABIERTA' " +
                        "ORDER BY FECHA DESC FETCH FIRST 1 ROWS ONLY";
                try (PreparedStatement stmt = con.prepareStatement(sqlCheck)) {
                    stmt.setInt(1, idAsignacion);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) idOrden = rs.getInt("ID");
                }

                if (idOrden == -1) {
                    String sqlOrden = "INSERT INTO ORDENES (MESA_ID, MESERO_ID, FECHA, ESTADO, ASIGNACION_ID) " +
                            "VALUES (?, ?, SYSTIMESTAMP, 'ABIERTA', ?)";
                    try (PreparedStatement ins = con.prepareStatement(sqlOrden, new String[]{"ID"})) {
                        ins.setInt(1, mesaId);
                        ins.setInt(2, idMesero);
                        ins.setInt(3, idAsignacion);
                        ins.executeUpdate();
                        ResultSet rs = ins.getGeneratedKeys();
                        if (rs.next()) idOrden = rs.getInt(1);
                    }
                } else {
                    try (PreparedStatement del = con.prepareStatement(
                            "DELETE FROM DETALLE_ORDEN WHERE ORDEN_ID = ?")) {
                        del.setInt(1, idOrden);
                        del.executeUpdate();
                    }
                }
            }

            String sqlDetalle = "INSERT INTO DETALLE_ORDEN (ORDEN_ID, PLATILLO_ID, CANTIDAD) VALUES (?, ?, ?)";
            try (PreparedStatement det = con.prepareStatement(sqlDetalle)) {
                for (FilaOrden fila : filas) {
                    int platilloId = obtenerIdPlatilloPorNombre(fila.getPlatillo());
                    int cantidad = Integer.parseInt(fila.getCantidad());
                    det.setInt(1, idOrden);
                    det.setInt(2, platilloId);
                    det.setInt(3, cantidad);
                    det.addBatch();
                }
                det.executeBatch();
            }

            con.commit();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/integradora/detalle_orden.fxml"));
            Parent root = loader.load();
            DetalleOrdenController controller = loader.getController();
            String ruta = (idOrdenEnEdicion != null)
                    ? "Mesas Asignadas/Editar Orden " + nombreMesa
                    : "Mesas Asignadas/Orden " + nombreMesa;
            controller.setDatosOrden(idOrden, idMesero, nombreMesero, ruta);

            Stage stage = (Stage) tablaOrden.getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error al guardar", "No se pudo registrar la orden.\n" + e.getMessage());
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

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, mensaje);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

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

    @FXML
    private void volverAtras(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/integradora/mesero.fxml"));
            Parent root = loader.load();
            MeseroController meseroController = loader.getController();
            meseroController.setIdMesero(idMesero, nombreMesero);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo volver atrás.");
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
}
