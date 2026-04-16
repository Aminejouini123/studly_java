package controllers.courses;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.shape.Arc;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import javafx.scene.Node;
import javafx.stage.Stage;
import models.Course;
import services.CourseService;
import java.sql.SQLException;
import java.text.SimpleDateFormat;

public class CourseDetailController extends BaseCourseController {

    @FXML
    private Label detailType, detailStatus, detailDifficulty, detailPriority;
    @FXML
    private Label detailTitle, detailDuration, detailSemester, detailCoefficient;
    @FXML
    private Arc coefficientArc;
    @FXML
    private Label resourceLinkLabel, detailFileName, detailComment, detailTeacher, detailCreatedAt, teacherInitials;
    @FXML
    private StackPane deleteModalOverlay;
    @FXML
    private WebView pdfWebView;
    @FXML
    private javafx.scene.layout.VBox previewContentArea;
    @FXML
    private javafx.scene.control.Button togglePreviewBtn;
    @FXML
    private javafx.scene.Node filePlaceholderLabel;

    private Course displayedCourse;

    @FXML
    public void handleBackToCoursesAction(javafx.event.ActionEvent event) {
        if (fromBackend && backendController != null) {
            backendController.restoreDashboard();
        } else if (fromBackend) {
            loadScene("/TEMPLATE/backend_courses.fxml", null, (javafx.scene.Node) event.getSource());
        } else {
            // Always reload the standalone full-page scene since we completely replaced the root 
            // when entering this detail view. Calling FrontendController.loadContent here modifies a detached node.
            loadScene("/gestion_cours/frontend_courses.fxml", null, (javafx.scene.Node) event.getSource());
        }
    }

    @FXML
    public void populateCourseDetails(Course course) {
        this.displayedCourse = course;

        detailTitle.setText(course.getName());
        detailType.setText(course.getType().toUpperCase());
        detailStatus.setText(course.getStatus().toUpperCase());
        detailPriority.setText(course.getPriority().toUpperCase());
        detailDifficulty.setText(course.getDifficulty_level().toUpperCase());
        detailDuration.setText(course.getDuration() + " hours");
        detailSemester.setText("Semester " + course.getSemester());
        detailCoefficient.setText(String.valueOf((int) course.getCoefficient()));

        // Arc Logic
        double targetAngle = (course.getCoefficient() / 10.0) * 360.0;
        coefficientArc.setLength(-Math.min(targetAngle, 360));

        resourceLinkLabel.setText(course.getCourse_link().isEmpty() ? "No link available" : course.getCourse_link());
        detailFileName.setText(course.getCourse_file().isEmpty() ? "No file attached" : course.getCourse_file());
        detailComment.setText(course.getComment().isEmpty() ? "No additional notes provided." : course.getComment());
        detailTeacher.setText(course.getTeacher_email());
        detailCreatedAt.setText(new SimpleDateFormat("dd MMM yyyy").format(course.getCreated_at()));


        // Initials
        updateTeacherInitials(course.getTeacher_email());

        // PDF Preview
        updateFilePreview(course);
    }

    private void updateTeacherInitials(String email) {
        if (teacherInitials == null || email.isEmpty())
            return;
        String initials = "UC";
        if (email.contains("@")) {
            String namePart = email.split("@")[0];
            if (namePart.contains(".")) {
                String[] parts = namePart.split("\\.");
                initials = (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
            } else {
                initials = namePart.substring(0, Math.min(2, namePart.length())).toUpperCase();
            }
        }
        teacherInitials.setText(initials);
    }

    private void updateFilePreview(Course course) {
        if (pdfWebView == null || previewContentArea == null)
            return;
            
        String filePath = course.getCourse_file();
        String link = course.getCourse_link();
        boolean hasPreviewable = false;

        if (filePath != null && !filePath.isEmpty()) {
            java.io.File file = new java.io.File(filePath);
            if (file.exists()) {
                String ext = filePath.toLowerCase();
                if (ext.endsWith(".png") || ext.endsWith(".jpg") || ext.endsWith(".jpeg") || ext.endsWith(".gif")) {
                    pdfWebView.getEngine().load(file.toURI().toString());
                    hasPreviewable = true;
                } else if (ext.endsWith(".pdf")) {
                    long sizeMB = file.length() / (1024 * 1024);
                    if (sizeMB > 5) {
                        String largeHtml = "<html><body style='font-family: Inter, sans-serif; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; background-color: #F1F5F9;'><div style='text-align: center; color: #475569; padding: 40px; background: white; border-radius: 12px; box-shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1);'><h2>📄 Large PDF Attached</h2><p>This PDF is too large (" + sizeMB + " MB) for the inline viewer.</p><p style='font-weight: bold;'>Please click the 'Download' button above to open it securely.</p></div></body></html>";
                        pdfWebView.getEngine().loadContent(largeHtml);
                        hasPreviewable = true;
                    } else {
                        try {
                            byte[] fileContent = java.nio.file.Files.readAllBytes(file.toPath());
                            String base64Pdf = java.util.Base64.getEncoder().encodeToString(fileContent);
                            String html = "<!DOCTYPE html><html><head>" +
                                "<script src='https://cdnjs.cloudflare.com/ajax/libs/pdf.js/2.16.105/pdf.min.js'></script>" +
                                "<style>body{background-color:#525659;display:flex;flex-direction:column;align-items:center;gap:15px;padding:20px;margin:0;font-family:sans-serif;} canvas{box-shadow:0 10px 15px -3px rgb(0 0 0 / 0.3); max-width: 100%; height: auto; border-radius: 4px;}</style>" +
                                "</head><body>" +
                                "<script>" +
                                "var pdfData = atob('" + base64Pdf + "');" +
                                "pdfjsLib.GlobalWorkerOptions.workerSrc = 'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/2.16.105/pdf.worker.min.js';" +
                                "var loadingTask = pdfjsLib.getDocument({data: pdfData});" +
                                "loadingTask.promise.then(function(pdf) {" +
                                "  for (let i = 1; i <= pdf.numPages; i++) {" +
                                "    pdf.getPage(i).then(function(page) {" +
                                "      var scale = 1.3;" +
                                "      var viewport = page.getViewport({scale: scale});" +
                                "      var canvas = document.createElement('canvas');" +
                                "      var context = canvas.getContext('2d');" +
                                "      canvas.height = viewport.height;" +
                                "      canvas.width = viewport.width;" +
                                "      document.body.appendChild(canvas);" +
                                "      var renderContext = { canvasContext: context, viewport: viewport };" +
                                "      page.render(renderContext);" +
                                "    });" +
                                "  }" +
                                "}).catch(function(error) { " +
                                "  document.body.innerHTML = '<h2 style=\"color:white\">Error rendering PDF inline. Please use the Download button.</h2>'; " +
                                "});" +
                                "</script></body></html>";
                            pdfWebView.getEngine().loadContent(html);
                            hasPreviewable = true;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } 
        
        if (!hasPreviewable && link != null && link.startsWith("http")) {
            pdfWebView.getEngine().load(link);
            hasPreviewable = true;
        }

        if (hasPreviewable) {
            pdfWebView.setVisible(true);
            filePlaceholderLabel.setVisible(false);
        } else {
            pdfWebView.setVisible(false);
            filePlaceholderLabel.setVisible(true);
        }
    }


    @FXML
    public void handleOpenExternally() {
        if (displayedCourse == null || displayedCourse.getCourse_file() == null || displayedCourse.getCourse_file().isEmpty()) return;
        try {
            java.io.File file = new java.io.File(displayedCourse.getCourse_file());
            if (file.exists()) {
                new ProcessBuilder("cmd", "/c", "start", "", file.getAbsolutePath()).start();
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleTogglePreview() {
        if (previewContentArea.isVisible()) {
            previewContentArea.setVisible(false);
            previewContentArea.setManaged(false);
            togglePreviewBtn.setText("Preview ▲");
        } else {
            previewContentArea.setVisible(true);
            previewContentArea.setManaged(true);
            togglePreviewBtn.setText("Preview ▼");
            updateFilePreview(displayedCourse);
        }
    }

    @FXML
    public void handleEditAction() {
        if (displayedCourse != null) {
            CourseEditController.startEdit(displayedCourse, (Stage) detailTitle.getScene().getWindow());
        }
    }

    @FXML
    public void openDeleteModal() {
        deleteModalOverlay.setVisible(true);
    }

    @FXML
    public void closeDeleteModal() {
        deleteModalOverlay.setVisible(false);
    }

    @FXML
    public void confirmDelete() {
        if (displayedCourse != null) {
            try {
                new CourseService().supprimer(displayedCourse.getId());
                goToCourses(null); // Return
                loadScene("/gestion_cours/frontend_courses.fxml", null, (javafx.scene.Node) detailTitle);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    public void goToExams(javafx.event.ActionEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/gestion_examen/frontend_exams.fxml"));
            javafx.scene.Parent root = loader.load();
            controllers.exams.ExamListController controller = loader.getController();
            controller.setCourse(displayedCourse);
            javafx.stage.Stage stage = (javafx.stage.Stage) detailTitle.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}
