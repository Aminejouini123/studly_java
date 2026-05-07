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
    @FXML private javafx.scene.image.ImageView avatarImage;
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

        // Load profile picture
        if (currentUser.getProfilePicture() != null && !currentUser.getProfilePicture().isEmpty()) {
            try {
                String path = currentUser.getProfilePicture();
                if (!path.startsWith("http")) {
                    // It's a local path
                    java.io.File file = new java.io.File(path);
                    if (file.exists()) {
                        avatarImage.setImage(new javafx.scene.image.Image(file.toURI().toString()));
                        avatarInitials.setVisible(false);
                    }
                } else {
                    // It's a URL (from Google/GitHub)
                    avatarImage.setImage(new javafx.scene.image.Image(path));
                    avatarInitials.setVisible(false);
                }
            } catch (Exception e) {
                System.err.println("Failed to load profile image: " + e.getMessage());
            }
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
    private void handleChoosePhoto() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Choose Profile Picture");
        fileChooser.getExtensionFilters().addAll(
            new javafx.stage.FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        
        java.io.File selectedFile = fileChooser.showOpenDialog(avatarImage.getScene().getWindow());
        if (selectedFile != null) {
            try {
                // Ensure directory exists
                java.io.File uploadDir = new java.io.File("uploads/profiles");
                if (!uploadDir.exists()) uploadDir.mkdirs();
                
                // Create unique filename
                String fileName = System.currentTimeMillis() + "_" + selectedFile.getName();
                java.io.File destFile = new java.io.File(uploadDir, fileName);
                
                // Copy file
                java.nio.file.Files.copy(selectedFile.toPath(), destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                
                // Update User model and DB path
                String finalPath = destFile.getAbsolutePath();
                currentUser.setProfilePicture(finalPath);
                
                // Update UI
                avatarImage.setImage(new javafx.scene.image.Image(destFile.toURI().toString()));
                avatarInitials.setVisible(false);
                
                System.out.println("Profile picture saved to: " + finalPath);
                
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Upload Error", "Failed to save profile picture: " + e.getMessage());
            }
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
