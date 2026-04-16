package controllers.user_controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.User;
import services.UserService;

import java.sql.SQLException;

public class LoginPage {

    @FXML
    private TextField emailInput;
    @FXML
    private PasswordField passwordInput;
    @FXML
    private Button loginButton;
    @FXML
    private Hyperlink resetPasswordLink;
    @FXML
    private Hyperlink signUpLink;

    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        loginButton.setOnAction(e -> handleLogin());
        signUpLink.setOnAction(e -> navigateTo("/getion_user/signUp_page.fxml", "Sign Up – Studly"));
        resetPasswordLink.setOnAction(e -> showAlert(Alert.AlertType.INFORMATION, "Reset Password",
                "Password reset is not yet implemented."));
    }

    private void handleLogin() {
        String email = emailInput.getText().trim();
        String password = passwordInput.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing Fields",
                    "Please enter both email and password.");
            return;
        }

        try {
            User user = userService.authenticateUser(email, password);

            if (user != null) {
                utils.SessionManager.setCurrentUser(user);

                if (user.getRoles() != null && user.getRoles().contains("ROLE_ADMIN")) {
                    navigateTo("/TEMPLATE/backend_management.fxml", "Admin Dashboard – Studly");
                } else {
                    navigateTo("/TEMPLATE/frontend_dashboard.fxml", "Dashboard – Studly");
                }
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
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
        } catch (Exception ex) {
            System.err.println("Navigation Error Details:");
            ex.printStackTrace();
            if (ex.getCause() != null) {
                System.err.println("Caused by:");
                ex.getCause().printStackTrace();
            }
            String msg = ex.getClass().getSimpleName()
                    + (ex.getCause() != null ? " caused by " + ex.getCause().getClass().getSimpleName() : "");
            showAlert(Alert.AlertType.ERROR, "Navigation Error", msg + "\nCheck console for details.");
        }
    }

    private void showAlert(Alert.AlertType type, String header, String content) {
        Alert alert = new Alert(type);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
