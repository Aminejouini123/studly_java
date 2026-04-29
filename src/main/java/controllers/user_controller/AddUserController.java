package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import models.User;
import services.UserService;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class AddUserController {

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneNumberField;
    @FXML private PasswordField passwordField;
    @FXML private ChoiceBox<String> roleChoiceBox;

    private final UserService userService = new UserService();
    private ListUserController listUserController;

    // E.164: + followed by 2..15 digits (first digit 1..9)
    private static final String E164_REGEX = "^\\+[1-9]\\d{1,14}$";

    @FXML
    public void initialize() {
        roleChoiceBox.getItems().addAll("ROLE_USER", "ROLE_ADMIN");
        roleChoiceBox.setValue("ROLE_USER");
    }

    public void setListUserController(ListUserController controller) {
        this.listUserController = controller;
    }

    @FXML
    private void handleSave() {
        String firstName = safeTrim(firstNameField);
        String lastName = safeTrim(lastNameField);
        String email = safeTrim(emailField);
        String phoneNumber = safeTrim(phoneNumberField);
        String password = passwordField == null ? "" : passwordField.getText().trim();
        String role = roleChoiceBox == null ? null : roleChoiceBox.getValue();

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing Fields", "Please fill in all fields.");
            return;
        }

        if (phoneNumber.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing Phone Number", "Please enter a phone number (format: +33612345678).");
            return;
        }
        if (!phoneNumber.matches(E164_REGEX)) {
            showAlert(Alert.AlertType.WARNING, "Invalid Phone Number", "Phone number must be in E.164 format (example: +33612345678).");
            return;
        }

        if (role == null || role.trim().isEmpty()) {
            role = "ROLE_USER";
        }

        User newUser = new User();
        newUser.setFirst_name(firstName);
        newUser.setLast_name(lastName);
        newUser.setEmail(email);
        newUser.setPhone_number(phoneNumber);
        newUser.setPassword(password);
        newUser.setRoles("[\"" + role + "\"]");
        newUser.setStatut("Active");
        newUser.setIs_verified(1);

        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        newUser.setCreated_at(now);
        newUser.setUpdated_at(now);

        try {
            userService.ajouter(newUser);
            showAlert(Alert.AlertType.INFORMATION, "Success", "User added successfully!");

            if (listUserController != null) {
                listUserController.refresh();
            }

            closeStage();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not add user: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        closeStage();
    }

    private void closeStage() {
        if (firstNameField == null) return;
        Stage stage = (Stage) firstNameField.getScene().getWindow();
        stage.close();
    }

    private static String safeTrim(TextField field) {
        return field == null || field.getText() == null ? "" : field.getText().trim();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

