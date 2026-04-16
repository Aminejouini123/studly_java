package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import models.User;
import utils.SessionManager;

import java.io.IOException;
import java.net.URL;

public class FrontendController {

    private static FrontendController instance;

    @FXML
    private Label dashboardNavLabel;

    @FXML
    private Label planningNavLabel;

    @FXML
    private Label coursesNavLabel;

    @FXML
    private Label groupsNavLabel;

    // Optional header fields (present in some variants of the dashboard FXML).
    @FXML
    private Label avatarInitials;

    @FXML
    private Label userNameLabel;

    @FXML
    private Label userRoleLabel;

    @FXML
    private StackPane contentHost;

    public FrontendController() {
        instance = this;
    }

    public static FrontendController getInstance() {
        return instance;
    }

    @FXML
    public void initialize() {
        setActiveNav(dashboardNavLabel);
        refreshUserHeader();
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

    public void loadContent(String resourcePath) {
        try {
            URL resource = getClass().getResource(resourcePath);
            if (resource == null) {
                throw new IllegalStateException("Missing FXML resource: " + resourcePath);
            }

            Node content = FXMLLoader.load(resource);
            contentHost.getChildren().setAll(content);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load FXML resource: " + resourcePath, e);
        }
    }

    public void goToDashboard(javafx.event.Event event) {
        showDashboard();
    }

    @FXML
    private void goToCourses(MouseEvent event) {
        loadContent("/gestion_cours/courses_body.fxml");
        setActiveNav(coursesNavLabel);
    }

    @FXML
    private void showGroups(MouseEvent event) {
        loadContent("/gestion_group/groups_dashboard.fxml");
        setActiveNav(groupsNavLabel);
    }

    @FXML
    private void showInvitations(MouseEvent event) {
        loadContent("/gestion_group/invitations_inbox.fxml");
    }

    @FXML
    public void showProfile() {
        loadContent("/getion_user/profile.fxml");
    }

    public void showEditProfile() {
        loadContent("/getion_user/edit_profile_settings.fxml");
    }

    public void refreshUserHeader() {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            return;
        }

        String firstName = user.getFirst_name() != null ? user.getFirst_name() : "";
        String lastName = user.getLast_name() != null ? user.getLast_name() : "";
        String fullName = (firstName + " " + lastName).trim();

        if (userNameLabel != null) {
            userNameLabel.setText(fullName.isEmpty() ? "User" : fullName);
        }

        if (userRoleLabel != null) {
            String roles = user.getRoles() != null ? user.getRoles() : "";
            if (roles.contains("ROLE_ADMIN")) {
                userRoleLabel.setText("Administrator");
            } else if (roles.contains("ROLE_TEACHER")) {
                userRoleLabel.setText("Teacher");
            } else {
                userRoleLabel.setText("Student");
            }
        }

        if (avatarInitials != null) {
            String initials = "";
            if (!firstName.isEmpty()) initials += firstName.substring(0, 1).toUpperCase();
            if (!lastName.isEmpty()) initials += lastName.substring(0, 1).toUpperCase();
            avatarInitials.setText(initials.isEmpty() ? "U" : initials);
        }
    }

    private void setActiveNav(Label activeLabel) {
        updateNavStyle(dashboardNavLabel, dashboardNavLabel == activeLabel);
        updateNavStyle(planningNavLabel, planningNavLabel == activeLabel);
        updateNavStyle(coursesNavLabel, coursesNavLabel == activeLabel);
        updateNavStyle(groupsNavLabel, groupsNavLabel == activeLabel);
    }

    private void updateNavStyle(Label label, boolean active) {
        if (label == null) {
            return;
        }

        label.getStyleClass().removeAll("nav-link", "nav-link-active");
        label.getStyleClass().add(active ? "nav-link-active" : "nav-link");
    }
}
