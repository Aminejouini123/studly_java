package controllers.user_controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.shape.Circle;
import models.User;
import utils.SessionManager;
import controllers.FrontendController;

public class ProfileController {

    @FXML private Label bigAvatarInitials;
    @FXML private Circle bigAvatarCircle;
    @FXML private Label profileNameLabel;
    @FXML private Label profileRoleLabel;
    @FXML private Label bioLabel;
    @FXML private Label fullNameValue;
    @FXML private Label emailValue;
    @FXML private Label phoneValue;
    @FXML private Label addressValue;
    @FXML private Label dobValue;
    @FXML private Label websiteValue;
    @FXML private Label educationValue;
    @FXML private Label skillsValue;
    @FXML private Button editProfileBtn;

    @FXML
    public void initialize() {
        loadUserProfile();
    }

    private void loadUserProfile() {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        // Header
        String firstName = user.getFirst_name() != null ? user.getFirst_name() : "";
        String lastName = user.getLast_name() != null ? user.getLast_name() : "";
        profileNameLabel.setText(firstName + " " + lastName);
        
        String roles = user.getRoles() != null ? user.getRoles() : "Member";
        if (roles.contains("ROLE_ADMIN")) {
            profileRoleLabel.setText("Administrator");
        } else if (roles.contains("ROLE_TEACHER")) {
            profileRoleLabel.setText("Teacher");
        } else {
            profileRoleLabel.setText("Student");
        }

        // Initials
        String initials = "";
        if (!firstName.isEmpty()) initials += firstName.substring(0, 1).toUpperCase();
        if (!lastName.isEmpty()) initials += lastName.substring(0, 1).toUpperCase();
        bigAvatarInitials.setText(initials);

        // Personal Information
        fullNameValue.setText(firstName + " " + lastName);
        emailValue.setText(user.getEmail());
        phoneValue.setText(user.getPhone_number() != null ? user.getPhone_number() : "N/A");
        addressValue.setText(user.getAddress() != null ? user.getAddress() : "N/A");
        dobValue.setText(user.getDate_of_birth() != null ? user.getDate_of_birth().toString() : "N/A");
        websiteValue.setText(user.getWebsite() != null ? user.getWebsite() : "N/A");
        
        // About & Other
        bioLabel.setText((user.getBio() == null || user.getBio().isEmpty()) ? "No bio added yet." : user.getBio());
        educationValue.setText((user.getEducation_level() == null || user.getEducation_level().isEmpty()) ? "Not specified" : user.getEducation_level());
        skillsValue.setText((user.getSkills() == null || user.getSkills().isEmpty()) ? "No skills listed" : user.getSkills());

        // Button Action
        editProfileBtn.setOnAction(e -> {
            FrontendController.getInstance().showEditProfile();
        });
    }
}
