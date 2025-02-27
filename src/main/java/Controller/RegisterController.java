package Controller;

import Dao.UserDAO;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;

import java.io.IOException;

public class RegisterController {

    @FXML
    private TextField usernameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button registerButton;
    @FXML
    private Hyperlink loginLink;


    @FXML
    private void initialize() {
        // Redirect to Login Page
        loginLink.setOnAction(event -> {
            try {
                Parent root = FXMLLoader.load(getClass().getResource("/com/example/fx/login.fxml"));
                Stage stage = (Stage) loginLink.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        // Register Button Action
        registerButton.setOnAction(event -> handleRegister());
    }

    private UserDAO userDAO;

    public RegisterController() {
        userDAO = new UserDAO(); // Initialize UserDAO
    }



    private void registerUser () {
        String name = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showAlert("Error", "All fields must be filled out.");
            return;
        }

        if (userDAO.userExists(email)) {
            showAlert("Error", "Email is already in use.");
            return;
        }

        if (userDAO.insertUser (name, email, password)) {
            showAlert("Success", "Registration successful!");
            clearFields();
        } else {
            showAlert("Error", "Registration failed. Please try again.");
        }
    }

    private void clearFields() {
        usernameField.clear();
        emailField.clear();
        passwordField.clear();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleRegister() {
        String name = usernameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();

        UserDAO userDAO = new UserDAO();
        if (userDAO.userExists(email)) {
            System.out.println("Email already exists.");
        } else {
            if (userDAO.insertUser(name, email, password)) {
                System.out.println("Registration successful!");
            } else {
                System.out.println("Registration failed.");
            }
        }
    }

}