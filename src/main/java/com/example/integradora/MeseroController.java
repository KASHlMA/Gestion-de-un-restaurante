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
    @FXML private TableColumn<MesaAsignada, Void> colTomarOrden;
    @FXML private TableColumn<MesaAsignada, Void> colCambios;
    @FXML private TableColumn<MesaAsignada, Void> colCerrarCuenta;

    private int idMesero;
    private ObservableList<MesaAsignada> datos = FXCollections.observableArrayList();

    // Se llama después del login
    public void setIdMesero(int id, String nombre) {
        this.idMesero = id;
        labelNombreMesero.setText(nombre);
        cargarMesasAsignadas();
    }

    @FXML
    public void initialize() {
        colMesa.setCellValueFactory(new PropertyValueFactory<>("mesa"));
        colHorario.setCellValueFactory(new PropertyValueFactory<>("horario"));

        // Botón "Tomar Orden"
        colTomarOrden.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("Tomar Orden");
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                MesaAsignada asignacion = getTableView().getItems().get(getIndex());
                btn.setDisable(asignacion.ordenEstado != null && !asignacion.ordenEstado.equals("ABIERTA"));
                btn.setOnAction(e -> tomarOrden(asignacion));
                setGraphic(btn);
            }
        });

        // Botón "Realizar Cambios"
        colCambios.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("Realizar Cambios");
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                MesaAsignada asignacion = getTableView().getItems().get(getIndex());
                btn.setDisable(asignacion.ordenEstado == null || !asignacion.ordenEstado.equals("ENVIADA"));
                btn.setOnAction(e -> solicitarCambio(asignacion));
                setGraphic(btn);
            }
        });

        // Botón "Cerrar Cuenta"
        colCerrarCuenta.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("Cerrar Cuenta");
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                MesaAsignada asignacion = getTableView().getItems().get(getIndex());
                btn.setDisable(asignacion.ordenEstado == null || !asignacion.ordenEstado.equals("ENVIADA"));
                btn.setOnAction(e -> cerrarCuenta(asignacion));
                setGraphic(btn);
            }
        });
    }

    /**
     * Llena la tabla con las mesas asignadas y consulta el estado de orden de cada una.
     */
    private void cargarMesasAsignadas() {
        datos.clear();
        String sql = "SELECT m.NOMBRE AS mesa, " +
                "TO_CHAR(am.HORARIO_INICIO, 'HH12:MI AM') || ' a ' || TO_CHAR(am.HORARIO_FIN, 'HH12:MI AM') AS horario, " +
                "(SELECT o.ESTADO FROM ORDENES o WHERE o.MESA_ID = m.ID AND o.MESERO_ID = ? ORDER BY o.FECHA DESC FETCH FIRST 1 ROWS ONLY) AS ORDEN_ESTADO " +
                "FROM ASIGNACIONES_MESAS am " +
                "JOIN MESAS m ON am.MESA_ID = m.ID " +
                "WHERE am.MESERO_ID = ?";
        try (Connection con = Conexion.conectar();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, idMesero);
            stmt.setInt(2, idMesero);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                datos.add(new MesaAsignada(
                        rs.getString("mesa"),
                        rs.getString("horario"),
                        rs.getString("ORDEN_ESTADO"))
                );
            }
            tablaMesasMesero.setItems(datos);
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudieron cargar tus mesas asignadas.");
        }
    }


    // Abrir pantalla para tomar orden
    private void tomarOrden(MesaAsignada asignacion) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/integradora/orden.fxml"));
            Parent root = loader.load();
            OrdenController ordenController = loader.getController();
            ordenController.setDatosMesa(idMesero, labelNombreMesero.getText(), asignacion.getMesa(), asignacion.getHorario());
            Stage stage = (Stage) tablaMesasMesero.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo abrir la pantalla de orden.");
        }
    }

    // Lógica para solicitar cambios
    private void solicitarCambio(MesaAsignada asignacion) {
        mostrarAlerta("Solicitud de Cambios", "Aquí iría la lógica para solicitar cambios.");
    }

    /**
     * Cambia la orden ENVIADA a CERRADA y muestra el resumen final.
     */
    private void cerrarCuenta(MesaAsignada asignacion) {
        try (Connection con = Conexion.conectar()) {
            // 1. Buscar el ID de la mesa
            int mesaId = obtenerIdMesaPorNombre(asignacion.getMesa());

            // 2. Buscar el ID de la orden ENVIADA (que aún no está cerrada)
            int idOrden = -1;
            String sqlOrden = "SELECT ID FROM ORDENES WHERE MESA_ID = ? AND MESERO_ID = ? AND ESTADO = 'ENVIADA'";
            try (PreparedStatement stmt = con.prepareStatement(sqlOrden)) {
                stmt.setInt(1, mesaId);
                stmt.setInt(2, idMesero);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) idOrden = rs.getInt("ID");
            }

            if (idOrden == -1) {
                mostrarAlerta("Error", "No se encontró una orden enviada para esta mesa.");
                return;
            }

            // 3. Cambia el estado de la orden a "CERRADA"
            String sqlCerrar = "UPDATE ORDENES SET ESTADO = 'CERRADA' WHERE ID = ?";
            try (PreparedStatement stmt = con.prepareStatement(sqlCerrar)) {
                stmt.setInt(1, idOrden);
                stmt.executeUpdate();
            }

            // 4. Navega al resumen/finalización
            mostrarResumenFinal(idOrden);

            // 5. Actualiza la tabla de mesas (opcional, ya que cambia de pantalla)
            // cargarMesasAsignadas();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo cerrar la cuenta.");
        }
    }

    /**
     * Muestra la pantalla de resumen/finalización de la orden
     */
    private void mostrarResumenFinal(int idOrden) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/integradora/finalizar_orden.fxml"));
            Parent root = loader.load();

            FinalizarOrdenController controller = loader.getController();
            controller.setDatosOrden(idOrden, idMesero, labelNombreMesero.getText(), "Mesas Asignadas/Resumen Final");

            Stage stage = (Stage) tablaMesasMesero.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo mostrar el resumen final.");
        }
    }

    private int obtenerIdMesaPorNombre(String nombreMesa) throws Exception {
        String sql = "SELECT ID FROM MESAS WHERE NOMBRE = ?";
        try (Connection con = Conexion.conectar();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, nombreMesa);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt("ID");
            else throw new Exception("No se encontró la mesa: " + nombreMesa);
        }
    }

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

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, mensaje);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    // Clase modelo con estado de la orden
    public static class MesaAsignada {
        private final String mesa;
        private final String horario;
        private final String ordenEstado;
        public MesaAsignada(String mesa, String horario, String ordenEstado) {
            this.mesa = mesa;
            this.horario = horario;
            this.ordenEstado = ordenEstado; // "ABIERTA", "ENVIADA", "CERRADA"...
        }
        public String getMesa() { return mesa; }
        public String getHorario() { return horario; }
        public String getOrdenEstado() { return ordenEstado; }
    }
}
