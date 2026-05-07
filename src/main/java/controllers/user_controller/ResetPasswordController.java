package controllers.user_controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import services.UserService;
import utils.PasswordUtil;

import java.sql.SQLException;

public class
ResetPasswordController {

    @FXML private TextField emailField;
    @FXML private TextField tokenField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button resetButton;
    @FXML private Hyperlink backToLoginLink;
    @FXML private Label tokenError;
    @FXML private Label passwordError;
    @FXML private Label confirmError;

    private final UserService userService = new UserService();
    private long tokenSentAt = 0;

    /** Called by LoginPage immediately after loading this scene. */
    public void initData(String email, long tokenSentAt) {
        emailField.setText(email);
        this.tokenSentAt = tokenSentAt;
    }

    @FXML
    private void handleReset() {
        resetErrors();

        String token    = tokenField.getText().trim();
        String newPwd   = newPasswordField.getText();
        String confirm  = confirmPasswordField.getText();
        boolean hasError = false;

        // Token field validation
        if (token.isEmpty()) {
            showError(tokenError, "Please enter the reset code");
            hasError = true;
        } else if (tokenSentAt > 0 && System.currentTimeMillis() - tokenSentAt > 30 * 60 * 1000L) {
            showError(tokenError, "Code expired – request a new one from the login page");
            hasError = true;
        }

        // Password validation
        if (newPwd.isEmpty()) {
            showError(passwordError, "New password is required");
            hasError = true;
        } else if (newPwd.length() < 6) {
            showError(passwordError, "Min 6 characters required");
            hasError = true;
        }

        if (confirm.isEmpty()) {
            showError(confirmError, "Please confirm the new password");
            hasError = true;
        } else if (!newPwd.equals(confirm)) {
            showError(confirmError, "Passwords do not match");
            hasError = true;
        }

        if (hasError) return;

        // Validate token against DB
        try {
            models.User user = userService.findByResetToken(token);
            if (user == null || !user.getEmail().equalsIgnoreCase(emailField.getText().trim())) {
                showError(tokenError, "Invalid reset code");
                return;
            }

            // Update password and clear the token
            userService.updatePassword(user.getId(), PasswordUtil.hash(newPwd));

            showAlert(Alert.AlertType.INFORMATION, "Password Updated",
                "Your password has been reset. Please log in with your new password.");
            navigateTo("/getion_user/auth_page.fxml", "Login – Studly");

        } catch (SQLException ex) {
            showAlert(Alert.AlertType.ERROR, "Database Error",
                "Could not update password: " + ex.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        navigateTo("/getion_user/auth_page.fxml", "Login – Studly");
    }

    private void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) resetButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error", ex.getMessage());
        }
    }

    private void resetErrors() {
        for (Label l : new Label[]{tokenError, passwordError, confirmError}) {
            l.setVisible(false);
            l.setManaged(false);
        }
    }

    private void showError(Label label, String message) {
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void showAlert(Alert.AlertType type, String header, String content) {
        Alert alert = new Alert(type);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}