module com.example.integradora {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens com.example.integradora to javafx.fxml;
    exports com.example.integradora;
}
