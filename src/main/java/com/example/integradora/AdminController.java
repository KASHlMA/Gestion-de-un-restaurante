package com.example.integradora;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import java.sql.*;

public class AdminController {

    @FXML private VBox contenidoCentral;

    // ----- Cambiar sección central -----
    private void setContenidoCentral(String rutaFXML) {
        try {
            Parent nodo = FXMLLoader.load(getClass().getResource(rutaFXML));
            contenidoCentral.getChildren().setAll(nodo);
        } catch (Exception e) {
            e.printStackTrace();
            contenidoCentral.getChildren().setAll(new Label("No se pudo cargar la pantalla."));
        }
    }

    // Si quieres cargar la vista inicial, usa initialize
    @FXML
    public void initialize() {
        mostrarMesas(); // Carga mesas_admin.fxml al inicio
    }

    @FXML
    private void mostrarMesas() {
        setContenidoCentral("/com/example/integradora/mesas_admin.fxml");
    }


    @FXML
    private void mostrarUsuarios() {
        setContenidoCentral("/com/example/integradora/usuarios_admin.fxml");
    }

    @FXML
    private void mostrarComida() {
        setContenidoCentral("/com/example/integradora/comida_admin.fxml");
    }

    @FXML
    private void mostrarDashboard() {
        setContenidoCentral("/com/example/integradora/admin_dashboard.fxml");
    }

    @FXML
    private void cerrarSesion() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/integradora/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) contenidoCentral.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo volver al login.");
        }
    }

    // ----- Utilidad -----
    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR, mensaje);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.showAndWait();
    }


    @FXML
    private void mostrarReportes() {
        // Temporal, puedes dejarlo vacío o mostrar un mensaje.
        System.out.println("Botón Reportes presionado.");
        // O muestra un Alert:
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Aquí iría la pantalla de Reportes para Admin.");
        alert.setHeaderText(null);
        alert.showAndWait();
    }

}
