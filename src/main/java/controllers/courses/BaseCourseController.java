package controllers.courses;

import javafx.fxml.FXML;
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
import java.net.URL;

import controllers.BackendCourseController;

public abstract class BaseCourseController {

    protected boolean fromBackend = false;
    protected BackendCourseController backendController;

    public void setFromBackend(boolean fromBackend) {
        this.fromBackend = fromBackend;
    }

    public void setBackendController(BackendCourseController backendController) {
        this.backendController = backendController;
    }

    public void returnToCourses(MouseEvent event) {
        if (fromBackend && backendController != null) {
            backendController.restoreDashboard();
        } else {
            goToCourses(event);
        }
    }

    public void returnToDashboard(Node fallbackNode) {
        if (fromBackend && backendController != null) {
            backendController.restoreDashboard();
        } else {
            loadScene("/TEMPLATE/frontend_dashboard.fxml", null, fallbackNode);
        }
    }

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

    public void goToDashboard(javafx.event.Event event) {
        loadScene("/TEMPLATE/frontend_dashboard.fxml", event, null);
    }

    @FXML
    public void goToCourses(MouseEvent event) {
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Node contentHost = stage.getScene().getRoot().lookup("#contentHost");
            java.net.URL res = getClass().getResource("/gestion_cours/frontend_courses.fxml");
            if (contentHost instanceof javafx.scene.layout.Pane && res != null) {
                Parent content = FXMLLoader.load(res);
                ((javafx.scene.layout.Pane) contentHost).getChildren().setAll(content);
                return;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        loadScene("/gestion_cours/frontend_courses.fxml", event, null);
    }

    @FXML
    public void goToPlanning(MouseEvent event) {
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Node contentHost = stage.getScene().getRoot().lookup("#contentHost");
            java.net.URL res = getClass().getResource("/Gestion de temps/planning_dashboard.fxml");
            if (contentHost instanceof javafx.scene.layout.Pane && res != null) {
                Parent content = FXMLLoader.load(res);
                ((javafx.scene.layout.Pane) contentHost).getChildren().setAll(content);
                return;
            }

            // Fallback: load full dashboard and request planning
            URL resource = getClass().getResource("/TEMPLATE/frontend_dashboard.fxml");
            System.out.println("BaseCourseController: goToPlanning loading /TEMPLATE/frontend_dashboard.fxml (url=" + resource + ")");
            if (resource == null) {
                throw new IOException("Missing FXML resource: /TEMPLATE/frontend_dashboard.fxml");
            }
            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof controllers.FrontendController) {
                ((controllers.FrontendController) controller).showPlanning();
            }

            stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
            javafx.application.Platform.runLater(() -> {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                alert.setTitle("Navigation Error");
                alert.setHeaderText("Unable to load Planning");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
            });
        }
    }

    @FXML
    public void goToAddCourse(MouseEvent event) {
        loadScene("/gestion_cours/frontend_add_course.fxml", event, null);
    }

    protected SVGPath createIcon(String path, String color, double size) {
        SVGPath svg = new SVGPath();
        svg.setContent(path);
        svg.setFill(Color.web(color));
        svg.setScaleX(size / 24.0);
        svg.setScaleY(size / 24.0);
        return svg;
    }

    protected VBox createStatItem(String iconPath, String value) {
        VBox item = new VBox(8);
        item.setAlignment(Pos.CENTER);
        SVGPath icon = createIcon(iconPath, "#0056D2", 18);
        Label valLabel = new Label(value);
        valLabel.getStyleClass().add("stat-item-label");
        item.getChildren().addAll(icon, valLabel);
        return item;
    }

    protected void navigateToFrontendCourseList(Node source) {
        controllers.FrontendController.getInstance().loadContent("/gestion_cours/courses_body.fxml");
    }
}
