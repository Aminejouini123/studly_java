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
import models.Course;
import models.Exam;
import services.ExamService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class BackendExamController {

    @FXML public TableView<Exam> examsTable;
    @FXML public TableColumn<Exam, String> colExamTitle;
    @FXML public TableColumn<Exam, java.sql.Date> colDate;
    @FXML public TableColumn<Exam, Integer> colDuration;
    @FXML public TableColumn<Exam, Double> colGrade;
    @FXML public TableColumn<Exam, String> colDifficulty;
    @FXML public TableColumn<Exam, String> colStatus;
    @FXML public TableColumn<Exam, Void> colActions;

    @FXML public TextField searchField;
    @FXML public Label totalExamsLabel;
    @FXML public Label passedExamsLabel;
    @FXML public Label titleLabel;
    @FXML private javafx.scene.layout.VBox contentArea;
    private javafx.collections.transformation.SortedList<Exam> sortedList;
    private java.util.List<javafx.scene.Node> dashboardCache;
    private javafx.scene.Parent originalRoot;

    private boolean fromBackend = true; // For the setup forms to know where to return
    private Course filteredCourse;

    private ExamService examService;
    private ObservableList<Exam> examsList;
    private FilteredList<Exam> filteredList;

    public void setFilteredCourse(Course course) {
        this.filteredCourse = course;
        // Force reload once the filter is set, in case initialize() already ran
        if (examService != null) {
            loadExams();
            if (titleLabel != null) {
                titleLabel.setText("Exam Administration: " + filteredCourse.getName().toUpperCase());
            }
        }
    }

    public BackendExamController() {
        this.examService = new ExamService();
        this.examsList = FXCollections.observableArrayList();
    }

    @FXML
    public void initialize() {
        setupTableColumns();
        setupDataBinding();
        loadExams();
        setupSearch();
        updateStatistics();
        
        if (filteredCourse != null) {
            titleLabel.setText("Exam Administration: " + filteredCourse.getName().toUpperCase());
        } else {
            titleLabel.setText("Global Exam Administration");
        }
    }

    private void setupDataBinding() {
        filteredList = new javafx.collections.transformation.FilteredList<>(examsList, p -> true);
        sortedList = new javafx.collections.transformation.SortedList<>(filteredList);
        sortedList.comparatorProperty().bind(examsTable.comparatorProperty());
        examsTable.setItems(sortedList);
    }

    private void setupTableColumns() {
        colExamTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colExamTitle.setCellFactory(column -> new TableCell<Exam, String>() {
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

        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colDuration.setCellValueFactory(new PropertyValueFactory<>("duration"));
        colGrade.setCellValueFactory(new PropertyValueFactory<>("grade"));
        
        colDifficulty.setCellValueFactory(new PropertyValueFactory<>("difficulty"));
        colDifficulty.setCellFactory(column -> new TableCell<Exam, String>() {
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
        colStatus.setCellFactory(column -> new TableCell<Exam, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                } else {
                    Circle dot = new Circle(4);
                    Label lbl = new Label(status);
                    lbl.setStyle("-fx-text-fill: #0F172A; -fx-font-size: 12px; -fx-font-weight: bold;");
                    if ("Passed".equalsIgnoreCase(status) || "Active".equalsIgnoreCase(status)) {
                        dot.setFill(Color.web("#4ade80"));
                    } else if ("Pending".equalsIgnoreCase(status)) {
                        dot.setFill(Color.web("#f59e0b"));
                    } else {
                        dot.setFill(Color.web("#f43f5e"));
                    }
                    HBox box = new HBox(8, dot, lbl);
                    box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    setGraphic(box);
                }
            }
        });

        colActions.setCellFactory(column -> new TableCell<Exam, Void>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Exam exam = getTableView().getItems().get(getIndex());
                    
                    Button editBtn = new Button("Edit");
                    editBtn.getStyleClass().add("btn-outline");
                    editBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 10; -fx-text-fill: #3b82f6; -fx-border-color: #3b82f6;");
                    editBtn.setOnAction(e -> handleEditExam(exam));
                    
                    Button deleteBtn = new Button("Delete");
                    deleteBtn.getStyleClass().add("btn-outline");
                    deleteBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 10; -fx-text-fill: #ef4444; -fx-border-color: #ef4444;");
                    deleteBtn.setOnAction(e -> {
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                        alert.setTitle("Delete Exam");
                        alert.setHeaderText("Delete exam: " + exam.getTitle() + "?");
                        alert.setContentText("This action is permanent.");
                        if (alert.showAndWait().get() == ButtonType.OK) {
                            try {
                                examService.supprimer(exam.getId());
                                loadExams();
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
    public void handleCreateNewExam() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gestion_examen/backend_form_exam.fxml"));
            javafx.scene.Parent formRoot = loader.load();
            controllers.exams.ExamAddController controller = loader.getController();
            controller.setFromBackend(true);
            controller.setBackendController(this);
            
            if (examsTable.getScene() != null) {
                this.originalRoot = examsTable.getScene().getRoot();
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
                Stage stage = (Stage) examsTable.getScene().getWindow();
                stage.getScene().setRoot(formRoot);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void restoreDashboard() {
        if (examsTable.getScene() != null && originalRoot != null && examsTable.getScene().getRoot() != originalRoot) {
            examsTable.getScene().setRoot(originalRoot);
        }
        
        if (contentArea != null && dashboardCache != null) {
            contentArea.getChildren().setAll(dashboardCache);
        }
        
        loadExams(); // Refresh data
    }

    private void handleEditExam(Exam exam) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gestion_examen/backend_edit_exam.fxml"));
            javafx.scene.Parent formRoot = loader.load();
            controllers.exams.ExamEditController controller = loader.getController();
            controller.setFromBackend(true);
            controller.setExam(exam);
            controller.setBackendController(this);
            
            if (examsTable.getScene() != null) {
                this.originalRoot = examsTable.getScene().getRoot();
            }
            
            // Professional in-place replacement with caching
            if (contentArea != null) {
                if (dashboardCache == null) {
                    dashboardCache = new java.util.ArrayList<>(contentArea.getChildren());
                }
                contentArea.getChildren().setAll(formRoot);
            } else {
                Stage stage = (Stage) examsTable.getScene().getWindow();
                stage.getScene().setRoot(formRoot);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void loadExams() {
        try {
            List<Exam> exams;
            if (filteredCourse != null) {
                exams = examService.recupererParCours(filteredCourse.getId());
            } else {
                exams = examService.recuperer();
            }
            examsList.setAll(exams);
            updateStatistics();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setupSearch() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredList.setPredicate(exam -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();
                
                return exam.getTitle().toLowerCase().contains(lowerCaseFilter) ||
                       (exam.getDifficulty() != null && exam.getDifficulty().toLowerCase().contains(lowerCaseFilter)) ||
                       (exam.getStatus() != null && exam.getStatus().toLowerCase().contains(lowerCaseFilter));
            });
            updateStatistics();
        });
    }

    private void updateStatistics() {
        totalExamsLabel.setText(String.valueOf(examsList.size()));
        long passedCount = examsList.stream()
                .filter(e -> e.getGrade() >= 10)
                .count();
        passedExamsLabel.setText(String.valueOf(passedCount));
    }

    @FXML
    public void handleShowCourses() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gestion_cours/backend_courses.fxml"));
            javafx.scene.Parent root = loader.load();
            Stage stage = (Stage) examsTable.getScene().getWindow();
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
            Stage stage = (Stage) examsTable.getScene().getWindow();
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
            Stage stage = (Stage) examsTable.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleExportExcel() {
        String[] headers = {"Title", "Date", "Duration", "Grade", "Difficulty", "Status"};
        utils.CSVExportUtil.exportToCSV(
            examsList,
            headers,
            item -> new String[]{
                item.getTitle(),
                item.getDate() != null ? item.getDate().toString() : "",
                String.valueOf(item.getDuration()),
                String.valueOf(item.getGrade()),
                item.getDifficulty(),
                item.getStatus()
            },
            "Exam_List_Export",
            (Stage) examsTable.getScene().getWindow()
        );
    }

}
