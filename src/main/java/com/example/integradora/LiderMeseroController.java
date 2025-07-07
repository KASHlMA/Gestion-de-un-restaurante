package com.example.integradora;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import java.io.IOException;

public class LiderMeseroController {

    @FXML private TableView<PlanDia> tablaMesas;
    @FXML private TableColumn<PlanDia, String> columnaMesas;
    @FXML private TableColumn<PlanDia, String> columnaMesero;
    @FXML private TableColumn<PlanDia, String> columnaHorario;
    @FXML private VBox contenidoCentral;

    private final ObservableList<PlanDia> datos = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        columnaMesas.setCellValueFactory(new PropertyValueFactory<>("mesa"));
        columnaMesero.setCellValueFactory(new PropertyValueFactory<>("mesero"));
        columnaHorario.setCellValueFactory(new PropertyValueFactory<>("horario"));

        // Datos simulados
        datos.add(new PlanDia("Mesa 1", "Carlos", "10:00 - 12:00"));
        datos.add(new PlanDia("Mesa 2", "Ana", "12:00 - 14:00"));
        datos.add(new PlanDia("Mesa 3", "Luis", "14:00 - 16:00"));

        tablaMesas.setItems(datos);
    }

    @FXML
    private void cerrarSesion() {
        System.out.println("Cerrando sesión...");
        // Aquí puedes agregar código para volver al login
    }

    @FXML
    private void mostrarAsignacionPlan() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/integradora/AsignacionPlan.fxml"));
            Node vistaAsignacion = loader.load();
            contenidoCentral.getChildren().setAll(vistaAsignacion);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
