package controllers.gestiondetemps;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import models.Event;

public class PomodoroController {

    @FXML
    private VBox mainContainer;

    private Label eventTitleLabel;
    private Label sessionInfoLabel;
    private Label timerLabel;
    private Label modeLabelText;
    private Arc progressArc;
    private Button startButton;
    private Button pauseButton;
    private Button resetButton;
    private Label sessionsLabel;
    private Label minutesLabel;
    private Label pomodorosLabel;

    private Event event;
    private Timeline timeline;

    // Constants
    private static final int WORK_TIME = 25 * 60; // 25 minutes
    private static final int SHORT_BREAK = 5 * 60; // 5 minutes
    private static final int LONG_BREAK = 20 * 60; // 20 minutes
    private static final int SESSIONS_BEFORE_LONG_BREAK = 4;

    // State
    private String currentMode = "work";
    private int currentSession = 1;
    private int timeLeft = WORK_TIME;
    private int totalTime = WORK_TIME;
    private boolean isRunning = false;
    private int completedSessions = 0;
    private int totalMinutes = 0;
    private int totalPomodoros = 0;

    @FXML
    private void initialize() {
        setupUI();
        updateMode();
        updateDisplay();
        updateProgress();
    }

    private void setupUI() {
        mainContainer.setAlignment(Pos.CENTER);
        mainContainer.setSpacing(20);
        mainContainer.setStyle("-fx-background-color: linear-gradient(to bottom right, #667eea, #764ba2); -fx-padding: 40;");

        // Container blanc
        VBox whiteContainer = new VBox(20);
        whiteContainer.setAlignment(Pos.CENTER);
        whiteContainer.setMaxWidth(450);
        whiteContainer.setStyle("-fx-background-color: white; -fx-background-radius: 24; -fx-padding: 40; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0, 0, 10);");

        // Titre événement
        eventTitleLabel = new Label("Pomodoro Timer");
        eventTitleLabel.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 24));
        eventTitleLabel.setTextFill(Color.web("#1F2937"));

        // Session info
        sessionInfoLabel = new Label("Session 1/4 - Travail");
        sessionInfoLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 16));
        sessionInfoLabel.setTextFill(Color.web("#6B7280"));

        // Timer circulaire
        StackPane timerCircle = createTimerCircle();

        // Boutons de contrôle
        HBox controlsBox = createControls();

        // Statistiques
        HBox statsBox = createStats();

        whiteContainer.getChildren().addAll(
                eventTitleLabel,
                sessionInfoLabel,
                timerCircle,
                controlsBox,
                statsBox
        );

        mainContainer.getChildren().add(whiteContainer);
    }

    private StackPane createTimerCircle() {
        StackPane stack = new StackPane();
        stack.setPrefSize(280, 280);

        // Cercle de fond
        Circle bgCircle = new Circle(130);
        bgCircle.setFill(Color.TRANSPARENT);
        bgCircle.setStroke(Color.web("#E5E7EB"));
        bgCircle.setStrokeWidth(12);

        // Arc de progression
        progressArc = new Arc(140, 140, 130, 130, 90, 0);
        progressArc.setType(ArcType.OPEN);
        progressArc.setFill(Color.TRANSPARENT);
        progressArc.setStroke(Color.web("#F44336"));
        progressArc.setStrokeWidth(12);

        // Timer display
        VBox timerBox = new VBox(5);
        timerBox.setAlignment(Pos.CENTER);

        timerLabel = new Label("25:00");
        timerLabel.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 64));
        timerLabel.setTextFill(Color.web("#1F2937"));

        modeLabelText = new Label("Travail");
        modeLabelText.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 18));
        modeLabelText.setTextFill(Color.web("#6B7280"));

        timerBox.getChildren().addAll(timerLabel, modeLabelText);

        stack.getChildren().addAll(bgCircle, progressArc, timerBox);
        return stack;
    }

    private HBox createControls() {
        HBox controls = new HBox(15);
        controls.setAlignment(Pos.CENTER);

        startButton = new Button("▶️ Démarrer");
        startButton.setStyle("-fx-background-color: linear-gradient(to bottom, #10B981, #059669); " +
                "-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; " +
                "-fx-padding: 14 28; -fx-background-radius: 12; -fx-cursor: hand;");
        startButton.setOnAction(e -> startTimer());

        pauseButton = new Button("⏸️ Pause");
        pauseButton.setStyle("-fx-background-color: linear-gradient(to bottom, #F59E0B, #D97706); " +
                "-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; " +
                "-fx-padding: 14 28; -fx-background-radius: 12; -fx-cursor: hand;");
        pauseButton.setOnAction(e -> pauseTimer());
        pauseButton.setVisible(false);

        resetButton = new Button("🔄 Réinitialiser");
        resetButton.setStyle("-fx-background-color: linear-gradient(to bottom, #6B7280, #4B5563); " +
                "-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; " +
                "-fx-padding: 14 28; -fx-background-radius: 12; -fx-cursor: hand;");
        resetButton.setOnAction(e -> resetTimer());

        controls.getChildren().addAll(startButton, pauseButton, resetButton);
        return controls;
    }

    private HBox createStats() {
        HBox stats = new HBox(40);
        stats.setAlignment(Pos.CENTER);
        stats.setStyle("-fx-padding: 30 0 0 0; -fx-border-color: #E5E7EB; -fx-border-width: 2 0 0 0;");

        VBox sessionsBox = createStatItem("0", "Sessions");
        sessionsLabel = (Label) sessionsBox.getChildren().get(0);

        VBox minutesBox = createStatItem("0", "Minutes");
        minutesLabel = (Label) minutesBox.getChildren().get(0);

        VBox pomodorosBox = createStatItem("0", "Pomodoros");
        pomodorosLabel = (Label) pomodorosBox.getChildren().get(0);

        stats.getChildren().addAll(sessionsBox, minutesBox, pomodorosBox);
        return stats;
    }

    private VBox createStatItem(String value, String label) {
        VBox box = new VBox(5);
        box.setAlignment(Pos.CENTER);

        Label valueLabel = new Label(value);
        valueLabel.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 32));
        valueLabel.setTextFill(Color.web("#1F2937"));

        Label textLabel = new Label(label);
        textLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        textLabel.setTextFill(Color.web("#6B7280"));

        box.getChildren().addAll(valueLabel, textLabel);
        return box;
    }

    public void setEvent(Event event) {
        this.event = event;
        if (event != null && event.getTitle() != null) {
            eventTitleLabel.setText(event.getTitle());
        }
    }

    private void startTimer() {
        if (isRunning) return;

        isRunning = true;
        startButton.setVisible(false);
        pauseButton.setVisible(true);

        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            timeLeft--;
            updateDisplay();
            updateProgress();

            if (timeLeft <= 0) {
                playSound();
                completeSession();
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void pauseTimer() {
        isRunning = false;
        if (timeline != null) {
            timeline.stop();
        }
        startButton.setVisible(true);
        pauseButton.setVisible(false);
    }

    private void resetTimer() {
        pauseTimer();
        currentMode = "work";
        currentSession = 1;
        timeLeft = WORK_TIME;
        totalTime = WORK_TIME;
        updateMode();
        updateDisplay();
        updateProgress();
    }

    private void completeSession() {
        pauseTimer();

        if (currentMode.equals("work")) {
            completedSessions++;
            totalMinutes += 25;
            totalPomodoros++;

            sessionsLabel.setText(String.valueOf(completedSessions));
            minutesLabel.setText(String.valueOf(totalMinutes));
            pomodorosLabel.setText(String.valueOf(totalPomodoros));

            if (currentSession >= SESSIONS_BEFORE_LONG_BREAK) {
                currentMode = "longBreak";
                timeLeft = LONG_BREAK;
                totalTime = LONG_BREAK;
                currentSession = 1;
            } else {
                currentMode = "shortBreak";
                timeLeft = SHORT_BREAK;
                totalTime = SHORT_BREAK;
            }
        } else {
            if (currentMode.equals("shortBreak")) {
                currentSession++;
            }
            currentMode = "work";
            timeLeft = WORK_TIME;
            totalTime = WORK_TIME;
        }

        updateMode();
        updateDisplay();
        updateProgress();

        // Auto-start après 2 secondes
        Timeline delay = new Timeline(new KeyFrame(Duration.seconds(2), e -> startTimer()));
        delay.play();
    }

    private void updateMode() {
        if (currentMode.equals("work")) {
            progressArc.setStroke(Color.web("#F44336"));
            modeLabelText.setText("Travail");
            sessionInfoLabel.setText("Session " + currentSession + "/" + SESSIONS_BEFORE_LONG_BREAK + " - Travail");
        } else if (currentMode.equals("shortBreak")) {
            progressArc.setStroke(Color.web("#4CAF50"));
            modeLabelText.setText("Pause courte");
            sessionInfoLabel.setText("Session " + currentSession + "/" + SESSIONS_BEFORE_LONG_BREAK + " - Pause courte");
        } else {
            progressArc.setStroke(Color.web("#2196F3"));
            modeLabelText.setText("Pause longue");
            sessionInfoLabel.setText("Pause longue - Bien joué! 🎉");
        }
    }

    private void updateDisplay() {
        int minutes = timeLeft / 60;
        int seconds = timeLeft % 60;
        timerLabel.setText(String.format("%02d:%02d", minutes, seconds));
    }

    private void updateProgress() {
        double progress = (double) timeLeft / totalTime;
        double angle = 360 * progress;
        progressArc.setLength(-angle);
    }

    private void playSound() {
        // Simple beep using JavaFX
        try {
            java.awt.Toolkit.getDefaultToolkit().beep();
        } catch (Exception e) {
            // Ignore if sound fails
        }
    }
}
