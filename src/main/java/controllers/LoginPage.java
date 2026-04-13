package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.User;
import services.UserService;

import java.sql.SQLException;
import java.util.List;

public class LoginPage {

    @FXML private TextField email_ID;
    @FXML private PasswordField mdp_id;
    @FXML private Button login_button;
    @FXML private Hyperlink reset_mdp_id;
    @FXML private Hyperlink sign_up_id;

    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        login_button.setOnAction(e -> handleLogin());
        sign_up_id.setOnAction(e -> navigateTo("/signUp_page.fxml", "Sign Up – Studly"));
        reset_mdp_id.setOnAction(e ->
            showAlert(Alert.AlertType.INFORMATION, "Reset Password",
                "Password reset is not yet implemented.")
        );
    }

    private void handleLogin() {
        String email    = email_ID.getText().trim();
        String password = mdp_id.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing Fields",
                "Please enter both email and password.");
            return;
        }

        try {
            List<User> users = userService.recuperer();
            boolean found = users.stream()
                .anyMatch(u -> u.getEmail().equalsIgnoreCase(email)
                            && u.getPassword().equals(password));

            if (found) {
                showAlert(Alert.AlertType.INFORMATION, "Welcome!",
                    "Login successful. Welcome, " + email + "!");
                // TODO: navigate to main dashboard
            } else {
                showAlert(Alert.AlertType.ERROR, "Login Failed",
                    "Invalid email or password. Please try again.");
            }
        } catch (SQLException ex) {
            showAlert(Alert.AlertType.ERROR, "Database Error",
                "Could not reach the database: " + ex.getMessage());
        }
    }

    private void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) login_button.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", ex.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String header, String content) {
        Alert alert = new Alert(type);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
