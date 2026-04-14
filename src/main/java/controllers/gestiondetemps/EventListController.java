package controllers.gestiondetemps;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import models.Event;
import services.EventStore;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public class EventListController {

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> sortComboBox;

    @FXML
    private FlowPane cardsContainer;

    @FXML
    private Label emptyStateLabel;

    private final ObservableList<Event> masterData = EventStore.getInstance().getEvents();

    @FXML
    private void initialize() {
        sortComboBox.getItems().setAll(
                "Date (Plus proche d'abord)",
                "Date (Plus lointaine d'abord)",
                "Titre (A-Z)",
                "Titre (Z-A)",
                "Priorite"
        );
        sortComboBox.setValue("Date (Plus proche d'abord)");

        searchField.textProperty().addListener((observable, oldValue, newValue) -> refreshCards());
        sortComboBox.valueProperty().addListener((observable, oldValue, newValue) -> refreshCards());
        masterData.addListener((javafx.collections.ListChangeListener<Event>) change -> refreshCards());

        refreshCards();
    }

    private void refreshCards() {
        List<Event> filteredEvents = new ArrayList<>(masterData);
        String filterText = searchField.getText();

        if (filterText != null && !filterText.trim().isEmpty()) {
            String normalizedFilter = filterText.toLowerCase(Locale.ROOT);
            filteredEvents = filteredEvents.stream()
                    .filter(event -> matchesFilter(event, normalizedFilter))
                    .collect(Collectors.toList());
        }

        filteredEvents.sort(resolveComparator(sortComboBox.getValue()));

        cardsContainer.getChildren().setAll(
                filteredEvents.stream()
                        .map(this::buildEventCard)
                        .collect(Collectors.toList())
        );

        emptyStateLabel.setVisible(filteredEvents.isEmpty());
        emptyStateLabel.setManaged(filteredEvents.isEmpty());
    }

    private boolean matchesFilter(Event event, String filter) {
        return contains(event.getTitle(), filter)
                || contains(event.getDescription(), filter)
                || contains(event.getType(), filter)
                || contains(event.getLocation(), filter)
                || contains(event.getPriority(), filter);
    }

    private boolean contains(String value, String filter) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(filter);
    }

    private Comparator<Event> resolveComparator(String sortValue) {
        if ("Date (Plus lointaine d'abord)".equals(sortValue)) {
            return Comparator.comparing(Event::getDate, Comparator.nullsLast(Comparator.reverseOrder()));
        }
        if ("Titre (A-Z)".equals(sortValue)) {
            return Comparator.comparing(Event::getTitle, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        }
        if ("Titre (Z-A)".equals(sortValue)) {
            return Comparator.comparing(Event::getTitle, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)).reversed();
        }
        if ("Priorite".equals(sortValue)) {
            return Comparator.comparingInt((Event event) -> getPriorityWeight(event.getPriority())).reversed();
        }
        return Comparator.comparing(Event::getDate, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private int getPriorityWeight(String priority) {
        if (priority == null) {
            return 0;
        }
        switch (priority.toLowerCase(Locale.ROOT)) {
            case "haute":
                return 3;
            case "moyenne":
                return 2;
            case "basse":
                return 1;
            default:
                return 0;
        }
    }

    private VBox buildEventCard(Event event) {
        VBox card = new VBox(16);
        card.getStyleClass().add("event-card");

        HBox header = new HBox(12);
        Label titleLabel = new Label(defaultValue(event.getTitle(), "Sans titre"));
        titleLabel.getStyleClass().add("event-card-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label dateBadge = new Label(formatDate(event.getDate()));
        dateBadge.getStyleClass().add("event-date-badge");
        header.getChildren().addAll(titleLabel, spacer, dateBadge);

        Label descriptionLabel = new Label(defaultValue(event.getDescription(), "Aucune description"));
        descriptionLabel.getStyleClass().add("event-description");
        descriptionLabel.setWrapText(true);

        HBox pillsRow = new HBox(10);
        pillsRow.getChildren().addAll(
                createInfoPill(event.getDuration() + "m", "pill-neutral"),
                createInfoPill(defaultValue(event.getPriority(), "Sans priorite"), "pill-priority"),
                createInfoPill(defaultValue(event.getStatus(), "En cours"), "pill-status")
        );

        Label sessionsLabel = new Label(event.getReminder_minutes() + " Sessions Plan");
        sessionsLabel.getStyleClass().add("event-sessions");

        HBox actionsRow = new HBox(12);
        Button editButton = new Button("Modifier event");
        editButton.getStyleClass().add("event-edit-button");
        editButton.setOnAction(actionEvent -> showEditDialog(event));

        Button deleteButton = new Button("Supprimer event");
        deleteButton.getStyleClass().add("event-delete-button");
        deleteButton.setOnAction(actionEvent -> deleteEvent(event));

        actionsRow.getChildren().addAll(editButton, deleteButton);

        card.getChildren().addAll(header, descriptionLabel, pillsRow, sessionsLabel, actionsRow);
        return card;
    }

    private Label createInfoPill(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().addAll("info-pill", styleClass);
        return label;
    }

    private void showEditDialog(Event event) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modifier event");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(12);
        form.setPadding(new Insets(15));

        TextField titleField = new TextField(event.getTitle());
        TextArea descriptionArea = new TextArea(event.getDescription());
        descriptionArea.setPrefRowCount(3);
        TextField locationField = new TextField(event.getLocation());
        ComboBox<String> priorityBox = new ComboBox<>();
        priorityBox.getItems().addAll("Haute", "Moyenne", "Basse");
        priorityBox.setValue(defaultValue(event.getPriority(), "Moyenne"));
        DatePicker datePicker = new DatePicker(event.getDate() == null ? java.time.LocalDate.now() : event.getDate().toLocalDate());

        form.add(new Label("Titre"), 0, 0);
        form.add(titleField, 1, 0);
        form.add(new Label("Description"), 0, 1);
        form.add(descriptionArea, 1, 1);
        form.add(new Label("Lieu"), 0, 2);
        form.add(locationField, 1, 2);
        form.add(new Label("Priorite"), 0, 3);
        form.add(priorityBox, 1, 3);
        form.add(new Label("Date"), 0, 4);
        form.add(datePicker, 1, 4);

        dialog.getDialogPane().setContent(form);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Event updatedEvent = copyEvent(event);
            updatedEvent.setTitle(titleField.getText().trim());
            updatedEvent.setDescription(descriptionArea.getText().trim());
            updatedEvent.setLocation(locationField.getText().trim());
            updatedEvent.setPriority(priorityBox.getValue());
            updatedEvent.setDate(Date.valueOf(datePicker.getValue()));
            EventStore.getInstance().updateEvent(updatedEvent);
        }
    }

    private void deleteEvent(Event event) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Supprimer event");
        confirmation.setHeaderText(null);
        confirmation.setContentText("Voulez-vous supprimer cet event ?");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            EventStore.getInstance().deleteEvent(event);
        }
    }

    private Event copyEvent(Event source) {
        Event copy = new Event();
        copy.setId(source.getId());
        copy.setTitle(source.getTitle());
        copy.setDescription(source.getDescription());
        copy.setType(source.getType());
        copy.setDuration(source.getDuration());
        copy.setLocation(source.getLocation());
        copy.setStatus(source.getStatus());
        copy.setPriority(source.getPriority());
        copy.setDifficulty(source.getDifficulty());
        copy.setDate(source.getDate());
        copy.setStart_time(source.getStart_time());
        copy.setEnd_time(source.getEnd_time());
        copy.setColor(source.getColor());
        copy.setCategory(source.getCategory());
        copy.setNotes(source.getNotes());
        copy.setAll_day(source.getAll_day());
        copy.setReminder_minutes(source.getReminder_minutes());
        copy.setGoogle_event_id(source.getGoogle_event_id());
        copy.setMotivation_id(source.getMotivation_id());
        copy.setUser_id(source.getUser_id());
        return copy;
    }

    private String formatDate(Date date) {
        if (date == null) {
            return "Sans date";
        }
        return date.toLocalDate().format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH));
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }
}
