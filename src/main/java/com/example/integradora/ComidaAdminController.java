package com.example.integradora;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.cell.PropertyValueFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ComidaAdminController {
    // --- Platillos ---
    @FXML private TableView<Platillo> tablaPlatillos;
    @FXML private TableColumn<Platillo, String> colCategoria;
    @FXML private TableColumn<Platillo, String> colPlatillo;
    @FXML private TableColumn<Platillo, String> colPrecio;
    @FXML private TableColumn<Platillo, String> colEstado;
    @FXML private TableColumn<Platillo, Void> colAcciones;
    @FXML private Button btnAgregarPlatillo;
    @FXML private Button btnAgregarCategoria;

    // --- Categorías ---
    @FXML private TableView<Categoria> tablaCategorias;
    @FXML private TableColumn<Categoria, String> colNombreCategoria;
    @FXML private TableColumn<Categoria, String> colEstadoCategoria;
    @FXML private TableColumn<Categoria, Void> colAccionesCategoria;

    private ObservableList<Platillo> datosPlatillos = FXCollections.observableArrayList();
    private ObservableList<Categoria> datosCategorias = FXCollections.observableArrayList();

    // --- MODELOS ---
    public static class Categoria {
        private final int id;
        private final String nombre;
        private final String estado;
        public Categoria(int id, String nombre, String estado) {
            this.id = id; this.nombre = nombre; this.estado = estado;
        }
        public int getId() { return id; }
        public String getNombre() { return nombre; }
        public String getEstado() { return estado; }
    }
    public static class Platillo {
        private final int id;
        private final String nombre;
        private final double precio;
        private final String estado;
        private final String categoria;
        public Platillo(int id, String nombre, double precio, String estado, String categoria) {
            this.id = id; this.nombre = nombre; this.precio = precio;
            this.estado = estado; this.categoria = categoria;
        }
        public int getId() { return id; }
        public String getNombre() { return nombre; }
        public String getPrecio() { return String.format("%.2f", precio); }
        public String getEstado() { return estado; }
        public String getCategoria() { return categoria; }
    }

    @FXML
    public void initialize() {
        // PLATILLOS
        colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        colPlatillo.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precio"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        cargarPlatillos();

        colAcciones.setCellFactory(param -> new TableCell<>() {
            private final Button btnEditar = new Button("Editar");
            private final Button btnEliminar = new Button("🗑");
            private final HBox box = new HBox(10, btnEditar, btnEliminar);
            {
                btnEditar.setOnAction(e -> abrirEditarPlatillo(getTableView().getItems().get(getIndex())));
                btnEliminar.setOnAction(e -> eliminarPlatillo(getTableView().getItems().get(getIndex())));
                btnEditar.setStyle("-fx-background-color: #D0E9F7; -fx-border-radius: 6; -fx-cursor: hand;");
                btnEliminar.setStyle("-fx-background-color: #FFD6D6; -fx-border-radius: 6; -fx-cursor: hand;");
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        // CATEGORIAS
        colNombreCategoria.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colEstadoCategoria.setCellValueFactory(new PropertyValueFactory<>("estado"));
        cargarCategorias();

        colAccionesCategoria.setCellFactory(param -> new TableCell<>() {
            private final Button btnEditar = new Button("Editar");
            private final Button btnEliminar = new Button("🗑");
            private final HBox box = new HBox(10, btnEditar, btnEliminar);
            {
                btnEditar.setOnAction(e -> abrirEditarCategoria(getTableView().getItems().get(getIndex())));
                btnEliminar.setOnAction(e -> eliminarCategoria(getTableView().getItems().get(getIndex())));
                btnEditar.setStyle("-fx-background-color: #D0E9F7; -fx-border-radius: 6; -fx-cursor: hand;");
                btnEliminar.setStyle("-fx-background-color: #FFD6D6; -fx-border-radius: 6; -fx-cursor: hand;");
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    // --- PLATILLOS ---
    private void cargarPlatillos() {
        datosPlatillos.clear();
        String sql = "SELECT p.ID, p.NOMBRE, p.PRECIO, p.ESTADO, c.NOMBRE AS CATEGORIA FROM PLATILLOS p JOIN CATEGORIAS c ON p.CATEGORIA_ID = c.ID";
        try (Connection con = Conexion.conectar();
             PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                datosPlatillos.add(new Platillo(
                        rs.getInt("ID"),
                        rs.getString("NOMBRE"),
                        rs.getDouble("PRECIO"),
                        rs.getString("ESTADO"),
                        rs.getString("CATEGORIA")
                ));
            }
            tablaPlatillos.setItems(datosPlatillos);
            tablaPlatillos.refresh(); // <--- fuerza refresco visual
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudieron cargar los platillos.");
        }
    }

    @FXML private void abrirAgregarPlatillo() { abrirFormularioPlatillo(null); }
    private void abrirEditarPlatillo(Platillo platillo) { abrirFormularioPlatillo(platillo); }
    private void abrirFormularioPlatillo(Platillo platilloEditar) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/integradora/formulario_platillo.fxml"));
            Parent root = loader.load();
            FormularioPlatilloController controller = loader.getController();
            controller.setDatos(platilloEditar, () -> {
                cargarPlatillos();
                cargarCategorias();
            });
            Stage stage = new Stage();
            stage.setTitle(platilloEditar == null ? "Agregar Platillo" : "Editar Platillo");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo abrir el formulario.");
        }
    }
    private void eliminarPlatillo(Platillo platillo) {
        String sql = "DELETE FROM PLATILLOS WHERE ID = ?";
        try (Connection con = Conexion.conectar();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, platillo.getId());
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                mostrarAlerta("Platillo eliminado", "El platillo fue eliminado exitosamente.");
                cargarPlatillos();
            }
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo eliminar el platillo.");
        }
    }

    // --- CATEGORIAS ---
    private void cargarCategorias() {
        datosCategorias.clear();
        String sql = "SELECT ID, NOMBRE, ESTADO FROM CATEGORIAS";
        try (Connection con = Conexion.conectar();
             PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                datosCategorias.add(new Categoria(
                        rs.getInt("ID"),
                        rs.getString("NOMBRE"),
                        rs.getString("ESTADO")
                ));
            }
            tablaCategorias.setItems(datosCategorias);
            tablaCategorias.refresh();
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudieron cargar las categorías.");
        }
    }

    @FXML private void abrirAgregarCategoria() { abrirFormularioCategoria(null); }
    private void abrirEditarCategoria(Categoria categoria) { abrirFormularioCategoria(categoria); }
    private void abrirFormularioCategoria(Categoria categoriaEditar) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/integradora/formulario_categoria.fxml"));
            Parent root = loader.load();
            FormularioCategoriaController controller = loader.getController();
            controller.setDatos(categoriaEditar, () -> {
                cargarCategorias();
                cargarPlatillos();
            });
            Stage stage = new Stage();
            stage.setTitle(categoriaEditar == null ? "Agregar Categoría" : "Editar Categoría");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo abrir el formulario de categoría.");
        }
    }
    private void eliminarCategoria(Categoria categoria) {
        // Checa relación con platillos antes de eliminar
        String sqlCheck = "SELECT 1 FROM PLATILLOS WHERE CATEGORIA_ID = ?";
        try (Connection con = Conexion.conectar();
             PreparedStatement stmt = con.prepareStatement(sqlCheck)) {
            stmt.setInt(1, categoria.getId());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                mostrarAlerta("No se puede eliminar", "Esta categoría tiene platillos relacionados.");
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo verificar la relación de la categoría.");
            return;
        }
        // Si no tiene platillos relacionados, elimina
        String sql = "DELETE FROM CATEGORIAS WHERE ID = ?";
        try (Connection con = Conexion.conectar();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, categoria.getId());
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                mostrarAlerta("Categoría eliminada", "La categoría fue eliminada exitosamente.");
                cargarCategorias();
                cargarPlatillos();
            }
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo eliminar la categoría.");
        }
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, mensaje);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
