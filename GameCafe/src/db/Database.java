package db;

import java.sql.*;

public class Database {
    private static final String URL = "jdbc:sqlite:cafe.db";

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static void init() {
        try (Connection conn = connect()) {
            Statement stmt = conn.createStatement();

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player (
                    id INTEGER PRIMARY KEY,
                    money INTEGER
                )
            """);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}