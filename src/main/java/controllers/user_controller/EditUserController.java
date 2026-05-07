package controllers.user_controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.User;
import services.UserService;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class EditUserController {

    @FXML private Label formTitle;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private ChoiceBox<String> roleChoiceBox;

    private final UserService userService = new UserService();
    private ListUserController listUserController;
    private User currentUser;

    @FXML
    public void initialize() {
        roleChoiceBox.getItems().addAll("ROLE_USER", "ROLE_ADMIN");
        formTitle.setText("Edit User");
    }

    public void setListUserController(ListUserController controller) {
        this.listUserController = controller;
    }

    public void setUserData(User user) {
        this.currentUser = user;
        firstNameField.setText(user.getFirstName());
        lastNameField.setText(user.getLastName());
        emailField.setText(user.getEmail());
        
        if (user.isAdmin()) roleChoiceBox.setValue("ROLE_ADMIN");
        else roleChoiceBox.setValue("ROLE_USER");
        
        passwordField.setPromptText("Leave empty to keep current password");
    }

    @FXML
    private void handleSave() {
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();
        String role = roleChoiceBox.getValue();

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing Fields", "First name, last name, and email are required.");
            return;
        }

        currentUser.setFirstName(firstName);
        currentUser.setLastName(lastName);
        currentUser.setEmail(email);
        currentUser.setRoles("[\"" + role + "\"]");
        
        // Only update password if a new one is provided
        if (!password.isEmpty()) {
            currentUser.setPassword(utils.PasswordUtil.hash(password));
        }
        
        currentUser.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));

        try {
            userService.modifier(currentUser);
            showAlert(Alert.AlertType.INFORMATION, "Success", "User updated successfully!");
            
            if (listUserController != null) {
                listUserController.refresh();
            }
            
            closeStage();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not update user: " + e.getMessage());
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
