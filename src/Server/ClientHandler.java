package Server;

import Dao.MessageDAO;
import Entities.User;
import Entities.Group;
import Dao.GroupDAO;
import Dao.UserDAO;
import Dao.DatabaseConnection;

import java.io.*;
import java.net.Socket;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ClientHandler extends Thread {
    private final DataInputStream dis;
    private final DataOutputStream dos;
    private final Socket commthread;
    private boolean Auth;
    private User userAccount;
    private UserDAO userDAO;
    private GroupDAO groupDAO;
    private MessageDAO messageDAO;
    private Connection conn;
    private Statement stmt;
    private ResultSet rs;

    public ClientHandler(Socket s, DataInputStream diss, DataOutputStream doss) {
        this.commthread = s;
        this.dis = diss;
        this.dos = doss;
        this.Auth = false;
        this.userDAO = new UserDAO();
        this.groupDAO = new GroupDAO();
        this.messageDAO = new MessageDAO();
        this.conn = DatabaseConnection.getConnection();
        if (this.conn != null) {
            try {
                this.stmt = this.conn.createStatement();
                this.start();
            } catch (SQLException e) {
                e.printStackTrace();
                error();
            }
        } else {
            error();
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                String choice = "";
                do {
                    this.showMenu();
                    choice = this.dis.readLine(); ;

                    String name, email, password;
                    switch (choice) {
                        case "a":
                            this.dos.writeUTF("Veuillez entrer votre nom :");
                            name = this.dis.readLine(); ;
                            this.dos.writeUTF("Veuillez entrer votre email :");
                            email = this.dis.readLine();
                            this.dos.writeUTF("Veuillez entrer votre mot de passe :");
                            password = this.dis.readLine(); ;
                            register(name, email, password);
                            break;
                        case "b":
                            this.dos.writeUTF("Veuillez entrer votre email :");
                            email = this.dis.readLine();
                            this.dos.writeUTF("Veuillez entrer votre mot de passe :");
                            password = this.dis.readLine(); ;
                            Auth = login(email, password);
                            break;
                        default:
                            this.dos.writeUTF("Choix invalide, veuillez réessayer");
                    }
                } while (!Auth);

                if (Auth) {

                    receiveAndDeleteMessages();
                    // receiveAndDeleteMessages();
                    userMenu();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            error();
        } finally {
            try {
                if (dis != null) dis.close();
                if (dos != null) dos.close();
                if (commthread != null && !commthread.isClosed()) commthread.close();
                if (conn != null && !conn.isClosed()) conn.close();
            } catch (IOException | SQLException e) {
                e.printStackTrace();
            }
        }
    }

}