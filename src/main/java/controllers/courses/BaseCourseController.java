package controllers.courses;

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

    public void goToCourses(MouseEvent event) {
        if (controllers.FrontendController.getInstance() != null) {
            controllers.FrontendController.getInstance().loadContent("/gestion_cours/courses_body.fxml");
        } else {
            loadScene("/gestion_cours/frontend_courses.fxml", event, null);
        }
    }

    public void goToAddCourse(MouseEvent event) {
        if (controllers.FrontendController.getInstance() != null) {
            controllers.FrontendController.getInstance().loadContent("/gestion_cours/add_course_body.fxml");
        } else {
            loadScene("/gestion_cours/frontend_add_course.fxml", event, null);
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
}
