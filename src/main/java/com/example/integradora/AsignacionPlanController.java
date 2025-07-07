package com.example.integradora;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Pair;
import java.util.ArrayList;
import java.util.List;
public class AsignacionPlanController {







        @FXML
        private ComboBox<String> meseroComboBox;

        @FXML
        private VBox contenedorMesas; // ✅ nombre correcto, coincide con el FXML


        private final List<HBox> filasMesas = new ArrayList<>();

        @FXML
        public void initialize() {
            // Inicializa con algunos datos de ejemplo
            meseroComboBox.getItems().addAll("Juan Pablo Perez", "Ana López", "Carlos Ruiz");
            agregarFilaMesa();
        }

        @FXML
        public void agregarFilaMesa() {
            ComboBox<String> mesaCombo = new ComboBox<>();
            mesaCombo.getItems().addAll("Mesa 1", "Mesa 2", "Mesa 3", "Mesa 4");
            mesaCombo.setPromptText("Mesa");

            ComboBox<String> horarioCombo = new ComboBox<>();
            horarioCombo.getItems().addAll("8:00 a 11:00 AM", "11:00 a 2:00 PM", "3:00 a 6:00 PM");
            horarioCombo.setPromptText("Horario");

            Button eliminarBtn = new Button("🗑");
            eliminarBtn.setOnAction(e -> eliminarFilaMesa(mesaCombo, horarioCombo, eliminarBtn));

            HBox fila = new HBox(10, mesaCombo, horarioCombo, eliminarBtn);
            filasMesas.add(fila);
            contenedorMesas.getChildren().add(fila); // ✅ CORRECTO
        }

        private void eliminarFilaMesa(ComboBox<String> mesaCombo, ComboBox<String> horarioCombo, Button eliminarBtn) {
            contenedorMesas.getChildren().removeIf(node ->
                    node instanceof HBox && ((HBox) node).getChildren().contains(eliminarBtn)); // ✅ CORRECTO
            filasMesas.removeIf(fila -> fila.getChildren().contains(eliminarBtn));
        }




    @FXML
    private void agregarMesa() {
        // Aquí va la lógica para añadir un HBox con ComboBoxes
        System.out.println("Se añadió una nueva mesa.");
    }


    @FXML
    public void asignarPlan() {
        String meseroSeleccionado = meseroComboBox.getValue();
        if (meseroSeleccionado == null || meseroSeleccionado.isEmpty()) {
            mostrarAlerta("Debe seleccionar un mesero.");
            return;
        }

        for (HBox fila : filasMesas) {
            ComboBox<String> mesaCombo = (ComboBox<String>) fila.getChildren().get(0);
            ComboBox<String> horarioCombo = (ComboBox<String>) fila.getChildren().get(1);

            String mesa = mesaCombo.getValue();
            String horario = horarioCombo.getValue();

            if (mesa == null || horario == null) {
                mostrarAlerta("Debe completar todas las mesas y horarios antes de asignar.");
                return;
            }

            PlanDia nuevoPlan = new PlanDia(meseroSeleccionado, mesa, horario);
            PlanesCompartidos.getInstancia().agregarPlan(nuevoPlan);
        }

        mostrarAlerta("Plan asignado correctamente.");
    }

    private void mostrarAlerta(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}



