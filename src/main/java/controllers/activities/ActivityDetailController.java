package controllers.activities;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.shape.Arc;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import models.Activity;
import models.Course;
import services.ActivityService;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.util.Duration;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.shape.SVGPath;
import javafx.scene.control.Button;

public class ActivityDetailController extends BaseActivityController {

    @FXML private Label detailTitle, detailDescription, detailInstructions, detailExpectedOutput, detailHints;
    @FXML private Label detailType, detailStatus, detailDifficulty, detailLevel, detailDuration;
    @FXML private Label detailCreatedAt, courseNameLabel;
    @FXML private Label durationPercentLabel, complexityMeta, linkLabel, fileLabel;
    @FXML private VBox resourceLinkBox, resourceFileBox;
    @FXML private Arc durationArc;
    @FXML private javafx.scene.layout.StackPane rootPane;

    private Activity currentActivity;
    private Course currentCourse;
    private final ActivityService activityService = new ActivityService();

    public void populateDetails(Activity activity, Course course) {
        this.currentActivity = activity;
        this.currentCourse = course;

        if (activity == null) return;

        // Basic Info
        detailTitle.setText(activity.getTitle() != null ? activity.getTitle() : "Untitled");
        detailDescription.setText(activity.getDescription() != null && !activity.getDescription().isEmpty() ? activity.getDescription() : "No overview provided.");
        detailInstructions.setText(activity.getInstructions() != null && !activity.getInstructions().isEmpty() ? activity.getInstructions() : "No specific instructions provided.");
        detailExpectedOutput.setText(activity.getExpected_output() != null && !activity.getExpected_output().isEmpty() ? activity.getExpected_output() : "No output defined.");
        detailHints.setText(activity.getHints() != null && !activity.getHints().isEmpty() ? activity.getHints() : "No hints available.");
        
        // Badges
        detailType.setText(activity.getType() != null ? activity.getType().toUpperCase() : "ACTIVITY");
        detailStatus.setText(activity.getStatus() != null ? activity.getStatus().toUpperCase() : "TO DO");
        detailDifficulty.setText(activity.getDifficulty() != null ? activity.getDifficulty().toUpperCase() : "MEDIUM");
        detailLevel.setText(activity.getLevel() != null ? activity.getLevel().toUpperCase() : "BEGINNER");
        
        // Metadata
        detailDuration.setText(String.valueOf(activity.getDuration()));
        if (course != null) {
            courseNameLabel.setText(course.getName());
        } else {
            courseNameLabel.setText("Standalone Activity");
        }
        

        complexityMeta.setText(activity.getDifficulty() != null ? activity.getDifficulty() : "Normal");
        
        if (activity.getCompleted_at() != null) {
            detailCreatedAt.setText(new SimpleDateFormat("dd MMM yyyy").format(activity.getCompleted_at()));
        }

        // Resources
        if (activity.getLink() == null || activity.getLink().isEmpty()) {
            resourceLinkBox.setVisible(false);
            resourceLinkBox.setManaged(false);
        } else {
            linkLabel.setText(activity.getLink());
        }

        if (activity.getFile() == null || activity.getFile().isEmpty()) {
            resourceFileBox.setVisible(false);
            resourceFileBox.setManaged(false);
        } else {
            fileLabel.setText(activity.getFile());
        }

        // Circular Progress Logic (Visual representation of duration relative to 120 mins)
        double duration = activity.getDuration();
        double maxDuration = 120.0; 
        double percentage = Math.min(duration / maxDuration, 1.0);
        durationArc.setLength(-(percentage * 360));
        durationPercentLabel.setText(String.valueOf((int)duration));
    }

    @FXML
    private void handleBack() {
        navigateToActivityList(detailTitle, currentCourse);
    }

    @FXML
    private void handleEdit() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/gestion_activites/frontend_edit_activity.fxml"));
            javafx.scene.Parent root = loader.load();
            ActivityEditController controller = loader.getController();
            controller.setActivity(currentActivity, currentCourse);
            if (controllers.FrontendController.getInstance() != null) {
                controllers.FrontendController.getInstance().loadContentNode(root);
            } else {
                javafx.stage.Stage stage = (javafx.stage.Stage) detailTitle.getScene().getWindow();
                stage.getScene().setRoot(root);
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDelete() {
        showSleekDeleteOverlay(currentActivity);
    }

    private void showSleekDeleteOverlay(Activity activity) {
        javafx.scene.layout.StackPane overlay = new javafx.scene.layout.StackPane();
        overlay.setStyle("-fx-background-color: rgba(15, 23, 42, 0.7);"); // Dark slate translucent
        overlay.setOpacity(0);
        
        VBox card = new VBox(25);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(40));
        card.setMaxSize(340, javafx.scene.layout.Region.USE_PREF_SIZE);
        card.getStyleClass().add("modern-confirm-card");
        
        SVGPath warnIcon = createIcon("M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z", "#EF4444", 48);
        
        VBox textContent = new VBox(8);
        textContent.setAlignment(Pos.CENTER);
        Label title = new Label("Delete Activity?");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #1E293B;");
        Label desc = new Label("This action cannot be undone.\n\"" + activity.getTitle() + "\" will be lost.");
        desc.getStyleClass().add("success-message");
        textContent.getChildren().addAll(title, desc);
        
        HBox buttons = new HBox(12);
        buttons.setAlignment(Pos.CENTER);
        Button cancelBtn = new Button("Keep it");
        cancelBtn.getStyleClass().add("btn-modern-cancel");
        cancelBtn.setOnAction(e -> {
            FadeTransition ft = new FadeTransition(Duration.millis(200), overlay);
            ft.setToValue(0);
            ft.setOnFinished(ev -> rootPane.getChildren().remove(overlay));
            ft.play();
        });
        
        Button confirmBtn = new Button("Delete Activity");
        confirmBtn.getStyleClass().add("btn-modern-delete");
        confirmBtn.setOnAction(e -> {
            try {
                activityService.supprimer(activity.getId());
                rootPane.getChildren().remove(overlay);
                handleBack(); // Navigate back after deletion
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });
        
        buttons.getChildren().addAll(cancelBtn, confirmBtn);
        card.getChildren().addAll(warnIcon, textContent, buttons);
        overlay.getChildren().add(card);
        
        rootPane.getChildren().add(overlay);
        
        FadeTransition ft = new FadeTransition(Duration.millis(250), overlay);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
        
        ScaleTransition st = new ScaleTransition(Duration.millis(300), card);
        st.setFromX(0.85); st.setFromY(0.85); st.setToX(1); st.setToY(1); st.play();
    }
}
