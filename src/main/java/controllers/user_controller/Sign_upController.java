package controllers.user_controller;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import models.User;
import services.UserService;
import utils.EmailService;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Sign_upController {

    @FXML private TextField sign_name_id;
    @FXML private TextField sign_lname_id;
    @FXML private TextField sign_email_id;
    @FXML private DatePicker date_id;
    @FXML private PasswordField passw_s_id;
    @FXML private PasswordField pass_s_id;
    @FXML private Button sign_button_id;
    @FXML private Button sendCodeButton;
    @FXML private Hyperlink return_login_id;

    // Verification section
    @FXML private VBox verificationSection;
    @FXML private TextField verificationCodeField;

    // Error labels
    @FXML private Label firstNameError;
    @FXML private Label lastNameError;
    @FXML private Label emailError;
    @FXML private Label dobError;
    @FXML private Label passwordError;
    @FXML private Label confirmPasswordError;
    @FXML private Label verificationCodeError;

    private static final long CODE_EXPIRY_MS = 10 * 60 * 1000L;

    private final UserService userService = new UserService();
    private String generatedCode = null;
    private boolean codeSent = false;
    private long codeGeneratedAt = 0;

    @FXML
    public void initialize() {
        sign_button_id.setOnAction(e -> handleSignUp());
        return_login_id.setOnAction(e -> navigateTo("/getion_user/auth_page.fxml", "Login – Studly"));
    }

    @FXML
    private void handleSendCode() {
        sendVerificationCodeAsync(false);
    }

    private void sendVerificationCodeAsync(boolean silent) {
        String email = sign_email_id.getText().trim();
        resetErrors();

        if (email.isEmpty()) {
            showError(emailError, "Email is required");
            return;
        }
        if (!email.matches("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$")) {
            showError(emailError, "Invalid email format");
            return;
        }

        if (sendCodeButton != null) {
            sendCodeButton.setDisable(true);
            sendCodeButton.setText("Sending…");
        }
        
        generatedCode = EmailService.generateCode();
        codeGeneratedAt = System.currentTimeMillis();

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                EmailService.sendVerificationCode(email, generatedCode);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            codeSent = true;
            verificationSection.setVisible(true);
            verificationSection.setManaged(true);
            if (sendCodeButton != null) {
                sendCodeButton.setText("Resend Code");
                sendCodeButton.setDisable(false);
            }
            if (!silent) {
                showAlert(Alert.AlertType.INFORMATION, "Code Sent",
                    "A 6-digit verification code was sent to " + email + ".\nCheck your inbox.");
            } else {
                // If silent (triggered by Create Account), maybe just show a small notification or label
                showError(verificationCodeError, "Verification code sent to your email!");
            }
        });

        task.setOnFailed(e -> {
            generatedCode = null;
            if (sendCodeButton != null) {
                sendCodeButton.setText("Send Code");
                sendCodeButton.setDisable(false);
            }
            Throwable ex = task.getException();
            showError(emailError, "Failed to send code: " + ex.getMessage());
        });

        new Thread(task, "email-sender").start();
    }

    private void handleSignUp() {
        String firstName = sign_name_id.getText().trim();
        String lastName  = sign_lname_id.getText().trim();
        String email     = sign_email_id.getText().trim();
        LocalDate dobValue = date_id.getValue();
        String password  = passw_s_id.getText().trim();
        String confirm   = pass_s_id.getText().trim();

        resetErrors();
        boolean hasError = false;

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
        if (dobValue == null) {
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

        // Email verification check
        if (!codeSent) {
            // Automatically send code if not sent yet
            sendVerificationCodeAsync(true);
            return; // Wait for user to enter code
        } else {
            String entered = verificationCodeField.getText().trim();
            if (entered.isEmpty()) {
                showError(verificationCodeError, "Please enter the verification code");
                hasError = true;
            } else if (System.currentTimeMillis() - codeGeneratedAt > CODE_EXPIRY_MS) {
                showError(verificationCodeError, "Code expired – request a new one");
                codeSent = false;
                hasError = true;
            } else if (!entered.equals(generatedCode)) {
                showError(verificationCodeError, "Incorrect code");
                hasError = true;
            }
        }

        if (hasError) return;

        User newUser = new User();
        newUser.setFirstName(firstName);
        newUser.setLastName(lastName);
        newUser.setEmail(email);
        newUser.setPassword(utils.PasswordUtil.hash(password));
        newUser.setDateOfBirth(Date.valueOf(dobValue));
        newUser.setRole(new models.Student());
        newUser.setIsVerified(1);   // email was verified
        newUser.setStatut("active");
        newUser.setScore(0);
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        newUser.setCreatedAt(now);
        newUser.setUpdatedAt(now);

        try {
            userService.ajouter(newUser);
            showAlert(Alert.AlertType.INFORMATION, "Account Created",
                "Your account was created successfully! You can now log in.");
            navigateTo("/getion_user/auth_page.fxml", "Login – Studly");
        } catch (SQLException ex) {
            ex.printStackTrace();
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
        Label[] labels = {firstNameError, lastNameError, emailError, dobError,
                          passwordError, confirmPasswordError, verificationCodeError};
        for (Label l : labels) {
            if (l != null) {
                l.setVisible(false);
                l.setManaged(false);
            }
        }
    }

    private void showError(Label label, String message) {
        if (label != null) {
            label.setText(message);
            label.setVisible(true);
            label.setManaged(true);
        }
    }

    private void showAlert(Alert.AlertType type, String header, String content) {
        Alert alert = new Alert(type);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}