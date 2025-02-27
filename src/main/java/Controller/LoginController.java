package Controller;

import Dao.UserDAO;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Hyperlink registerLink;

    @FXML
    private void initialize() {
        // Redirect to Register Page
        registerLink.setOnAction(event -> {
            try {
                // Debug the path
                System.out.println("Loading FXML from: " + getClass().getResource("/com/example/fx/register.fxml"));

                // Load the register.fxml file
                Parent root = FXMLLoader.load(getClass().getResource("/com/example/fx/register.fxml"));
                Stage stage = (Stage) registerLink.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Error loading register.fxml: " + e.getMessage());
            }
        });
    }
    @FXML
    private void handleLogin() {
        String email = emailField.getText();
        String password = passwordField.getText();

        UserDAO userDAO = new UserDAO();
        try {
            if (userDAO.getUserByEmailAndPassword(email, password).next()) {
                System.out.println("Login successful!");
                // Redirect to the main application window or dashboard
            } else {
                System.out.println("Invalid email or password.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}