package controllers;

import controllers.courses.BaseCourseController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import models.User;
import utils.SessionManager;

import java.io.IOException;
import java.net.URL;

public class FrontendController extends BaseCourseController {

    @FXML private Label dashboardNavLabel;
    @FXML private Label planningNavLabel;
    @FXML private Label coursesNavLabel;
    @FXML private Label groupsNavLabel;
    @FXML private StackPane contentHost;

    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;
    @FXML private Label avatarInitials;
    @FXML private Circle avatarCircle;
    @FXML private Circle profileAvatar;

    private static FrontendController instance;

    public FrontendController() {
        instance = this;
    }

    public static FrontendController getInstance() {
        return instance;
    }

    @FXML
    public void initialize() {
        instance = this;
        setActiveNav(dashboardNavLabel);
        refreshUserHeader();
    }

    public void loadContent(String resourcePath) {
        try {
            URL resource = getClass().getResource(resourcePath);
            if (resource == null) {
                throw new IOException("Missing FXML resource: " + resourcePath);
            }
            FXMLLoader loader = new FXMLLoader(resource);
            Node content = loader.load();
            loadContentNode(content);
        } catch (IOException e) {
            e.printStackTrace();
            javafx.application.Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Load Error");
                alert.setHeaderText("Unable to load component");
                alert.setContentText("Resource: " + resourcePath + "\nError: " + e.getMessage());
                alert.showAndWait();
            });
        }
    }

    public void loadContentNode(Node node) {
        if (contentHost == null || node == null) return;
        contentHost.getChildren().setAll(node);
    }

    @FXML
    public void showDashboard() {
        if (contentHost != null) {
            contentHost.getChildren().clear();
        }
        setActiveNav(dashboardNavLabel);
    }

    @FXML
    public void showPlanning() {
        loadContent("/Gestion de temps/planning_dashboard.fxml");
        setActiveNav(planningNavLabel);
    }

    @FXML
    public void showGroups() {
        loadContent("/gestion_group/groups_dashboard.fxml");
        setActiveNav(groupsNavLabel);
    }

    @FXML
    public void showInvitations() {
        loadContent("/gestion_group/invitations_inbox.fxml");
        setActiveNav(groupsNavLabel);
    }

    @FXML
    public void showProfile() {
        loadContent("/getion_user/profile.fxml");
    }

    public void showEditProfile() {
        loadContent("/getion_user/edit_profile_settings.fxml");
    }

    // Some older code uses this name.
    public void refreshUserInfo() {
        refreshUserHeader();
    }

    public void refreshUserHeader() {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        String firstName = user.getFirst_name() != null ? user.getFirst_name() : "";
        String lastName = user.getLast_name() != null ? user.getLast_name() : "";
        String fullName = (firstName + " " + lastName).trim();

        if (userNameLabel != null) {
            userNameLabel.setText(fullName.isEmpty() ? "User" : fullName);
        }

        String roles = user.getRoles() != null ? user.getRoles() : "";
        if (userRoleLabel != null) {
            if (roles.contains("ROLE_ADMIN")) userRoleLabel.setText("Administrator");
            else if (roles.contains("ROLE_TEACHER")) userRoleLabel.setText("Teacher");
            else userRoleLabel.setText("Student");
        }

        if (avatarInitials != null) {
            String initials = "";
            if (!firstName.isEmpty()) initials += firstName.substring(0, 1).toUpperCase();
            if (!lastName.isEmpty()) initials += lastName.substring(0, 1).toUpperCase();
            avatarInitials.setText(initials.isEmpty() ? "U" : initials);
        }

        Circle targetCircle = avatarCircle != null ? avatarCircle : profileAvatar;
        if (targetCircle != null) {
            if (roles.contains("ROLE_ADMIN")) targetCircle.setStyle("-fx-fill: #ef4444;");
            else if (roles.contains("ROLE_TEACHER")) targetCircle.setStyle("-fx-fill: #f59e0b;");
            else targetCircle.setStyle("-fx-fill: #004fb0;");
        }
    }

    @FXML
    public void goToCourses(MouseEvent event) {
        goToCourses((javafx.event.Event) event);
    }

    @FXML
    public void showGroups(MouseEvent event) {
        showGroups();
    }

    @FXML
    public void showInvitations(MouseEvent event) {
        showInvitations();
    }

    @Override
    public void goToCourses(javafx.event.Event event) {
        loadContent("/gestion_cours/courses_body.fxml");
        setActiveNav(coursesNavLabel);
    }

    @Override
    public void goToDashboard(javafx.event.Event event) {
        showDashboard();
    }

    private void setActiveNav(Label activeLabel) {
        if (dashboardNavLabel != null) dashboardNavLabel.getStyleClass().remove("active");
        if (planningNavLabel != null) planningNavLabel.getStyleClass().remove("active");
        if (coursesNavLabel != null) coursesNavLabel.getStyleClass().remove("active");
        if (groupsNavLabel != null) groupsNavLabel.getStyleClass().remove("active");
        if (activeLabel != null && !activeLabel.getStyleClass().contains("active")) {
            activeLabel.getStyleClass().add("active");
        }
    }
}

