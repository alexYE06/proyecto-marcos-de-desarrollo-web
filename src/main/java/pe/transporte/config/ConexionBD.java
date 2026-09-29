package pe.transporte.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionBD {

    // 1. Reemplaza 'nombre_de_tu_bd' por el nombre de la BD donde corriste el script CREATE TABLE
    // 2. Reemplaza 'tu_password' por la contraseña de tu usuario postgres
    private static final String URL = "jdbc:postgresql://localhost:5432/bd_terminal";
    private static final String USER = "postgres";
    private static final String PASSWORD = "lafiru123";

    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ No se encontró el driver JDBC de PostgreSQL.");
            e.printStackTrace();
        }
    }

    public static Connection getConexion() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
