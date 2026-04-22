package controllers.gestiondetemps;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import models.Event;
import services.EventStore;

import java.io.IOException;
import java.net.URL;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class AddEventController {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("H:mm");

    @FXML private Node rootContainer;
    @FXML private TextField titleField;
    @FXML private ComboBox<String> typeComboBox;
    @FXML private DatePicker datePicker;
    @FXML private TextField startTimeField;
    @FXML private TextField endTimeField;
    @FXML private TextField locationField;
    @FXML private ComboBox<String> priorityComboBox;
    @FXML private TextArea descriptionArea;
    @FXML private TextField reminderField;

    private final LocationAutocompleteHelper locationAutocompleteHelper = new LocationAutocompleteHelper();

    @FXML
    private void initialize() {
        datePicker.setValue(LocalDate.now());
        datePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (date.isBefore(LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color: #e0e0e0; -fx-text-fill: #aaaaaa;");
                }
            }
        });
        locationAutocompleteHelper.bind(locationField);
    }

    @FXML
    private void handleAddEvent(ActionEvent actionEvent) {
        if (!validateInput()) {
            return;
        }

        Event event = buildEvent();
        if ("Etude".equalsIgnoreCase(event.getType())) {
            openStudyQuizFlow(event);
            return;
        }

        EventStore.getInstance().addEvent(event);
        showAlert(Alert.AlertType.INFORMATION, "Succes", "L'evenement a ete ajoute avec succes.");
        clearForm();
    }

    @FXML
    private void handleCancel(ActionEvent actionEvent) {
        loadScreen("/Gestion de temps/events_list.fxml");
    }

    private void openStudyQuizFlow(Event event) {
        try {
            URL resource = getClass().getResource("/Gestion de temps/study_quiz.fxml");
            if (resource == null) {
                throw new IOException("Missing FXML resource: /Gestion de temps/study_quiz.fxml");
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent content = loader.load();
            StudyQuizController controller = loader.getController();
            controller.configure(event);
            replaceCurrentContent(content);
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", e.getMessage());
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
            showAlert(Alert.AlertType.ERROR, "Navigation Error", e.getMessage());
        }
    }

    private Event buildEvent() {
        String reminderStr = reminderField.getText().trim();
        LocalDate localDate = datePicker.getValue();

        Timestamp startTs = null;
        Timestamp endTs = null;
        try {
            if (!startTimeField.getText().trim().isEmpty()) {
                startTs = Timestamp.valueOf(LocalDateTime.of(localDate, parseTime(startTimeField.getText().trim())));
            }
            if (!endTimeField.getText().trim().isEmpty()) {
                endTs = Timestamp.valueOf(LocalDateTime.of(localDate, parseTime(endTimeField.getText().trim())));
            }
        } catch (DateTimeParseException ignored) {
        }

        Event event = new Event();
        event.setTitle(titleField.getText().trim());
        event.setType(typeComboBox.getValue());
        event.setDate(Date.valueOf(localDate));
        event.setLocation(locationField.getText().trim());
        event.setPriority(priorityComboBox.getValue());
        event.setDescription(descriptionArea.getText().trim());
        event.setStart_time(startTs);
        event.setEnd_time(endTs);
        event.setStatus("En cours");
        event.setCategory(typeComboBox.getValue());
        event.setNotes(descriptionArea.getText().trim());
        event.setReminder_minutes(reminderStr.isEmpty() ? 1 : Integer.parseInt(reminderStr));
        event.setDuration(calculateDurationMinutes(startTs, endTs));
        return event;
    }

    private boolean validateInput() {
        StringBuilder err = new StringBuilder();
        if (titleField.getText() == null || titleField.getText().trim().isEmpty()) {
            err.append("- Le titre est obligatoire.\n");
        }
        if (typeComboBox.getValue() == null) {
            err.append("- Le type est obligatoire.\n");
        }
        if (datePicker.getValue() == null) {
            err.append("- La date est obligatoire.\n");
        } else if (datePicker.getValue().isBefore(LocalDate.now())) {
            err.append("- La date ne peut pas etre dans le passe.\n");
        }

        boolean startOk = startTimeField.getText().trim().isEmpty()
                || startTimeField.getText().trim().matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$");
        boolean endOk = endTimeField.getText().trim().isEmpty()
                || endTimeField.getText().trim().matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$");

        if (!startOk) {
            err.append("- Heure de debut invalide (HH:mm).\n");
        }
        if (!endOk) {
            err.append("- Heure de fin invalide (HH:mm).\n");
        }

        if (startOk && endOk
                && !startTimeField.getText().trim().isEmpty()
                && !endTimeField.getText().trim().isEmpty()) {
            if (!parseTime(endTimeField.getText().trim()).isAfter(parseTime(startTimeField.getText().trim()))) {
                err.append("- L'heure de fin doit etre apres l'heure de debut.\n");
            }
        }

        if (!reminderField.getText().trim().isEmpty()) {
            try {
                Integer.parseInt(reminderField.getText().trim());
            } catch (NumberFormatException e) {
                err.append("- Le rappel doit etre un entier.\n");
            }
        }

        if (err.length() > 0) {
            showAlert(Alert.AlertType.ERROR, "Erreur de validation", err.toString());
            return false;
        }
        return true;
    }

    private int calculateDurationMinutes(Timestamp start, Timestamp end) {
        if (start != null && end != null) {
            long millis = end.getTime() - start.getTime();
            return millis <= 0L ? 60 : (int) (millis / 60000L);
        }
        return 60;
    }

    private LocalTime parseTime(String value) {
        return LocalTime.parse(value, TIME_FMT);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void clearForm() {
        titleField.clear();
        typeComboBox.setValue(null);
        datePicker.setValue(LocalDate.now());
        startTimeField.clear();
        endTimeField.clear();
        locationField.clear();
        priorityComboBox.setValue(null);
        reminderField.clear();
        descriptionArea.clear();
    }
}
