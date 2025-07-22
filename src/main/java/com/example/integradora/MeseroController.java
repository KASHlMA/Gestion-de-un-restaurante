package com.example.integradora;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class MeseroController {

    @FXML private Label labelNombreMesero;
    @FXML private TableView<MesaAsignada> tablaMesasMesero;
    @FXML private TableColumn<MesaAsignada, String> colMesa;
    @FXML private TableColumn<MesaAsignada, String> colHorario;
    @FXML private TableColumn<MesaAsignada, Void> colAccion;

    private int idMesero; // Se recibe desde el LoginController
    private ObservableList<MesaAsignada> datos = FXCollections.observableArrayList();

    /**
     * Este método lo llama LoginController después del login,
     * y aquí recibes el ID y nombre del mesero autenticado.
     */
    public void setIdMesero(int id, String nombre) {
        this.idMesero = id;
        labelNombreMesero.setText(nombre);
        cargarMesasAsignadas();
    }

    @FXML
    public void initialize() {
        colMesa.setCellValueFactory(new PropertyValueFactory<>("mesa"));
        colHorario.setCellValueFactory(new PropertyValueFactory<>("horario"));

        // Botón "Tomar Orden" en cada fila
        colAccion.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("Tomar Orden");
            {
                btn.setOnAction(event -> {
                    MesaAsignada asignacion = getTableView().getItems().get(getIndex());
                    tomarOrden(asignacion.getMesa(), asignacion.getHorario());
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    /**
     * Consulta la base y llena la tabla con solo las mesas de este mesero.
     */
    private void cargarMesasAsignadas() {
        datos.clear();
        String sql = "SELECT m.NOMBRE AS mesa, " +
                "TO_CHAR(am.HORARIO_INICIO, 'HH12:MI AM') || ' a ' || TO_CHAR(am.HORARIO_FIN, 'HH12:MI AM') AS horario " +
                "FROM ASIGNACIONES_MESAS am " +
                "JOIN MESAS m ON am.MESA_ID = m.ID " +
                "WHERE am.MESERO_ID = ?";
        try (Connection con = Conexion.conectar();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, idMesero);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                datos.add(new MesaAsignada(rs.getString("mesa"), rs.getString("horario")));
            }
            tablaMesasMesero.setItems(datos);
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudieron cargar tus mesas asignadas.");
        }
    }

    /**
     * Método que se llama cuando el mesero da click en "Tomar Orden"
     */
    private void tomarOrden(String mesa, String horario) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/integradora/orden.fxml"));
            Parent root = loader.load();
            OrdenController ordenController = loader.getController();
            ordenController.setDatosMesa(idMesero, labelNombreMesero.getText(), mesa, horario);
            Stage stage = (Stage) tablaMesasMesero.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo abrir la pantalla de orden.");
        }
    }


    /**
     * Método que se llama desde el botón "Cerrar Sesión" del FXML.
     * Regresa a la pantalla de login.
     */
    @FXML
    private void cerrarSesion(javafx.event.ActionEvent event) {
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

    /**
     * Método utilitario para mostrar una alerta de información.
     */
    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, mensaje);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    // Modelo para la tabla
    public static class MesaAsignada {
        private final String mesa;
        private final String horario;
        public MesaAsignada(String mesa, String horario) {
            this.mesa = mesa;
            this.horario = horario;
        }
        public String getMesa() { return mesa; }
        public String getHorario() { return horario; }
    }
}
