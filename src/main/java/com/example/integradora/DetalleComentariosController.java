package com.example.integradora;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.format.DateTimeFormatter;

public class DetalleComentariosController {

    @FXML private TableView<ComentarioDetalle> tablaComentarios;
    @FXML private TableColumn<ComentarioDetalle, String> colMesero, colCliente, colComentario, colFecha, colCalificacion;
    @FXML private ComboBox<String> comboOrden;

    public static class ComentarioDetalle {
        private final String mesero;
        private final String cliente;
        private final String comentario;
        private final String fecha;
        private final String calificacion;
        public ComentarioDetalle(String mesero, String cliente, String comentario, String fecha, String calificacion) {
            this.mesero = mesero;
            this.cliente = cliente;
            this.comentario = comentario;
            this.fecha = fecha;
            this.calificacion = calificacion;
        }
        public String getMesero() { return mesero; }
        public String getCliente() { return cliente; }
        public String getComentario() { return comentario; }
        public String getFecha() { return fecha; }
        public String getCalificacion() { return calificacion; }
    }

    @FXML
    public void initialize() {
        colMesero.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getMesero()));
        colCliente.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getCliente()));
        colComentario.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getComentario()));
        colFecha.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getFecha()));
        colCalificacion.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getCalificacion()));

        comboOrden.setItems(FXCollections.observableArrayList("Fecha", "Calificación", "Mesero", "Cliente"));
        comboOrden.setValue("Fecha");
        comboOrden.setOnAction(e -> cargarComentarios());
        cargarComentarios();
    }

    private void cargarComentarios() {
        ObservableList<ComentarioDetalle> datos = FXCollections.observableArrayList();
        String ordenSql = switch (comboOrden.getValue()) {
            case "Calificación" -> "C.CALIFICACION DESC";
            case "Mesero" -> "U.NOMBRE";
            case "Cliente" -> "C.NOMBRE_CLIENTE";
            default -> "C.FECHA DESC";
        };

        String sql = "SELECT U.NOMBRE AS MESERO, C.NOMBRE_CLIENTE AS CLIENTE, C.COMENTARIO, C.FECHA, C.CALIFICACION " + "FROM CALIFICACIONES C " + "JOIN USUARIOS U ON C.MESERO_ID = U.ID "  + "ORDER BY " + ordenSql;

        try (Connection con = Conexion.conectar();
             PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yy");
            while (rs.next()) {
                String mesero = rs.getString("MESERO");
                String cliente = rs.getString("CLIENTE");
                String comentario = rs.getString("COMENTARIO");
                String fecha = "";
                java.sql.Date fechaSql = rs.getDate("FECHA");
                if (fechaSql != null) {
                    fecha = fechaSql.toLocalDate().format(dtf);
                }
                double calif = rs.getDouble("CALIFICACION");
                String estrellas = getStars(calif);
                datos.add(new ComentarioDetalle(mesero, cliente, comentario, fecha, estrellas));
            }
        } catch (Exception e) {
        e.printStackTrace();
        System.out.println("Error al cargar comentarios: " + e.getMessage());
    }


        tablaComentarios.setItems(datos);
    }

    private String getStars(double promedio) {
        StringBuilder sb = new StringBuilder();
        int llenas = (int) promedio;
        for (int i = 0; i < llenas; i++) sb.append("★");
        for (int i = llenas; i < 5; i++) sb.append("☆");
        return sb.toString();
    }
}
