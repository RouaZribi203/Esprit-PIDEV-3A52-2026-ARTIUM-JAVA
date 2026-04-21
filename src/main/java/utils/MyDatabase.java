package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class  MyDatabase {
    private   final String URl = "jdbc:mysql://localhost:3306/artium_db";
    private final String USERNAME = "root";
    private final String PASSWORD = "";
    private Connection connection;
    private  static MyDatabase instance ;

    public static MyDatabase getInstance() {
        if (instance == null) {
            instance = new MyDatabase();
        }
        return instance;
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(URl, USERNAME, PASSWORD);
            }
        } catch (SQLException e) {
            System.err.println("Database connection lost/failed: " + e.getMessage());
        }
        return connection;
    }

    private MyDatabase() {
        try {
            connection = DriverManager.getConnection(URl,USERNAME,PASSWORD);
            System.out.println("Connected to database successfully");
        } catch (SQLException e) {
            System.err.println("Failed to connect to database: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
