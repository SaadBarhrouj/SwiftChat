package Server.persistence;

import java.io.*;

public class SerializationManager {
    public static void serialize(Object obj, String filePath) throws IOException {
        System.out.println("[SERIALIZE] Writing to " + filePath);
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(filePath))) {
            oos.writeObject(obj);
        }
    }

    public static Object deserialize(String filePath)
            throws IOException, ClassNotFoundException {
        System.out.println("[DESERIALIZE] Reading from " + filePath);
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(filePath))) {
            return ois.readObject();
        }
    }
}