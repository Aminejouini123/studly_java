package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;

public class FrontendController {

    @FXML
    private Label dashboardNavLabel;

    @FXML
    private Label planningNavLabel;

    @FXML
    private Label groupsNavLabel;

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

    @FXML
    private void showGroups() {
        loadContent("/gestion_group/groups_dashboard.fxml");
        setActiveNav(groupsNavLabel);
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
