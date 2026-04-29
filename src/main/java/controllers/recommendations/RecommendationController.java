package controllers.recommendations;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import models.Recommendation;
import models.User;
import services.RecommendationService;
import utils.SessionManager;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class RecommendationController {

    @FXML private HBox              skillsContainer;
    @FXML private VBox              loadingPane;
    @FXML private ProgressIndicator loadingSpinner;
    @FXML private Label             statusLabel;
    @FXML private VBox              contentPane;
    @FXML private FlowPane          jobsContainer;
    @FXML private FlowPane          coursesContainer;
    @FXML private Label             jobCountLabel;
    @FXML private Label             courseCountLabel;

    private final RecommendationService service      = new RecommendationService();
    private final ObjectMapper          objectMapper = new ObjectMapper();

    @FXML
    public void initialize() {
        loadRecommendations();
    }

    @FXML
    public void loadRecommendations() {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            showStatus("Please log in to see recommendations.");
            return;
        }

        List<String> skills = parseSkills(user.getSkills());
        renderSkillTags(skills);

        if (skills.isEmpty()) {
            showStatus("Add skills to your profile to receive personalized job and course recommendations.");
            return;
        }

        if (!service.isConfigured()) {
            showStatus("API keys not configured. Edit src/main/resources/recommendation.properties " +
                       "with your Adzuna and YouTube API keys to enable recommendations.");
            return;
        }

        showLoading(true);

        Task<List<Recommendation>> task = new Task<>() {
            @Override
            protected List<Recommendation> call() throws Exception {
                List<Recommendation> all = new ArrayList<>();
                all.addAll(service.fetchJobs(skills));
                all.addAll(service.fetchCourses(skills));
                return all;
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> populateResults(task.getValue())));
        task.setOnFailed(e -> Platform.runLater(() -> {
            if (task.getException() != null) task.getException().printStackTrace();
            showStatus("Could not load recommendations. Check your internet connection and API keys.");
        }));

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    // ── Populate UI ───────────────────────────────────────────────────────────

    private void populateResults(List<Recommendation> recommendations) {
        jobsContainer.getChildren().clear();
        coursesContainer.getChildren().clear();

        List<Recommendation> jobs = recommendations.stream()
                .filter(r -> r.getType() == Recommendation.Type.JOB)
                .sorted((a, b) -> b.getMatchScore() - a.getMatchScore())
                .toList();

        List<Recommendation> courses = recommendations.stream()
                .filter(r -> r.getType() == Recommendation.Type.COURSE)
                .sorted((a, b) -> b.getMatchScore() - a.getMatchScore())
                .toList();

        jobs.stream().map(this::buildCard).filter(n -> n != null).forEach(jobsContainer.getChildren()::add);
        courses.stream().map(this::buildCard).filter(n -> n != null).forEach(coursesContainer.getChildren()::add);

        jobCountLabel.setText(String.valueOf(jobs.size()));
        courseCountLabel.setText(String.valueOf(courses.size()));

        if (jobs.isEmpty())
            jobsContainer.getChildren().add(emptyNote("No job matches found for your skills."));
        if (courses.isEmpty())
            coursesContainer.getChildren().add(emptyNote("No courses found — verify your YouTube API key."));

        showLoading(false);
        contentPane.setVisible(true);
        contentPane.setManaged(true);
    }

    private Node buildCard(Recommendation rec) {
        try {
            URL fxmlUrl = getClass().getResource("/recommendations/recommendation_card.fxml");
            if (fxmlUrl == null) {
                System.err.println("recommendation_card.fxml not found on classpath.");
                return null;
            }
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Node card = loader.load();
            RecommendationCardController ctrl = loader.getController();
            ctrl.setRecommendation(rec);
            return card;
        } catch (IOException e) {
            System.err.println("RecommendationController: failed to load card: " + e.getMessage());
            return null;
        }
    }

    private Label emptyNote(String text) {
        Label lbl = new Label(text);
        lbl.getStyleClass().add("rec-empty");
        return lbl;
    }

    // ── Skill tags ────────────────────────────────────────────────────────────

    private void renderSkillTags(List<String> skills) {
        skillsContainer.getChildren().clear();
        for (String skill : skills) {
            Label tag = new Label(skill);
            tag.getStyleClass().add("rec-skill-tag");
            skillsContainer.getChildren().add(tag);
        }
    }

    // ── State helpers ─────────────────────────────────────────────────────────

    private void showLoading(boolean on) {
        loadingPane.setVisible(on);
        loadingPane.setManaged(on);
        if (on) {
            statusLabel.setVisible(false);
            statusLabel.setManaged(false);
            contentPane.setVisible(false);
            contentPane.setManaged(false);
        }
    }

    private void showStatus(String message) {
        loadingPane.setVisible(false);
        loadingPane.setManaged(false);
        contentPane.setVisible(false);
        contentPane.setManaged(false);
        statusLabel.setText(message);
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }

    // ── Skills JSON parsing ───────────────────────────────────────────────────

    private List<String> parseSkills(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}