package controllers.gestiondetemps;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import models.Event;
import services.EventStore;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class AddEventController {

    @FXML
    private Node rootContainer;

    @FXML
    private TextField titleField;

    @FXML
    private ComboBox<String> typeComboBox;

    @FXML
    private DatePicker datePicker;

    @FXML
    private TextField startTimeField;

    @FXML
    private TextField endTimeField;

    @FXML
    private TextField locationField;

    @FXML
    private ComboBox<String> priorityComboBox;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private TextField reminderField;

    @FXML
    private void initialize() {
    }

    @FXML
    private void handleAddEvent(ActionEvent actionEvent) {
        if (!validateInput()) {
            return;
        }

        String title = titleField.getText().trim();
        String type = typeComboBox.getValue();
        LocalDate localDate = datePicker.getValue();
        String location = locationField.getText().trim();
        String priority = priorityComboBox.getValue();
        String description = descriptionArea.getText().trim();
        String reminderStr = reminderField.getText().trim();

        Timestamp startTimestamp = null;
        Timestamp endTimestamp = null;
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        try {
            if (!startTimeField.getText().trim().isEmpty()) {
                LocalTime startTime = LocalTime.parse(startTimeField.getText().trim(), timeFormatter);
                startTimestamp = Timestamp.valueOf(LocalDateTime.of(localDate, startTime));
            }
            if (!endTimeField.getText().trim().isEmpty()) {
                LocalTime endTime = LocalTime.parse(endTimeField.getText().trim(), timeFormatter);
                endTimestamp = Timestamp.valueOf(LocalDateTime.of(localDate, endTime));
            }
        } catch (DateTimeParseException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur de format", "Le format de l'heure doit etre HH:mm.");
            return;
        }

        Event event = new Event();
        event.setTitle(title);
        event.setType(type);
        event.setDate(Date.valueOf(localDate));
        event.setLocation(location);
        event.setPriority(priority);
        event.setDescription(description);
        event.setStart_time(startTimestamp);
        event.setEnd_time(endTimestamp);
        event.setStatus("En cours");
        event.setCategory(type);
        event.setNotes(description);
        event.setReminder_minutes(reminderStr.isEmpty() ? 1 : Integer.parseInt(reminderStr));
        event.setDuration(calculateDurationMinutes(startTimestamp, endTimestamp));

        EventStore.getInstance().addEvent(event);

        showAlert(Alert.AlertType.INFORMATION, "Succes", "L'evenement a ete ajoute avec succes.");
        clearForm();
    }

    @FXML
    private void handleCancel(ActionEvent actionEvent) {
        if (rootContainer != null && rootContainer.getParent() instanceof javafx.scene.layout.Pane) {
            ((javafx.scene.layout.Pane) rootContainer.getParent()).getChildren().remove(rootContainer);
        }
    }

    private boolean validateInput() {
        StringBuilder errorMessage = new StringBuilder();

        if (titleField.getText() == null || titleField.getText().trim().isEmpty()) {
            errorMessage.append("- Le titre est obligatoire.\n");
        }
        if (typeComboBox.getValue() == null) {
            errorMessage.append("- Le type est obligatoire.\n");
        }
        if (datePicker.getValue() == null) {
            errorMessage.append("- La date est obligatoire.\n");
        }
        if (!startTimeField.getText().trim().isEmpty() && !startTimeField.getText().trim().matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")) {
            errorMessage.append("- L'heure de debut doit etre au format HH:mm.\n");
        }
        if (!endTimeField.getText().trim().isEmpty() && !endTimeField.getText().trim().matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")) {
            errorMessage.append("- L'heure de fin doit etre au format HH:mm.\n");
        }
        if (!reminderField.getText().trim().isEmpty()) {
            try {
                Integer.parseInt(reminderField.getText().trim());
            } catch (NumberFormatException e) {
                errorMessage.append("- Le rappel doit etre un entier valide.\n");
            }
        }

        if (errorMessage.length() > 0) {
            showAlert(Alert.AlertType.ERROR, "Erreur de validation", errorMessage.toString());
            return false;
        }

        return true;
    }

    private int calculateDurationMinutes(Timestamp startTimestamp, Timestamp endTimestamp) {
        if (startTimestamp == null || endTimestamp == null) {
            return 60;
        }

        long millis = endTimestamp.getTime() - startTimestamp.getTime();
        if (millis <= 0) {
            return 60;
        }
        return (int) (millis / 60000);
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void clearForm() {
        titleField.clear();
        typeComboBox.setValue(null);
        datePicker.setValue(null);
        startTimeField.clear();
        endTimeField.clear();
        locationField.clear();
        priorityComboBox.setValue(null);
        reminderField.clear();
        descriptionArea.clear();
    }
}
