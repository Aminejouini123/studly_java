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
    @FXML private Button sign_button_id;
    @FXML private Hyperlink return_login_id;

    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        sign_button_id.setOnAction(e -> handleSignUp());
        return_login_id.setOnAction(e -> navigateTo("/auth_page.fxml", "Login – Studly"));
    }

    private void handleSignUp() {
        String firstName = sign_name_id.getText().trim();
        String lastName  = sign_lname_id.getText().trim();
        String email     = sign_email_id.getText().trim();
        String dobText   = date_id.getText().trim();
        String password  = passw_s_id.getText().trim();
        String confirm   = pass_s_id.getText().trim();

        // --- Validation ---
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()
                || dobText.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing Fields",
                "Please fill in all required fields.");
            return;
        }

        if (!email.matches("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$")) {
            showAlert(Alert.AlertType.WARNING, "Invalid Email",
                "Please enter a valid email address.");
            return;
        }

        if (!password.equals(confirm)) {
            showAlert(Alert.AlertType.WARNING, "Password Mismatch",
                "Passwords do not match. Please try again.");
            return;
        }

        if (password.length() < 6) {
            showAlert(Alert.AlertType.WARNING, "Weak Password",
                "Password must be at least 6 characters long.");
            return;
        }

        Date dob;
        try {
            dob = Date.valueOf(LocalDate.parse(dobText));
        } catch (DateTimeParseException ex) {
            showAlert(Alert.AlertType.WARNING, "Invalid Date",
                "Date of birth must be in YYYY-MM-DD format.");
            return;
        }

        // --- Build User ---
        User newUser = new User();
        newUser.setFirst_name(firstName);
        newUser.setLast_name(lastName);
        newUser.setEmail(email);
        newUser.setPassword(password);
        newUser.setDate_of_birth(dob);
        newUser.setRoles("ROLE_USER");
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
            navigateTo("/auth_page.fxml", "Login – Studly");
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

    private void showAlert(Alert.AlertType type, String header, String content) {
        Alert alert = new Alert(type);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
