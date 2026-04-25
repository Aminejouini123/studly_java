package controllers.activities;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import models.Course;
import models.quiz.QuizConfig;
import models.quiz.QuizQuestion;
import models.quiz.QuizResult;
import services.chat.QuizGeneratorService;
import models.Activity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class QuizController extends BaseActivityController {

    @FXML private StackPane rootPane;
    @FXML private Label courseLabel;

    // config
    @FXML private HBox configPane;
    @FXML private TextField activityNameField;
    @FXML private Spinner<Integer> questionsSpinner;
    @FXML private ComboBox<String> typeCombo;
    @FXML private ComboBox<String> difficultyCombo;
    @FXML private TextField topicField;
    @FXML private Button generateBtn;
    @FXML private Label statusLabel;

    // quiz
    @FXML private VBox quizPane;
    @FXML private Label quizTitleLabel;
    @FXML private Label progressLabel;
    @FXML private ProgressBar progressBar;
    @FXML private Label questionPromptLabel;
    @FXML private VBox answersBox;
    @FXML private Label feedbackLabel;
    @FXML private Button nextBtn;

    // result
    @FXML private VBox resultPane;
    @FXML private Label scoreLabel;
    @FXML private Label percentLabel;

    private final QuizGeneratorService quizGeneratorService = new QuizGeneratorService();
    private final services.ActivityService activityService = new services.ActivityService();

    private Course course;
    private File coursePdf;

    private List<QuizQuestion> questions = new ArrayList<>();
    private int idx = 0;
    private int correctCount = 0;
    private Activity currentActivity;
    private boolean reviewMode = false;

    @FXML
    public void initialize() {
        questionsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 30, 10));
        typeCombo.getItems().setAll("Multiple Choice (MCQ)", "True / False", "Short Answer");
        typeCombo.setValue("Multiple Choice (MCQ)");
        difficultyCombo.getItems().setAll("Easy", "Medium", "Hard");
        difficultyCombo.setValue("Medium");
        statusLabel.setText("");

        showConfig();
    }

    public void setCourse(Course course) {
        this.course = course;
        String name = course != null && course.getName() != null ? course.getName() : "—";
        if (courseLabel != null) {
            courseLabel.setText(name);
        }
        if (topicField != null && (topicField.getText() == null || topicField.getText().isBlank())) {
            topicField.setText(name);
        }
        if (activityNameField != null && (activityNameField.getText() == null || activityNameField.getText().isBlank())) {
            activityNameField.setText(name + " — Quiz");
        }
        String p = course != null ? course.getCourse_file() : null;
        if (p != null && !p.trim().isEmpty()) {
            File f = new File(p.trim());
            if (f.exists() && f.isFile() && f.getName().toLowerCase().endsWith(".pdf")) {
                coursePdf = f;
            }
        }
    }

    @FXML
    public void handleBack() {
        if (course == null) return;
        try {
            // If we came from course detail, go back there
            if (controllers.FrontendController.getInstance() != null) {
                // Check if we can find the CourseDetailController or just reload course details
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/gestion_cours/frontend_course_detail.fxml"));
                javafx.scene.Parent root = loader.load();
                controllers.courses.CourseDetailController controller = loader.getController();
                controller.populateCourseDetails(course);
                controllers.FrontendController.getInstance().loadContentNode(root);
            } else {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/gestion_activites/frontend_activities.fxml"));
                javafx.scene.Parent root = loader.load();
                ActivityListController controller = loader.getController();
                controller.setCourse(course);
                rootPane.getScene().setRoot(root);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleGenerate() {
        try {
            if (!quizGeneratorService.isConfigured()) {
                statusLabel.setText("Missing API key.");
                return;
            }
            if (coursePdf == null) {
                statusLabel.setText("PDF not found.");
                return;
            }

            QuizConfig cfg = readConfig();
            setBusy(true, "Initiating Generation...");

            CompletableFuture.supplyAsync(() -> {
                try {
                    return quizGeneratorService.generateQuizFromPdf(coursePdf, course != null ? course.getName() : "", cfg);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).thenAccept(qs -> Platform.runLater(() -> {
                this.questions = qs;
                this.idx = 0;
                this.correctCount = 0;
                saveQuizToDb(cfg);
                showQuiz();
                renderQuestion();
                setBusy(false, "");
            })).exceptionally(ex -> {
                Platform.runLater(() -> {
                    setBusy(false, "");
                    statusLabel.setText("Failed: " + rootMessage(ex));
                    ex.printStackTrace();
                });
                return null;
            });
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Error: " + e.getMessage());
            setBusy(false, "");
        }
    }

    @FXML
    public void handleNext() {
        idx++;
        if (idx >= questions.size()) {
            showResult();
        } else {
            renderQuestion();
        }
    }

    @FXML
    public void handleRetry() {
        idx = 0;
        correctCount = 0;
        showQuiz();
        renderQuestion();
    }

    @FXML
    public void handleBackToConfig() {
        showConfig();
    }

    private QuizConfig readConfig() {
        String name = activityNameField != null ? activityNameField.getText() : "";
        int n = questionsSpinner != null ? questionsSpinner.getValue() : 10;
        QuizConfig.QuestionType t = QuizConfig.QuestionType.MCQ;
        if (typeCombo != null) {
            String v = typeCombo.getValue();
            if ("True / False".equals(v)) t = QuizConfig.QuestionType.TRUE_FALSE;
            else if ("Short Answer".equals(v)) t = QuizConfig.QuestionType.SHORT_ANSWER;
        }
        QuizConfig.Difficulty d = QuizConfig.Difficulty.MEDIUM;
        if (difficultyCombo != null) {
            String v = difficultyCombo.getValue();
            if ("Easy".equals(v)) d = QuizConfig.Difficulty.EASY;
            else if ("Hard".equals(v)) d = QuizConfig.Difficulty.HARD;
        }
        String topic = topicField != null ? topicField.getText() : "";
        return new QuizConfig(name, n, t, d, topic);
    }

    private void renderQuestion() {
        if (questions == null || questions.isEmpty()) return;
        QuizQuestion q = questions.get(idx);

        quizTitleLabel.setText("AI Assessment");
        progressLabel.setText("QUESTION " + (idx + 1) + " / " + questions.size());
        progressBar.setProgress((idx + 1) / (double) questions.size());

        questionPromptLabel.setText(q.getPrompt());
        feedbackLabel.setText("");
        nextBtn.setDisable(true);
        answersBox.getChildren().clear();

        if (q.getType() == QuizConfig.QuestionType.SHORT_ANSWER) {
            TextField field = new TextField();
            field.setPromptText("Type your answer...");
            field.getStyleClass().add("modern-input");
            Button submit = new Button("Submit Answer");
            submit.getStyleClass().add("btn-ai-generate");
            submit.setOnAction(e -> validateShortAnswer(q, field.getText()));
            answersBox.getChildren().addAll(field, submit);
            return;
        }

        for (String opt : q.getOptions()) {
            Button b = new Button(opt);
            b.setMaxWidth(Double.MAX_VALUE);
            b.getStyleClass().add("choice-card-dark");
            if (reviewMode) {
                b.setDisable(true);
                if (opt.equalsIgnoreCase(q.getCorrectAnswer())) {
                    b.setStyle("-fx-background-color: rgba(34, 197, 94, 0.2); -fx-border-color: #22C55E; -fx-text-fill: #4ADE80; -fx-font-weight: 800;");
                }
            } else {
                b.setOnAction(e -> {
                    highlightSelection(opt);
                    validateChoice(q, opt);
                });
            }
            answersBox.getChildren().add(b);
        }
        
        if (reviewMode) {
            feedbackLabel.setText("CORRECTION: " + q.getExplanation());
            nextBtn.setDisable(false);
            nextBtn.setText(idx >= questions.size() - 1 ? "Finish Review" : "Next Question");
        }
    }

    private void highlightSelection(String selected) {
        for (Node n : answersBox.getChildren()) {
            if (n instanceof Button) {
                Button b = (Button) n;
                b.getStyleClass().removeAll("choice-card-dark-selected");
                if (b.getText().equals(selected)) b.getStyleClass().add("choice-card-dark-selected");
            }
        }
    }

    private void validateChoice(QuizQuestion q, String chosen) {
        disableAnswerButtons(true);
        boolean correct = chosen != null && chosen.equalsIgnoreCase(q.getCorrectAnswer());
        if (correct) correctCount++;

        for (Node n : answersBox.getChildren()) {
            if (n instanceof Button) {
                Button b = (Button) n;
                String t = b.getText();
                if (t.equalsIgnoreCase(q.getCorrectAnswer())) {
                    b.setStyle("-fx-background-color: rgba(34, 197, 94, 0.2); -fx-border-color: #22C55E; -fx-text-fill: #4ADE80; -fx-font-weight: 800;");
                } else if (t.equalsIgnoreCase(chosen)) {
                    b.setStyle("-fx-background-color: rgba(239, 68, 68, 0.2); -fx-border-color: #EF4444; -fx-text-fill: #FCA5A5; -fx-font-weight: 800;");
                }
            }
        }
        feedbackLabel.setText((correct ? "Correct. " : "Incorrect. ") + q.getExplanation());
        nextBtn.setDisable(false);
    }

    private void validateShortAnswer(QuizQuestion q, String typed) {
        disableAnswerButtons(true);
        boolean correct = typed != null && typed.trim().equalsIgnoreCase(q.getCorrectAnswer().trim());
        if (correct) correctCount++;
        feedbackLabel.setText((correct ? "Correct. " : "Incorrect. ") + q.getExplanation());
        nextBtn.setDisable(false);
    }

    private void disableAnswerButtons(boolean disabled) {
        for (Node n : answersBox.getChildren()) n.setDisable(disabled);
    }

    private void showConfig() {
        configPane.setVisible(true); configPane.setManaged(true);
        quizPane.setVisible(false); quizPane.setManaged(false);
        resultPane.setVisible(false); resultPane.setManaged(false);
        statusLabel.setText("");
    }

    private void showQuiz() {
        configPane.setVisible(false); configPane.setManaged(false);
        quizPane.setVisible(true); quizPane.setManaged(true);
        resultPane.setVisible(false); resultPane.setManaged(false);
        fadeIn(quizPane);
    }

    private void showResult() {
        configPane.setVisible(false); configPane.setManaged(false);
        quizPane.setVisible(false); quizPane.setManaged(false);
        resultPane.setVisible(true); resultPane.setManaged(true);
        QuizResult res = new QuizResult(correctCount, questions.size());
        String scoreText = res.getCorrect() + " / " + res.getTotal();
        scoreLabel.setText(scoreText);
        percentLabel.setText(res.getPercentRounded() + "%");
        fadeIn(resultPane);

        // Save result to DB if not in review mode
        if (!reviewMode && currentActivity != null) {
            try {
                currentActivity.setStatus("Completed");
                currentActivity.setExpected_output(scoreText);
                currentActivity.setCompleted_at(new java.sql.Timestamp(System.currentTimeMillis()));
                activityService.modifier(currentActivity);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void setBusy(boolean busy, String status) {
        if (generateBtn != null) {
            generateBtn.setDisable(busy);
            generateBtn.setText(busy ? "Initiating Generation..." : "Initiate Generation");
        }
        if (statusLabel != null) statusLabel.setText(status);
    }

    private void fadeIn(Node node) {
        FadeTransition ft = new FadeTransition(Duration.millis(400), node);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    private void saveQuizToDb(QuizConfig cfg) {
        if (course == null) return;
        try {
            String questionsJson = quizGeneratorService.serializeQuestions(questions);
            Activity act = new Activity(
                cfg.getActivityName().isBlank() ? (course.getName() + " — Quiz") : cfg.getActivityName(),
                "AI Generated Quiz about " + cfg.getTopic(),
                null, null, 15, "To Do", cfg.getDifficulty().name(), "Intermediate", "Quiz",
                questionsJson, // Use instructions field for JSON
                "Pending", "Complete all questions.", null, course.getId(), 1
            );
            activityService.ajouter(act);
            this.currentActivity = act;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setQuizActivity(Activity activity) {
        this.currentActivity = activity;
        this.course = new Course(); // Dummy for now if needed, but we should load the real course if possible
        this.course.setId(activity.getCourse_id());
        
        try {
            String json = activity.getInstructions();
            if (json != null && !json.isBlank()) {
                this.questions = quizGeneratorService.deserializeQuestions(json);
                this.idx = 0;
                this.correctCount = 0;
                
                if ("Completed".equalsIgnoreCase(activity.getStatus())) {
                    this.reviewMode = true;
                    // Try to parse previous score if needed
                }
                
                showQuiz();
                renderQuestion();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String rootMessage(Throwable ex) {
        Throwable t = ex;
        while (t.getCause() != null) t = t.getCause();
        return t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
    }
}
