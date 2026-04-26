package controllers.courses;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

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
import javafx.application.Platform;
import javafx.scene.control.Alert;
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
        
        // Filtre statut : All + Active + Pending
        ObservableList<String> statuses = FXCollections.observableArrayList("All", "Active", "Pending");
        if (statusFilter != null) {
            statusFilter.setItems(statuses);
        }

        ObservableList<String> sortOptions = FXCollections.observableArrayList(
                "Name (A-Z)",
                "Name (Z-A)",
                "Newest first",
                "Oldest first",
                "Coefficient (high to low)",
                "Coefficient (low to high)",
                "Duration (short to long)",
                "Duration (long to short)",
                "Semester (by number)");
        if (sortComboBox != null) {
            sortComboBox.setItems(sortOptions);
        }

        // Professional UI Fix: Force list cell styling to prevent "blank" popups
        styleComboBox(statusFilter);
        styleComboBox(sortComboBox);
        
        if (statusFilter != null) {
            statusFilter.setValue("All");
        }
        if (sortComboBox != null) {
            sortComboBox.setValue("Name (A-Z)");
        }
        
        loadCourses();
    }

    private void styleComboBox(javafx.scene.control.ComboBox<String> combo) {
        if (combo == null) return;
        
        combo.setButtonCell(new javafx.scene.control.ListCell<String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(combo.getPromptText());
                } else {
                    setText(item);
                    setTextFill(javafx.scene.paint.Color.web("#0F172A"));
                }
            }
        });
        
        combo.setCellFactory(lv -> new javafx.scene.control.ListCell<String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setTextFill(javafx.scene.paint.Color.web("#1E293B"));
                }
            }
        });
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
        // Load courses on a background thread to avoid blocking the FX thread
        javafx.concurrent.Task<java.util.List<Course>> task = new javafx.concurrent.Task<>() {
            @Override
            protected java.util.List<Course> call() throws Exception {
                return new CourseService().recuperer();
            }
        };

        task.setOnRunning(ev -> {
            if (coursesContainer != null) {
                coursesContainer.setDisable(true);
            }
        });

        task.setOnSucceeded(ev -> {
            allCourses = task.getValue() != null ? task.getValue() : new ArrayList<>();
            updateCourseDisplay();
            if (coursesContainer != null) {
                coursesContainer.setDisable(false);
            }
            updateCourseDisplay();
        });
    }

    // --- Core Logic: Search & Filter ---
    private void updateCourseDisplay() {
        if (coursesContainer == null)
            return;

        String searchText = searchField != null ? searchField.getText().toLowerCase().trim() : "";
        String statusCriterion = (statusFilter != null && statusFilter.getValue() != null)
                ? statusFilter.getValue().trim()
                : "All";

        List<Course> filtered = allCourses.stream()
                .filter(c -> {
                    String name = c.getName() != null ? c.getName().toLowerCase() : "";
                    String email = c.getTeacher_email() != null ? c.getTeacher_email().toLowerCase() : "";
                    return searchText.isEmpty() || name.contains(searchText) || email.contains(searchText);
                })
                .filter(c -> matchesStatusFilter(c, statusCriterion))
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

    /** Status filter: All (no filter), Active, or Pending. */
    private static boolean matchesStatusFilter(Course c, String criterion) {
        String crit = (criterion == null || criterion.isEmpty()) ? "All" : criterion.trim();
        if (crit.equalsIgnoreCase("All")) {
            return true;
        }
        String raw = c.getStatus();
        if (raw == null || raw.isBlank()) {
            return crit.equalsIgnoreCase("Active");
        }
        return raw.trim().equalsIgnoreCase(crit);
    }

    private static int semesterRank(Course c) {
        String s = c.getSemester();
        if (s == null || s.trim().isEmpty()) {
            return Integer.MAX_VALUE;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)").matcher(s);
        return m.find() ? Integer.parseInt(m.group(1)) : Integer.MAX_VALUE - 1;
    }

    // --- Core Logic: Tri (Sort) ---
    private void applySorting(List<Course> list) {
        String sortCriterion = sortComboBox != null && sortComboBox.getValue() != null
                ? sortComboBox.getValue().trim()
                : "Name (A-Z)";
        Comparator<Course> byName = Comparator.comparing(
                c -> c.getName() != null ? c.getName().toLowerCase() : "",
                String.CASE_INSENSITIVE_ORDER);
        switch (sortCriterion) {
            case "Name (A-Z)":
                list.sort(byName);
                break;
            case "Name (Z-A)":
                list.sort(byName.reversed());
                break;
            case "Newest first":
                list.sort(Comparator
                        .comparing(Course::getCreated_at, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparingInt(c -> -c.getId()));
                break;
            case "Oldest first":
                list.sort(Comparator
                        .comparing(Course::getCreated_at, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparingInt(Course::getId));
                break;
            case "Coefficient (high to low)":
                list.sort((c1, c2) -> Double.compare(c2.getCoefficient(), c1.getCoefficient()));
                break;
            case "Coefficient (low to high)":
                list.sort(Comparator.comparingDouble(Course::getCoefficient));
                break;
            case "Duration (short to long)":
                list.sort(Comparator.comparingInt(Course::getDuration));
                break;
            case "Duration (long to short)":
                list.sort((c1, c2) -> Integer.compare(c2.getDuration(), c1.getDuration()));
                break;
            case "Semester (by number)":
                list.sort(Comparator.comparingInt(CourseListController::semesterRank).thenComparing(byName));
                break;
            // Legacy labels from older builds
            case "Coefficient (High-Low)":
                list.sort((c1, c2) -> Double.compare(c2.getCoefficient(), c1.getCoefficient()));
                break;
            case "Semester (1-2)":
                list.sort(Comparator.comparingInt(CourseListController::semesterRank).thenComparing(byName));
                break;
            case "Duration (Shortest)":
                list.sort(Comparator.comparingInt(Course::getDuration));
                break;
            default:
                list.sort(byName);
                break;
        }
    }

    private void updateStats(List<Course> list) {
        long total = list.size();
        long active = list.stream()
                .filter(c -> c.getStatus() != null && c.getStatus().equalsIgnoreCase("Active"))
                .count();
        long pending = list.stream()
                .filter(c -> c.getStatus() != null && c.getStatus().equalsIgnoreCase("Pending"))
                .count();
        long archived = list.stream()
                .filter(c -> c.getStatus() != null
                        && (c.getStatus().equalsIgnoreCase("Archived") || c.getStatus().equalsIgnoreCase("Draft")))
                .count();

        if (statTotal != null) {
            statTotal.setText(String.valueOf(total));
        }
        if (statInProgress != null) {
            statInProgress.setText(String.valueOf(active));
        }
        if (statCompleted != null) {
            statCompleted.setText(String.valueOf(pending));
        }
        if (statPending != null) {
            statPending.setText(String.valueOf(archived));
        }
    }

    private VBox createCourseCard(Course course) {
        VBox card = new VBox(20);
        card.getStyleClass().add("course-card");
        card.setMinWidth(400);
        card.setMaxWidth(450);

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        String courseName = course.getName() != null ? course.getName().toLowerCase() : "untitled";
        Label title = new Label(courseName);
        title.getStyleClass().add("course-card-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        String statusText = course.getStatus() != null ? course.getStatus().toUpperCase() : "UNKNOWN";
        Label statusBadge = new Label(statusText);
        statusBadge.getStyleClass().add("status-badge");
        String status = course.getStatus() != null ? course.getStatus().toLowerCase() : "";
        if (status.equals("active") || status.contains("progress")) {
            statusBadge.getStyleClass().add("status-badge-progress");
        } else if (status.equals("archived") || status.equals("draft") || status.contains("complete")) {
            statusBadge.getStyleClass().add("status-badge-completed");
        } else {
            statusBadge.getStyleClass().add("status-badge-pending");
        }
        header.getChildren().addAll(title, spacer, statusBadge);

        // Teacher
        HBox teacherRow = new HBox(8);
        teacherRow.setAlignment(Pos.CENTER_LEFT);
        SVGPath personIcon = createIcon(
                "M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z",
                "#64748B", 14);
        Label teacher = new Label(course.getTeacher_email() != null ? course.getTeacher_email() : "—");
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

        // Actions Hub
        VBox actions = new VBox(10);
        actions.setPadding(new Insets(15, 0, 0, 0));
        
        // Primary Action
        Button viewBtn = new Button("View Details");
        viewBtn.getStyleClass().add("btn-primary-action");
        javafx.scene.Node viewGraphic = createIcon("M12 4l-1.41 1.41L16.17 11H4v2h12.17l-5.58 5.59L12 20l8-8z", "white", 14);
        viewGraphic.setMouseTransparent(true);
        viewBtn.setGraphic(viewGraphic);
        viewBtn.setMaxWidth(Double.MAX_VALUE);
        viewBtn.setOnAction(e -> {
            e.consume();
            showCourseDetailView(course);
        });

        // Sub-Management Row (Exam & Activity)
        HBox subRow = new HBox(10);
        subRow.setAlignment(Pos.CENTER);
        
        Button examBtn = new Button("Exams");
        examBtn.getStyleClass().add("btn-secondary-exam");
        examBtn.setGraphic(createIcon("M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z", "#4338CA", 14));
        examBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(examBtn, Priority.ALWAYS);
        examBtn.setOnAction(e -> {
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/gestion_examen/frontend_exams.fxml"));
                javafx.scene.Parent root = loader.load();
                controllers.exams.ExamListController controller = loader.getController();
                controller.setCourse(course);
                if (controllers.FrontendController.getInstance() != null) {
                    controllers.FrontendController.getInstance().loadContentNode(root);
                } else {
                    Stage stage = (Stage) card.getScene().getWindow();
                    stage.getScene().setRoot(root);
                }
            } catch (java.io.IOException ex) { ex.printStackTrace(); }
        });

        Button activityBtn = new Button("Activities");
        activityBtn.getStyleClass().add("btn-secondary-activity");
        activityBtn.setGraphic(createIcon("M3 13h2v-2H3v2zm0 4h2v-2H3v2zm0-8h2V7H3v2zm4 4h14v-2H7v2zm0 4h14v-2H7v2zM7 7v2h14V7H7z", "#0E7490", 14));
        activityBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(activityBtn, Priority.ALWAYS);
        activityBtn.setOnAction(e -> {
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/gestion_activites/frontend_activities.fxml"));
                javafx.scene.Parent root = loader.load();
                controllers.activities.ActivityListController controller = loader.getController();
                controller.setCourse(course);
                if (controllers.FrontendController.getInstance() != null) {
                    controllers.FrontendController.getInstance().loadContentNode(root);
                } else {
                    Stage stage = (Stage) card.getScene().getWindow();
                    stage.getScene().setRoot(root);
                }
            } catch (java.io.IOException ex) { ex.printStackTrace(); }
        });
        subRow.getChildren().addAll(examBtn, activityBtn);

        // Management Row (Edit & Delete)
        HBox manageRow = new HBox(10);
        manageRow.setAlignment(Pos.CENTER);
        
        Button editBtn = new Button("Edit");
        editBtn.getStyleClass().add("btn-small-edit");
        editBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(editBtn, Priority.ALWAYS);
        editBtn.setGraphic(createIcon("M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25z", "#7C3AED", 14));
        editBtn.setOnAction(e -> CourseEditController.startEdit(course, (Stage) card.getScene().getWindow()));

        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().add("btn-small-delete");
        deleteBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(deleteBtn, Priority.ALWAYS);
        deleteBtn.setGraphic(createIcon("M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12z", "#DC2626", 14));
        deleteBtn.setOnAction(e -> showSleekDeleteOverlay(course));

        manageRow.getChildren().addAll(editBtn, deleteBtn);
        
        actions.getChildren().addAll(viewBtn, subRow, manageRow);
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

        // Invisible overlay still receives mouse hits until fade-in completes — block clicks on cards underneath
        overlay.setMouseTransparent(true);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), overlay);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.setOnFinished(ev -> overlay.setMouseTransparent(false));

        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(300), card);
        scaleUp.setFromX(0.8);
        scaleUp.setFromY(0.8);
        scaleUp.setToX(1.0);
        scaleUp.setToY(1.0);

        fadeIn.play();
        scaleUp.play();
    }

    private void hideOverlay(StackPane overlay, VBox card) {
        overlay.setMouseTransparent(true);
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
        final Course courseRef = course;
        Platform.runLater(() -> {
            try {
                java.net.URL url = CourseDetailController.class.getResource("/gestion_cours/frontend_course_detail.fxml");
                if (url == null) {
                    url = Thread.currentThread().getContextClassLoader()
                            .getResource("gestion_cours/frontend_course_detail.fxml");
                }
                if (url == null) {
                    showCourseDetailError("Missing FXML", "Resource not found: gestion_cours/frontend_course_detail.fxml");
                    return;
                }
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(url);
                javafx.scene.Parent root = loader.load();
                CourseDetailController controller = loader.getController();
                // WebView / preview: attach to scene first, then populate (avoids many Windows/JavaFX failures).
                if (controllers.FrontendController.getInstance() != null) {
                    controllers.FrontendController.getInstance().loadContentNode(root);
                } else if (coursesContainer != null && coursesContainer.getScene() != null) {
                    Stage stage = (Stage) coursesContainer.getScene().getWindow();
                    stage.getScene().setRoot(root);
                } else {
                    showCourseDetailError("Navigation", "No window or dashboard to show course details.");
                    return;
                }
                controller.populateCourseDetails(courseRef);
            } catch (Throwable t) {
                t.printStackTrace();
                String msg = t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
                showCourseDetailError("Course details", msg);
            }
        });
    }

    private static void showCourseDetailError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }
}
