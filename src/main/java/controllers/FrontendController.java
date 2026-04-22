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

    @FXML
    private Label dashboardNavLabel;

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

    @FXML
    public void showDashboard() {
        contentHost.getChildren().clear();
        setActiveNav(dashboardNavLabel);
    }

    @FXML
    public void showPlanning() {
        loadContent("/Gestion de temps/planning_dashboard.fxml");
        setActiveNav(planningNavLabel);
    }

    public void showProfile() {
        loadContent("/getion_user/profile.fxml");
        setActiveNav(null); // No nav label for profile
    }

    public void showEditProfile() {
        loadContent("/getion_user/edit_profile_settings.fxml");
        setActiveNav(null);
    }

    private void loadContent(String resourcePath) {
        try {
            URL resource = getClass().getResource(resourcePath);
            System.out.println("FrontendController: loading resource -> " + resourcePath + " (url=" + resource + ")");
            if (resource == null) {
                throw new IOException("Missing FXML resource: " + resourcePath);
            }

            Node content = FXMLLoader.load(resource);
            System.out.println("FrontendController: loaded content for " + resourcePath + ", nodes=" + (content == null ? "null" : content.getClass().getSimpleName()));
            contentHost.getChildren().setAll(content);
        } catch (IOException e) {
            e.printStackTrace();
            javafx.application.Platform.runLater(() -> {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                alert.setTitle("Load Error");
                alert.setHeaderText("Unable to load component");
                alert.setContentText("Resource: " + resourcePath + "\nError: " + e.getMessage());
                alert.showAndWait();
            });
        }
    }

    private void setActiveNav(Label activeLabel) {
        updateNavStyle(dashboardNavLabel, dashboardNavLabel == activeLabel);
        updateNavStyle(planningNavLabel, planningNavLabel == activeLabel);
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
