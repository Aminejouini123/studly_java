package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

import java.io.IOException;

public class BackendController {

    @FXML private StackPane mainContentHost;
    
    @FXML private Button overviewBtn;
    @FXML private Button usersBtn;
    @FXML private Button timeBtn;
    @FXML private Button coursesBtn;

    @FXML
    public void initialize() {
        showUsers();
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
            e.printStackTrace();
        }
    }

    private void setActiveButton(Button activeBtn) {
        Button[] buttons = {overviewBtn, usersBtn, timeBtn, coursesBtn};
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
}
