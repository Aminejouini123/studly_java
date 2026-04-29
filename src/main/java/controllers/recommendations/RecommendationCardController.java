package controllers.recommendations;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import models.Recommendation;

import java.awt.Desktop;
import java.net.URI;

public class RecommendationCardController {

    @FXML private Label  typeBadge;
    @FXML private Label  scoreBadge;
    @FXML private Label  titleLabel;
    @FXML private Label  providerLabel;
    @FXML private Button viewButton;

    public void setRecommendation(Recommendation rec) {
        titleLabel.setText(rec.getTitle().isBlank() ? "Untitled" : rec.getTitle());
        providerLabel.setText(rec.getProvider());

        boolean isJob = rec.getType() == Recommendation.Type.JOB;
        typeBadge.setText(isJob ? "JOB" : "COURSE");
        typeBadge.setStyle(badgeStyle(isJob ? "#3B82F6" : "#10B981"));

        int score = rec.getMatchScore();
        String scoreColor = score >= 70 ? "#22C55E" : score >= 40 ? "#F59E0B" : "#EF4444";
        scoreBadge.setText(score + "% match");
        scoreBadge.setStyle(badgeStyle(scoreColor));

        viewButton.setOnAction(e -> openUrl(rec.getUrl()));
    }

    private String badgeStyle(String bgColor) {
        return "-fx-background-color: " + bgColor + ";" +
               "-fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold;" +
               "-fx-background-radius: 6; -fx-padding: 3 10 3 10;";
    }

    private void openUrl(String url) {
        try {
            if (url == null || url.isBlank()) return;
            Desktop.getDesktop().browse(URI.create(url));
        } catch (Exception e) {
            System.err.println("RecommendationCardController: cannot open URL: " + url);
        }
    }
}