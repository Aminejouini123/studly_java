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
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.util.Duration;
import models.Roadmap;
import models.RoadmapStep;
import services.RoadmapService;
import services.chat.OpenRouterConfigStore;
import utils.SessionManager;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class RoadmapController {

    @FXML private TextField skillInput;
    @FXML private Button    generateBtn;
    @FXML private VBox      mainRoadmapArea;
    @FXML private Pane      mapPane;
    @FXML private VBox      detailCardContainer;
    @FXML private VBox      loadingOverlay;
    @FXML private Label     statusLabel;

    @FXML private VBox      apiKeySetupPane;
    @FXML private TextField apiKeyField;
    @FXML private Label     keyPathLabel;

    @FXML private Button    saveRoadmapBtn;
    @FXML private Button    myRoadmapsBtn;
    @FXML private VBox      savedRoadmapsPane;
    @FXML private VBox      savedRoadmapsList;

    private RoadmapService roadmapService;

    private String                       currentSkill;
    private List<RoadmapStep>            currentSteps;
    private List<RoadmapStepController>  stepControllers = new ArrayList<>();
    private boolean                      savedRoadmapsOpen = false;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMM d, yyyy").withZone(ZoneId.systemDefault());

    @FXML
    public void initialize() {
        try {
            roadmapService = new RoadmapService();
        } catch (Exception ex) {
            ex.printStackTrace();
            showStatus("Could not initialize AI service: " + ex.getMessage(), true);
            return;
        }

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
        hideSaveRoadmapBtn();
        runGeneration(skill);
    }

    // ── Save roadmap ──────────────────────────────────────────────────────────

    @FXML
    public void handleSaveRoadmap() {
        if (currentSteps == null || currentSteps.isEmpty()) return;
        var user = SessionManager.getCurrentUser();
        if (user == null) {
            showStatus("You must be logged in to save roadmaps.", true);
            return;
        }
        Roadmap roadmap = new Roadmap(currentSkill, user.getId(), currentSteps);
        try {
            roadmapService.saveRoadmap(roadmap);
            saveRoadmapBtn.setText("✓ Saved");
            saveRoadmapBtn.setDisable(true);
            showStatus("Roadmap saved to your profile!", false);
            // Activate checkboxes now that steps have DB ids
            for (RoadmapStepController ctrl : stepControllers) {
                ctrl.activateCheckbox();
            }
        } catch (Exception e) {
            showStatus("Failed to save: " + e.getMessage(), true);
        }
    }

    // ── Saved roadmaps panel ──────────────────────────────────────────────────

    @FXML
    public void handleToggleSavedRoadmaps() {
        savedRoadmapsOpen = !savedRoadmapsOpen;
        if (savedRoadmapsOpen) {
            loadSavedRoadmapsPanel();
            showSavedPanel(true);
        } else {
            showSavedPanel(false);
        }
    }

    private void loadSavedRoadmapsPanel() {
        var user = SessionManager.getCurrentUser();
        savedRoadmapsList.getChildren().clear();

        if (user == null) {
            Label msg = makeInfoLabel("Log in to view saved roadmaps.", "#EF4444");
            savedRoadmapsList.getChildren().add(msg);
            return;
        }
        try {
            List<Roadmap> roadmaps = roadmapService.getSavedRoadmaps(user.getId());
            if (roadmaps.isEmpty()) {
                savedRoadmapsList.getChildren().add(makeInfoLabel("No saved roadmaps yet. Generate one and hit Save!", "#64748B"));
            } else {
                for (Roadmap rm : roadmaps) {
                    savedRoadmapsList.getChildren().add(buildSavedRoadmapBtn(rm));
                }
            }
        } catch (Exception e) {
            savedRoadmapsList.getChildren().add(makeInfoLabel("Could not load: " + e.getMessage(), "#EF4444"));
        }
    }

    private Button buildSavedRoadmapBtn(Roadmap roadmap) {
        long done = roadmap.getSteps().stream().filter(RoadmapStep::isCompleted).count();
        long total = roadmap.getSteps().size();
        String dateStr = roadmap.getCreatedAt() != null
                ? DATE_FMT.format(roadmap.getCreatedAt().toInstant()) : "";
        String label = roadmap.getSkill() + "  (" + done + "/" + total + " steps)  ·  " + dateStr;

        Button btn = new Button(label);
        btn.getStyleClass().add("saved-roadmap-item");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(e -> loadSavedRoadmap(roadmap));
        return btn;
    }

    private void loadSavedRoadmap(Roadmap roadmap) {
        showSavedPanel(false);
        savedRoadmapsOpen = false;

        currentSkill = roadmap.getSkill();
        currentSteps = roadmap.getSteps();
        skillInput.setText(roadmap.getSkill());

        mapPane.getChildren().clear();
        detailCardContainer.getChildren().clear();
        mainRoadmapArea.setVisible(false);
        mainRoadmapArea.setManaged(false);
        stepControllers.clear();
        hideStatus();
        hideSaveRoadmapBtn();

        drawMap(currentSteps);
    }

    // ── Background generation ─────────────────────────────────────────────────

    private void runGeneration(String skill) {
        setLoading(true);
        mapPane.getChildren().clear();
        detailCardContainer.getChildren().clear();
        mainRoadmapArea.setVisible(false);
        mainRoadmapArea.setManaged(false);
        stepControllers.clear();
        hideStatus();
        currentSteps = null;

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
            currentSkill = skill;
            currentSteps = svc.getValue();
            drawMap(currentSteps);
            showSaveRoadmapBtn();
        }));

        svc.setOnFailed(e -> Platform.runLater(() -> {
            setLoading(false);
            Throwable ex = svc.getException();
            String msg = ex != null ? ex.getMessage() : "Unknown error";

            if (isAuthError(msg)) {
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

    // ── Rendering Snake Map & Detail Card ─────────────────────────────────────

    private List<Circle> mapCircles = new ArrayList<>();
    private List<Label> mapLabels = new ArrayList<>();

    private void drawMap(List<RoadmapStep> steps) {
        mapPane.getChildren().clear();
        detailCardContainer.getChildren().clear();
        mapCircles.clear();
        mapLabels.clear();
        
        if (steps == null || steps.isEmpty()) {
            showStatus("The AI returned no steps. Try rephrasing the skill name.", false);
            return;
        }

        mainRoadmapArea.setVisible(true);
        mainRoadmapArea.setManaged(true);

        int nodesPerRow = 4;
        double startX = 100;
        double startY = 80;
        double xSpacing = 240;
        double ySpacing = 140;
        double radius = 22;

        List<double[]> coords = new ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            int row = i / nodesPerRow;
            int col = i % nodesPerRow;
            
            // Left-to-right aligned grid (no snake)
            double x = startX + (col * xSpacing);
            double y = startY + (row * ySpacing);
            coords.add(new double[]{x, y});
        }

        // Draw connecting lines only for nodes on the same row
        for (int i = 0; i < coords.size() - 1; i++) {
            double[] p1 = coords.get(i);
            double[] p2 = coords.get(i + 1);
            
            if (p1[1] == p2[1]) {
                Path path = new Path();
                path.getStyleClass().add("map-line");
                path.getElements().add(new MoveTo(p1[0], p1[1]));
                path.getElements().add(new LineTo(p2[0], p2[1]));
                
                mapPane.getChildren().add(path);
                
                path.setOpacity(0);
                FadeTransition ft = new FadeTransition(Duration.millis(300), path);
                ft.setToValue(0.8);
                ft.setDelay(Duration.millis(i * 100L));
                ft.play();
            }
        }

        // Draw nodes
        for (int i = 0; i < steps.size(); i++) {
            final int index = i;
            double[] p = coords.get(i);
            
            StackPane nodeGroup = new StackPane();
            nodeGroup.setLayoutX(p[0] - radius);
            nodeGroup.setLayoutY(p[1] - radius);
            
            Circle circle = new Circle(radius);
            circle.getStyleClass().add("map-node");
            mapCircles.add(circle);
            
            Label number = new Label(String.valueOf(i + 1));
            number.getStyleClass().add("map-node-text");
            
            nodeGroup.getChildren().addAll(circle, number);
            
            Label titleLabel = new Label(steps.get(i).getTitle());
            titleLabel.getStyleClass().add("map-node-label");
            titleLabel.setMaxWidth(120);
            titleLabel.setWrapText(true);
            
            // Center label below node
            titleLabel.layoutXProperty().bind(nodeGroup.layoutXProperty().add(radius).subtract(titleLabel.widthProperty().divide(2)));
            titleLabel.setLayoutY(p[1] + radius + 10);
            mapLabels.add(titleLabel);
            
            nodeGroup.setOnMouseClicked(e -> selectStep(index, steps));
            titleLabel.setOnMouseClicked(e -> selectStep(index, steps));
            
            mapPane.getChildren().addAll(nodeGroup, titleLabel);
            
            nodeGroup.setOpacity(0);
            titleLabel.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(400), nodeGroup);
            ft.setFromValue(0); ft.setToValue(1);
            ft.setDelay(Duration.millis(i * 100L));
            ft.play();
            
            FadeTransition ftLabel = new FadeTransition(Duration.millis(400), titleLabel);
            ftLabel.setFromValue(0); ftLabel.setToValue(1);
            ftLabel.setDelay(Duration.millis(i * 100L));
            ftLabel.play();
        }
        
        // Auto-select first step
        if (!steps.isEmpty()) {
            selectStep(0, steps);
        }
    }

    private void selectStep(int index, List<RoadmapStep> steps) {
        for (int i = 0; i < mapCircles.size(); i++) {
            mapCircles.get(i).getStyleClass().remove("map-node-selected");
            mapLabels.get(i).getStyleClass().remove("map-node-label-selected");
            if (i == index) {
                mapCircles.get(i).getStyleClass().add("map-node-selected");
                mapLabels.get(i).getStyleClass().add("map-node-label-selected");
            }
        }
        loadDetailCard(steps.get(index), index == steps.size() - 1);
    }

    private void loadDetailCard(RoadmapStep step, boolean isLast) {
        try {
            detailCardContainer.getChildren().clear();
            URL fxml = getClass().getResource("/roadmap/RoadmapStepItem.fxml");
            if (fxml == null) { System.err.println("RoadmapStepItem.fxml not found."); return; }
            FXMLLoader loader = new FXMLLoader(fxml);
            Node card = loader.load();
            RoadmapStepController ctrl = loader.getController();
            ctrl.setStep(step, isLast, roadmapService);
            
            // Only add to list if not already there, but here we just need one active controller
            // Wait, save functionality needs all controllers to activate checkboxes.
            // In the new UX, we might only show one checkbox at a time.
            // For now, let's just clear and add this one to stepControllers so Save works for it, 
            // but actually Save needs to activate ALL checkboxes. 
            // We'll just manage the single active controller.
            stepControllers.clear();
            stepControllers.add(ctrl);
            if (saveRoadmapBtn.getText().equals("✓ Saved")) {
                ctrl.activateCheckbox();
            }
            
            detailCardContainer.getChildren().add(card);
            
            // Fade in card
            card.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(300), card);
            ft.setFromValue(0); ft.setToValue(1);
            ft.play();
            
        } catch (IOException e) {
            System.err.println("Failed to load step card: " + e.getMessage());
        }
    }

    // ── Animation ─────────────────────────────────────────────────────────────

    private void animateIn(Node node, int index) {
        node.setOpacity(0);
        FadeTransition fade  = new FadeTransition(Duration.millis(450), node);
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
    }

    private void setLoading(boolean on) {
        loadingOverlay.setVisible(on);
        loadingOverlay.setManaged(on);
        generateBtn.setDisable(on);
        skillInput.setDisable(on);
    }

    private void showSaveRoadmapBtn() {
        saveRoadmapBtn.setText("Save Roadmap");
        saveRoadmapBtn.setDisable(false);
        saveRoadmapBtn.setVisible(true);
        saveRoadmapBtn.setManaged(true);
    }

    private void hideSaveRoadmapBtn() {
        saveRoadmapBtn.setVisible(false);
        saveRoadmapBtn.setManaged(false);
    }

    private void showSavedPanel(boolean on) {
        savedRoadmapsPane.setVisible(on);
        savedRoadmapsPane.setManaged(on);
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

    private static Label makeInfoLabel(String text, String color) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px;");
        return lbl;
    }
}