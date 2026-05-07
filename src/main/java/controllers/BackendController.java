package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import models.User;
import utils.SessionManager;

import java.io.IOException;

public class BackendController {

    @FXML private StackPane mainContentHost;
    
    @FXML private Button overviewBtn;
    @FXML private Button usersBtn;
    @FXML private Button groupsBtn;
    @FXML private Button timeBtn;
    @FXML private Button coursesBtn;
    
    @FXML private Label adminNameLabel;
    @FXML private Label adminRoleLabel;
    @FXML private Circle adminAvatar;

    @FXML
    public void initialize() {
        showUsers();
        
        // Update profile header
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser != null) {
            if (adminNameLabel != null) {
                adminNameLabel.setText(currentUser.getFullName());
            }
            if (adminRoleLabel != null) {
                adminRoleLabel.setText(currentUser.getRoles() != null && currentUser.getRoles().contains("ADMIN") ? "ADMINISTRATOR" : "MODERATOR");
            }
        }
    }

    @FXML
    public void showOverview() {
        setActiveButton(overviewBtn);
        if (mainContentHost != null) {
            mainContentHost.getChildren().clear();
        }
    }

    @FXML
    public void showUsers() {
        setActiveButton(usersBtn);
        loadContent("/TEMPLATE/backend_users.fxml");
    }

    @FXML
    public void showGroups() {
        setActiveButton(groupsBtn);
        loadContent("/gestion_group/backend_groups_management.fxml");
    }

    @FXML
    public void showTimeManagement() {
        setActiveButton(timeBtn);
        loadContent("/TEMPLATE/backend_time.fxml");
    }

    @FXML
    public void handleShowCourses() {
        setActiveButton(coursesBtn);
        loadContent("/gestion_cours/backend_courses.fxml");
    }

    private void loadContent(String fxmlPath) {
        if (mainContentHost == null) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node content = loader.load();
            mainContentHost.getChildren().setAll(content);
        } catch (IOException e) {
            System.err.println("Error loading FXML content: " + fxmlPath);
            e.printStackTrace();
        }
    }

    private void setActiveButton(Button activeBtn) {
        Button[] buttons = {overviewBtn, usersBtn, groupsBtn, timeBtn, coursesBtn};
        for (Button btn : buttons) {
            if (btn == null) continue;
            btn.getStyleClass().remove("nav-button-active");
            if (!btn.getStyleClass().contains("nav-button")) {
                btn.getStyleClass().add("nav-button");
            }
            if (btn.getGraphic() instanceof SVGPath) {
                SVGPath svg = (SVGPath) btn.getGraphic();
                if (svg.getStroke() != null && svg.getStroke() != Color.TRANSPARENT) {
                    svg.setStroke(Color.web("#64748B"));
                } else {
                    svg.setFill(Color.web("#64748B"));
                }
            }
        }
        
        if (activeBtn == null) return;
        activeBtn.getStyleClass().remove("nav-button");
        activeBtn.getStyleClass().add("nav-button-active");
        if (activeBtn.getGraphic() instanceof SVGPath) {
            SVGPath svg = (SVGPath) activeBtn.getGraphic();
            if (svg.getStroke() != null && svg.getStroke() != Color.TRANSPARENT) {
                svg.setStroke(Color.web("#004fb0"));
            } else {
                svg.setFill(Color.web("#38bdf8"));
            }
        }
    }

    @FXML
    public void handleExportExcel() {
        System.out.println("Export logic should be handled by sub-controllers.");
    }
    @FXML
    private void handleLogout() {
        utils.SessionManager.clearSession();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/getion_user/auth_page.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) mainContentHost.getScene().getWindow();
            stage.setScene(new javafx.scene.Scene(root));
            stage.setTitle("Login – Studly");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
