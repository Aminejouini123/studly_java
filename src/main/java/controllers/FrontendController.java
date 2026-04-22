package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.Node;
import javafx.fxml.FXMLLoader;
import models.User;
import utils.SessionManager;
import java.io.IOException;
import java.net.URL;

public class FrontendController extends controllers.courses.BaseCourseController {

    @FXML private Label dashboardNavLabel;
    @FXML private Label planningNavLabel;
    @FXML private Label coursesNavLabel;
    @FXML private StackPane contentHost;

    // Profile header components
    @FXML private HBox profileSection;
    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;
    @FXML private Label avatarInitials;
    @FXML private Circle avatarCircle;

    private static FrontendController instance;

    public static FrontendController getInstance() {
        return instance;
    }

    @FXML
    public void initialize() {
        instance = this;
        setActiveNav(dashboardNavLabel);
        loadUserInfo();
    }

    private void loadUserInfo() {
        User user = SessionManager.getCurrentUser();
        if (user != null) {
            String firstName = user.getFirst_name() != null ? user.getFirst_name() : "";
            String lastName = user.getLast_name() != null ? user.getLast_name() : "";
            userNameLabel.setText(firstName + " " + lastName);
            
            // Set role display
            String roles = user.getRoles() != null ? user.getRoles() : "Member";
            if (roles.contains("ROLE_ADMIN")) {
                userRoleLabel.setText("Administrator");
            } else if (roles.contains("ROLE_TEACHER")) {
                userRoleLabel.setText("Teacher");
            } else {
                userRoleLabel.setText("Student");
            }

            // Set initials
            String initials = "";
            if (!firstName.isEmpty()) initials += firstName.substring(0, 1).toUpperCase();
            if (!lastName.isEmpty()) initials += lastName.substring(0, 1).toUpperCase();
            avatarInitials.setText(initials);
        }
    }

    // Called after profile edits to refresh header UI.
    public void refreshUserHeader() {
        loadUserInfo();
    }

    @FXML
    private void showDashboard() {
        contentHost.getChildren().clear();
        setActiveNav(dashboardNavLabel);
    }

    @FXML
    private void showPlanning() {
        loadContent("/Gestion de temps/planning_dashboard.fxml");
        setActiveNav(planningNavLabel);
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
    public void showProfile() {
        loadContent("/getion_user/profile.fxml");
        setActiveNav(null); // Clear active nav state as we're on profile
    }

    @FXML
    public void showEditProfile() {
        loadContent("/getion_user/edit_profile_settings.fxml");
    }

    public void loadContent(String resourcePath) {
        try {
            URL resource = getClass().getResource(resourcePath);
            if (resource == null) {
                System.err.println("FXML NOT FOUND: " + resourcePath);
                return;
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Node content = loader.load();
            contentHost.getChildren().setAll(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Show an already-built node inside the dashboard (keeps nav/header).
     * Do not use {@code TOP_LEFT} alignment here: with that alignment, StackPane keeps the child's
     * preferred size only, so detail views inside the ScrollPane often appear blank or a thin strip.
     */
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

    private void setActiveNav(Label activeLabel) {
        updateNavStyle(dashboardNavLabel, dashboardNavLabel == activeLabel);
        updateNavStyle(planningNavLabel, planningNavLabel == activeLabel);
        updateNavStyle(coursesNavLabel, coursesNavLabel == activeLabel);
    }

    private void updateNavStyle(Label label, boolean active) {
        if (label == null) return;
        label.getStyleClass().removeAll("nav-link", "nav-link-active");
        label.getStyleClass().add(active ? "nav-link-active" : "nav-link");
    }
}
