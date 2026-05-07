package controllers.roadmap;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.util.Duration;
import models.Resource;
import models.RoadmapStep;
import services.RoadmapService;

import java.awt.Desktop;
import java.net.URI;

public class RoadmapStepController {


    @FXML private CheckBox  completedCheckBox;
    @FXML private Label     titleLabel;
    @FXML private Label     descriptionLabel;
    @FXML private FlowPane  resourcesContainer;

    private RoadmapStep    step;
    private RoadmapService service;

    /** Called by RoadmapController after loading the FXML. */
    public void setStep(RoadmapStep step, boolean isLast, RoadmapService service) {
        this.step    = step;
        this.service = service;

        titleLabel.setText(step.getTitle() != null ? step.getTitle() : "");
        descriptionLabel.setText(step.getDescription() != null ? step.getDescription() : "");

        resourcesContainer.getChildren().clear();
        if (step.getResources() != null) {
            for (Resource res : step.getResources()) {
                resourcesContainer.getChildren().add(buildResourceButton(res));
            }
        }

        // Activate checkbox only when this step has a DB id
        if (step.getId() > 0) {
            activateCheckbox();
        }
    }

    /** Called after a roadmap is saved so the step now has a DB id. */
    public void activateCheckbox() {
        completedCheckBox.setSelected(step.isCompleted());
        applyCompletedStyle(step.isCompleted());
        completedCheckBox.setVisible(true);
        completedCheckBox.setManaged(true);

        completedCheckBox.setOnAction(e -> {
            boolean done = completedCheckBox.isSelected();
            step.setCompleted(done);
            applyCompletedStyle(done);
            if (service != null) {
                try {
                    service.toggleStepCompletion(step.getId(), done);
                } catch (Exception ex) {
                    // Revert on DB failure
                    step.setCompleted(!done);
                    completedCheckBox.setSelected(!done);
                    applyCompletedStyle(!done);
                    System.err.println("toggleStepCompletion failed: " + ex.getMessage());
                }
            }
        });
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void applyCompletedStyle(boolean done) {
        if (done) {
            if (!titleLabel.getStyleClass().contains("step-title-done"))
                titleLabel.getStyleClass().add("step-title-done");
        } else {
            titleLabel.getStyleClass().remove("step-title-done");
        }
        FadeTransition fade = new FadeTransition(Duration.millis(250), titleLabel);
        fade.setToValue(done ? 0.45 : 1.0);
        fade.play();
    }

    private Button buildResourceButton(Resource res) {
        Button btn = new Button(res.getLabel());
        btn.getStyleClass().add("resource-btn");
        btn.setOnAction(e -> openLink(res.getUrl()));
        return btn;
    }

    private static void openLink(String url) {
        try {
            if (url == null || url.isBlank()) return;
            Desktop.getDesktop().browse(URI.create(url));
        } catch (Exception e) {
            System.err.println("RoadmapStepController: cannot open URL " + url + " — " + e.getMessage());
        }
    }
}