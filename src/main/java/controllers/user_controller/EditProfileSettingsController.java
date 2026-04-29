package controllers.user_controller;

import controllers.FrontendController;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import models.User;
import services.UserService;
import utils.SessionManager;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class EditProfileSettingsController {

    @FXML private Label avatarSideLabel;
    @FXML private Label roleSideLabel;
    @FXML private Label avatarInitials;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextArea bioArea;
    @FXML private TextField jobTitleField;
    @FXML private TextField educationField;
    @FXML private TextField skillsField;
    @FXML private TextField phoneField;
    @FXML private TextField dobField;
    @FXML private TextField addressField;
    @FXML private TextField websiteField;

    private final UserService userService = new UserService();
    private User currentUser;

    @FXML
    public void initialize() {
        currentUser = SessionManager.getCurrentUser();
        if (currentUser != null) {
            loadUserData();
        }
    }

    private void loadUserData() {
        avatarSideLabel.setText(currentUser.getFullName());
        avatarInitials.setText(currentUser.getInitials());
        
        roleSideLabel.setText(currentUser.isAdmin() ? "Administrator" : "Student");

        firstNameField.setText(currentUser.getFirstName());
        lastNameField.setText(currentUser.getLastName());
        emailField.setText(currentUser.getEmail());
        bioArea.setText(currentUser.getBio());
        jobTitleField.setText(currentUser.getJobTitle());
        educationField.setText(currentUser.getEducationLevel());
        skillsField.setText(currentUser.getFormattedSkills());
        phoneField.setText(currentUser.getPhoneNumber());
        addressField.setText(currentUser.getAddress());
        websiteField.setText(currentUser.getWebsite());
        
        if (currentUser.getDateOfBirth() != null) {
            dobField.setText(currentUser.getDateOfBirth().toString());
        }
    }

    @FXML
    private void handleSaveChanges() {
        try {
            currentUser.setFirstName(firstNameField.getText());
            currentUser.setLastName(lastNameField.getText());
            currentUser.setBio(bioArea.getText());
            currentUser.setJobTitle(jobTitleField.getText());
            currentUser.setEducationLevel(educationField.getText());
            currentUser.setFormattedSkills(skillsField.getText());
            currentUser.setPhoneNumber(phoneField.getText());
            currentUser.setAddress(addressField.getText());
            currentUser.setWebsite(websiteField.getText());
            currentUser.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));

            String dobText = dobField.getText().trim();
            if (!dobText.isEmpty()) {
                try {
                    // Strict calendar validation
                    java.time.LocalDate.parse(dobText); 
                    currentUser.setDateOfBirth(Date.valueOf(dobText));
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Invalid Date", "The date '" + dobText + "' is not a valid date. Please use YYYY-MM-DD format.");
                    return;
                }
            }

            userService.modifier(currentUser);
            SessionManager.setCurrentUser(currentUser); // Update session
            
            // Refresh main dashboard header if possible
            if (FrontendController.getInstance() != null) {
                FrontendController.getInstance().refreshUserHeader();
            }

            showAlert(Alert.AlertType.INFORMATION, "Success", "Profile updated successfully!");
            handleCancel(); // Return to profile view
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to save changes: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        if (FrontendController.getInstance() != null) {
            FrontendController.getInstance().showProfile();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
