package controllers;

import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import models.Event;
import services.EventStore;
import controllers.gestiondetemps.AddEventController;
import controllers.gestiondetemps.MotivationSetupController;

import java.io.IOException;
import java.sql.Date;
import java.util.Optional;

public class BackendTimeController {

    @FXML private TableView<Event> eventsTable;
    @FXML private TableColumn<Event, Integer> colId;
    @FXML private TableColumn<Event, String> colTitle;
    @FXML private TableColumn<Event, Date> colDate;
    @FXML private TableColumn<Event, String> colType;
    @FXML private TableColumn<Event, Integer> colDuration;
    @FXML private TableColumn<Event, String> colPriority;
    @FXML private TableColumn<Event, String> colMotivation;
    @FXML private TableColumn<Event, String> colActions;

    @FXML private TextField searchField;
    @FXML private Label totalEventsLabel;
    @FXML private Label pendingEventsLabel;

    private FilteredList<Event> filteredEvents;

    @FXML
    public void initialize() {
        setupTable();
        loadEvents();
        setupSearch();
        updateStats();
    }

    private void setupTable() {
        colId.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("id"));
        colTitle.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("title"));
        colDate.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("date"));
        colType.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("type"));
        colDuration.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("duration"));

        // Priority / Status column
        colPriority.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    Event event = getTableRow().getItem();
                    String priority = event.getPriority();
                    Label label = new Label(priority != null ? priority : "Moyenne");
                    label.getStyleClass().add("status-badge");
                    
                    // Simple color coding
                    if ("Haute".equalsIgnoreCase(priority)) label.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #ef4444;");
                    else if ("Moyenne".equalsIgnoreCase(priority)) label.setStyle("-fx-background-color: #fef9c3; -fx-text-fill: #ca8a04;");
                    else label.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #22c55e;");
                    
                    setGraphic(label);
                }
            }
        });

        // Motivation column
        colMotivation.setCellFactory(column -> new TableCell<>() {
            private final Button btn = new Button();
            {
                SVGPath brainIcon = new SVGPath();
                brainIcon.setContent("M12,2C6.5,2,2,6.5,2,12s4.5,10,10,10s10-4.5,10-10S17.5,2,12,2z M12,20c-4.4,0-8-3.6-8-8s3.6-8,8-8s8,3.6,8,8S16.4,20,12,20z");
                brainIcon.setFill(Color.web("#6366f1"));
                btn.setGraphic(brainIcon);
                btn.getStyleClass().add("btn-icon-only");
                btn.setOnAction(event -> {
                    Event e = getTableRow().getItem();
                    handleMotivation(e);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        // Actions column
        colActions.setCellFactory(column -> new TableCell<>() {
            private final Button editBtn = new Button();
            private final Button deleteBtn = new Button();
            private final HBox container = new HBox(8, editBtn, deleteBtn);
            {
                editBtn.getStyleClass().add("btn-action-edit");
                deleteBtn.getStyleClass().add("btn-action-delete");
                
                editBtn.setOnAction(event -> {
                    Event e = getTableRow().getItem();
                    handleEditEvent(e);
                });
                
                deleteBtn.setOnAction(event -> {
                    Event e = getTableRow().getItem();
                    handleDeleteEvent(e);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });
    }

    private void loadEvents() {
        filteredEvents = new FilteredList<>(EventStore.getInstance().getEvents(), p -> true);
        eventsTable.setItems(filteredEvents);
        
        // Update stats when list changes
        EventStore.getInstance().getEvents().addListener((javafx.collections.ListChangeListener<Event>) c -> updateStats());
    }

    private void setupSearch() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredEvents.setPredicate(event -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();
                if (event.getTitle() != null && event.getTitle().toLowerCase().contains(lowerCaseFilter)) return true;
                if (event.getType() != null && event.getType().toLowerCase().contains(lowerCaseFilter)) return true;
                if (event.getLocation() != null && event.getLocation().toLowerCase().contains(lowerCaseFilter)) return true;
                return false;
            });
            updateStats();
        });
    }

    private void updateStats() {
        int total = filteredEvents.size();
        long pending = filteredEvents.stream()
                .filter(e -> "En cours".equalsIgnoreCase(e.getStatus()) || e.getStatus() == null)
                .count();
        
        totalEventsLabel.setText(String.valueOf(total));
        pendingEventsLabel.setText(String.valueOf(pending));
    }

    @FXML
    private void handleAddEvent() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Gestion de temps/add_event.fxml"));
            javafx.scene.Parent form = loader.load();
            
            Stage stage = new Stage();
            stage.setTitle("Add Event");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(form));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleEditEvent(Event event) {
        // For now, reuse add_event with pre-filled data if possible
        // (Existing AddEventController might need modification to support editing)
        System.out.println("Editing event: " + event.getTitle());
    }

    private void handleDeleteEvent(Event event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Event");
        alert.setHeaderText("Are you sure?");
        alert.setContentText("Do you really want to delete the event: " + event.getTitle() + "?");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            EventStore.getInstance().deleteEvent(event);
        }
    }

    private void handleMotivation(Event event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Gestion de temps/motivation_setup.fxml"));
            javafx.scene.Parent root = loader.load();
            
            MotivationSetupController controller = loader.getController();
            Stage stage = new Stage();
            
            controller.configure(event, stage::close);
            
            stage.setTitle("Motivation Setup");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
