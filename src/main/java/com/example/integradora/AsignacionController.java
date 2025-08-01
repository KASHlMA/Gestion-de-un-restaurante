package com.example.integradora;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
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

    @FXML private ComboBox<String> comboMesero;
    @FXML private VBox mesasBox;

    // Guarda cada fila de selección (mesa+horario)
    private final List<HBox> filasMesas = new ArrayList<>();

    private ObservableList<String> mesasDisponibles = FXCollections.observableArrayList();
    private ObservableList<String> horariosDisponibles = FXCollections.observableArrayList(
            "8:00 a 11:00 AM", "11:00 a 3:00 PM", "3:00 a 6:00 PM", "6:00 a 10:00 PM"
    );

    @FXML
    public void initialize() {
        cargarMeseros();
        cargarMesas();
        agregarFilaMesa();
    }

    /** Llena el ComboBox de meseros (solo los activos) desde la base de datos. */
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

    /** Llena la lista de mesas disponibles desde la base de datos. */
    private void cargarMesas() {
        mesasDisponibles.clear();
        String sql = "SELECT NOMBRE FROM MESAS WHERE ESTADO = 'ACTIVO'";
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


    /** Acción del botón "+ Añadir otra mesa". */
    @FXML
    private void agregarFilaMesa() {
        ComboBox<String> comboMesa = new ComboBox<>(mesasDisponibles);
        comboMesa.setPromptText("Mesa");
        comboMesa.setPrefWidth(150);

        ComboBox<String> comboHorario = new ComboBox<>(horariosDisponibles);
        comboHorario.setPromptText("Horario");
        comboHorario.setPrefWidth(150);

        Button btnEliminar = new Button("🗑");
        btnEliminar.setStyle("-fx-background-color: transparent; -fx-font-size: 16px;");

        HBox fila = new HBox(10, comboMesa, comboHorario, btnEliminar);
        fila.setStyle("-fx-alignment: CENTER_LEFT;");

        btnEliminar.setOnAction(e -> {
            mesasBox.getChildren().remove(fila);
            filasMesas.remove(fila);
        });

        mesasBox.getChildren().add(fila);
        filasMesas.add(fila);
    }

    /** Acción del botón "Asignar Plan". Ahora guarda realmente en la base de datos. */
    @FXML
    private void asignarPlan() {
        String meseroNombre = comboMesero.getValue();
        if (meseroNombre == null || meseroNombre.isEmpty()) {
            mostrarAlerta("Selecciona un mesero", "Debes seleccionar un mesero.");
            return;
        }
        if (filasMesas.isEmpty()) {
            mostrarAlerta("Añade al menos una mesa", "Debes asignar al menos una mesa.");
            return;
        }

        Integer meseroId = obtenerIdMeseroPorNombre(meseroNombre);
        if (meseroId == null) {
            mostrarAlerta("Error", "No se encontró el ID del mesero.");
            return;
        }

        boolean exito = true;
        for (HBox fila : filasMesas) {
            ComboBox<?> comboMesa = (ComboBox<?>) fila.getChildren().get(0);
            ComboBox<?> comboHorario = (ComboBox<?>) fila.getChildren().get(1);
            String mesaNombre = (String) comboMesa.getValue();
            String horario = (String) comboHorario.getValue();

            if (mesaNombre == null || horario == null) {
                mostrarAlerta("Completa los campos", "Selecciona una mesa y un horario en cada fila.");
                return;
            }
            Integer mesaId = obtenerIdMesaPorNombre(mesaNombre);
            if (mesaId == null) {
                mostrarAlerta("Error", "No se encontró el ID de la mesa: " + mesaNombre);
                return;
            }

            // Extrae hora inicio y fin (asume formato "8:00 a 11:00 AM")
            String[] partes = horario.split(" a ");
            String horaInicio = partes[0].trim();
            String horaFin = partes[1].trim();

            // Llama al método para guardar la asignación
            boolean insertado = guardarAsignacionEnBase(meseroId, mesaId, horaInicio, horaFin);
            if (!insertado) {
                exito = false;
                break;
            }
        }

        if (exito) {
            mostrarAlerta("Éxito", "¡Plan asignado y guardado en la base de datos!");
            // Limpia selección después de guardar
            comboMesero.getSelectionModel().clearSelection();
            mesasBox.getChildren().clear();
            filasMesas.clear();
            agregarFilaMesa();
        }
    }

    /**
     * Busca el ID del mesero dado su nombre.
     * @return El ID o null si no se encuentra.
     */
    private Integer obtenerIdMeseroPorNombre(String nombre) {
        String sql = "SELECT ID FROM USUARIOS WHERE NOMBRE = ? AND ROL='MESERO' AND ESTADO='ACTIVO'";
        try (Connection con = Conexion.conectar();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, nombre);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("ID");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Busca el ID de la mesa dado su nombre.
     * @return El ID o null si no se encuentra.
     */
    private Integer obtenerIdMesaPorNombre(String nombre) {
        String sql = "SELECT ID FROM MESAS WHERE NOMBRE = ?";
        try (Connection con = Conexion.conectar();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, nombre);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("ID");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Inserta la asignación en la base de datos.
     * @return true si fue exitosa, false si hubo error.
     */
    private boolean guardarAsignacionEnBase(int meseroId, int mesaId, String horaInicio, String horaFin) {
        String sql = "INSERT INTO ASIGNACIONES_MESAS (MESERO_ID, MESA_ID, HORARIO_INICIO, HORARIO_FIN) " +
                "VALUES (?, ?, TO_DATE(?, 'HH12:MI AM'), TO_DATE(?, 'HH12:MI AM'))";
        try (Connection con = Conexion.conectar();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, meseroId);
            stmt.setInt(2, mesaId);
            stmt.setString(3, horaInicio);
            stmt.setString(4, horaFin);
            stmt.executeUpdate();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo guardar la asignación:\n" + e.getMessage());
            return false;
        }
    }

    /** Muestra un mensaje emergente. */
    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, mensaje);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
