package controllers.exams;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import javafx.util.Duration;
import models.Course;

import java.io.IOException;

public abstract class BaseExamController {

    protected void loadScene(String fxmlPath, javafx.event.Event event, Node fallbackNode) {
        try {
            java.net.URL res = getClass().getResource(fxmlPath);
            if (res == null) {
                System.err.println("CRITICAL: Could not find FXML file: " + fxmlPath);
                return;
            }
            FXMLLoader loader = new FXMLLoader(res);
            Parent root = loader.load();
            Stage stage;
            if (event != null && event.getSource() instanceof Node) {
                stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            } else if (fallbackNode != null && fallbackNode.getScene() != null) {
                stage = (Stage) fallbackNode.getScene().getWindow();
            } else {
                System.err.println("CRITICAL: No valid scene context found to switch scenes.");
                return;
            }
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("ERROR loading FXML: " + fxmlPath);
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

    protected void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    protected void showSuccessNotification(StackPane rootPane, String titleText, String messageText, Runnable onFinish) {
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("success-overlay");
        overlay.setOpacity(0);

        VBox card = new VBox(20);
        card.getStyleClass().add("success-card");
        card.setAlignment(javafx.geometry.Pos.CENTER);
        card.setMaxSize(340, Region.USE_PREF_SIZE);

        VBox iconBox = new VBox();
        iconBox.getStyleClass().add("success-icon-box");
        iconBox.getChildren().add(createIcon("M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z", "#16A34A", 32));

        Label title = new Label(titleText);
        title.getStyleClass().add("success-title");
        Label message = new Label(messageText);
        message.getStyleClass().add("success-message");

        card.getChildren().addAll(iconBox, title, message);
        overlay.getChildren().add(card);
        rootPane.getChildren().add(overlay);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), overlay);
        fadeIn.setToValue(1);
        fadeIn.play();

        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1.5), e -> {
            if (onFinish != null) onFinish.run();
        }));
        timeline.play();
    }

    protected void showErrorNotification(StackPane rootPane, String titleText, String msg) {
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("error-overlay");
        
        VBox card = new VBox(15);
        card.getStyleClass().add("error-card");
        card.setAlignment(javafx.geometry.Pos.CENTER);
        card.setMaxSize(340, Region.USE_PREF_SIZE);

        VBox iconBox = new VBox();
        iconBox.getStyleClass().add("error-icon-box");
        iconBox.getChildren().add(createIcon("M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z", "#E11D48", 28));

        Label title = new Label(titleText);
        title.getStyleClass().add("error-title");
        Label message = new Label(msg);
        message.getStyleClass().add("error-message");
        message.setWrapText(true);
        
        Button closeBtn = new Button("Try Again");
        closeBtn.getStyleClass().add("btn-error-close");
        closeBtn.setOnAction(e -> rootPane.getChildren().remove(overlay));

        card.getChildren().addAll(iconBox, title, message, closeBtn);
        overlay.getChildren().add(card);
        rootPane.getChildren().add(overlay);
    }

    protected void navigateToFrontendCourseList(Node anchor) {
        if (controllers.FrontendController.getInstance() != null) {
            controllers.FrontendController.getInstance().loadContent("/gestion_cours/courses_body.fxml");
        } else {
            loadScene("/gestion_cours/frontend_courses.fxml", null, anchor);
        }
    }

    protected void showConfirmOverlay(StackPane rootPane, String titleText, String msgText, String confirmBtnText, String cancelBtnText, Runnable onConfirm) {
        StackPane overlay = new StackPane();
        overlay.setAlignment(javafx.geometry.Pos.CENTER);
        overlay.setStyle("-fx-background-color: rgba(15, 23, 42, 0.4);");
        
        VBox card = new VBox(25);
        card.setAlignment(javafx.geometry.Pos.CENTER);
        card.setMaxSize(340, Region.USE_PREF_SIZE);
        card.getStyleClass().add("modern-confirm-card");
        
        SVGPath warnIcon = createIcon("M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z", "#EF4444", 48);
        
        VBox textContent = new VBox(8);
        textContent.setAlignment(javafx.geometry.Pos.CENTER);
        Label title = new Label(titleText);
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #1E293B;");
        Label desc = new Label(msgText);
        desc.getStyleClass().add("success-message");
        textContent.getChildren().addAll(title, desc);
        
        HBox buttons = new HBox(12);
        buttons.setAlignment(javafx.geometry.Pos.CENTER);
        Button cancelBtn = new Button(cancelBtnText);
        cancelBtn.getStyleClass().add("btn-modern-cancel");
        cancelBtn.setOnAction(e -> rootPane.getChildren().remove(overlay));
        
        Button confirmBtn = new Button(confirmBtnText);
        confirmBtn.getStyleClass().add("btn-modern-delete");
        confirmBtn.setOnAction(e -> {
            rootPane.getChildren().remove(overlay);
            if (onConfirm != null) onConfirm.run();
        });
        
        buttons.getChildren().addAll(cancelBtn, confirmBtn);
        card.getChildren().addAll(warnIcon, textContent, buttons);
        overlay.getChildren().add(card);
        rootPane.getChildren().add(overlay);
    }

    protected void navigateToExamList(Node sourceNode, Course course) {
        if (sourceNode == null || sourceNode.getScene() == null) {
            return;
        }
        if (course == null) {
            navigateToFrontendCourseList(sourceNode);
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gestion_examen/frontend_exams.fxml"));
            Parent root = loader.load();
            ExamListController controller = loader.getController();
            controller.setCourse(course);
            if (controllers.FrontendController.getInstance() != null) {
                controllers.FrontendController.getInstance().loadContentNode(root);
            } else {
                Stage stage = (Stage) sourceNode.getScene().getWindow();
                stage.getScene().setRoot(root);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    protected boolean fromBackend = false;
    protected controllers.backend.BackendExamController backendController;

    public void setFromBackend(boolean fromBackend) {
        this.fromBackend = fromBackend;
    }

    public void setBackendController(controllers.backend.BackendExamController controller) {
        this.backendController = controller;
    }

    protected void returnToDashboard(javafx.scene.Node anchor) {
        if (fromBackend && backendController != null) {
            backendController.restoreDashboard();
        } else if (fromBackend) {
            loadScene("/gestion_examen/backend_exams.fxml", null, anchor);
        } else {
            loadScene("/gestion_examen/frontend_exams.fxml", null, anchor);
        }
    }
}
