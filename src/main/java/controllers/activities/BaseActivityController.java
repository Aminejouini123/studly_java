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
}
