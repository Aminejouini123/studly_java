package controllers;

import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;
import controllers.courses.BaseCourseController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
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
    @FXML private Label recommendationsNavLabel;
    @FXML private Label roadmapNavLabel;
    @FXML private StackPane contentHost;
    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;
    @FXML private Label avatarInitials;
    @FXML private Circle avatarCircle; // Linked to Circle in FXML
    @FXML private Circle profileAvatar; // Alias for consistency if needed

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
        refreshUserInfo();
    }

    public void refreshUserInfo() {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        if (userNameLabel != null) {
            String fullName = user.getFullName();
            userNameLabel.setText(fullName.trim().isEmpty() ? "User" : fullName);
        }

        if (userRoleLabel != null) {
            if (user.isAdmin()) {
                userRoleLabel.setText("Administrator");
            } else if (user.isTeacher()) {
                userRoleLabel.setText("Teacher");
            } else {
                userRoleLabel.setText("Student");
            }
        }

        if (avatarInitials != null) {
            String initials = user.getInitials();
            avatarInitials.setText(initials.isEmpty() ? "U" : initials);
        }
        
        // Set profile avatar color based on role
        Circle targetCircle = avatarCircle != null ? avatarCircle : profileAvatar;
        if (targetCircle != null) {
            if (user.isAdmin()) {
                targetCircle.setStyle("-fx-fill: #ef4444;");
            } else if (user.isTeacher()) {
                targetCircle.setStyle("-fx-fill: #f59e0b;");
            } else {
                targetCircle.setStyle("-fx-fill: #004fb0;");
            }
        }
    }

    /**
     * @deprecated Use refreshUserInfo()
     */
    public void refreshUserHeader() {
        refreshUserInfo();
    }

    @FXML
    private void showDashboard() {
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

    @Override
    public void goToCourses(javafx.event.Event event) {
        loadContent("/gestion_cours/courses_body.fxml");
        setActiveNav(coursesNavLabel);
    }

    @Override
    public void goToDashboard(javafx.event.Event event) {
        showDashboard();
    }

    @FXML
    public void showRecommendations() {
        loadContent("/recommendations/recommendations.fxml");
        setActiveNav(recommendationsNavLabel);
    }

    @FXML
    public void showRoadmap() {
        loadContent("/roadmap/RoadmapView.fxml");
        setActiveNav(roadmapNavLabel);
    }

    @FXML
    public void showProfile() {
        loadContent("/getion_user/profile.fxml");
        setActiveNav(null); // No nav label for profile
    }

    public void showEditProfile() {
        loadContent("/getion_user/edit_profile_settings.fxml");
        setActiveNav(null);
    }

    public void loadContent(String resourcePath) {
        try {
            URL resource = getClass().getResource(resourcePath);
            if (resource == null) {
                System.err.println("FXML NOT FOUND: " + resourcePath);
                return;
            }

            Node content = FXMLLoader.load(resource);
            if (contentHost != null) {
                contentHost.getChildren().setAll(content);
            }
        } catch (IOException e) {
            System.err.println("Error loading FXML content: " + resourcePath);
            e.printStackTrace();
        }
    }

    public void loadContentNode(Node content) {
        if (contentHost == null || content == null) {
            return;
        }
        if (content instanceof javafx.scene.layout.Region) {
            javafx.scene.layout.Region r = (javafx.scene.layout.Region) content;
            r.setMaxWidth(Double.MAX_VALUE);
            r.setMaxHeight(Double.MAX_VALUE);
        }
        contentHost.getChildren().setAll(content);
    }

    public void navigateToFrontendCourseList(Node source) {
        loadContent("/gestion_cours/courses_body.fxml");
    }

    private void setActiveNav(Label activeLabel) {
        updateNavStyle(dashboardNavLabel, dashboardNavLabel == activeLabel);
        updateNavStyle(planningNavLabel, planningNavLabel == activeLabel);
        updateNavStyle(coursesNavLabel, coursesNavLabel == activeLabel);
        updateNavStyle(groupsNavLabel, groupsNavLabel != null && groupsNavLabel == activeLabel);
        updateNavStyle(recommendationsNavLabel, recommendationsNavLabel != null && recommendationsNavLabel == activeLabel);
        updateNavStyle(roadmapNavLabel, roadmapNavLabel != null && roadmapNavLabel == activeLabel);
    }

    private void updateNavStyle(Label label, boolean active) {
        if (label == null) return;
        label.getStyleClass().removeAll("nav-link", "nav-link-active");
        label.getStyleClass().add(active ? "nav-link-active" : "nav-link");
    }
}
