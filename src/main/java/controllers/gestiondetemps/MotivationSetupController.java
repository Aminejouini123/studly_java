package controllers.gestiondetemps;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import models.Event;
import models.Motivation;
import services.MotivationStore;

public class MotivationSetupController {

    @FXML
    private Label setupSubtitleLabel;

    @FXML
    private Label currentStatusLabel;

    @FXML
    private TextField motivationLevelField;

    @FXML
    private ComboBox<String> emotionComboBox;

    @FXML
    private TextField preparationField;

    @FXML
    private TextField rewardField;

    private Event event;
    private Runnable onClose;

    @FXML
    private void initialize() {
        emotionComboBox.getItems().setAll("Focused", "Calm", "Excited", "Stressed", "Tired", "Confident");
    }

    public void configure(Event event, Runnable onClose) {
        this.event = event;
        this.onClose = onClose;

        String eventName = event == null || event.getTitle() == null || event.getTitle().trim().isEmpty()
                ? "cet event"
                : event.getTitle();
        setupSubtitleLabel.setText("Define your motivation strategy for " + eventName + ".");

        Motivation motivation = MotivationStore.getInstance().getForEvent(event);
        if (motivation != null) {
            motivationLevelField.setText(String.valueOf(motivation.getMotivation_level()));
            emotionComboBox.setValue(motivation.getEmotion());
            preparationField.setText(motivation.getPreparation());
            rewardField.setText(motivation.getReward());
        } else {
            motivationLevelField.setText("5");
            emotionComboBox.setValue("Stressed");
        }

        refreshStatus();
    }

    @FXML
    private void handleBack() {
        closeView();
    }

    @FXML
    private void handleCancel() {
        closeView();
    }

    @FXML
    private void handleDelete() {
        MotivationStore.getInstance().deleteForEvent(event);
        closeView();
    }

    @FXML
    private void handleSave() {
        if (!validateInput()) {
            return;
        }

        Motivation motivation = MotivationStore.getInstance().getForEvent(event);
        if (motivation == null) {
            motivation = new Motivation();
        }

        motivation.setMotivation_level(Integer.parseInt(motivationLevelField.getText().trim()));
        motivation.setEmotion(emotionComboBox.getValue());
        motivation.setPreparation(preparationField.getText().trim());
        motivation.setReward(rewardField.getText().trim());

        MotivationStore.getInstance().saveForEvent(event, motivation);
        refreshStatus();
        closeView();
    }

    private boolean validateInput() {
        try {
            int level = Integer.parseInt(motivationLevelField.getText().trim());
            if (level < 1 || level > 10) {
                showError("Motivation level must be between 1 and 10.");
                return false;
            }
        } catch (NumberFormatException e) {
            showError("Motivation level must be a valid number.");
            return false;
        }

        if (emotionComboBox.getValue() == null || emotionComboBox.getValue().trim().isEmpty()) {
            showError("Please choose an emotion.");
            return false;
        }

        return true;
    }

    private void refreshStatus() {
        String levelText = motivationLevelField.getText() == null || motivationLevelField.getText().trim().isEmpty()
                ? "-"
                : motivationLevelField.getText().trim();
        String emotionText = emotionComboBox.getValue() == null ? "-" : emotionComboBox.getValue();
        currentStatusLabel.setText("Level: " + levelText + "   Emotion: " + emotionText);
    }

    private void closeView() {
        if (onClose != null) {
            onClose.run();
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Motivation");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
