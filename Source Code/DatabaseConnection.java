import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/medical_inventory";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "";

    // This MUST return a Connection, not a boolean
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found", e);
        }
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }

    // Test connection returns boolean
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            System.out.println("Database connection test successful!");
            return true;
        } catch (SQLException e) {
            System.out.println("Database connection test failed: " + e.getMessage());
            return false;
        }
    }
}
