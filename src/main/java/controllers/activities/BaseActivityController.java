package controllers.activities;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.scene.shape.SVGPath;
import javafx.scene.paint.Color;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.geometry.Pos;
import java.io.IOException;
import models.Course;

public abstract class BaseActivityController {

    protected void loadScene(String fxmlPath, javafx.event.Event event, Node fallbackNode) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage;
            if (event != null) {
                stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            } else {
                stage = (Stage) fallbackNode.getScene().getWindow();
            }
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected SVGPath createIcon(String path, String color, double size) {
        SVGPath svg = new SVGPath();
        svg.setContent(path);
        svg.setFill(Color.web(color));
        svg.setScaleX(size / 24.0);
        svg.setScaleY(size / 24.0);
        return svg;
    }

    protected VBox createStatItem(String iconPath, String value, String label, String color) {
        VBox item = new VBox(8);
        item.setAlignment(Pos.CENTER);
        item.getStyleClass().add("activity-stat-card");
        item.setPrefWidth(180);
        
        Label valLabel = new Label(value);
        valLabel.getStyleClass().add("stat-value");
        valLabel.setStyle("-fx-text-fill: white;");
        
        Label descLabel = new Label(label.toUpperCase());
        descLabel.getStyleClass().add("stat-label");
        descLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.6);");

        item.getChildren().addAll(valLabel, descLabel);
        return item;
    }

    protected void showAlert(String title, String content) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    protected void navigateToFrontendCourseList(Node anchor) {
        if (controllers.FrontendController.getInstance() != null) {
            controllers.FrontendController.getInstance().loadContent("/gestion_cours/courses_body.fxml");
        } else {
            loadScene("/gestion_cours/frontend_courses.fxml", null, anchor);
        }
    }

    /** Reload activity list for the given course (required after add/update from forms). */
    protected void navigateToActivityList(Node anchor, Course course) {
        if (anchor == null || anchor.getScene() == null) {
            return;
        }
        if (course == null) {
            navigateToFrontendCourseList(anchor);
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gestion_activites/frontend_activities.fxml"));
            Parent root = loader.load();
            ActivityListController controller = loader.getController();
            controller.setCourse(course);
            if (controllers.FrontendController.getInstance() != null) {
                controllers.FrontendController.getInstance().loadContentNode(root);
            } else {
                Stage stage = (Stage) anchor.getScene().getWindow();
                stage.getScene().setRoot(root);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected boolean fromBackend = false;
    protected controllers.backend.BackendActivityController backendController;

    public void setFromBackend(boolean fromBackend) {
        this.fromBackend = fromBackend;
    }

    public void setBackendController(controllers.backend.BackendActivityController controller) {
        this.backendController = controller;
    }

    protected void returnToDashboard(javafx.scene.Node anchor) {
        if (fromBackend && backendController != null) {
            backendController.restoreDashboard();
        } else if (fromBackend) {
            loadScene("/gestion_activites/backend_activities.fxml", null, anchor);
        } else {
            loadScene("/gestion_activites/frontend_activities.fxml", null, anchor);
        }
    }
}
