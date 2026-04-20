package controllers.exams;

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
import models.Exam;
import services.ExamService;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import java.util.stream.Collectors;

public class ExamListController extends BaseExamController {

    @FXML
    private StackPane rootPane;
    @FXML
    private Label courseTitleLabel;
    @FXML
    private Label statTotal, statSuccessRate, statAvgGrade, statUpcoming;
    @FXML
    private FlowPane examsContainer;
    @FXML
    private VBox emptyState;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> statusFilter;
    @FXML
    private ComboBox<String> sortComboBox;

    private Course currentCourse;
    private List<Exam> allExams = new ArrayList<>();
    private final ExamService examService = new ExamService();

    @FXML
    public void initialize() {
        if (statusFilter != null) {
            statusFilter.getItems().addAll("All statuses", "Pending", "Passed", "Failed", "Aborted");
            statusFilter.setValue("All statuses");
            statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> updateDisplay());
        }
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> updateDisplay());
        }
        if (sortComboBox != null) {
            sortComboBox.setValue("Newest First");
            sortComboBox.valueProperty().addListener((obs, oldVal, newVal) -> updateDisplay());
        }
    }

    public void setCourse(Course course) {
        this.currentCourse = course;
        courseTitleLabel.setText(course.getName());
        loadExams();
    }

    private void loadExams() {
        if (currentCourse == null) return;
        try {
            allExams = examService.recupererParCours(currentCourse.getId());
            updateDisplay();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateDisplay() {
        if (searchField == null || statusFilter == null || sortComboBox == null) return;
        
        String searchText = searchField.getText().toLowerCase().trim();
        String selectedStatus = statusFilter.getValue();
        String selectedSort = sortComboBox.getValue();

        List<Exam> filtered = allExams.stream()
                .filter(e -> searchText.isEmpty() || (e.getTitle() != null && e.getTitle().toLowerCase().contains(searchText)))
                .filter(e -> selectedStatus == null || selectedStatus.equals("All statuses") || (e.getStatus() != null && e.getStatus().equalsIgnoreCase(selectedStatus)))
                .collect(Collectors.toList());

        // Sorting
        if (selectedSort != null) {
            switch (selectedSort) {
                case "Title (A-Z)":
                    filtered.sort((e1, e2) -> e1.getTitle().compareToIgnoreCase(e2.getTitle()));
                    break;
                case "Grade (High-Low)":
                    filtered.sort((e1, e2) -> Double.compare(e2.getGrade(), e1.getGrade()));
                    break;
                case "Date (Soonest)":
                    filtered.sort((e1, e2) -> e1.getDate().compareTo(e2.getDate()));
                    break;
                case "Newest First":
                    filtered.sort((e1, e2) -> Integer.compare(e2.getId(), e1.getId()));
                    break;
            }
        }

        examsContainer.getChildren().clear();
        
        if (filtered.isEmpty()) {
            emptyState.setVisible(true);
            emptyState.setManaged(true);
            examsContainer.setVisible(false);
            examsContainer.setManaged(false);
        } else {
            emptyState.setVisible(false);
            emptyState.setManaged(false);
            examsContainer.setVisible(true);
            examsContainer.setManaged(true);
            for (Exam exam : filtered) {
                examsContainer.getChildren().add(createExamCard(exam));
            }
        }
        
        updateStats(filtered);
    }

    private void updateStats(List<Exam> list) {
        long total = allExams.size();
        long passed = allExams.stream().filter(e -> e.getGrade() >= 10).count();
        double avg = allExams.stream().mapToDouble(Exam::getGrade).average().orElse(0.0);
        long upcoming = allExams.stream().filter(e -> e.getDate().after(new java.util.Date())).count();

        statTotal.setText(String.valueOf(total));
        statSuccessRate.setText(total == 0 ? "0%" : (int)((passed * 100.0) / total) + "%");
        statAvgGrade.setText(String.format("%.2f", avg));
        statUpcoming.setText(String.valueOf(upcoming));
    }

    private VBox createExamCard(Exam exam) {
        VBox card = new VBox(20);
        card.getStyleClass().add("course-card");
        card.setMinWidth(400);
        card.setMaxWidth(440);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label title = new Label(exam.getTitle().toLowerCase());
        title.getStyleClass().add("course-card-title");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label statusBadge = createStatusBadge(exam.getStatus(), exam.getGrade());
        header.getChildren().addAll(title, spacer, statusBadge);

        HBox dateRow = new HBox(8);
        dateRow.setAlignment(Pos.CENTER_LEFT);
        SVGPath calendarIcon = createIcon("M19 4h-1V2h-2v2H8V2H6v2H5c-1.11 0-1.99.9-1.99 2L3 20c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 16H5V10h14v10zm0-12H5V6h14v2z", "#64748B", 14);
        Label dateLabel = new Label(exam.getDate().toString());
        dateLabel.getStyleClass().add("course-card-teacher");
        dateRow.getChildren().addAll(calendarIcon, dateLabel);

        HBox statsBox = new HBox(30);
        statsBox.getStyleClass().add("card-stats-container");
        statsBox.setAlignment(Pos.CENTER);
        
        statsBox.getChildren().addAll(
            createStatItem("M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67V7z", exam.getDuration() + " min"),
            createStatItem("M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z", exam.getDifficulty()),
            createStatItem("M9 11H7v2h2v-2zm4 0h-2v2h2v-2zm4 0h-2v2h2v-2zm2-7h-1V2h-2v2H8V2H6v2H5c-1.11 0-1.99.9-1.99 2L3 20c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 16H5V9h14v11z", String.valueOf(exam.getGrade()))
        );

        VBox footerActions = new VBox(12);
        footerActions.setPadding(new Insets(10, 0, 0, 0));
        
        Button viewBtn = new Button("View Details");
        viewBtn.getStyleClass().add("btn-primary-action");
        viewBtn.setGraphic(createIcon("M12 4l-1.41 1.41L16.17 11H4v2h12.17l-5.58 5.59L12 20l8-8z", "white", 14));
        viewBtn.setMaxWidth(Double.MAX_VALUE);
        viewBtn.setOnAction(e -> handleViewExam(exam));

        HBox actionRow = new HBox(12);
        Button editBtn = new Button("Edit");
        editBtn.getStyleClass().add("btn-small-edit");
        editBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(editBtn, Priority.ALWAYS);
        editBtn.setGraphic(createIcon("M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25z", "#7C3AED", 14));
        editBtn.setOnAction(e -> handleEditExam(exam));

        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().add("btn-small-delete");
        deleteBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(deleteBtn, Priority.ALWAYS);
        deleteBtn.setGraphic(createIcon("M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12z", "#DC2626", 14));
        deleteBtn.setOnAction(e -> showDeleteOverlay(exam));

        actionRow.getChildren().addAll(editBtn, deleteBtn);
        footerActions.getChildren().addAll(viewBtn, actionRow);

        card.getChildren().addAll(header, dateRow, statsBox, footerActions);
        
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

    private Label createStatusBadge(String status, double grade) {
        if (status == null || status.isEmpty()) status = "Pending";
        Label badge = new Label(status.toUpperCase());
        badge.getStyleClass().add("status-badge");
        String s = status.toLowerCase();
        if (s.contains("pending")) badge.getStyleClass().add("status-badge-progress");
        else if (s.contains("passed") || grade >= 10) badge.getStyleClass().add("status-badge-completed");
        else badge.getStyleClass().add("status-badge-pending");
        return badge;
    }

    private HBox createStatItem(String iconPath, String text) {
        HBox item = new HBox(6);
        item.setAlignment(Pos.CENTER_LEFT);
        item.getChildren().addAll(createIcon(iconPath, "#64748B", 14), new Label(text));
        ((Label)item.getChildren().get(1)).setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px; -fx-font-weight: bold;");
        return item;
    }

    private void showDeleteOverlay(Exam exam) {
        showConfirmOverlay(
            rootPane,
            "Delete Exam?",
            "This action cannot be undone.\n\"" + exam.getTitle() + "\" will be lost.",
            "Delete Exam",
            "Keep it",
            () -> {
                try {
                    examService.supprimer(exam.getId());
                    loadExams();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        );
    }

    private void handleViewExam(Exam exam) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gestion_examen/frontend_exam_detail.fxml"));
            Parent root = loader.load();
            ExamDetailController controller = loader.getController();
            controller.setExam(exam, currentCourse);
            Stage stage = (Stage) examsContainer.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleEditExam(Exam exam) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gestion_examen/frontend_edit_exam.fxml"));
            Parent root = loader.load();
            ExamEditController controller = loader.getController();
            controller.setExam(exam, currentCourse);
            Stage stage = (Stage) examsContainer.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleAddExam() {
        if (currentCourse == null) {
            showAlert("Error", "No course selected.");
            return;
        }
        try {
            java.net.URL res = getClass().getResource("/gestion_examen/frontend_add_exam.fxml");
            if (res == null) {
                System.err.println("Could not find FXML: /gestion_examen/frontend_add_exam.fxml");
                return;
            }
            FXMLLoader loader = new FXMLLoader(res);
            Parent root = loader.load();
            
            ExamAddController controller = loader.getController();
            controller.setCourse(currentCourse);
            
            Stage stage = (Stage) examsContainer.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("ERROR loading Add Exam FXML");
            e.printStackTrace();
        }
    }

    @FXML
    public void handleBackToCourses() {
        loadScene("/gestion_cours/frontend_courses.fxml", null, courseTitleLabel);
    }
}
