package controllers.user_controller;

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

    @FXML
    private TextField firstNameField;
    @FXML
    private TextField lastNameField;
    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private ChoiceBox<String> roleChoiceBox;

    private final UserService userService = new UserService();
    private ListUserController listUserController;

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
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();
        String role = roleChoiceBox.getValue();

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing Fields", "Please fill in all fields.");
            return;
        }

        User newUser = new User();
        newUser.setFirst_name(firstName);
        newUser.setLast_name(lastName);
        newUser.setEmail(email);
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
        Stage stage = (Stage) firstNameField.getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
