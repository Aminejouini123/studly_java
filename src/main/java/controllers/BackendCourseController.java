package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import models.Course;
import models.User;
import services.CourseService;
import utils.SessionManager;
import controllers.backend.BackendExamController;
import controllers.backend.BackendActivityController;
import controllers.courses.CourseAddController;
import controllers.courses.CourseEditController;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class BackendCourseController {

    @FXML public TableView<Course> coursesTable;
    @FXML public TableColumn<Course, String> colCourseName;
    @FXML public TableColumn<Course, String> colTeacher;
    @FXML public TableColumn<Course, String> colSemester;
    @FXML public TableColumn<Course, String> colType;
    @FXML public TableColumn<Course, String> colStatus;
    @FXML public TableColumn<Course, Void> colActions;

    @FXML public TextField searchField;
    @FXML public Button sortDateBtn;
    @FXML private javafx.scene.layout.VBox contentArea;

    @FXML public Label filterAll;
    @FXML public Label filterActive;
    @FXML public Label filterPending;

    @FXML public Label totalCoursesLabel;
    @FXML public Label pendingCoursesLabel;

    private CourseService courseService;
    private ObservableList<Course> coursesList;
    private javafx.collections.transformation.FilteredList<Course> filteredList;
    private javafx.collections.transformation.SortedList<Course> sortedList;
    private java.util.List<javafx.scene.Node> dashboardCache;
    private javafx.scene.Parent originalRoot; // To restore scene root if needed

    public BackendCourseController() {
        this.courseService = new CourseService();
        this.coursesList = FXCollections.observableArrayList();
    }

    @FXML
    public void initialize() {
        setupTableColumns();
        setupDataBinding(); // Use best practice binding
        loadCourses();
        setupSearch();
        setupFilters();
        updateStatistics();
    }

    private void setupDataBinding() {
        filteredList = new javafx.collections.transformation.FilteredList<>(coursesList, p -> true);
        sortedList = new javafx.collections.transformation.SortedList<>(filteredList);
        sortedList.comparatorProperty().bind(coursesTable.comparatorProperty());
        coursesTable.setItems(sortedList);
    }

    private void setupTableColumns() {
        colCourseName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCourseName.setCellFactory(column -> new javafx.scene.control.TableCell<Course, String>() {
            @Override
            protected void updateItem(String name, boolean empty) {
                super.updateItem(name, empty);
                if (empty || name == null) {
                    setGraphic(null);
                } else {
                    Course course = getTableView().getItems().get(getIndex());
                    Label nameLbl = new Label(name);
                    nameLbl.setStyle("-fx-text-fill: #0F172A; -fx-font-weight: bold; -fx-font-size: 13px;");
                    Label dateLbl = new Label("Added: " + (course.getCreated_at() != null ? course.getCreated_at().toString().split(" ")[0] : "N/A"));
                    dateLbl.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");
                    javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(2, nameLbl, dateLbl);
                    box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    setGraphic(box);
                }
            }
        });

        colTeacher.setCellValueFactory(new PropertyValueFactory<>("teacher_email"));
        colTeacher.setCellFactory(column -> new javafx.scene.control.TableCell<Course, String>() {
            @Override
            protected void updateItem(String email, boolean empty) {
                super.updateItem(email, empty);
                if (empty || email == null) {
                    setGraphic(null);
                } else {
                    Label emailLbl = new Label(email);
                    emailLbl.setStyle("-fx-text-fill: #0F172A; -fx-font-size: 12px;");
                    setGraphic(emailLbl);
                }
            }
        });

        colSemester.setCellValueFactory(new PropertyValueFactory<>("semester"));
        colSemester.setCellFactory(column -> new javafx.scene.control.TableCell<Course, String>() {
            @Override
            protected void updateItem(String semester, boolean empty) {
                super.updateItem(semester, empty);
                if (empty || semester == null) {
                    setGraphic(null);
                } else {
                    Label lbl = new Label(semester.toUpperCase());
                    lbl.getStyleClass().add("role-pill-support");
                    setGraphic(lbl);
                }
            }
        });

        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colType.setCellFactory(column -> new javafx.scene.control.TableCell<Course, String>() {
            @Override
            protected void updateItem(String type, boolean empty) {
                super.updateItem(type, empty);
                if (empty || type == null) {
                    setGraphic(null);
                } else {
                    Label lbl = new Label(type.toUpperCase());
                    lbl.getStyleClass().add("role-pill-admin");
                    setGraphic(lbl);
                }
            }
        });

        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(column -> new javafx.scene.control.TableCell<Course, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                } else {
                    javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(4);
                    Label lbl = new Label(status);
                    lbl.setStyle("-fx-text-fill: #0F172A; -fx-font-size: 12px; -fx-font-weight: bold;");
                    if ("Active".equalsIgnoreCase(status)) {
                        dot.setFill(javafx.scene.paint.Color.web("#4ade80"));
                    } else if ("Pending".equalsIgnoreCase(status)) {
                        dot.setFill(javafx.scene.paint.Color.web("#f59e0b")); // Orange for pending
                    } else {
                        dot.setFill(javafx.scene.paint.Color.web("#f43f5e")); // Red for flagged/others
                    }
                    javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(8, dot, lbl);
                    box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    setGraphic(box);
                }
            }
        });

        colActions.setCellFactory(column -> new javafx.scene.control.TableCell<Course, Void>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Course course = getTableView().getItems().get(getIndex());

                    // Exam Button
                    Button examBtn = new Button("Examen");
                    examBtn.getStyleClass().addAll("btn-outline");
                    examBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 10; -fx-cursor: hand; -fx-text-fill: #8b5cf6; -fx-border-color: #8b5cf6; -fx-border-radius: 4; -fx-background-radius: 4;");
                    examBtn.setOnAction(e -> handleManageExam(course));

                    // Activity Button
                    Button activityBtn = new Button("Activity");
                    activityBtn.getStyleClass().addAll("btn-outline");
                    activityBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 10; -fx-cursor: hand; -fx-text-fill: #eab308; -fx-border-color: #eab308; -fx-border-radius: 4; -fx-background-radius: 4;");
                    activityBtn.setOnAction(e -> handleManageActivity(course));

                    // Edit Button
                    Button editBtn = new Button();
                    javafx.scene.shape.SVGPath editIcon = new javafx.scene.shape.SVGPath();
                    editIcon.setContent("M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25z");
                    editIcon.setFill(javafx.scene.paint.Color.web("#3b82f6"));
                    editIcon.setScaleX(0.7); editIcon.setScaleY(0.7);
                    editBtn.setGraphic(editIcon);
                    editBtn.getStyleClass().add("btn-edit");
                    editBtn.setTooltip(new javafx.scene.control.Tooltip("Edit Course"));
                    editBtn.setOnAction(e -> handleEditCourse(course));

                    // Delete Button
                    Button deleteBtn = new Button();
                    javafx.scene.shape.SVGPath trashIcon = new javafx.scene.shape.SVGPath();
                    trashIcon.setContent("M 6 19 c 0 1.1 0.9 2 2 2 h 8 c 1.1 0 2 -0.9 2 -2 V 7 H 6 v 12 Z M 19 4 h -3.5 l -1 -1 h -5 l -1 1 H 5 v 2 h 14 V 4 Z");
                    trashIcon.setFill(javafx.scene.paint.Color.web("#ef4444"));
                    trashIcon.setScaleX(0.7); trashIcon.setScaleY(0.7);
                    deleteBtn.setGraphic(trashIcon);
                    deleteBtn.getStyleClass().add("btn-delete");
                    deleteBtn.setTooltip(new javafx.scene.control.Tooltip("Delete Course"));
                    deleteBtn.setOnAction(e -> handleDeleteCourse(course));

                    javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(2, examBtn, activityBtn, editBtn, deleteBtn);
                    box.setAlignment(javafx.geometry.Pos.CENTER);
                    setGraphic(box);
                }
            }
        });
    }

    private void loadCourses() {
        try {
            User currentUser = SessionManager.getCurrentUser();
            if (currentUser != null) {
                List<Course> courses;
                if (currentUser.getRoles() != null && currentUser.getRoles().contains("ROLE_ADMIN")) {
                    courses = courseService.recuperer();
                } else {
                    courses = courseService.recupererParUser(currentUser.getId());
                }
                coursesList.setAll(courses); // UI updates automatically due to binding
                updateStatistics();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setupSearch() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters();
        });
    }

    private void setupFilters() {
        filterAll.setCursor(javafx.scene.Cursor.HAND);
        filterActive.setCursor(javafx.scene.Cursor.HAND);
        filterPending.setCursor(javafx.scene.Cursor.HAND);

        filterAll.setOnMouseClicked(e -> {
            setActiveFilter(filterAll);
            applyFilters();
        });
        filterActive.setOnMouseClicked(e -> {
            setActiveFilter(filterActive);
            applyFilters();
        });
        filterPending.setOnMouseClicked(e -> {
            setActiveFilter(filterPending);
            applyFilters();
        });
    }

    private void setActiveFilter(Label label) {
        filterAll.getStyleClass().remove("filter-text-active");
        filterActive.getStyleClass().remove("filter-text-active");
        filterPending.getStyleClass().remove("filter-text-active");
        
        filterAll.getStyleClass().add("filter-text");
        filterActive.getStyleClass().add("filter-text");
        filterPending.getStyleClass().add("filter-text");
        
        label.getStyleClass().remove("filter-text");
        label.getStyleClass().add("filter-text-active");
    }

    private void applyFilters() {
        String searchText = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        String activeFilterText = "ALL COURSES";
        if (filterActive.getStyleClass().contains("filter-text-active")) activeFilterText = "ACTIVE";
        if (filterPending.getStyleClass().contains("filter-text-active")) activeFilterText = "PENDING";

        final String statusFilter = activeFilterText;

        filteredList.setPredicate(course -> {
            // Search filter
            boolean matchesSearch = searchText.isEmpty() ||
                course.getName().toLowerCase().contains(searchText) ||
                (course.getTeacher_email() != null && course.getTeacher_email().toLowerCase().contains(searchText)) ||
                (course.getSemester() != null && course.getSemester().toLowerCase().contains(searchText));

            if (!matchesSearch) return false;

            // Status filter
            if (statusFilter.equals("ALL COURSES")) return true;
            return statusFilter.equalsIgnoreCase(course.getStatus());
        });
        updateStatistics();
    }

    private void updateStatistics() {
        if (coursesList == null) return;
        totalCoursesLabel.setText(String.valueOf(coursesList.size()));
        long pendingCount = coursesList.stream()
                .filter(c -> "Pending".equalsIgnoreCase(c.getStatus()))
                .count();
        pendingCoursesLabel.setText(String.valueOf(pendingCount));
    }

    @FXML
    public void handleShowUsers() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/TEMPLATE/backend_management.fxml"));
            javafx.scene.Parent root = loader.load();
            Stage stage = (Stage) coursesTable.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleExportExcel() {
        String[] headers = {"Name", "Teacher", "Semester", "Difficulty", "Type", "Status", "Coefficient", "Duration"};
        utils.CSVExportUtil.exportToCSV(
            coursesList,
            headers,
            item -> new String[]{
                item.getName(),
                item.getTeacher_email(),
                item.getSemester(),
                item.getDifficulty_level(),
                item.getType(),
                item.getStatus(),
                String.valueOf(item.getCoefficient()),
                String.valueOf(item.getDuration())
            },
            "Course_List_Export",
            (Stage) coursesTable.getScene().getWindow()
        );
    }

    @FXML
    public void handleCreateNewCourse() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gestion_cours/backend_form_course.fxml"));
            javafx.scene.Parent formRoot = loader.load();
            controllers.courses.CourseAddController controller = loader.getController();
            controller.setFromBackend(true);
            controller.setBackendController(this);
            
            // Record the current state so we can restore it later
            if (coursesTable.getScene() != null) {
                this.originalRoot = coursesTable.getScene().getRoot();
            }
            
            if (contentArea != null) {
                if (dashboardCache == null) {
                    dashboardCache = new java.util.ArrayList<>(contentArea.getChildren());
                }
                contentArea.getChildren().setAll(formRoot);
            } else {
                Stage stage = (Stage) coursesTable.getScene().getWindow();
                stage.getScene().setRoot(formRoot);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void restoreDashboard() {
        if (coursesTable.getScene() != null && originalRoot != null && coursesTable.getScene().getRoot() != originalRoot) {
            coursesTable.getScene().setRoot(originalRoot);
        }
        
        if (contentArea != null && dashboardCache != null) {
            contentArea.getChildren().setAll(dashboardCache);
        }
        
        loadCourses(); // Refresh data from DB
    }

    private void handleManageExam(Course course) {
        if (course == null) {
            System.err.println("Error: Course object is null in handleManageExam");
            return;
        }
        try {
            String fxmlPath = "/gestion_examen/backend_exams.fxml";
            java.net.URL resource = getClass().getResource(fxmlPath);
            if (resource == null) {
                throw new IOException("Could not find FXML resource at: " + fxmlPath);
            }
            
            FXMLLoader loader = new FXMLLoader(resource);
            javafx.scene.Parent root = loader.load();
            
            BackendExamController controller = loader.getController();
            if (controller == null) {
                throw new Exception("BackendExamController is null - check fx:controller in " + fxmlPath);
            }
            
            controller.setFilteredCourse(course);
            
            if (coursesTable.getScene() == null) {
                throw new Exception("Courses table scene is null");
            }
            
            Stage stage = (Stage) coursesTable.getScene().getWindow();
            stage.getScene().setRoot(root);
            
        } catch (Exception e) {
            System.err.println("Navigation Error (Exams): " + e.getMessage());
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Navigation Error");
            alert.setHeaderText("Could not open Exam Dashboard");
            alert.setContentText("Details: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void handleManageActivity(Course course) {
        if (course == null) {
            System.err.println("Error: Course object is null in handleManageActivity");
            return;
        }
        try {
            String fxmlPath = "/gestion_activites/backend_activities.fxml";
            java.net.URL resource = getClass().getResource(fxmlPath);
            if (resource == null) {
                throw new IOException("Could not find FXML resource at: " + fxmlPath);
            }

            FXMLLoader loader = new FXMLLoader(resource);
            javafx.scene.Parent root = loader.load();
            
            BackendActivityController controller = loader.getController();
            if (controller == null) {
                throw new Exception("BackendActivityController is null - check fx:controller in " + fxmlPath);
            }

            controller.setFilteredCourse(course);
            
            if (coursesTable.getScene() == null) {
                throw new Exception("Courses table scene is null");
            }

            Stage stage = (Stage) coursesTable.getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (Exception e) {
            System.err.println("Navigation Error (Activities): " + e.getMessage());
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Navigation Error");
            alert.setHeaderText("Could not open Activity Dashboard");
            alert.setContentText("Details: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void handleEditCourse(Course course) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gestion_cours/backend_edit_course.fxml"));
            javafx.scene.Parent formRoot = loader.load();
            controllers.courses.CourseEditController controller = loader.getController();
            controller.setFromBackend(true);
            controller.setCourse(course);
            controller.setBackendController(this);
            
            if (coursesTable.getScene() != null) {
                this.originalRoot = coursesTable.getScene().getRoot();
            }
            
            if (contentArea != null) {
                if (dashboardCache == null) {
                    dashboardCache = new java.util.ArrayList<>(contentArea.getChildren());
                }
                contentArea.getChildren().setAll(formRoot);
            } else {
                Stage stage = (Stage) coursesTable.getScene().getWindow();
                stage.getScene().setRoot(formRoot);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleDeleteCourse(Course course) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Course");
        alert.setHeaderText("Are you sure you want to delete: " + course.getName() + "?");
        alert.setContentText("This action cannot be undone.");

        if (alert.showAndWait().get() == ButtonType.OK) {
            try {
                courseService.supprimer(course.getId());
                loadCourses();
                updateStatistics();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
