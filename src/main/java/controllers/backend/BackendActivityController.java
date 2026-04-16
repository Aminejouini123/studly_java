package controllers.backend;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import models.Activity;
import models.Course;
import services.ActivityService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class BackendActivityController {

    @FXML public TableView<Activity> activitiesTable;
    @FXML public TableColumn<Activity, String> colTitle;
    @FXML public TableColumn<Activity, String> colType;
    @FXML public TableColumn<Activity, String> colLevel;
    @FXML public TableColumn<Activity, String> colDifficulty;
    @FXML public TableColumn<Activity, Integer> colDuration;
    @FXML public TableColumn<Activity, String> colStatus;
    @FXML public TableColumn<Activity, Void> colActions;

    @FXML public TextField searchField;
    @FXML public Label totalActivitiesLabel;
    @FXML public Label completedActivitiesLabel;
    @FXML public Label titleLabel;
    @FXML private javafx.scene.layout.VBox contentArea;
    private java.util.List<javafx.scene.Node> dashboardCache;
    private javafx.collections.transformation.SortedList<Activity> sortedList;
    private javafx.scene.Parent originalRoot;

    private ActivityService activityService;
    private ObservableList<Activity> activitiesList;
    private FilteredList<Activity> filteredList;
    private Course filteredCourse;

    public void setFilteredCourse(Course course) {
        this.filteredCourse = course;
        // Force reload once the filter is set, in case initialize() already ran
        if (activityService != null) {
            loadActivities();
            if (titleLabel != null) {
                titleLabel.setText("Activity Administration: " + filteredCourse.getName().toUpperCase());
            }
        }
    }

    public BackendActivityController() {
        this.activityService = new ActivityService();
        this.activitiesList = FXCollections.observableArrayList();
    }

    @FXML
    public void initialize() {
        setupTableColumns();
        setupDataBinding();
        loadActivities();
        setupSearch();
        updateStatistics();

        if (filteredCourse != null) {
            titleLabel.setText("Activity Administration: " + filteredCourse.getName().toUpperCase());
        } else {
            titleLabel.setText("Global Activity Administration");
        }
    }

    private void setupDataBinding() {
        filteredList = new javafx.collections.transformation.FilteredList<>(activitiesList, p -> true);
        sortedList = new javafx.collections.transformation.SortedList<>(filteredList);
        sortedList.comparatorProperty().bind(activitiesTable.comparatorProperty());
        activitiesTable.setItems(sortedList);
    }

    private void setupTableColumns() {
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colTitle.setCellFactory(column -> new TableCell<Activity, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label lbl = new Label(item);
                    lbl.setStyle("-fx-text-fill: #0F172A; -fx-font-weight: bold; -fx-font-size: 13px;");
                    setGraphic(lbl);
                }
            }
        });

        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colLevel.setCellValueFactory(new PropertyValueFactory<>("level"));
        colDuration.setCellValueFactory(new PropertyValueFactory<>("duration"));

        colDifficulty.setCellValueFactory(new PropertyValueFactory<>("difficulty"));
        colDifficulty.setCellFactory(column -> new TableCell<Activity, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label lbl = new Label(item.toUpperCase());
                    lbl.getStyleClass().add("role-pill-support");
                    setGraphic(lbl);
                }
            }
        });

        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(column -> new TableCell<Activity, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                } else {
                    Circle dot = new Circle(4);
                    Label lbl = new Label(status);
                    lbl.setStyle("-fx-text-fill: #0F172A; -fx-font-size: 12px; -fx-font-weight: bold;");
                    if ("Completed".equalsIgnoreCase(status) || "Active".equalsIgnoreCase(status)) {
                        dot.setFill(Color.web("#4ade80"));
                    } else if ("In Progress".equalsIgnoreCase(status)) {
                        dot.setFill(Color.web("#3b82f6"));
                    } else {
                        dot.setFill(Color.web("#f59e0b")); // To Do or others
                    }
                    HBox box = new HBox(8, dot, lbl);
                    box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    setGraphic(box);
                }
            }
        });

        colActions.setCellFactory(column -> new TableCell<Activity, Void>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Activity activity = getTableView().getItems().get(getIndex());
                    
                    Button editBtn = new Button("Edit");
                    editBtn.getStyleClass().add("btn-outline");
                    editBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 10; -fx-text-fill: #3b82f6; -fx-border-color: #3b82f6;");
                    editBtn.setOnAction(e -> handleEditActivity(activity));
                    
                    Button deleteBtn = new Button("Delete");
                    deleteBtn.getStyleClass().add("btn-outline");
                    deleteBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 10; -fx-text-fill: #ef4444; -fx-border-color: #ef4444;");
                    deleteBtn.setOnAction(e -> {
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                        alert.setTitle("Delete Activity");
                        alert.setHeaderText("Delete activity: " + activity.getTitle() + "?");
                        alert.setContentText("This action is permanent.");
                        if (alert.showAndWait().get() == ButtonType.OK) {
                            try {
                                activityService.supprimer(activity.getId());
                                loadActivities();
                                updateStatistics();
                            } catch (SQLException ex) {
                                ex.printStackTrace();
                            }
                        }
                    });

                    HBox box = new HBox(5, editBtn, deleteBtn);
                    box.setAlignment(javafx.geometry.Pos.CENTER);
                    setGraphic(box);
                }
            }
        });
    }

    @FXML
    public void handleCreateNewActivity() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gestion_activites/backend_form_activity.fxml"));
            javafx.scene.Parent formRoot = loader.load();
            controllers.activities.ActivityAddController controller = loader.getController();
            controller.setFromBackend(true);
            controller.setBackendController(this);
            
            if (activitiesTable.getScene() != null) {
                this.originalRoot = activitiesTable.getScene().getRoot();
            }
            
            
            // Context-Aware Improvement: Pass the current course if we are in a filtered view
            if (filteredCourse != null) {
                controller.setCourse(filteredCourse);
            }
            
            // Professional in-place replacement with caching
            if (contentArea != null) {
                if (dashboardCache == null) {
                    dashboardCache = new java.util.ArrayList<>(contentArea.getChildren());
                }
                contentArea.getChildren().setAll(formRoot);
            } else {
                Stage stage = (Stage) activitiesTable.getScene().getWindow();
                stage.getScene().setRoot(formRoot);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void restoreDashboard() {
        if (activitiesTable.getScene() != null && originalRoot != null && activitiesTable.getScene().getRoot() != originalRoot) {
            activitiesTable.getScene().setRoot(originalRoot);
        }
        
        if (contentArea != null && dashboardCache != null) {
            contentArea.getChildren().setAll(dashboardCache);
        }
        
        loadActivities(); // Refresh data
    }

    private void handleEditActivity(Activity activity) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gestion_activites/backend_edit_activity.fxml"));
            javafx.scene.Parent formRoot = loader.load();
            controllers.activities.ActivityEditController controller = loader.getController();
            controller.setFromBackend(true);
            controller.setActivity(activity);
            controller.setBackendController(this);
            
            if (activitiesTable.getScene() != null) {
                this.originalRoot = activitiesTable.getScene().getRoot();
            }
            
            // Professional in-place replacement with caching
            if (contentArea != null) {
                if (dashboardCache == null) {
                    dashboardCache = new java.util.ArrayList<>(contentArea.getChildren());
                }
                contentArea.getChildren().setAll(formRoot);
            } else {
                Stage stage = (Stage) activitiesTable.getScene().getWindow();
                stage.getScene().setRoot(formRoot);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void loadActivities() {
        try {
            List<Activity> activities;
            if (filteredCourse != null) {
                activities = activityService.recupererParCours(filteredCourse.getId());
            } else {
                activities = activityService.recuperer();
            }
            activitiesList.setAll(activities);
            updateStatistics();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setupSearch() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredList.setPredicate(activity -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();
                
                return activity.getTitle().toLowerCase().contains(lowerCaseFilter) ||
                       (activity.getType() != null && activity.getType().toLowerCase().contains(lowerCaseFilter)) ||
                       (activity.getStatus() != null && activity.getStatus().toLowerCase().contains(lowerCaseFilter)) ||
                       (activity.getLevel() != null && activity.getLevel().toLowerCase().contains(lowerCaseFilter)) ||
                       (activity.getDifficulty() != null && activity.getDifficulty().toLowerCase().contains(lowerCaseFilter));
            });
            updateStatistics();
        });
    }

    private void updateStatistics() {
        totalActivitiesLabel.setText(String.valueOf(activitiesList.size()));
        long completedCount = activitiesList.stream()
                .filter(a -> "Completed".equalsIgnoreCase(a.getStatus()))
                .count();
        completedActivitiesLabel.setText(String.valueOf(completedCount));
    }

    @FXML
    public void handleShowCourses() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gestion_cours/backend_courses.fxml"));
            javafx.scene.Parent root = loader.load();
            Stage stage = (Stage) activitiesTable.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleShowOverview() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/TEMPLATE/backend_management.fxml"));
            javafx.scene.Parent root = loader.load();
            Stage stage = (Stage) activitiesTable.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleShowUsers() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/TEMPLATE/backend_management.fxml"));
            javafx.scene.Parent root = loader.load();
            Stage stage = (Stage) activitiesTable.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleExportExcel() {
        String[] headers = {"Title", "Type", "Status", "Duration (min)", "Difficulty", "Level"};
        utils.CSVExportUtil.exportToCSV(
            activitiesList,
            headers,
            item -> new String[]{
                item.getTitle(),
                item.getType(),
                item.getStatus(),
                String.valueOf(item.getDuration()),
                item.getDifficulty(),
                item.getLevel()
            },
            "Activity_List_Export",
            (Stage) activitiesTable.getScene().getWindow()
        );
    }

}
