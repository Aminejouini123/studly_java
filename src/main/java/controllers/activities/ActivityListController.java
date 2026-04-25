package controllers.activities;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.scene.paint.Color;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.scene.shape.SVGPath;
import javafx.scene.control.Button;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.util.Duration;
import javafx.stage.Stage;
import models.Course;
import models.Activity;
import services.ActivityService;
import services.chat.QuizGeneratorService;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ActivityListController extends BaseActivityController {

    @FXML
    private StackPane rootPane;
    @FXML
    private Label courseTitleLabel;
    @FXML
    private Label statTotal, statToDo, statInProgress, statCompleted;
    @FXML
    private FlowPane activitiesContainer;
    @FXML
    private VBox emptyState;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> statusFilter;
    @FXML
    private ComboBox<String> sortComboBox;

    private Course currentCourse;
    private List<Activity> allActivities = new ArrayList<>();
    private final ActivityService activityService = new ActivityService();
    private final QuizGeneratorService quizGeneratorService = new QuizGeneratorService();

    @FXML
    public void initialize() {
        if (statusFilter != null) {
            statusFilter.getItems().addAll("All statuses", "To Do", "In Progress", "Completed");
            statusFilter.setValue("All statuses");
            statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> updateDisplay());
        }
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> updateDisplay());
        }
        if (sortComboBox != null) {
            sortComboBox.getItems().setAll("Newest First", "Title (A-Z)", "Title (Z-A)", "Duration (Asc)", "Duration (Desc)");
            sortComboBox.setValue("Newest First");
            sortComboBox.valueProperty().addListener((obs, oldVal, newVal) -> updateDisplay());
        }
    }

    public void setCourse(Course course) {
        this.currentCourse = course;
        courseTitleLabel.setText(course.getName());
        loadActivities();
    }

    private void loadActivities() {
        if (currentCourse == null) return;
        try {
            allActivities = activityService.recupererParCours(currentCourse.getId());
            updateDisplay();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateDisplay() {
        if (activitiesContainer == null) return;

        String searchText = searchField != null ? searchField.getText().toLowerCase().trim() : "";
        String selectedStatus = statusFilter != null ? statusFilter.getValue() : "All statuses";
        String selectedSort = sortComboBox != null ? sortComboBox.getValue() : "Newest First";

        List<Activity> filtered = allActivities.stream()
                .filter(a -> searchText.isEmpty() ||
                            (a.getTitle() != null && a.getTitle().toLowerCase().contains(searchText)) ||
                            (a.getDescription() != null && a.getDescription().toLowerCase().contains(searchText)))
                .filter(a -> selectedStatus == null || selectedStatus.equals("All statuses") || (a.getStatus() != null && a.getStatus().equalsIgnoreCase(selectedStatus)))
                .collect(Collectors.toList());

        if (selectedSort != null) {
            switch (selectedSort) {
                case "Title (A-Z)":
                    filtered.sort(java.util.Comparator.comparing(a -> a.getTitle() != null ? a.getTitle().toLowerCase() : "", String.CASE_INSENSITIVE_ORDER));
                    break;
                case "Title (Z-A)":
                    filtered.sort((a1, a2) -> {
                        String t1 = a1.getTitle() != null ? a1.getTitle() : "";
                        String t2 = a2.getTitle() != null ? a2.getTitle() : "";
                        return t2.compareToIgnoreCase(t1);
                    });
                    break;
                case "Duration (Asc)":
                    filtered.sort(java.util.Comparator.comparingInt(Activity::getDuration));
                    break;
                case "Duration (Desc)":
                    filtered.sort((a1, a2) -> Integer.compare(a2.getDuration(), a1.getDuration()));
                    break;
                case "Newest First":
                default:
                    filtered.sort((a1, a2) -> {
                        int comp = 0;
                        if (a1.getCompleted_at() != null && a2.getCompleted_at() != null) {
                            comp = a2.getCompleted_at().compareTo(a1.getCompleted_at());
                        } else if (a1.getCompleted_at() != null) {
                            comp = -1;
                        } else if (a2.getCompleted_at() != null) {
                            comp = 1;
                        }

                        if (comp == 0) {
                            return Integer.compare(a2.getId(), a1.getId());
                        }
                        return comp;
                    });
                    break;
            }
        }

        activitiesContainer.getChildren().clear();
        
        if (filtered.isEmpty()) {
            emptyState.setVisible(true);
            emptyState.setManaged(true);
            activitiesContainer.setVisible(false);
            activitiesContainer.setManaged(false);
        } else {
            emptyState.setVisible(false);
            emptyState.setManaged(false);
            activitiesContainer.setVisible(true);
            activitiesContainer.setManaged(true);
            for (Activity activity : filtered) {
                VBox card = createActivityCard(activity);
                activitiesContainer.getChildren().add(card);
            }
        }
        
        updateStats(filtered);
    }

    private void updateStats(List<Activity> list) {
        long total = allActivities.size();
        long todo = allActivities.stream().filter(a -> a.getStatus() != null && a.getStatus().equalsIgnoreCase("To Do")).count();
        long inProgress = allActivities.stream().filter(a -> a.getStatus() != null && a.getStatus().equalsIgnoreCase("In Progress")).count();
        long completed = allActivities.stream().filter(a -> a.getStatus() != null && a.getStatus().equalsIgnoreCase("Completed")).count();

        if (statTotal != null) statTotal.setText(String.valueOf(total));
        if (statToDo != null) statToDo.setText(String.valueOf(todo));
        if (statInProgress != null) statInProgress.setText(String.valueOf(inProgress));
        if (statCompleted != null) statCompleted.setText(String.valueOf(completed));
    }

    private VBox createActivityCard(Activity activity) {
        VBox card = new VBox(20);
        card.getStyleClass().add("course-card");
        card.setMinWidth(400);
        card.setMaxWidth(440);

        // Header: Title and Status Badge
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        
        String titleText = activity.getTitle() != null ? activity.getTitle().toLowerCase() : "untitled";
        Label title = new Label(titleText);
        title.getStyleClass().add("course-card-title");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label statusBadge = createStatusBadge(activity.getStatus());
        header.getChildren().addAll(title, spacer, statusBadge);

        // Type Hub: Modern Type Indicator (Folder Icon)
        HBox typeRow = new HBox(8);
        typeRow.setAlignment(Pos.CENTER_LEFT);
        SVGPath folderIcon = createIcon(
                "M10 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z",
                "#64748B", 14);
        Label typeLabel = new Label(activity.getType() != null ? activity.getType() : "—");
        typeLabel.getStyleClass().add("course-card-teacher"); // Reuse teacher styling for secondary info
        typeRow.getChildren().addAll(folderIcon, typeLabel);

        // Rich Metadata: Stats Container (Duration, Difficulty, Level)
        HBox statsBox = new HBox(30);
        statsBox.getStyleClass().add("card-stats-container");
        statsBox.setAlignment(Pos.CENTER);
        
        statsBox.getChildren().addAll(
            createStatItem("M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67V7z", activity.getDuration() + " min"),
            createStatItem("M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z", activity.getDifficulty()),
            createStatItem("M5 13.18v4L12 21l7-3.82v-4L12 17l-7-3.82zM12 3L1 9l11 6 9-4.91V17h2V9L12 3z", activity.getLevel())
        );

        // Footer Actions Hub
        VBox footerActions = new VBox(12);
        footerActions.setPadding(new Insets(10, 0, 0, 0));
        
        // Primary Action
        Button viewBtn = new Button("View Exercise");
        viewBtn.getStyleClass().add("btn-primary-action");
        viewBtn.setGraphic(createIcon("M12 4l-1.41 1.41L16.17 11H4v2h12.17l-5.58 5.59L12 20l8-8z", "white", 14));
        viewBtn.setMaxWidth(Double.MAX_VALUE);
        viewBtn.setOnAction(e -> handleViewActivity(activity));

        // Secondary Options (Edit/Delete)
        HBox actionRow = new HBox(12);
        Button editBtn = new Button("Edit");
        editBtn.getStyleClass().add("btn-small-edit");
        editBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(editBtn, Priority.ALWAYS);
        editBtn.setGraphic(createIcon("M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z", "#7C3AED", 14));
        editBtn.setOnAction(e -> handleEditActivity(activity));

        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().add("btn-small-delete");
        deleteBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(deleteBtn, Priority.ALWAYS);
        deleteBtn.setGraphic(createIcon("M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z", "#DC2626", 14));
        deleteBtn.setOnAction(e -> showSleekDeleteOverlay(activity));

        actionRow.getChildren().addAll(editBtn, deleteBtn);
        footerActions.getChildren().addAll(viewBtn, actionRow);

        card.getChildren().addAll(header, typeRow, statsBox, footerActions);
        
        // Premium Hover Effect
        card.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), card);
            st.setToX(1.02); st.setToY(1.02); st.play();
        });
        card.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), card);
            st.setToX(1.0); st.setToY(1.0); st.play();
        });

        return card;
    }

    private Label createStatusBadge(String status) {
        if (status == null || status.isEmpty()) status = "To Do";
        Label badge = new Label(status.toUpperCase());
        badge.getStyleClass().add("status-badge");
        String s = status.toLowerCase();
        if (s.contains("to do")) badge.getStyleClass().add("status-badge-pending");
        else if (s.contains("progress")) badge.getStyleClass().add("status-badge-progress");
        else badge.getStyleClass().add("status-badge-completed");
        return badge;
    }

    private HBox createStatItem(String iconPath, String text) {
        HBox item = new HBox(6);
        item.setAlignment(Pos.CENTER_LEFT);
        item.getChildren().addAll(createIcon(iconPath, "#64748B", 14), new Label(text));
        ((Label)item.getChildren().get(1)).setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px; -fx-font-weight: bold;");
        return item;
    }

    private SVGPath createIcon(String path, String color, int size) {
        SVGPath svg = new SVGPath();
        svg.setContent(path);
        svg.setFill(javafx.scene.paint.Color.web(color));
        svg.setScaleX(size / 24.0);
        svg.setScaleY(size / 24.0);
        return svg;
    }

    private void showSleekDeleteOverlay(Activity activity) {
        StackPane overlay = new StackPane();
        overlay.setAlignment(Pos.CENTER);
        overlay.setStyle("-fx-background-color: rgba(15, 23, 42, 0.4);");
        
        VBox card = new VBox(25);
        card.setAlignment(Pos.CENTER);
        card.setMaxSize(340, javafx.scene.layout.Region.USE_PREF_SIZE);
        card.getStyleClass().add("modern-confirm-card");
        
        SVGPath warnIcon = createIcon("M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z", "#EF4444", 48);
        
        VBox textContent = new VBox(8);
        textContent.setAlignment(Pos.CENTER);
        Label title = new Label("Delete Activity?");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #1E293B;");
        Label desc = new Label("This action cannot be undone.\n\"" + activity.getTitle() + "\" will be lost.");
        desc.getStyleClass().add("success-message");
        textContent.getChildren().addAll(title, desc);
        
        HBox buttons = new HBox(12);
        buttons.setAlignment(Pos.CENTER);
        Button cancelBtn = new Button("Keep it");
        cancelBtn.getStyleClass().add("btn-modern-cancel");
        cancelBtn.setOnAction(e -> {
            FadeTransition ft = new FadeTransition(Duration.millis(200), overlay);
            ft.setToValue(0);
            ft.setOnFinished(ev -> rootPane.getChildren().remove(overlay));
            ft.play();
        });
        
        Button confirmBtn = new Button("Delete Activity");
        confirmBtn.getStyleClass().add("btn-modern-delete");
        confirmBtn.setOnAction(e -> {
            try {
                activityService.supprimer(activity.getId());
                rootPane.getChildren().remove(overlay);
                loadActivities(); // Refresh list
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });
        
        buttons.getChildren().addAll(cancelBtn, confirmBtn);
        card.getChildren().addAll(warnIcon, textContent, buttons);
        overlay.getChildren().add(card);
        
        rootPane.getChildren().add(overlay);
        
        FadeTransition ft = new FadeTransition(Duration.millis(250), overlay);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
        
        ScaleTransition st = new ScaleTransition(Duration.millis(300), card);
        st.setFromX(0.85); st.setFromY(0.85); st.setToX(1); st.setToY(1); st.play();
    }

    private void showInMainOrShell(javafx.scene.Parent root) {
        if (controllers.FrontendController.getInstance() != null) {
            controllers.FrontendController.getInstance().loadContentNode(root);
        } else {
            javafx.stage.Stage stage = (javafx.stage.Stage) activitiesContainer.getScene().getWindow();
            stage.getScene().setRoot(root);
        }
    }

    private void handleViewActivity(Activity activity) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/gestion_activites/frontend_activity_detail.fxml"));
            javafx.scene.Parent root = loader.load();
            
            ActivityDetailController controller = loader.getController();
            controller.populateDetails(activity, currentCourse);
            
            showInMainOrShell(root);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Navigation Error", "Could not load activity details: " + e.getMessage());
        }
    }

    private void handleEditActivity(Activity activity) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/gestion_activites/frontend_edit_activity.fxml"));
            javafx.scene.Parent root = loader.load();
            
            ActivityEditController controller = loader.getController();
            controller.setActivity(activity, currentCourse);
            
            showInMainOrShell(root);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBackToCourses() {
        if (fromBackend) {
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/TEMPLATE/backend_courses.fxml"));
                javafx.scene.Parent root = loader.load();
                javafx.stage.Stage stage = (javafx.stage.Stage) activitiesContainer.getScene().getWindow();
                stage.getScene().setRoot(root);
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        } else {
            navigateToFrontendCourseList(courseTitleLabel);
        }
    }

    @FXML
    private void handleSmartGenerate() {
        if (currentCourse == null) {
            showAlert("Error", "No course selected.");
            return;
        }
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/gestion_activites/frontend_quiz_generator.fxml"));
            javafx.scene.Parent root = loader.load();
            QuizController controller = loader.getController();
            controller.setCourse(currentCourse);
            showInMainOrShell(root);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not open quiz generator: " + e.getMessage());
        }
    }

    @FXML
    private void handleAddActivity() {
        if (currentCourse == null) {
            showAlert("Error", "No course selected. Open activities from a course card.");
            return;
        }
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/gestion_activites/frontend_add_activity.fxml"));
            javafx.scene.Parent root = loader.load();
            ActivityAddController controller = loader.getController();
            controller.setCourse(currentCourse);
            showInMainOrShell(root);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}
