package com.example.fx;


import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Load the FXML file
            Parent root = FXMLLoader.load(getClass().getResource("/com/example/fx/register.fxml")); // Ensure the path is correct
            primaryStage.setTitle("User  Registration");
            primaryStage.setScene(new Scene(root, 540, 450)); // Set the scene with the desired dimensions
            primaryStage.setResizable(false); // Optional: Prevent resizing
            primaryStage.show(); // Show the primary stage
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args); // Launch the JavaFX application
    }
}