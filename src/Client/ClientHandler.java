package Client;

import Server.ServerChat;
import java.io.*;
import java.net.Socket;
import java.sql.*;
import java.util.Base64;

/**
 * ClientHandler class handles communication between the server and a connected client.
 * It extends the Thread class to enable concurrent handling of multiple clients.
 * Each client is assigned a separate thread to allow simultaneous interactions.
 */
public class ClientHandler extends Thread {
    private final DataInputStream dis; // Input stream to receive data from the client
    private final DataOutputStream dos; // Output stream to send data to the client
    private final Socket commthread; // Socket for client communication
    private Connection conn; // Database connection
    private Statement stmt; // SQL statement execution
    private ResultSet rs; // Result set for database queries
    private String msg; // Message received from the client
    private String receiver; // Receiver of the message
    private boolean Auth;
    private UserAccount userAccount;

    /**
     * Constructor to initialize the client handler with the client's socket and streams.
     *
     * @param s    Client socket for communication
     * @param diss Data input stream to receive data
     * @param doss Data output stream to send data
     */
    public ClientHandler(Socket s, DataInputStream diss, DataOutputStream doss) {
        this.commthread = s;
        this.dis = diss;
        this.dos = doss;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            this.conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/SwiftChat", "root", "");
            this.stmt = this.conn.createStatement();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            System.out.println("Erreur base de données");
        }
    }

    /**
     * Overrides the run method to handle client communication in a separate thread.
     * The method sends a welcome message and logs the client's IP address.
     */
    @Override
    public void run() {
        try {
            while (true) {
                String choice = "";
                do {
                    this.dos.writeUTF("=======  Menu  ===================");
                    this.dos.writeUTF("========   a.Register ================");
                    this.dos.writeUTF("========     b.Login  ================");
                    this.dos.writeUTF("========      c.quitter ==============");
                    choice = this.dis.readLine();
                    String name, email, password;

                    switch (choice) {
                        case "a":
                            this.dos.writeUTF("Entrer votre nom :");
                            name = this.dis.readLine();
                            this.dos.writeUTF("Entrer votre email:");
                            email = this.dis.readLine();
                            this.dos.writeUTF("Entrer votre mot de passe:");
                            password = this.dis.readLine();
                            register(name, email, password);
                            break;
                        case "b":
                            this.dos.writeUTF("Entrer votre email:");
                            email = this.dis.readLine();
                            this.dos.writeUTF("Entrer votre mot de passe:");
                            password = this.dis.readLine();
                            Auth = login(email, password);
                            break;
                    }
                } while (!Auth);

                if (Auth) {
                    try {
                        String sql = "SELECT * FROM messages WHERE receiver_Email='" + this.userAccount.getEmail() + "'";
                        rs = stmt.executeQuery(sql);
                        while (rs.next()) {
                            if (rs.getString("messageType").equals("text"))
                                dos.writeUTF(rs.getString("messageType") + "@@@" + rs.getString("sender_Email") + "@@@" + rs.getString("date") + "@@@" + rs.getString("message"));
                            else {
                                dos.writeUTF(rs.getString("messageType") + "@@@" + rs.getString("sender_Email") + "@@@" + rs.getString("date") + "@@@" + rs.getString("fileName"));
                                byte[] bytes = Base64.getDecoder().decode(rs.getString("message"));
                                this.dos.writeInt(bytes.length);
                                this.dos.write(bytes);
                            }
                        }
                    } catch (SQLException | IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean login(String login, String password) {
        try {
            String sql = "SELECT * FROM users WHERE email = '" + login + "' AND password = '" + password + "'";
            this.rs = this.stmt.executeQuery(sql);
            if (this.rs.next()) {
                if (rs.getString("password").equals(password)) {
                    this.dos.writeUTF("Login Successful");
                    System.out.println(login + " : Connexion reussie");
                    this.userAccount = new UserAccount(this.rs.getInt("user_id"), this.rs.getString("email"),
                            this.rs.getString("name"), this.dos, this.dis);
                    return true;
                }
            }
            this.dos.writeUTF("Login Failed");
            System.out.println(login + " : Echec de connexion");
            return false;
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean register(String name, String email, String password) {
        try {
            String sql = "SELECT * FROM users WHERE email = '" + email + "'";
            this.rs = this.stmt.executeQuery(sql);
            if (this.rs.next()) {
                this.dos.writeUTF("Email already used");
                System.out.println(email + " deja utilise");
                return false;
            } else {
                sql = "INSERT INTO users (name, email, password) VALUES ('" + name + "','" + email + "','" + password + "')";
                this.stmt.executeUpdate(sql);
                this.dos.writeUTF("Registration Successful");
                System.out.println("Inscription reussie");
                return true;
            }
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
