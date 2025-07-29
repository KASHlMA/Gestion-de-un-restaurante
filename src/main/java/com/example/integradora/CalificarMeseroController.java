package com.example.integradora;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class CalificarMeseroController {

    @FXML private ToggleButton star1, star2, star3, star4, star5;
    @FXML private TextField txtNombre;
    @FXML private TextArea txtComentario;
    @FXML private Label labelFecha;
    @FXML private Button btnEnviar;

    private int idMesero; // Recibe el id del mesero calificado
    private int idOrden;  // Recibe la orden asociada

    // Para pasar datos antes de mostrar pantalla
    public void setDatos(int idMesero, int idOrden) {
        this.idMesero = idMesero;
        this.idOrden = idOrden;
    }

    @FXML
    public void initialize() {
        // Poner fecha actual
        labelFecha.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        // Solo permite seleccionar de izquierda a derecha (estilo estrellas)
        ToggleButton[] stars = {star1, star2, star3, star4, star5};
        for (int i = 0; i < stars.length; i++) {
            final int score = i + 1;
            stars[i].setOnAction(e -> {
                for (int j = 0; j < stars.length; j++)
                    stars[j].setSelected(j < score);
            });
        }
    }

    @FXML
    private void enviarCalificacion() {
        int calificacion = 0;
        if (star5.isSelected())      calificacion = 5;
        else if (star4.isSelected()) calificacion = 4;
        else if (star3.isSelected()) calificacion = 3;
        else if (star2.isSelected()) calificacion = 2;
        else if (star1.isSelected()) calificacion = 1;

        String nombre = txtNombre.getText().trim();
        String comentario = txtComentario.getText().trim();

        if (calificacion == 0) {
            mostrarAlerta("Selecciona una calificación con estrellas.");
            return;
        }
        if (nombre.isEmpty()) {
            mostrarAlerta("Por favor, escribe tu nombre.");
            return;
        }

        // Guardar en la base de datos (ajusta nombres de tabla/campos si es necesario)
        String sql = "INSERT INTO CALIFICACIONES (MESERO_ID, NOMBRE_CLIENTE, CALIFICACION, COMENTARIO, FECHA) VALUES (?, ?, ?, ?, SYSDATE)";
        try (Connection con = Conexion.conectar();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, idMesero);
            stmt.setString(2, nombre);
            stmt.setInt(3, calificacion);
            stmt.setString(4, comentario);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("No se pudo guardar la calificación.");
            return;
        }


        mostrarAlerta("¡Gracias por tu calificación!");

        // Regresa a login o cierra la ventana (ajusta el flujo a lo que necesites)
        Stage stage = (Stage) btnEnviar.getScene().getWindow();
        stage.close();
    }

    private void mostrarAlerta(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, mensaje);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
