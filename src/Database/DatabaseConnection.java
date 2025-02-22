package Database;

import java.sql.*;

public class DatabaseConnection {
    private Connection conn;
    private Statement stmt;

    public DatabaseConnection(String url, String user, String password) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            this.conn = DriverManager.getConnection(url, user, password);
            this.stmt = this.conn.createStatement();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            System.out.println("Erreur de connexion à la base de données");
        }
    }

    public Statement getStatement() {
        return this.stmt;
    }

    public Connection getConnection() {
        return this.conn;
    }

    public void close() {
        try {
            if (this.stmt != null) this.stmt.close();
            if (this.conn != null) this.conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
