package controllers.courses;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import java.io.IOException;
import javafx.scene.shape.SVGPath;
import javafx.scene.paint.Color;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.geometry.Pos;
import models.User;
import services.CourseService;
import utils.SessionManager;

public abstract class BaseCourseController {

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
        if (controllers.FrontendController.getInstance() != null) {
            controllers.FrontendController.getInstance().goToDashboard(event);
        } else {
            loadScene("/TEMPLATE/frontend_dashboard.fxml", event, null);
        }
    }

    public void goToCourses(javafx.event.Event event) {
        if (controllers.FrontendController.getInstance() != null) {
            controllers.FrontendController.getInstance().loadContent("/gestion_cours/courses_body.fxml");
        } else {
            loadScene("/gestion_cours/frontend_courses.fxml", event, null);
        }
    }

    /** Planning from embedded course pages (same shell as dashboard when available). */
    public void goToPlanning(javafx.event.Event event) {
        controllers.FrontendController fc = controllers.FrontendController.getInstance();
        if (fc != null) {
            fc.showPlanning();
            return;
        }
        openDashboardThen(event, controllers.FrontendController::showPlanning);
    }

    public void goToGroups(javafx.event.Event event) {
        controllers.FrontendController fc = controllers.FrontendController.getInstance();
        if (fc != null) {
            fc.showGroups();
            return;
        }
        openDashboardThen(event, controllers.FrontendController::showGroups);
    }

    private void openDashboardThen(javafx.event.Event event, java.util.function.Consumer<controllers.FrontendController> action) {
        Node source = event != null ? (Node) event.getSource() : null;
        if (source == null || source.getScene() == null) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/TEMPLATE/frontend_dashboard.fxml"));
            Parent root = loader.load();
            controllers.FrontendController shell = loader.getController();
            Stage stage = (Stage) source.getScene().getWindow();
            stage.getScene().setRoot(root);
            action.accept(shell);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void goToAddCourse(javafx.event.Event event) {
        if (controllers.FrontendController.getInstance() != null) {
            controllers.FrontendController.getInstance().loadContent("/gestion_cours/add_course_body.fxml");
        } else {
            loadScene("/gestion_cours/add_course_body.fxml", event, null);
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

    protected VBox createStatItem(String iconPath, String value) {
        VBox item = new VBox(8);
        item.setAlignment(Pos.CENTER);
        SVGPath icon = createIcon(iconPath, "#0056D2", 18);
        Label valLabel = new Label(value);
        valLabel.getStyleClass().add("stat-item-label");
        item.getChildren().addAll(icon, valLabel);
        return item;
    }
    protected boolean fromBackend = false;
    protected controllers.BackendCourseController backendController;

    public void setFromBackend(boolean fromBackend) {
        this.fromBackend = fromBackend;
    }

    public void setBackendController(controllers.BackendCourseController controller) {
        this.backendController = controller;
    }

    /** When the main shell hosts content in {@code contentHost}, reload the course list there; otherwise replace the scene root. */
    protected void navigateToFrontendCourseList(Node anchor) {
        if (controllers.FrontendController.getInstance() != null) {
            controllers.FrontendController.getInstance().loadContent("/gestion_cours/courses_body.fxml");
        } else {
            loadScene("/gestion_cours/frontend_courses.fxml", null, anchor);
        }
    }

    protected void returnToDashboard(javafx.scene.Node anchor) {
        if (fromBackend && backendController != null) {
            backendController.restoreDashboard();
        } else if (fromBackend) {
            loadScene("/gestion_cours/backend_courses.fxml", null, anchor);
        } else {
            goToCourses(null);
        }
    }

    protected void returnToCourses(javafx.event.Event event) {
        if (fromBackend && backendController != null) {
            backendController.restoreDashboard();
        } else if (fromBackend) {
            loadScene("/gestion_cours/backend_courses.fxml", event, null);
        } else {
            goToCourses(event);
        }
    }
}
