package com.example.integradora;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DashboardCalificacionesController {

    @FXML private Label lblPromedio, lblComentarios, lblMejorMesero, lblMejorCalificacion;
    @FXML private TableView<RankingMesero> tablaRanking;
    @FXML private TableColumn<RankingMesero, String> colNombre, colEstrellas, colPromedio, colComentario;
    @FXML private ListView<String> listaComentarios;

    public static class RankingMesero {
        private final String nombre;
        private final String estrellas;
        private final String promedio;
        private final String comentario;
        public RankingMesero(String nombre, String estrellas, String promedio, String comentario) {
            this.nombre = nombre; this.estrellas = estrellas;
            this.promedio = promedio; this.comentario = comentario;
        }
        public String getNombre() { return nombre; }
        public String getEstrellas() { return estrellas; }
        public String getPromedio() { return promedio; }
        public String getComentario() { return comentario; }
    }

    @FXML
    public void initialize() {
        colNombre.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getNombre()));
        colEstrellas.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getEstrellas()));
        colPromedio.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getPromedio()));
        colComentario.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getComentario()));
        cargarResumen();
    }

    private void cargarResumen() {
        try (Connection con = Conexion.conectar()) {
            // Promedio
            try (PreparedStatement st = con.prepareStatement("SELECT ROUND(AVG(CALIFICACION),1) FROM CALIFICACIONES")) {
                ResultSet rs = st.executeQuery();
                if (rs.next()) lblPromedio.setText(rs.getString(1));
            }
            // Comentarios recibidos
            try (PreparedStatement st = con.prepareStatement("SELECT COUNT(*) FROM CALIFICACIONES")) {
                ResultSet rs = st.executeQuery();
                if (rs.next()) lblComentarios.setText(rs.getString(1));
            }
            // Mejor mesero
            try (PreparedStatement st = con.prepareStatement(
                    "SELECT U.NOMBRE, ROUND(AVG(C.CALIFICACION),1) AS PROMEDIO " +
                            "FROM CALIFICACIONES C JOIN USUARIOS U ON C.MESERO_ID = U.ID " +
                            "GROUP BY U.NOMBRE ORDER BY PROMEDIO DESC FETCH FIRST 1 ROW ONLY")) {
                ResultSet rs = st.executeQuery();
                if (rs.next()) {
                    lblMejorMesero.setText(rs.getString("NOMBRE"));
                    lblMejorCalificacion.setText(rs.getString("PROMEDIO"));
                }
            }
            // Ranking meseros
            ObservableList<RankingMesero> lista = FXCollections.observableArrayList();
            try (PreparedStatement st = con.prepareStatement(
                    "SELECT U.NOMBRE, ROUND(AVG(C.CALIFICACION),1) AS PROMEDIO, " +
                            "(SELECT COMENTARIO FROM CALIFICACIONES C2 WHERE C2.MESERO_ID = U.ID AND ROWNUM = 1 AND C2.COMENTARIO IS NOT NULL) AS COMENTARIO " +
                            "FROM CALIFICACIONES C JOIN USUARIOS U ON C.MESERO_ID = U.ID GROUP BY U.NOMBRE, U.ID ORDER BY PROMEDIO DESC")) {
                ResultSet rs = st.executeQuery();
                while (rs.next()) {
                    double promedio = rs.getDouble("PROMEDIO");
                    String estrellas = estrellasUnicode(promedio);
                    lista.add(new RankingMesero(
                            rs.getString("NOMBRE"),
                            estrellas,
                            String.valueOf(promedio),
                            rs.getString("COMENTARIO") != null ? rs.getString("COMENTARIO") : ""
                    ));
                }
            }
            tablaRanking.setItems(lista);

            // Comentarios recientes
            ObservableList<String> comentarios = FXCollections.observableArrayList();
            try (PreparedStatement st = con.prepareStatement(
                    "SELECT COMENTARIO FROM CALIFICACIONES  ORDER BY FECHA DESC FETCH FIRST 5 ROWS ONLY")) {
                ResultSet rs = st.executeQuery();
                while (rs.next()) comentarios.add(rs.getString("COMENTARIO"));
            }
            listaComentarios.setItems(comentarios);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String estrellasUnicode(double promedio) {
        StringBuilder sb = new StringBuilder();
        int llenas = (int) promedio;
        for (int i = 0; i < llenas; i++) sb.append("★");
        for (int i = llenas; i < 5; i++) sb.append("☆");
        return sb.toString();
    }

    @FXML
    private void abrirDetalleComentarios() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/integradora/detalle_comentarios.fxml"));
            Parent root = loader.load();

            // dueño para que quede encima de la ventana principal (opcional pero recomendado)
            Stage owner = (Stage) tablaRanking.getScene().getWindow();

            Stage stage = new Stage();
            stage.setTitle("Comentarios Detallados");
            stage.initOwner(owner);
            stage.initModality(javafx.stage.Modality.WINDOW_MODAL); // si quieres que sea modal

            Scene scene = new Scene(root);
            stage.setScene(scene);

            // Permite que el root crezca al maximizar
            if (root instanceof javafx.scene.layout.Region r) {
                r.setMinSize(javafx.scene.layout.Region.USE_COMPUTED_SIZE, javafx.scene.layout.Region.USE_COMPUTED_SIZE);
                r.setPrefSize(javafx.scene.layout.Region.USE_COMPUTED_SIZE, javafx.scene.layout.Region.USE_COMPUTED_SIZE);
                r.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                r.prefWidthProperty().bind(scene.widthProperty());
                r.prefHeightProperty().bind(scene.heightProperty());
            }

            // Maximiza el diálogo
            stage.setMaximized(true);
            // algunos entornos aplican mejor si lo fuerzas al mostrarse
            stage.setOnShown(ev -> stage.setMaximized(true));

            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
