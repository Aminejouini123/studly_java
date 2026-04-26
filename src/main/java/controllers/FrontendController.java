package controllers;

import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;
import controllers.courses.BaseCourseController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;

public class FrontendController extends BaseCourseController {

    @FXML private Label dashboardNavLabel;
    @FXML private Label planningNavLabel;
    @FXML private Label coursesNavLabel;
    @FXML private Label groupsNavLabel;
    @FXML private StackPane contentHost;

    @FXML
    private Label planningNavLabel;

    @FXML
    private Label userNameLabel;

    @FXML
    private Label userRoleLabel;

    @FXML
    private Label avatarInitials;

    @FXML
    private StackPane contentHost;

    private static FrontendController instance;

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
        models.User user = utils.SessionManager.getCurrentUser();
        if (user != null) {
            String firstName = user.getFirst_name() != null ? user.getFirst_name() : "";
            String lastName = user.getLast_name() != null ? user.getLast_name() : "";
            userNameLabel.setText(firstName + " " + lastName);
            
            String roles = user.getRoles() != null ? user.getRoles() : "Member";
            userRoleLabel.setText(roles.contains("ROLE_ADMIN") ? "Administrator" : "Student");
            
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

    private void loadUserInfo() {
        refreshUserHeader();
    }

    @FXML
    private void showDashboard() {
        contentHost.getChildren().clear();
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
            System.out.println("FrontendController: loaded content for " + resourcePath + ", nodes=" + (content == null ? "null" : content.getClass().getSimpleName()));
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

    public void navigateToFrontendCourseList(Node source) {
        loadContent("/gestion_cours/courses_body.fxml");
    }

    private void setActiveNav(Label activeLabel) {
        updateNavStyle(dashboardNavLabel, dashboardNavLabel == activeLabel);
        updateNavStyle(planningNavLabel, planningNavLabel == activeLabel);
        updateNavStyle(coursesNavLabel, coursesNavLabel == activeLabel);
        updateNavStyle(groupsNavLabel, groupsNavLabel != null && groupsNavLabel == activeLabel);
    }

    private void updateNavStyle(Label label, boolean active) {
        if (label == null) {
            return;
        }

        label.getStyleClass().removeAll("nav-link", "nav-link-active");
        label.getStyleClass().add(active ? "nav-link-active" : "nav-link");
    }

    // Navigation methods are inherited from BaseCourseController
    // but we can override or add dashboard-specific ones here
}
