package Controller;

import Dao.UserDAO;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class RegisterController {

    @FXML
    private TextField usernameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button registerButton;

    private UserDAO userDAO;

    public RegisterController() {
        userDAO = new UserDAO(); // Initialize UserDAO
    }

    @FXML
    private void initialize() {
        registerButton.setOnAction(event -> registerUser ());
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

}