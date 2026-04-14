package controllers.gestiondetemps;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import models.Event;
import services.EventStore;

import java.io.IOException;
import java.net.URL;
import java.sql.Date;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class PlanningDashboardController {

    @FXML
    private Label eventsCountLabel;

    @FXML
    private Label completedCountLabel;

    @FXML
    private Label inProgressCountLabel;

    @FXML
    private Label totalCountLabel;

    @FXML
    private VBox eventCardsContainer;

    @FXML
    private StackPane planningContentHost;

    private final List<Event> events = EventStore.getInstance().getEvents();

    @FXML
    private void initialize() {
        EventStore.getInstance().getEvents().addListener((javafx.collections.ListChangeListener<Event>) change -> renderOverview());
        renderOverview();
    }

    @FXML
    private void showOverview() {
        renderOverview();
    }

    @FXML
    private void showEventsTable() {
        loadIntoHost("/Gestion de temps/events_list.fxml");
    }

    @FXML
    private void showAddEventForm() {
        loadIntoHost("/Gestion de temps/add_event.fxml");
    }

    private void renderOverview() {
        planningContentHost.getChildren().clear();

        long completed = events.stream()
                .filter(event -> "Termine".equalsIgnoreCase(event.getStatus()))
                .count();
        long inProgress = events.stream()
                .filter(event -> "En cours".equalsIgnoreCase(event.getStatus()))
                .count();

        eventsCountLabel.setText(String.valueOf(events.size()));
        completedCountLabel.setText(String.valueOf(completed));
        inProgressCountLabel.setText(String.valueOf(inProgress));
        totalCountLabel.setText(String.valueOf(events.size()));

        eventCardsContainer.getChildren().setAll(
                events.stream()
                        .sorted(Comparator.comparing(Event::getDate, Comparator.nullsLast(Comparator.naturalOrder())))
                        .map(this::buildEventCard)
                        .collect(Collectors.toList())
        );
    }

    private HBox buildEventCard(Event event) {
        HBox metaRow = new HBox(8);
        metaRow.getChildren().addAll(
                createPill(event.getDuration() + "m", "#EEF2FF", "#5163D7"),
                createPill(event.getPriority(), colorForPriorityBackground(event.getPriority()), colorForPriorityText(event.getPriority())),
                createPill(event.getStatus(), "#E0F2FE", "#0284C7")
        );

        VBox card = new VBox(14);
        card.getStyleClass().add("planning-event-card");

        Label title = new Label(event.getTitle());
        title.getStyleClass().add("planning-event-title");

        Label date = new Label(formatDate(event.getDate()));
        date.getStyleClass().add("planning-event-date");

        Label description = new Label(event.getDescription());
        description.getStyleClass().add("planning-event-description");
        description.setWrapText(true);

        Label sessions = new Label(event.getReminder_minutes() + " Sessions Plan");
        sessions.getStyleClass().add("planning-event-sessions");

        HBox header = new HBox(12);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(title, spacer, date);

        HBox actionsRow = new HBox(12);
        Button motivationButton = new Button("Motivation");
        motivationButton.getStyleClass().add("planning-motivation-button");
        motivationButton.setOnAction(actionEvent -> showMotivationForm(event));

        actionsRow.getChildren().add(motivationButton);
        card.getChildren().addAll(header, description, metaRow, sessions, actionsRow);
        return new HBox(card);
    }

    private Label createPill(String text, String backgroundColor, String textColor) {
        Label pill = new Label(text == null ? "" : text);
        pill.setStyle(
                "-fx-background-color: " + backgroundColor + ";" +
                "-fx-text-fill: " + textColor + ";" +
                "-fx-background-radius: 999;" +
                "-fx-padding: 6 12;" +
                "-fx-font-size: 12px;" +
                "-fx-font-weight: 700;"
        );
        return pill;
    }

    private void loadIntoHost(String resourcePath) {
        try {
            URL resource = getClass().getResource(resourcePath);
            if (resource == null) {
                throw new IllegalStateException("Missing FXML resource: " + resourcePath);
            }

            Node content = FXMLLoader.load(resource);
            planningContentHost.getChildren().setAll(content);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load FXML resource: " + resourcePath, e);
        }
    }

    private void showMotivationForm(Event event) {
        try {
            URL resource = getClass().getResource("/Gestion de temps/motivation_setup.fxml");
            if (resource == null) {
                throw new IllegalStateException("Missing FXML resource: /Gestion de temps/motivation_setup.fxml");
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Node content = loader.load();
            MotivationSetupController controller = loader.getController();
            controller.configure(event, this::showOverview);
            planningContentHost.getChildren().setAll(content);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load FXML resource: /Gestion de temps/motivation_setup.fxml", e);
        }
    }

    private String formatDate(Date date) {
        if (date == null) {
            return "";
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH);
        return date.toLocalDate().format(formatter);
    }

    private String colorForPriorityBackground(String priority) {
        if (priority == null) {
            return "#E2E8F0";
        }

        switch (priority.toLowerCase(Locale.ROOT)) {
            case "haute":
                return "#FEE2E2";
            case "moyenne":
                return "#FEF3C7";
            default:
                return "#DCFCE7";
        }
    }

    private String colorForPriorityText(String priority) {
        if (priority == null) {
            return "#475569";
        }

        switch (priority.toLowerCase(Locale.ROOT)) {
            case "haute":
                return "#DC2626";
            case "moyenne":
                return "#D97706";
            default:
                return "#15803D";
        }
    }
}
