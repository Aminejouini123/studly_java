package controllers.courses;

import javafx.fxml.FXML;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Button;
import javafx.scene.shape.SVGPath;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import models.Course;
import services.CourseService;

import javafx.scene.layout.StackPane;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.util.Duration;
import javafx.scene.Node;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class CourseListController extends BaseCourseController {

    @FXML
    private FlowPane coursesContainer;
    @FXML
    private StackPane rootPane;
    @FXML
    private VBox emptyState;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> statusFilter;
    @FXML
    private ComboBox<String> sortComboBox;
    @FXML
    private Label statTotal, statInProgress, statCompleted, statPending;

    private List<Course> allCourses = new ArrayList<>();

    @FXML
    public void initialize() {
        setupListeners();
        loadCourses();
    }

    private void setupListeners() {
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> updateCourseDisplay());
        }
        if (statusFilter != null) {
            statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> updateCourseDisplay());
        }
        if (sortComboBox != null) {
            sortComboBox.valueProperty().addListener((obs, oldVal, newVal) -> updateCourseDisplay());
        }
    }

    private void loadCourses() {
        try {
            CourseService service = new CourseService();
            allCourses = service.recuperer();
            updateCourseDisplay();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --- Core Logic: Search & Filter ---
    private void updateCourseDisplay() {
        if (coursesContainer == null)
            return;

        String searchText = searchField != null ? searchField.getText().toLowerCase() : "";
        String statusCriterion = (statusFilter != null && statusFilter.getValue() != null) ? statusFilter.getValue()
                : "All statuses";

        List<Course> filtered = allCourses.stream()
                .filter(c -> c.getName().toLowerCase().contains(searchText)
                        || c.getTeacher_email().toLowerCase().contains(searchText))
                .filter(c -> statusCriterion.equals("All statuses") || c.getStatus().equalsIgnoreCase(statusCriterion))
                .collect(Collectors.toList());

        applySorting(filtered);

        coursesContainer.getChildren().clear();
        if (filtered.isEmpty()) {
            emptyState.setVisible(true);
            emptyState.setManaged(true);
            coursesContainer.setVisible(false);
            coursesContainer.setManaged(false);
        } else {
            emptyState.setVisible(false);
            emptyState.setManaged(false);
            coursesContainer.setVisible(true);
            coursesContainer.setManaged(true);
            for (Course course : filtered) {
                coursesContainer.getChildren().add(createCourseCard(course));
            }
        }

        updateStats(filtered);
    }

    // --- Core Logic: Tri (Sort) ---
    private void applySorting(List<Course> list) {
        String sortCriterion = sortComboBox.getValue() != null ? sortComboBox.getValue() : "Name (A-Z)";
        switch (sortCriterion) {
            case "Name (A-Z)":
                list.sort(Comparator.comparing(c -> c.getName().toLowerCase()));
                break;
            case "Coefficient (High-Low)":
                list.sort((c1, c2) -> Double.compare(c2.getCoefficient(), c1.getCoefficient()));
                break;
            case "Semester (1-2)":
                list.sort(Comparator.comparing(Course::getSemester));
                break;
            case "Duration (Shortest)":
                list.sort(Comparator.comparingInt(Course::getDuration));
                break;
        }
    }

    private void updateStats(List<Course> list) {
        long total = list.size();
        long inProgress = list.stream().filter(c -> c.getStatus().equalsIgnoreCase("In Progress")).count();
        long completed = list.stream().filter(c -> c.getStatus().equalsIgnoreCase("Completed")).count();
        long pending = list.stream().filter(c -> c.getStatus().equalsIgnoreCase("Pending")).count();

        if (statTotal != null)
            statTotal.setText(String.valueOf(total));
        if (statInProgress != null)
            statInProgress.setText(String.valueOf(inProgress));
        if (statCompleted != null)
            statCompleted.setText(String.valueOf(completed));
        if (statPending != null)
            statPending.setText(String.valueOf(pending));
    }

    private VBox createCourseCard(Course course) {
        VBox card = new VBox(20);
        card.getStyleClass().add("course-card");
        card.setMinWidth(400);
        card.setMaxWidth(450);

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(course.getName().toLowerCase());
        title.getStyleClass().add("course-card-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label statusBadge = new Label(course.getStatus().toUpperCase());
        statusBadge.getStyleClass().add("status-badge");
        String status = course.getStatus().toLowerCase();
        if (status.contains("progress"))
            statusBadge.getStyleClass().add("status-badge-progress");
        else if (status.contains("complete"))
            statusBadge.getStyleClass().add("status-badge-completed");
        else
            statusBadge.getStyleClass().add("status-badge-pending");
        header.getChildren().addAll(title, spacer, statusBadge);

        // Teacher
        HBox teacherRow = new HBox(8);
        teacherRow.setAlignment(Pos.CENTER_LEFT);
        SVGPath personIcon = createIcon(
                "M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z",
                "#64748B", 14);
        Label teacher = new Label(course.getTeacher_email());
        teacher.getStyleClass().add("course-card-teacher");
        teacherRow.getChildren().addAll(personIcon, teacher);

        // Stats
        HBox statsBox = new HBox(30);
        statsBox.getStyleClass().add("card-stats-container");
        statsBox.setAlignment(Pos.CENTER);
        statsBox.getChildren().addAll(
                createStatItem(
                        "M12 2C6.477 2 2 6.477 2 12s4.477 10 10 10 10-4.477 10-10S17.523 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm.5-13H11v6l5.2 3.2.8-1.3-4.5-2.7V7z",
                        course.getDuration() + "h"),
                createStatItem(
                        "M19 4h-1V2h-2v2H8V2H6v2H5c-1.11 0-1.99.9-1.99 2L3 20c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 16H5V9h14v11zM7 11h2v2H7v2zm4 0h2v2h-2zm4 0h2v2h-2zm4 0h2v2h-2zm4 0h2v2h-2z",
                        course.getSemester()),
                createStatItem(
                        "M20 6h-4V4c0-1.11-.89-2-2-2h-4c-1.11 0-2 .89-2 2v2H4c-1.11 0-1.99.89-1.99 2L2 19c0 1.11.89 2 2 2h16c1.11 0 2-.89 2-2V8c0-1.11-.89-2-2-2zm-6 0h-4V4h4v2z",
                        "Coef: " + (int) course.getCoefficient()));

        // Actions
        VBox actions = new VBox(12);
        actions.setPadding(new Insets(10, 0, 0, 0));
        Button viewBtn = new Button("View Details");
        viewBtn.getStyleClass().add("btn-primary-action");
        viewBtn.setGraphic(createIcon("M12 4l-1.41 1.41L16.17 11H4v2h12.17l-5.58 5.59L12 20l8-8z", "white", 14));
        viewBtn.setMaxWidth(Double.MAX_VALUE);
        viewBtn.setOnAction(e -> showCourseDetailView(course));

        HBox actionRow = new HBox(12);
        Button examBtn = new Button("Exam");
        examBtn.getStyleClass().add("btn-secondary-exam");
        examBtn.setGraphic(createIcon("M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z", "#4338CA", 14));
        examBtn.setMaxWidth(Double.MAX_VALUE);
        examBtn.setOnAction(e -> {
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/gestion_examen/frontend_exams.fxml"));
                javafx.scene.Parent root = loader.load();
                controllers.exams.ExamListController controller = loader.getController();
                controller.setCourse(course);
                Stage stage = (Stage) card.getScene().getWindow();
                stage.getScene().setRoot(root);
            } catch (java.io.IOException ex) {
                ex.printStackTrace();
            }
        });

        Button activityBtn = new Button("Activity");
        activityBtn.getStyleClass().add("btn-secondary-activity");
        activityBtn.setGraphic(createIcon("M3 13h2v-2H3v2zm0 4h2v-2H3v2zm0-8h2V7H3v2zm4 4h14v-2H7v2zm0 4h14v-2H7v2zM7 7v2h14V7H7z", "#0E7490", 14));
        activityBtn.setMaxWidth(Double.MAX_VALUE);
        activityBtn.setOnAction(e -> {
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/gestion_activites/frontend_activities.fxml"));
                javafx.scene.Parent root = loader.load();
                controllers.activities.ActivityListController controller = loader.getController();
                controller.setCourse(course);
                Stage stage = (Stage) card.getScene().getWindow();
                stage.getScene().setRoot(root);
            } catch (java.io.IOException ex) {
                ex.printStackTrace();
            }
        });

        Button editBtn = new Button("Edit");
        editBtn.getStyleClass().add("btn-small-edit");
        editBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(editBtn, Priority.ALWAYS);
        editBtn.setGraphic(createIcon("M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z", "#7C3AED", 14));
        editBtn.setOnAction(e -> CourseEditController.startEdit(course, (Stage) card.getScene().getWindow()));

        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().add("btn-small-delete");
        deleteBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(deleteBtn, Priority.ALWAYS);
        deleteBtn.setGraphic(createIcon("M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z", "#DC2626", 14));
        deleteBtn.setOnAction(e -> showSleekDeleteOverlay(course));

        actionRow.getChildren().addAll(editBtn, deleteBtn);
        actions.getChildren().addAll(viewBtn, examBtn, activityBtn, actionRow);

        card.getChildren().addAll(header, teacherRow, statsBox, actions);
        return card;
    }

    private void showSleekDeleteOverlay(Course course) {
        // 1. Create the dark overlay
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("modern-confirm-overlay");
        overlay.setOpacity(0);

        // 2. Create the confirmation card
        VBox card = new VBox(15);
        card.getStyleClass().add("modern-confirm-card");
        card.setAlignment(Pos.CENTER);
        card.setMaxSize(340, javafx.scene.layout.Region.USE_PREF_SIZE);

        // Icon Box
        VBox iconBox = new VBox();
        iconBox.getStyleClass().add("modern-confirm-icon-box");
        SVGPath trashIcon = createIcon("M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z", "#EF4444", 22);
        iconBox.getChildren().add(trashIcon);

        Label title = new Label("Delete Course?");
        title.getStyleClass().add("modern-confirm-title");

        Label message = new Label("You're about to delete \"" + course.getName() + "\". This action cannot be undone.");
        message.getStyleClass().add("modern-confirm-message");
        message.setWrapText(true);
        message.setAlignment(Pos.CENTER);

        HBox buttonRow = new HBox(15);
        buttonRow.setAlignment(Pos.CENTER);
        buttonRow.setPadding(new Insets(10, 0, 0, 0));

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-modern-cancel");
        cancelBtn.setOnAction(e -> hideOverlay(overlay, card));

        Button confirmBtn = new Button("Delete Permanently");
        confirmBtn.getStyleClass().add("btn-modern-delete");
        confirmBtn.setOnAction(e -> {
            try {
                new CourseService().supprimer(course.getId());
                hideOverlay(overlay, card);
                loadCourses();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        buttonRow.getChildren().addAll(cancelBtn, confirmBtn);
        card.getChildren().addAll(iconBox, title, message, buttonRow);
        overlay.getChildren().add(card);
        rootPane.getChildren().add(overlay);

        // 3. Animations
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), overlay);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(300), card);
        scaleUp.setFromX(0.8);
        scaleUp.setFromY(0.8);
        scaleUp.setToX(1.0);
        scaleUp.setToY(1.0);

        fadeIn.play();
        scaleUp.play();
    }

    private void hideOverlay(StackPane overlay, VBox card) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), overlay);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(200), card);
        scaleDown.setToX(0.9);
        scaleDown.setToY(0.9);

        fadeOut.setOnFinished(e -> rootPane.getChildren().remove(overlay));
        
        fadeOut.play();
        scaleDown.play();
    }

    private void showCourseDetailView(Course course) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/gestion_cours/frontend_course_detail.fxml"));
            javafx.scene.Parent root = loader.load();
            CourseDetailController controller = loader.getController();
            controller.populateCourseDetails(course);
            Stage stage = (Stage) coursesContainer.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}
