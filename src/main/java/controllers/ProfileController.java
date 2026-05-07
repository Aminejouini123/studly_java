package controllers;

import controllers.user_controller.FaceRegistrationController;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import models.User;
import utils.FaceAuthService;
import utils.SessionManager;

public class ProfileController {

    @FXML private Label  bigAvatarInitials;
    @FXML private Circle bigAvatarCircle;
    @FXML private javafx.scene.image.ImageView bigAvatarImage;
    @FXML private Label  profileNameLabel;
    @FXML private Label  profileRoleLabel;
    @FXML private Label  bioLabel;
    @FXML private Label  fullNameValue;
    @FXML private Label  emailValue;
    @FXML private Label  phoneValue;
    @FXML private Label  addressValue;
    @FXML private Label  dobValue;
    @FXML private Label  websiteValue;
    @FXML private Label  educationValue;
    @FXML private Label  skillsValue;
    @FXML private Button editProfileBtn;

    // Face ID controls
    @FXML private Label  faceIdStatusLabel;
    @FXML private Button faceIdActionBtn;

    private FaceAuthService faceAuth;

    @FXML
    public void initialize() {
        try {
            faceAuth = new FaceAuthService();
        } catch (Throwable t) {
            // OpenCV unavailable — disable the Face ID section gracefully
            if (faceIdActionBtn != null) {
                faceIdActionBtn.setDisable(true);
                faceIdActionBtn.setText("Unavailable");
            }
            if (faceIdStatusLabel != null) {
                String errorMsg = t.getMessage();
                if (errorMsg == null) errorMsg = t.getClass().getSimpleName();
                faceIdStatusLabel.setText("Face ID unavailable: " + errorMsg);
                System.err.println("Face ID init error: ");
                t.printStackTrace();
            }
        }
        loadUserProfile();
    }

    private void loadUserProfile() {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        profileNameLabel.setText(user.getFullName());

        if      (user.isAdmin())   profileRoleLabel.setText("Administrator");
        else if (user.isTeacher()) profileRoleLabel.setText("Teacher");
        else                       profileRoleLabel.setText("Student");

        bigAvatarInitials.setText(user.getInitials());

        // Load profile picture
        if (bigAvatarImage != null) {
            String path = user.getProfilePicture();
            if (path != null && !path.isEmpty()) {
                try {
                    if (!path.startsWith("http")) {
                        java.io.File file = new java.io.File(path);
                        if (file.exists()) {
                            bigAvatarImage.setImage(new javafx.scene.image.Image(file.toURI().toString()));
                            bigAvatarInitials.setVisible(false);
                        }
                    } else {
                        bigAvatarImage.setImage(new javafx.scene.image.Image(path));
                        bigAvatarInitials.setVisible(false);
                    }
                } catch (Exception e) {
                    System.err.println("Failed to load profile avatar: " + e.getMessage());
                }
            } else {
                bigAvatarImage.setImage(null);
                bigAvatarInitials.setVisible(true);
            }
        }

        fullNameValue.setText(user.getFullName());
        emailValue.setText(user.getEmail());
        phoneValue.setText(user.getPhoneNumber() != null ? user.getPhoneNumber() : "N/A");
        addressValue.setText(user.getAddress() != null ? user.getAddress() : "N/A");
        dobValue.setText(user.getDateOfBirth() != null ? user.getDateOfBirth().toString() : "N/A");
        websiteValue.setText(user.getWebsite() != null ? user.getWebsite() : "N/A");
        bioLabel.setText((user.getBio() == null || user.getBio().isEmpty())
            ? "No bio added yet." : user.getBio());
        educationValue.setText((user.getEducationLevel() == null || user.getEducationLevel().isEmpty())
            ? "Not specified" : user.getEducationLevel());
        skillsValue.setText(user.getFormattedSkills().isEmpty()
            ? "No skills listed" : user.getFormattedSkills());

        editProfileBtn.setOnAction(e -> FrontendController.getInstance().showEditProfile());

        refreshFaceIdStatus(user.getId());
    }

    // ---- Face ID ----

    private void refreshFaceIdStatus(int userId) {
        if (faceAuth == null || faceIdStatusLabel == null || faceIdActionBtn == null) return;

        boolean registered = faceAuth.hasRegisteredFace(userId);
        faceIdStatusLabel.setStyle(registered
            ? "-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#16a34a;"
            : "-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#1E293B;");
        faceIdStatusLabel.setText(registered ? "✓ Registered" : "Not registered");
        faceIdActionBtn.setText(registered ? "Remove Face ID" : "Register Face ID");
        faceIdActionBtn.setStyle(registered
            ? "-fx-background-color:#dc2626;-fx-text-fill:white;-fx-font-weight:bold;"
              + "-fx-padding:9 20;-fx-background-radius:8;-fx-font-size:13px;-fx-cursor:hand;"
            : "-fx-background-color:#004fb0;-fx-text-fill:white;-fx-font-weight:bold;"
              + "-fx-padding:9 20;-fx-background-radius:8;-fx-font-size:13px;-fx-cursor:hand;");
    }

    @FXML
    private void handleFaceIdAction() {
        User user = SessionManager.getCurrentUser();
        if (user == null || faceAuth == null) return;

        int userId = user.getId();

        if (faceAuth.hasRegisteredFace(userId)) {
            // Remove existing model
            faceAuth.deleteFaceModel(userId);
            refreshFaceIdStatus(userId);
        } else {
            // Open registration dialog
            Stage owner = (Stage) faceIdActionBtn.getScene().getWindow();
            new FaceRegistrationController(userId, faceAuth, () -> refreshFaceIdStatus(userId))
                .show(owner);
        }
    }
}