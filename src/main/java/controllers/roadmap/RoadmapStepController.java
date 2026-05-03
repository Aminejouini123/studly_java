package controllers.roadmap;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import models.Resource;
import models.RoadmapStep;

import java.awt.Desktop;
import java.net.URI;

public class RoadmapStepController {

    @FXML private Label     stepNumberLabel;
    @FXML private Region    timelineConnector;
    @FXML private Label     titleLabel;
    @FXML private Label     descriptionLabel;
    @FXML private FlowPane  resourcesContainer;

    /** Called by RoadmapController after loading the FXML. */
    public void setStep(RoadmapStep step, boolean isLast) {
        stepNumberLabel.setText(String.valueOf(step.getStepNumber()));
        titleLabel.setText(step.getTitle() != null ? step.getTitle() : "");
        descriptionLabel.setText(step.getDescription() != null ? step.getDescription() : "");

        // Remove the connector line on the final card so it doesn't dangle
        if (isLast) {
            timelineConnector.setVisible(false);
            timelineConnector.setManaged(false);
        }

        resourcesContainer.getChildren().clear();
        if (step.getResources() != null) {
            for (Resource res : step.getResources()) {
                resourcesContainer.getChildren().add(buildResourceButton(res));
            }
        }
    }

    private Button buildResourceButton(Resource res) {
        Button btn = new Button(res.getLabel());
        btn.getStyleClass().add("resource-btn");
        btn.setOnAction(e -> openLink(res.getUrl()));
        return btn;
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private static void openLink(String url) {
        try {
            if (url == null || url.isBlank()) return;
            Desktop.getDesktop().browse(URI.create(url));
        } catch (Exception e) {
            System.err.println("RoadmapStepController: cannot open URL " + url + " — " + e.getMessage());
        }
    }
}