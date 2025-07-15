package com.example.integradora; // Ajusta al paquete real donde la pongas

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Conexion {
    private static final String UBICACION_WALLET = "C:\\Users\\HUAWEI\\IdeaProjects\\INTEGRADORA\\src\\wallet";
    private static final String JDBC_URL = "jdbc:oracle:thin:@kashima_high"; // Usa el alias de tu tnsnames.ora
    private static final String USER = "ADMIN";
    private static final String PASS = "Knoxotics_Kashima50";

    static {
        System.setProperty("oracle.net.tns_admin", UBICACION_WALLET);
    }

    public static Connection conectar() throws SQLException, ClassNotFoundException {
        Class.forName("oracle.jdbc.OracleDriver");
        return DriverManager.getConnection(JDBC_URL, USER, PASS);
    }
}
