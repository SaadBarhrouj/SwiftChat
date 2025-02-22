package Entities;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Base64;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import Database.DatabaseConnection;
import java.util.HashMap;
import java.util.Map;

public class User {
    private int id;
    private String name;
    private String email;
    private String password;
    private DataOutputStream dos;
    private DataInputStream dis;
    private Lock lock = new ReentrantLock();
    private static Map<String, DataOutputStream> mapDos = new HashMap<>(); // Stocker les streams des utilisateurs en ligne

    private Connection conn;

    public User(int id, String email, String name, DataOutputStream dos, DataInputStream dis) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.dos = dos;
        this.dis = dis;
        this.conn = DatabaseConnection.getConnection();
        mapDos.put(email, dos); // Ajouter au map des utilisateurs en ligne
    }

    public void lockMe() {
        lock.lock();
    }

    public void unlockMe() {
        lock.unlock();
    }

    public boolean sendMessage(String target, String msg) {
        String[] s = msg.split(":");

        if (!s[0].equals("videoCall") && !s[0].equals("audioCall")) {
            System.out.println(this.getEmail() + " envoie " + s[0] + " à " + target);
        }

        byte[] bytes = new byte[0];

        if (!s[0].equals("text")) {
            try {
                int i = dis.readInt();
                bytes = new byte[i];
                dis.readFully(bytes);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        if (mapDos.containsKey(target)) { // Vérifier si le destinataire est en ligne
            try {
                DataOutputStream targetDos = mapDos.get(target);
                targetDos.writeUTF(msg);
                if (!s[0].equals("text")) {
                    targetDos.writeInt(bytes.length);
                    targetDos.write(bytes);
                }
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Échec d'envoi du message");
                return false;
            }
        }

        // Si l'utilisateur n'est pas en ligne, sauvegarder le message dans la base de données
        String sql;
        if (s[0].equals("text")) {
            sql = "INSERT INTO messages (sender_Email, receiver_Email, message, messageType, date) VALUES (?, ?, ?, ?, ?)";
        } else {
            sql = "INSERT INTO messages (sender_Email, receiver_Email, message, messageType, fileName, date) VALUES (?, ?, ?, ?, ?, ?)";
        }

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, this.getEmail());
            pstmt.setString(2, target);
            pstmt.setString(3, s[0].equals("text") ? s[3] : new String(Base64.getEncoder().encode(bytes)));
            pstmt.setString(4, s[0]);
            if (!s[0].equals("text")) {
                pstmt.setString(5, s[3]);
                pstmt.setString(6, s[2]);
            } else {
                pstmt.setString(5, s[2]);
            }
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public int getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void disconnect() {
       /* mapDos.remove(this);
        for (int i = 0; i < this.myGroups.size(); i++) {
            this.myGroups.get(i).memberDisconnected(this.getId());
            if (this.myGroups.get(i).allOffline()) {
                allGroups.remove(this.myGroups.get(i));
            }
        }
        Iterator<Map.Entry<Profile, DataOutputStream>> iterator = mapDos.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Profile, DataOutputStream> entry = iterator.next();
            entry.getKey().lockMe();
            try {
                entry.getValue().writeUTF("disconnection@@@" + this.getEmail());
            } catch (IOException e) {
                e.printStackTrace();
            }
            entry.getKey().unlockMe();
        }*/
        System.out.println(this.getEmail() + " disconnect");
    }
}
