package controllers.gestiondetemps;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import models.Event;
import org.json.JSONArray;
import org.json.JSONObject;
import services.EventStore;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class StudyQuizController {

    private enum QuizStep {
        LEARNING_STYLE,
        LEVEL
    }

    @FXML private VBox rootContainer;
    @FXML private Label headerLabel;
    @FXML private Label helperLabel;
    @FXML private Label statusLabel;
    @FXML private VBox questionsContainer;
    @FXML private Button primaryButton;
    @FXML private Button secondaryButton;

    private final StudyApiClient apiClient = new StudyApiClient();
    private final List<ToggleGroup> learningStyleGroups = new ArrayList<>();
    private final List<ToggleGroup> levelQuestionGroups = new ArrayList<>();

    private Event event;
    private JSONArray learningQuestions = new JSONArray();
    private JSONArray levelQuestions = new JSONArray();
    private QuizStep currentStep = QuizStep.LEARNING_STYLE;
    private String learningStyle = "";
    private String estimatedLevel = "";
    private static final int MIN_LEVEL_QUESTIONS = 5;

    public void configure(Event event) {
        this.event = event;
        loadLearningStyleQuiz();
    }

    @FXML
    private void handlePrimaryAction() {
        if (currentStep == QuizStep.LEARNING_STYLE) {
            submitLearningStyleAnswers();
        } else {
            submitLevelAnswer();
        }
    }

    @FXML
    private void handleSecondaryAction() {
        loadScreen("/Gestion de temps/add_event.fxml");
    }

    private void loadLearningStyleQuiz() {
        setLoadingState(true, "Chargement du quiz de style d'apprentissage...");
        CompletableFuture.supplyAsync(() -> {
            try {
                return apiClient.getLearningStyleQuestions();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).whenComplete((questions, error) -> Platform.runLater(() -> {
            setLoadingState(false, "");
            if (error != null) {
                showError("Impossible de charger le quiz.\n" + rootCauseMessage(error));
                return;
            }
            learningQuestions = questions;
            renderLearningStyleQuiz();
        }));
    }

    private void renderLearningStyleQuiz() {
        currentStep = QuizStep.LEARNING_STYLE;
        learningStyleGroups.clear();
        questionsContainer.getChildren().clear();

        headerLabel.setText("Quiz d'apprentissage");
        helperLabel.setText("Repondez a chaque question pour detecter votre style d'apprentissage.");
        statusLabel.setText("");
        primaryButton.setText("Analyser mon style");
        secondaryButton.setText("Retour");

        for (int index = 0; index < learningQuestions.length(); index++) {
            JSONObject question = learningQuestions.getJSONObject(index);
            Label questionLabel = new Label((index + 1) + ". " + question.optString("question", "Question"));
            questionLabel.getStyleClass().add("quiz-question");
            questionLabel.setWrapText(true);

            VBox block = new VBox(10);
            block.getStyleClass().add("quiz-card");
            block.getChildren().add(questionLabel);

            ToggleGroup group = new ToggleGroup();
            learningStyleGroups.add(group);

            JSONArray options = question.optJSONArray("options");
            if (options != null) {
                for (int optionIndex = 0; optionIndex < options.length(); optionIndex++) {
                    String optionText = options.optString(optionIndex, "");
                    RadioButton radioButton = new RadioButton(optionText);
                    radioButton.setToggleGroup(group);
                    radioButton.setUserData(extractAnswerCode(optionText, optionIndex));
                    radioButton.getStyleClass().add("quiz-option");
                    radioButton.setWrapText(true);
                    block.getChildren().add(radioButton);
                }
            }

            questionsContainer.getChildren().add(block);
        }
    }

    private void submitLearningStyleAnswers() {
        JSONArray answers = new JSONArray();
        for (int index = 0; index < learningStyleGroups.size(); index++) {
            Toggle selected = learningStyleGroups.get(index).getSelectedToggle();
            if (selected == null) {
                showError("Veuillez repondre a toutes les questions avant de continuer.");
                return;
            }

            JSONObject question = learningQuestions.getJSONObject(index);
            answers.put(new JSONObject()
                    .put("question_id", question.optString("id", "q" + (index + 1)))
                    .put("answer", String.valueOf(selected.getUserData())));
        }

        setLoadingState(true, "Analyse du style en cours...");
        CompletableFuture.supplyAsync(() -> {
            try {
                JSONObject styleResponse = apiClient.analyzeLearningStyle(event, answers);
                String subject = event.getTitle() == null || event.getTitle().isBlank() ? "Etude" : event.getTitle().trim();
                JSONArray levelResponses = new JSONArray();
                for (int index = 0; index < MIN_LEVEL_QUESTIONS; index++) {
                    levelResponses.put(apiClient.getLevelQuestion(subject, "moyenne"));
                }
                return new JSONObject()
                        .put("styleResponse", styleResponse)
                        .put("levelQuestions", levelResponses);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).whenComplete((result, error) -> Platform.runLater(() -> {
            setLoadingState(false, "");
            if (error != null) {
                showError("Impossible d'analyser le style.\n" + rootCauseMessage(error));
                return;
            }
            learningStyle = extractLearningStyle(result.getJSONObject("styleResponse"));
            levelQuestions = result.getJSONArray("levelQuestions");
            renderLevelQuestion();
        }));
    }

    private void renderLevelQuestion() {
        currentStep = QuizStep.LEVEL;
        questionsContainer.getChildren().clear();
        levelQuestionGroups.clear();

        headerLabel.setText("Evaluation du niveau");
        helperLabel.setText("Repondez aux questions suivantes pour estimer votre niveau.");
        statusLabel.setText("Style detecte : " + displayValue(learningStyle, "non determine"));
        primaryButton.setText("Generer mon plan");
        secondaryButton.setText("Retour au formulaire");

        for (int questionIndex = 0; questionIndex < levelQuestions.length(); questionIndex++) {
            JSONObject levelQuestion = levelQuestions.getJSONObject(questionIndex);

            Label questionLabel = new Label((questionIndex + 1) + ". " + levelQuestion.optString("question", "Question de niveau"));
            questionLabel.getStyleClass().add("quiz-question");
            questionLabel.setWrapText(true);

            VBox block = new VBox(10);
            block.getStyleClass().add("quiz-card");
            block.getChildren().add(questionLabel);

            ToggleGroup group = new ToggleGroup();
            levelQuestionGroups.add(group);

            JSONArray options = levelQuestion.optJSONArray("options");
            if (options != null) {
                for (int index = 0; index < options.length(); index++) {
                    String optionText = options.optString(index, "");
                    RadioButton radioButton = new RadioButton(optionText);
                    radioButton.setToggleGroup(group);
                    radioButton.setUserData(extractAnswerCode(optionText, index));
                    radioButton.getStyleClass().add("quiz-option");
                    radioButton.setWrapText(true);
                    block.getChildren().add(radioButton);
                }
            }

            questionsContainer.getChildren().add(block);
        }
    }

    private void submitLevelAnswer() {
        JSONArray answers = new JSONArray();
        for (int index = 0; index < levelQuestionGroups.size(); index++) {
            ToggleGroup group = levelQuestionGroups.get(index);
            if (group.getSelectedToggle() == null) {
                showError("Veuillez repondre a toutes les questions d'evaluation.");
                return;
            }

            JSONObject levelQuestion = levelQuestions.getJSONObject(index);
            String selectedAnswer = String.valueOf(group.getSelectedToggle().getUserData());
            answers.put(new JSONObject()
                    .put("question_id", levelQuestion.optString("id", "lq" + (index + 1)))
                    .put("user_answer", selectedAnswer)
                    .put("correct_answer", levelQuestion.optString("correct_answer", "A"))
                    .put("difficulty", levelQuestion.optString("difficulty", "moyenne")));
        }

        setLoadingState(true, "Generation du plan d'etude...");
        CompletableFuture.supplyAsync(() -> {
            try {
                JSONObject levelResponse = apiClient.estimateLevel(event, answers);
                String level = extractLevel(levelResponse);
                JSONObject plan = apiClient.generateStudyPlan(event, learningStyle, level);
                return new JSONObject()
                        .put("level", level)
                        .put("plan", plan);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).whenComplete((result, error) -> Platform.runLater(() -> {
            setLoadingState(false, "");
            if (error != null) {
                showError("Impossible de generer le plan d'etude.\n" + rootCauseMessage(error));
                return;
            }

            estimatedLevel = result.optString("level", "");
            JSONObject plan = result.getJSONObject("plan");
            event.setNotes(plan.toString(2));
            EventStore.getInstance().addEvent(event);
            
            // Show success message
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Succès");
            alert.setHeaderText("Plan d'étude généré !");
            alert.setContentText("Votre plan d'étude personnalisé a été créé.\nVous pouvez le consulter en cliquant sur 'Voir Plan' dans la liste des événements.");
            alert.showAndWait();
            
            // Return to events list
            loadScreen("/Gestion de temps/events_list.fxml");
        }));
    }

    private void openPlanResultScreen(JSONObject plan) {
        try {
            System.out.println("=== Opening Plan Result Screen ===");
            System.out.println("Plan JSON: " + plan.toString(2));
            System.out.println("Learning Style: " + learningStyle);
            System.out.println("Estimated Level: " + estimatedLevel);
            
            URL resource = getClass().getResource("/Gestion de temps/study_plan_result.fxml");
            if (resource == null) {
                throw new IOException("Missing FXML resource: /Gestion de temps/study_plan_result.fxml");
            }

            System.out.println("FXML Resource found: " + resource);
            
            FXMLLoader loader = new FXMLLoader(resource);
            Parent content = loader.load();
            System.out.println("FXML loaded successfully");
            
            StudyPlanResultController controller = loader.getController();
            System.out.println("Controller obtained: " + controller);
            
            controller.configure(event, plan, learningStyle, estimatedLevel);
            System.out.println("Controller configured");
            
            replaceCurrentContent(content);
            System.out.println("Content replaced successfully");
        } catch (IOException e) {
            System.err.println("Error opening plan result screen: " + e.getMessage());
            e.printStackTrace();
            showError(e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            showError("Erreur inattendue: " + e.getMessage());
        }
    }

    private void replaceCurrentContent(Parent content) {
        if (rootContainer.getParent() instanceof Pane) {
            ((Pane) rootContainer.getParent()).getChildren().setAll(content);
            return;
        }

        if (rootContainer.getScene() != null) {
            rootContainer.getScene().setRoot(content);
        }
    }

    private void loadScreen(String path) {
        try {
            URL resource = getClass().getResource(path);
            if (resource == null) {
                throw new IOException("Missing FXML resource: " + path);
            }
            Parent content = FXMLLoader.load(resource);
            replaceCurrentContent(content);
        } catch (IOException e) {
            showError(e.getMessage());
        }
    }

    private void setLoadingState(boolean loading, String message) {
        primaryButton.setDisable(loading);
        secondaryButton.setDisable(loading);
        statusLabel.setText(message);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur API Etude");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String extractAnswerCode(String optionText, int optionIndex) {
        if (optionText != null && !optionText.isEmpty() && Character.isLetter(optionText.charAt(0))) {
            return String.valueOf(optionText.charAt(0)).toUpperCase();
        }
        return String.valueOf((char) ('A' + optionIndex));
    }

    private String extractLearningStyle(JSONObject response) {
        String[] keys = {"style", "learning_style", "style_apprentissage", "result"};
        for (String key : keys) {
            if (response.has(key)) {
                return response.optString(key, "");
            }
        }
        return "";
    }

    private String extractLevel(JSONObject response) {
        String[] keys = {"level", "niveau", "estimated_level", "resultat"};
        for (String key : keys) {
            if (response.has(key)) {
                return response.optString(key, "");
            }
        }
        return "";
    }

    private String displayValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String rootCauseMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.toString() : current.getMessage();
    }
}
