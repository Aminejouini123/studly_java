package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.User;
import services.UserService;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

public class Sign_upController {

    @FXML private TextField sign_name_id;
    @FXML private TextField sign_lname_id;
    @FXML private TextField sign_email_id;
    @FXML private TextField date_id;
    @FXML private PasswordField passw_s_id;
    @FXML private PasswordField pass_s_id;
    @FXML private Label firstNameError;
    @FXML private Label lastNameError;
    @FXML private Label emailError;
    @FXML private Label dobError;
    @FXML private Label passwordError;
    @FXML private Label confirmPasswordError;
    @FXML private Button sign_button_id;
    @FXML private Hyperlink return_login_id;

    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        sign_button_id.setOnAction(e -> handleSignUp());
        return_login_id.setOnAction(e -> navigateTo("/getion_user/auth_page.fxml", "Login – Studly"));
    }

    private void handleSignUp() {
        String firstName = sign_name_id.getText().trim();
        String lastName  = sign_lname_id.getText().trim();
        String email     = sign_email_id.getText().trim();
        String dobText   = date_id.getText().trim();
        String password  = passw_s_id.getText().trim();
        String confirm   = pass_s_id.getText().trim();

        // --- Reset Errors ---
        resetErrors();

        boolean hasError = false;

        // --- Validation ---
        if (firstName.isEmpty()) {
            showError(firstNameError, "First name is required");
            hasError = true;
        }

        if (lastName.isEmpty()) {
            showError(lastNameError, "Last name is required");
            hasError = true;
        }

        if (email.isEmpty()) {
            showError(emailError, "Email is required");
            hasError = true;
        } else if (!email.matches("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$")) {
            showError(emailError, "Invalid email format");
            hasError = true;
        }

        if (dobText.isEmpty()) {
            showError(dobError, "Date of birth is required");
            hasError = true;
        }

        if (password.isEmpty()) {
            showError(passwordError, "Password is required");
            hasError = true;
        } else if (password.length() < 6) {
            showError(passwordError, "Min 6 characters required");
            hasError = true;
        }

        if (confirm.isEmpty()) {
            showError(confirmPasswordError, "Please confirm password");
            hasError = true;
        } else if (!password.equals(confirm)) {
            showError(confirmPasswordError, "Passwords do not match");
            hasError = true;
        }

        if (hasError) return;

        Date dob;
        try {
            dob = Date.valueOf(LocalDate.parse(dobText));
        } catch (DateTimeParseException ex) {
            showError(dobError, "Format: YYYY-MM-DD");
            return;
        }

        // --- Build User ---
        User newUser = new User();
        newUser.setFirst_name(firstName);
        newUser.setLast_name(lastName);
        newUser.setEmail(email);
        newUser.setPassword(password);
        newUser.setDate_of_birth(dob);
        newUser.setRoles("[\"ROLE_USER\"]");
        newUser.setIs_verified(0);
        newUser.setStatut("active");
        newUser.setScore(0);
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        newUser.setCreated_at(now);
        newUser.setUpdated_at(now);

        // --- Persist ---
        try {
            userService.ajouter(newUser);
            showAlert(Alert.AlertType.INFORMATION, "Account Created",
                "Your account was created successfully! You can now log in.");
            navigateTo("/getion_user/auth_page.fxml", "Login – Studly");
        } catch (SQLException ex) {
            showAlert(Alert.AlertType.ERROR, "Database Error",
                "Could not create account: " + ex.getMessage());
        }
    }

    private void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) sign_button_id.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", ex.getMessage());
        }
    }

    private void resetErrors() {
        Label[] labels = {firstNameError, lastNameError, emailError, dobError, passwordError, confirmPasswordError};
        for (Label l : labels) {
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
