package controllers;

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
        String firstName = currentUser.getFirst_name() != null ? currentUser.getFirst_name() : "";
        String lastName = currentUser.getLast_name() != null ? currentUser.getLast_name() : "";
        
        avatarSideLabel.setText(firstName + " " + lastName);
        avatarInitials.setText(getInitials(firstName, lastName));
        
        String roles = currentUser.getRoles() != null ? currentUser.getRoles() : "Member";
        roleSideLabel.setText(roles.contains("ROLE_ADMIN") ? "Administrator" : "Student");

        firstNameField.setText(firstName);
        lastNameField.setText(lastName);
        emailField.setText(currentUser.getEmail());
        bioArea.setText(currentUser.getBio());
        jobTitleField.setText(currentUser.getJob_title());
        educationField.setText(currentUser.getEducation_level());
        
        // Format skills for display: ["a","b"] -> a, b
        String rawSkills = currentUser.getSkills();
        if (rawSkills != null && rawSkills.startsWith("[") && rawSkills.endsWith("]")) {
            skillsField.setText(rawSkills.replace("[", "").replace("]", "").replace("\"", "").replace(",", ", "));
        } else {
            skillsField.setText(rawSkills);
        }

        phoneField.setText(currentUser.getPhone_number());
        addressField.setText(currentUser.getAddress());
        websiteField.setText(currentUser.getWebsite());
        
        if (currentUser.getDate_of_birth() != null) {
            dobField.setText(currentUser.getDate_of_birth().toString());
        }
    }

    private String getInitials(String first, String last) {
        String initials = "";
        if (!first.isEmpty()) initials += first.substring(0, 1).toUpperCase();
        if (!last.isEmpty()) initials += last.substring(0, 1).toUpperCase();
        return initials;
    }

    @FXML
    private void handleSaveChanges() {
        try {
            currentUser.setFirst_name(firstNameField.getText());
            currentUser.setLast_name(lastNameField.getText());
            currentUser.setBio(bioArea.getText());
            currentUser.setJob_title(jobTitleField.getText());
            currentUser.setEducation_level(educationField.getText());
            
            // Format skills for DB: a, b -> ["a","b"]
            String inputSkills = skillsField.getText();
            if (inputSkills != null && !inputSkills.trim().isEmpty()) {
                String[] parts = inputSkills.split(",");
                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < parts.length; i++) {
                    json.append("\"").append(parts[i].trim()).append("\"");
                    if (i < parts.length - 1) json.append(",");
                }
                json.append("]");
                currentUser.setSkills(json.toString());
            } else {
                currentUser.setSkills("[]");
            }

            currentUser.setPhone_number(phoneField.getText());
            currentUser.setAddress(addressField.getText());
            currentUser.setWebsite(websiteField.getText());
            currentUser.setUpdated_at(Timestamp.valueOf(LocalDateTime.now()));

            String dobText = dobField.getText().trim();
            if (!dobText.isEmpty()) {
                try {
                    // Strict calendar validation
                    java.time.LocalDate.parse(dobText); 
                    currentUser.setDate_of_birth(Date.valueOf(dobText));
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Invalid Date", "The date '" + dobText + "' is not a valid date. Please use YYYY-MM-DD format.");
                    return;
                }
            }

            userService.modifier(currentUser);
            SessionManager.setCurrentUser(currentUser); // Update session
            
            // Refresh main dashboard header if possible
            FrontendController.getInstance().initialize(); 

            showAlert(Alert.AlertType.INFORMATION, "Success", "Profile updated successfully!");
            handleCancel(); // Return to profile view
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to save changes: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        FrontendController.getInstance().showProfile();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
