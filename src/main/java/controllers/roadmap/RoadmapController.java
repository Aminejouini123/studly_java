package controllers.roadmap;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import models.RoadmapStep;
import services.RoadmapService;
import services.chat.OpenRouterConfigStore;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.List;

public class RoadmapController {

    @FXML private TextField skillInput;
    @FXML private Button    generateBtn;
    @FXML private Button    changeKeyBtn;
    @FXML private VBox      roadmapContainer;
    @FXML private VBox      loadingOverlay;
    @FXML private Label     statusLabel;

    @FXML private VBox      apiKeySetupPane;
    @FXML private TextField apiKeyField;
    @FXML private Label     keyPathLabel;

    private RoadmapService roadmapService = new RoadmapService();

    @FXML
    public void initialize() {
        skillInput.setOnAction(e -> handleGenerate());
        String keyPath = new OpenRouterConfigStore().getConfigPath().toString();
        if (keyPathLabel != null) {
            keyPathLabel.setText("Stored at: " + keyPath);
        }
        if (!roadmapService.isConfigured()) {
            showApiKeySetup(true);
        }
    }

    // ── API-key setup ─────────────────────────────────────────────────────────

    @FXML
    public void handleSaveApiKey() {
        String key = apiKeyField.getText().trim();
        if (key.isBlank()) {
            showStatus("Please paste a valid OpenRouter API key (starts with sk-or-).", true);
            return;
        }
        try {
            new OpenRouterConfigStore().saveApiKey(key);
            roadmapService = new RoadmapService();
            showApiKeySetup(false);
            apiKeyField.clear();
            showStatus("API key saved. Enter a skill and generate your roadmap.", false);
        } catch (IOException e) {
            showStatus("Could not save key: " + e.getMessage(), true);
        }
    }

    @FXML
    public void handleChangeKey() {
        apiKeyField.clear();
        showApiKeySetup(true);
        hideStatus();
    }

    @FXML
    public void openOpenRouterSite() {
        try {
            Desktop.getDesktop().browse(URI.create("https://openrouter.ai/keys"));
        } catch (Exception ignored) {}
    }

    // ── Generate ──────────────────────────────────────────────────────────────

    @FXML
    public void handleGenerate() {
        String skill = skillInput.getText().trim();
        if (skill.isBlank()) {
            showStatus("Enter a skill name (e.g. \"Python\", \"React\", \"DevOps\").", true);
            return;
        }
        if (!roadmapService.isConfigured()) {
            showApiKeySetup(true);
            return;
        }
        runGeneration(skill);
    }

    // ── Background generation ─────────────────────────────────────────────────

    private void runGeneration(String skill) {
        setLoading(true);
        roadmapContainer.getChildren().clear();
        hideStatus();

        Service<List<RoadmapStep>> svc = new Service<>() {
            @Override
            protected Task<List<RoadmapStep>> createTask() {
                return new Task<>() {
                    @Override
                    protected List<RoadmapStep> call() throws Exception {
                        return roadmapService.generateRoadmap(skill);
                    }
                };
            }
        };

        svc.setOnSucceeded(e -> Platform.runLater(() -> {
            setLoading(false);
            renderSteps(svc.getValue());
        }));

        svc.setOnFailed(e -> Platform.runLater(() -> {
            setLoading(false);
            Throwable ex = svc.getException();
            String msg = ex != null ? ex.getMessage() : "Unknown error";

            if (isAuthError(msg)) {
                // 401 → key is invalid/expired; force re-entry
                apiKeyField.clear();
                showStatus("API key is invalid or expired — enter a new one below.", true);
                showApiKeySetup(true);
            } else {
                showStatus("Generation failed: " + msg, true);
            }
            if (ex != null) ex.printStackTrace();
        }));

        svc.start();
    }

    private static boolean isAuthError(String msg) {
        if (msg == null) return false;
        String lower = msg.toLowerCase();
        return lower.contains("401") || lower.contains("unauthorized")
                || lower.contains("missing authentication") || lower.contains("invalid/revoked");
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    private void renderSteps(List<RoadmapStep> steps) {
        if (steps == null || steps.isEmpty()) {
            showStatus("The AI returned no steps. Try rephrasing the skill name.", false);
            return;
        }
        for (int i = 0; i < steps.size(); i++) {
            Node card = buildCard(steps.get(i), i == steps.size() - 1);
            if (card != null) {
                roadmapContainer.getChildren().add(card);
                animateIn(card, i);
            }
        }
    }

    private Node buildCard(RoadmapStep step, boolean isLast) {
        try {
            URL fxml = getClass().getResource("/roadmap/RoadmapStepItem.fxml");
            if (fxml == null) { System.err.println("RoadmapStepItem.fxml not found."); return null; }
            FXMLLoader loader = new FXMLLoader(fxml);
            Node card = loader.load();
            RoadmapStepController ctrl = loader.getController();
            ctrl.setStep(step, isLast);
            return card;
        } catch (IOException e) {
            System.err.println("Failed to load step card: " + e.getMessage());
            return null;
        }
    }

    // ── Animation ─────────────────────────────────────────────────────────────

    private void animateIn(Node node, int index) {
        node.setOpacity(0);
        FadeTransition fade   = new FadeTransition(Duration.millis(450), node);
        fade.setFromValue(0); fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(450), node);
        slide.setFromY(28); slide.setToY(0);
        ParallelTransition anim = new ParallelTransition(node, fade, slide);
        anim.setDelay(Duration.millis(index * 90L));
        anim.play();
    }

    // ── UI state helpers ──────────────────────────────────────────────────────

    private void showApiKeySetup(boolean on) {
        apiKeySetupPane.setVisible(on);
        apiKeySetupPane.setManaged(on);
        generateBtn.setDisable(on);
        changeKeyBtn.setVisible(!on);
        changeKeyBtn.setManaged(!on);
    }

    private void setLoading(boolean on) {
        loadingOverlay.setVisible(on);
        loadingOverlay.setManaged(on);
        generateBtn.setDisable(on);
        skillInput.setDisable(on);
    }

    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setStyle(isError
                ? "-fx-text-fill: #EF4444; -fx-font-size: 13px;"
                : "-fx-text-fill: #16A34A; -fx-font-size: 13px; -fx-font-weight: bold;");
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }

    private void hideStatus() {
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
    }
}
