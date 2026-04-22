package controllers.exams;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;
import java.io.IOException;
import models.Course;
import models.Exam;

public class ExamDetailController extends BaseExamController {

    @FXML
    public Label examTitle, examType, examStatus, examDifficulty, examDuration, examGrade, examDate, courseNameLabel;
    public Label fileNameLabel, linkLabel, targetGradeLabel;
    @FXML
    public WebView pdfWebView;
    @FXML
    public VBox filePlaceholder;
    @FXML
    public VBox previewContentArea;
    @FXML
    public Button togglePreviewBtn;
    @FXML
    public javafx.scene.shape.Arc scoreArc;
    @FXML
    public javafx.scene.layout.StackPane deleteModalOverlay, rootPane;

    private Exam currentExam;
    private Course currentCourse;

    public void setExam(Exam exam, Course course) {
        this.currentExam = exam;
        this.currentCourse = course;

        examTitle.setText(exam.getTitle());
        examType.setText("CRITICAL EVALUATION");
        examStatus.setText(exam.getStatus().toUpperCase());
        examDifficulty.setText(exam.getDifficulty().toUpperCase());
        examDuration.setText(exam.getDuration() + " min");
        examGrade.setText(String.valueOf(exam.getGrade()));
        examDate.setText(exam.getDate().toString());
        courseNameLabel.setText(course.getName());


        targetGradeLabel.setText(String.valueOf(exam.getGrade())); // Assuming target is stored or same for now

        fileNameLabel.setText(exam.getFile().isEmpty() ? "No file" : exam.getFile());
        linkLabel.setText(exam.getLink().isEmpty() ? "No link" : exam.getLink());

        // Update Score Ring
        double gradeValue = exam.getGrade();
        double angle = (gradeValue / 20.0) * 360.0;
        scoreArc.setLength(-angle); // Clockwise from top

        updatePreview(exam);
    }

    private void updatePreview(Exam exam) {
        if (pdfWebView == null || filePlaceholder == null)
            return;
            
        String filePath = exam.getFile();
        String link = exam.getLink();
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
            filePlaceholder.setVisible(false);
        } else {
            pdfWebView.setVisible(false);
            filePlaceholder.setVisible(true);
        }
    }


    @FXML
    public void handleOpenExternally() {
        if (currentExam == null || currentExam.getFile() == null || currentExam.getFile().isEmpty()) return;
        try {
            java.io.File file = new java.io.File(currentExam.getFile());
            if (file.exists()) {
                new ProcessBuilder("cmd", "/c", "start", "", file.getAbsolutePath()).start();
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleTogglePreview() {
        if (previewContentArea != null && togglePreviewBtn != null) {
            if (previewContentArea.isVisible()) {
                previewContentArea.setVisible(false);
                previewContentArea.setManaged(false);
                togglePreviewBtn.setText("Preview ▲");
            } else {
                previewContentArea.setVisible(true);
                previewContentArea.setManaged(true);
                togglePreviewBtn.setText("Preview ▼");
                updatePreview(currentExam);
            }
        }
    }

    @FXML
    public void handleBack() {
        navigateToExamList(examTitle, currentCourse);
    }

    @FXML
    public void handleEditAction() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gestion_examen/frontend_edit_exam.fxml"));
            Parent root = loader.load();
            ExamEditController controller = loader.getController();
            controller.setExam(currentExam, currentCourse);
            if (controllers.FrontendController.getInstance() != null) {
                controllers.FrontendController.getInstance().loadContentNode(root);
            } else {
                Stage stage = (Stage) examTitle.getScene().getWindow();
                stage.getScene().setRoot(root);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleDelete() {
        showSleekDeleteOverlay();
    }

    private void showSleekDeleteOverlay() {
        javafx.scene.layout.StackPane overlay = new javafx.scene.layout.StackPane();
        overlay.setStyle("-fx-background-color: rgba(15, 23, 42, 0.7);");
        overlay.setOpacity(0);
        
        VBox card = new VBox(25);
        card.setAlignment(javafx.geometry.Pos.CENTER);
        card.setPadding(new javafx.geometry.Insets(40));
        card.setMaxSize(340, javafx.scene.layout.Region.USE_PREF_SIZE);
        card.getStyleClass().add("modern-confirm-card");
        
        javafx.scene.shape.SVGPath warnIcon = createIcon("M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z", "#EF4444", 48);
        
        VBox textContent = new VBox(8);
        textContent.setAlignment(javafx.geometry.Pos.CENTER);
        Label title = new Label("Delete Exam?");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #1E293B;");
        Label desc = new Label("This action cannot be undone.\n\"" + currentExam.getTitle() + "\" will be lost.");
        desc.getStyleClass().add("success-message");
        textContent.getChildren().addAll(title, desc);
        
        javafx.scene.layout.HBox buttons = new javafx.scene.layout.HBox(12);
        buttons.setAlignment(javafx.geometry.Pos.CENTER);
        Button cancelBtn = new Button("Keep it");
        cancelBtn.getStyleClass().add("btn-modern-cancel");
        cancelBtn.setOnAction(e -> {
            javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(200), overlay);
            ft.setToValue(0);
            ft.setOnFinished(ev -> ((javafx.scene.layout.StackPane)examTitle.getScene().getRoot()).getChildren().remove(overlay));
            ft.play();
        });
        
        Button confirmBtn = new Button("Delete Exam");
        confirmBtn.getStyleClass().add("btn-modern-delete");
        confirmBtn.setOnAction(e -> {
            try {
                new services.ExamService().supprimer(currentExam.getId());
                ((javafx.scene.layout.StackPane)examTitle.getScene().getRoot()).getChildren().remove(overlay);
                handleBack();
            } catch (java.sql.SQLException ex) {
                ex.printStackTrace();
            }
        });
        
        buttons.getChildren().addAll(cancelBtn, confirmBtn);
        card.getChildren().addAll(warnIcon, textContent, buttons);
        overlay.getChildren().add(card);
        
        ((javafx.scene.layout.StackPane)examTitle.getScene().getRoot()).getChildren().add(overlay);
        
        javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(250), overlay);
        fadeIn.setFromValue(0); fadeIn.setToValue(1); fadeIn.play();
        
        javafx.animation.ScaleTransition scaleUp = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(300), card);
        scaleUp.setFromX(0.85); scaleUp.setFromY(0.85); scaleUp.setToX(1); scaleUp.setToY(1); scaleUp.play();
    }
}
