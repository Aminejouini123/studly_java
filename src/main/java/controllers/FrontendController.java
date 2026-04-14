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
    private StackPane contentHost;

    @FXML
    public void initialize() {
        setActiveNav(dashboardNavLabel);
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

    private void loadContent(String resourcePath) {
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
