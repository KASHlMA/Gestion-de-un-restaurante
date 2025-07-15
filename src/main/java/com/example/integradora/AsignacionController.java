package com.example.integradora;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.Button;
import javafx.scene.control.Alert;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class AsignacionController {

    // Combo para seleccionar el mesero
    @FXML private ComboBox<String> comboMesero;
    // VBox donde se agregan las filas de mesas dinámicamente
    @FXML private VBox mesasBox;

    // Opcional: almacena todas las filas para referencia
    private final List<HBox> filasMesas = new ArrayList<>();

    // Lista de mesas y horarios posibles
    private ObservableList<String> mesasDisponibles = FXCollections.observableArrayList();
    private ObservableList<String> horariosDisponibles = FXCollections.observableArrayList(
            "8:00 a 11:00 AM", "11:00 a 3:00 PM", "3:00 a 6:00 PM", "6:00 a 10:00 PM"
    );

    @FXML
    public void initialize() {
        // Llenar el ComboBox de meseros al iniciar
        cargarMeseros();
        // Llenar la lista de mesas disponibles
        cargarMesas();

        // Al iniciar, agrega una fila por defecto
        agregarFilaMesa();
    }

    /**
     * Llenar el ComboBox de meseros desde la base de datos.
     */
    private void cargarMeseros() {
        ObservableList<String> meseros = FXCollections.observableArrayList();
        String sql = "SELECT NOMBRE FROM USUARIOS WHERE ROL='MESERO' AND ESTADO='ACTIVO'";
        try (Connection con = Conexion.conectar();
             PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                meseros.add(rs.getString("NOMBRE"));
            }
            comboMesero.setItems(meseros);
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudieron cargar los meseros.");
        }
    }

    /**
     * Llenar la lista de mesas disponibles desde la base.
     */
    private void cargarMesas() {
        mesasDisponibles.clear();
        String sql = "SELECT NOMBRE FROM MESAS";
        try (Connection con = Conexion.conectar();
             PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                mesasDisponibles.add(rs.getString("NOMBRE"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudieron cargar las mesas.");
        }
    }

    /**
     * Maneja el botón "+ Añadir otra mesa". Agrega una nueva fila de selección.
     */
    @FXML
    private void agregarFilaMesa() {
        // ComboBox para mesa y horario
        ComboBox<String> comboMesa = new ComboBox<>(mesasDisponibles);
        comboMesa.setPromptText("Mesa");
        comboMesa.setPrefWidth(150);

        ComboBox<String> comboHorario = new ComboBox<>(horariosDisponibles);
        comboHorario.setPromptText("Horario");
        comboHorario.setPrefWidth(150);

        // Botón para eliminar la fila
        Button btnEliminar = new Button("🗑");
        btnEliminar.setStyle("-fx-background-color: transparent; -fx-font-size: 16px;");
        // Elimina la fila de la vista y la lista
        HBox fila = new HBox(10, comboMesa, comboHorario, btnEliminar);
        fila.setStyle("-fx-alignment: CENTER_LEFT;");

        btnEliminar.setOnAction(e -> {
            mesasBox.getChildren().remove(fila);
            filasMesas.remove(fila);
        });

        mesasBox.getChildren().add(fila);
        filasMesas.add(fila);
    }

    /**
     * Maneja el botón "Asignar Plan".
     * Aquí deberías implementar la lógica para guardar en la base de datos.
     */
    @FXML
    private void asignarPlan() {
        String mesero = comboMesero.getValue();
        if (mesero == null || mesero.isEmpty()) {
            mostrarAlerta("Selecciona un mesero", "Debes seleccionar un mesero.");
            return;
        }
        if (filasMesas.isEmpty()) {
            mostrarAlerta("Añade al menos una mesa", "Debes asignar al menos una mesa.");
            return;
        }

        // Aquí recolectas los datos seleccionados
        List<String> mesasSeleccionadas = new ArrayList<>();
        List<String> horariosSeleccionados = new ArrayList<>();
        for (HBox fila : filasMesas) {
            ComboBox<?> comboMesa = (ComboBox<?>) fila.getChildren().get(0);
            ComboBox<?> comboHorario = (ComboBox<?>) fila.getChildren().get(1);
            String mesa = (String) comboMesa.getValue();
            String horario = (String) comboHorario.getValue();

            if (mesa == null || horario == null) {
                mostrarAlerta("Completa los campos", "Selecciona una mesa y un horario en cada fila.");
                return;
            }
            mesasSeleccionadas.add(mesa);
            horariosSeleccionados.add(horario);
        }

        // Aquí pondrías la lógica real para guardar las asignaciones en tu base de datos
        // Por ahora solo muestra un mensaje de éxito
        mostrarAlerta("Éxito", "¡Plan asignado correctamente!");

        // (Opcional) Limpia la selección después de guardar
        comboMesero.getSelectionModel().clearSelection();
        mesasBox.getChildren().clear();
        filasMesas.clear();
        agregarFilaMesa();
    }

    /**
     * Muestra un mensaje emergente.
     */
    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, mensaje);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
