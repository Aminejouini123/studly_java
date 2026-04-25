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
import java.util.concurrent.CompletableFuture;
import javafx.application.Platform;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;
import models.Course;
import models.Exam;
import models.chat.ChatMessage;
import services.chat.ChatService;
import utils.PdfTextExtractor;
import services.ExamService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.rendering.ImageType;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import services.audio.TtsService;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

public class ExamDetailController extends BaseExamController {

    @FXML
    public Label examTitle, examType, examStatus, examDifficulty, examDuration, examGrade, examDate, courseNameLabel, detailCreatedAt;
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
    @FXML
    private ScrollPane pdfFxScroll;
    @FXML
    private ImageView pdfPageImageView;
    @FXML
    private Button pdfPrevBtn, pdfNextBtn, pdfZoomOutBtn, pdfZoomInBtn, pdfFitWidthBtn, pdfFitPageBtn, pdfFullscreenBtn;
    @FXML
    private Label pdfPageIndicator, pdfZoomLabel, detailFileMeta;
    @FXML
    private Button openFileToolbarBtn, downloadFileToolbarBtn;
    @FXML
    private HBox pdfControlsBox;
    @FXML
    private StackPane pdfCenterPane;

    private Exam currentExam;
    private Course currentCourse;
    private final ExamService examService = new ExamService();

    // Chatbot UI Components
    @FXML private javafx.scene.layout.VBox chatPanel;
    @FXML private ListView<ChatMessage> chatListView;
    @FXML private TextField chatInputField;
    @FXML private Button chatSendButton, chatFabButton;
    @FXML private Label chatStatusLabel;
    @FXML private javafx.scene.layout.StackPane chatOverlay;

    private final ChatService chatService = new ChatService();
    private final ObservableList<ChatMessage> chatMessages = FXCollections.observableArrayList();
    private ChatMessage pendingAiBubble = null;
    private CompletableFuture<?> chatInFlight = null;
    private volatile String cachedExamText = "";
    private volatile boolean examTextLoading = false;

    // PDF Viewer State
    private enum FitMode { NONE, WIDTH, PAGE }
    private PDDocument pdfDoc;
    private PDFRenderer pdfRenderer;
    private String pdfLoadedPath;
    private int pdfPageCount = 0;
    private int pdfCurrentPage = 0;
    private double pdfZoom = 1.0;
    private FitMode pdfFitMode = FitMode.WIDTH;

    private static final float PDF_RENDER_DPI = 144f;
    private static final double PDF_ZOOM_MIN = 0.5;
    private static final double PDF_ZOOM_MAX = 3.0;
    private static final double PDF_ZOOM_STEP = 0.15;

    // Audio / TTS State
    private final TtsService ttsService = new TtsService();
    private MediaPlayer mediaPlayer;
    @FXML private Button ttsBtn;

    public void setExam(Exam exam, Course course) {
        this.currentExam = exam;
        this.currentCourse = course;

        if (exam == null) {
            return;
        }

        if (examTitle != null) examTitle.setText(safeText(exam.getTitle(), "Untitled"));
        if (examType != null) examType.setText("CRITICAL EVALUATION");
        if (examStatus != null) examStatus.setText(safeText(exam.getStatus(), "Pending").toUpperCase());
        if (examDifficulty != null) examDifficulty.setText(safeText(exam.getDifficulty(), "—").toUpperCase());
        if (examDuration != null) examDuration.setText(exam.getDuration() + " min");
        if (examGrade != null) examGrade.setText(String.valueOf(exam.getGrade()));
        if (examDate != null) examDate.setText(exam.getDate() != null ? exam.getDate().toString() : "—");
        if (detailCreatedAt != null) detailCreatedAt.setText(exam.getDate() != null ? exam.getDate().toString() : "Just now");
        if (courseNameLabel != null) courseNameLabel.setText(course != null ? safeText(course.getName(), "—") : "—");

        if (targetGradeLabel != null) targetGradeLabel.setText(String.valueOf(exam.getGrade())); // Assuming target is stored or same for now

        String file = exam.getFile();
        String link = exam.getLink();
        if (fileNameLabel != null) fileNameLabel.setText(file == null || file.isBlank() ? "No file" : fileBasename(file));
        if (linkLabel != null) linkLabel.setText(link == null || link.isBlank() ? "No link" : link);

        // Update file metadata if possible
        if (detailFileMeta != null && file != null && !file.isBlank()) {
            java.io.File f = new java.io.File(file);
            if (f.exists()) {
                String ext = extensionOf(file).toUpperCase();
                detailFileMeta.setText(ext + " · " + formatFileSize(f.length()));
            } else {
                detailFileMeta.setText("Not found");
            }
        }

        // Update Score Ring
        if (scoreArc != null) {
            double gradeValue = exam.getGrade();
            double angle = (gradeValue / 20.0) * 360.0;
            scoreArc.setLength(-angle); // Clockwise from top
        }

        updatePreview(exam);
        initializeChat();
        warmExamTextCache();
    }

    @FXML
    public void initialize() {
        if (pdfFxScroll != null) {
            pdfFxScroll.setPannable(true);
            pdfFxScroll.addEventFilter(ScrollEvent.SCROLL, e -> {
                if (e.isControlDown()) {
                    if (e.getDeltaY() > 0) handlePdfZoomIn();
                    else if (e.getDeltaY() < 0) handlePdfZoomOut();
                    e.consume();
                }
            });
            pdfFxScroll.viewportBoundsProperty().addListener((obs, oldV, newV) -> {
                if (pdfFitMode != FitMode.NONE) Platform.runLater(this::applyFitModeNow);
            });
        }
        updatePdfControls();
    }

    private void warmExamTextCache() {
        if (examTextLoading) return;
        if (currentExam == null) return;
        String file = currentExam.getFile();
        if (file == null || file.isBlank()) return;
        java.io.File f = new java.io.File(file);
        if (!f.exists() || !file.toLowerCase().endsWith(".pdf")) return;
        examTextLoading = true;
        CompletableFuture.supplyAsync(() -> {
            try {
                String txt = PdfTextExtractor.extractText(f, 20_000);
                return txt == null ? "" : txt.trim();
            } catch (Exception e) {
                return "";
            }
        }).thenAccept(txt -> {
            cachedExamText = txt == null ? "" : txt;
            examTextLoading = false;
        });
    }

    private static String safeText(String s, String fallback) {
        if (s == null) return fallback;
        String t = s.trim();
        return t.isEmpty() ? fallback : t;
    }

    private void initializeChat() {
        if (chatListView != null) {
            chatListView.setItems(chatMessages);
            chatListView.setCellFactory(lv -> new ChatBubbleCell());
        }
        ensureChatWelcome();
    }

    private void updatePreview(Exam exam) {
        if (exam == null) return;
        String filePath = exam.getFile();
        boolean hasFile = filePath != null && !filePath.trim().isEmpty();

        if (pdfControlsBox != null) pdfControlsBox.setVisible(hasFile);
        
        if (!hasFile) {
            if (pdfWebView != null) pdfWebView.setVisible(false);
            if (pdfFxScroll != null) pdfFxScroll.setVisible(false);
            if (filePlaceholder != null) {
                filePlaceholder.setVisible(true);
                filePlaceholder.setManaged(true);
            }
            return;
        }

        java.io.File file = new java.io.File(filePath);
        if (!file.exists()) {
            if (pdfWebView != null) pdfWebView.setVisible(false);
            if (pdfFxScroll != null) pdfFxScroll.setVisible(false);
            if (filePlaceholder != null) filePlaceholder.setVisible(true);
            return;
        }

        if (filePlaceholder != null) {
            filePlaceholder.setVisible(false);
            filePlaceholder.setManaged(false);
        }

        String ext = filePath.toLowerCase();
        if (ext.endsWith(".pdf")) {
            long sizeMB = file.length() / (1024 * 1024);
            if (sizeMB > 12) { // Use WebView for very large PDFs as PDFBox might OOM
                if (pdfFxScroll != null) { pdfFxScroll.setVisible(false); pdfFxScroll.setManaged(false); }
                if (pdfWebView != null) {
                    pdfWebView.setVisible(true);
                    pdfWebView.setManaged(true);
                    loadPdfInWebView(file);
                }
                if (pdfControlsBox != null) pdfControlsBox.setVisible(false);
            } else {
                if (pdfWebView != null) { pdfWebView.setVisible(false); pdfWebView.setManaged(false); }
                if (pdfFxScroll != null) {
                    pdfFxScroll.setVisible(true);
                    pdfFxScroll.setManaged(true);
                    openPdf(file);
                }
                if (pdfControlsBox != null) pdfControlsBox.setVisible(true);
            }
        } else if (ext.endsWith(".png") || ext.endsWith(".jpg") || ext.endsWith(".jpeg")) {
            if (pdfFxScroll != null) { pdfFxScroll.setVisible(false); pdfFxScroll.setManaged(false); }
            if (pdfWebView != null) {
                pdfWebView.setVisible(true);
                pdfWebView.setManaged(true);
                pdfWebView.getEngine().load(file.toURI().toString());
            }
            if (pdfControlsBox != null) pdfControlsBox.setVisible(false);
        }
    }

    private void loadPdfInWebView(java.io.File file) {
        try {
            byte[] fileContent = java.nio.file.Files.readAllBytes(file.toPath());
            String base64Pdf = java.util.Base64.getEncoder().encodeToString(fileContent);
            String html = "<!DOCTYPE html><html><head>" +
                "<script src='https://cdnjs.cloudflare.com/ajax/libs/pdf.js/2.16.105/pdf.min.js'></script>" +
                "<style>body{background-color:#0F172A;display:flex;flex-direction:column;align-items:center;gap:15px;padding:20px;margin:0;} canvas{box-shadow:0 10px 15px -3px rgb(0 0 0 / 0.3); max-width: 100%; height: auto; border-radius: 4px;}</style>" +
                "</head><body><script>" +
                "var pdfData = atob('" + base64Pdf + "');" +
                "pdfjsLib.GlobalWorkerOptions.workerSrc = 'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/2.16.105/pdf.worker.min.js';" +
                "pdfjsLib.getDocument({data: pdfData}).promise.then(function(pdf) {" +
                "  for (let i = 1; i <= pdf.numPages; i++) {" +
                "    pdf.getPage(i).then(function(page) {" +
                "      var viewport = page.getViewport({scale: 1.5});" +
                "      var canvas = document.createElement('canvas');" +
                "      canvas.height = viewport.height; canvas.width = viewport.width;" +
                "      document.body.appendChild(canvas);" +
                "      page.render({canvasContext: canvas.getContext('2d'), viewport: viewport});" +
                "    });" +
                "  }" +
                "});" +
                "</script></body></html>";
            pdfWebView.getEngine().loadContent(html);
        } catch (Exception e) { e.printStackTrace(); }
    }

    // --- PDFBox Navigation Logic ---

    private void openPdf(java.io.File file) {
        try {
            if (pdfDoc != null) pdfDoc.close();
            pdfDoc = PDDocument.load(file);
            pdfRenderer = new PDFRenderer(pdfDoc);
            pdfPageCount = pdfDoc.getNumberOfPages();
            pdfCurrentPage = 0;
            pdfZoom = 1.0;
            pdfFitMode = FitMode.WIDTH;
            renderPdfPage();
            updatePdfControls();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void renderPdfPage() {
        if (pdfRenderer == null || pdfDoc == null) return;
        try {
            BufferedImage bim = pdfRenderer.renderImageWithDPI(pdfCurrentPage, PDF_RENDER_DPI, ImageType.RGB);
            pdfPageImageView.setImage(SwingFXUtils.toFXImage(bim, null));
            applyFitModeNow();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void applyFitModeNow() {
        if (pdfPageImageView.getImage() == null || pdfFxScroll == null) return;
        double viewW = pdfFxScroll.getViewportBounds().getWidth() - 44;
        double imgW = pdfPageImageView.getImage().getWidth();
        if (pdfFitMode == FitMode.WIDTH) {
            pdfZoom = viewW / imgW;
        } else if (pdfFitMode == FitMode.PAGE) {
            double viewH = pdfFxScroll.getViewportBounds().getHeight() - 44;
            double imgH = pdfPageImageView.getImage().getHeight();
            pdfZoom = Math.min(viewW / imgW, viewH / imgH);
        }
        pdfPageImageView.setFitWidth(imgW * pdfZoom);
        pdfPageImageView.setFitHeight(pdfPageImageView.getImage().getHeight() * pdfZoom);
        updatePdfControls();
    }

    @FXML public void handlePdfPrevPage() { if (pdfCurrentPage > 0) { pdfCurrentPage--; renderPdfPage(); } }
    @FXML public void handlePdfNextPage() { if (pdfCurrentPage < pdfPageCount - 1) { pdfCurrentPage++; renderPdfPage(); } }
    @FXML public void handlePdfZoomIn() { pdfFitMode = FitMode.NONE; pdfZoom = Math.min(pdfZoom + PDF_ZOOM_STEP, PDF_ZOOM_MAX); applyZoom(); }
    @FXML public void handlePdfZoomOut() { pdfFitMode = FitMode.NONE; pdfZoom = Math.max(pdfZoom - PDF_ZOOM_STEP, PDF_ZOOM_MIN); applyZoom(); }
    @FXML public void handlePdfFitWidth() { pdfFitMode = FitMode.WIDTH; applyFitModeNow(); }
    @FXML public void handlePdfFitPage() { pdfFitMode = FitMode.PAGE; applyFitModeNow(); }
    @FXML
    public void handlePdfFullscreen() {
        if (currentExam == null || currentExam.getFile() == null || currentExam.getFile().isEmpty()) {
            return;
        }
        File file = new File(currentExam.getFile());
        if (!file.exists()) {
            return;
        }
        openPdfInDedicatedFullscreen(file);
    }

    private void openPdfInDedicatedFullscreen(File file) {
        final PDDocument doc;
        final PDFRenderer renderer;
        try {
            doc = PDDocument.load(file);
            renderer = new PDFRenderer(doc);
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }

        final int pageCount = doc.getNumberOfPages();
        if (pageCount <= 0) {
            try { doc.close(); } catch (Exception ignored) {}
            return;
        }

        final javafx.stage.Stage stage = new javafx.stage.Stage(javafx.stage.StageStyle.DECORATED);
        stage.setTitle(fileBasename(file.getAbsolutePath()));

        final ImageView imageView = new ImageView();
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setCache(true);

        final StackPane centerPane = new StackPane();
        centerPane.setAlignment(javafx.geometry.Pos.CENTER);
        centerPane.setStyle("-fx-background-color: #0B1220;");
        StackPane pageCard = new StackPane(imageView);
        pageCard.setStyle("-fx-background-color: #111827; -fx-background-radius: 12; -fx-padding: 12;");
        centerPane.getChildren().add(pageCard);

        final ScrollPane scroll = new ScrollPane(centerPane);
        scroll.setPannable(true);
        scroll.setFitToWidth(false);
        scroll.setFitToHeight(false);
        scroll.setStyle("-fx-background: #0B1220; -fx-background-color: #0B1220;");

        final Label pageLabel = new Label();
        pageLabel.setStyle("-fx-text-fill: #E2E8F0; -fx-font-size: 12px; -fx-font-weight: 800;");
        final Label zoomLabel = new Label();
        zoomLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px; -fx-font-weight: 800;");

        final Button prev = new Button("◀");
        final Button next = new Button("▶");
        final Button zoomOut = new Button("−");
        final Button zoomIn = new Button("+");
        final Button fitW = new Button("Fit W");
        final Button fitP = new Button("Fit P");
        final Button exit = new Button("Exit");

        Button[] allBtns = new Button[]{prev, next, zoomOut, zoomIn, fitW, fitP};
        for (Button b : allBtns) {
            b.setMnemonicParsing(false);
            b.setStyle("-fx-background-color: rgba(255,255,255,0.10); -fx-text-fill: #E2E8F0; -fx-background-radius: 10; -fx-font-weight: 800; -fx-font-size: 12px; -fx-padding: 9 12; -fx-cursor: hand;");
        }
        exit.setMnemonicParsing(false);
        exit.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white; -fx-background-radius: 10; -fx-font-weight: 800; -fx-font-size: 12px; -fx-padding: 9 14; -fx-cursor: hand;");

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        final javafx.scene.layout.HBox toolbar = new javafx.scene.layout.HBox(10, prev, next, pageLabel, spacer, zoomOut, zoomLabel, zoomIn, fitW, fitP, exit);
        toolbar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        toolbar.setPadding(new javafx.geometry.Insets(10, 14, 10, 14));
        toolbar.setStyle("-fx-background-color: rgba(15,23,42,0.85); -fx-border-color: rgba(148,163,184,0.18); -fx-border-width: 0 0 1 0;");

        final javafx.scene.layout.BorderPane root = new javafx.scene.layout.BorderPane(scroll);
        root.setTop(toolbar);
        root.setStyle("-fx-background-color: #0B1220;");

        final javafx.scene.Scene scene = new javafx.scene.Scene(root, 1200, 800);
        stage.setScene(scene);

        final class State {
            int page = 0;
            double zoom = 1.0;
            FitMode fit = FitMode.WIDTH;
        }
        final State s = new State();

        Runnable updateControls = () -> {
            prev.setDisable(s.page <= 0);
            next.setDisable(s.page >= pageCount - 1);
            pageLabel.setText((s.page + 1) + " / " + pageCount);
            zoomLabel.setText((int) Math.round(s.zoom * 100) + "%");
        };

        Runnable applyFit = () -> {
            javafx.scene.image.Image img = imageView.getImage();
            if (img == null) return;
            double vw = scroll.getViewportBounds().getWidth();
            double vh = scroll.getViewportBounds().getHeight();
            if (vw <= 40 || vh <= 40) return;
            centerPane.setMinWidth(vw);
            centerPane.setMinHeight(vh);
            double usableW = Math.max(200, vw - 80);
            double usableH = Math.max(200, vh - 80);
            double z = (s.fit == FitMode.PAGE) ? Math.min(usableW / img.getWidth(), usableH / img.getHeight()) : (usableW / img.getWidth());
            s.zoom = Math.max(PDF_ZOOM_MIN, Math.min(PDF_ZOOM_MAX, z));
            imageView.setScaleX(s.zoom);
            imageView.setScaleY(s.zoom);
            updateControls.run();
            scroll.setHvalue(0.5);
            scroll.setVvalue(0.0);
        };

        Runnable render = () -> {
            try {
                BufferedImage bim = renderer.renderImageWithDPI(s.page, PDF_RENDER_DPI, ImageType.RGB);
                javafx.scene.image.Image fx = SwingFXUtils.toFXImage(bim, null);
                imageView.setImage(fx);
                imageView.setScaleX(s.zoom);
                imageView.setScaleY(s.zoom);
                updateControls.run();
                Platform.runLater(applyFit);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        };

        scroll.viewportBoundsProperty().addListener((o, a, b) -> {
            if (s.fit != FitMode.NONE) {
                Platform.runLater(applyFit);
            }
        });
        scroll.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, e -> {
            if (e.isControlDown()) {
                s.fit = FitMode.NONE;
                if (e.getDeltaY() > 0) s.zoom = Math.min(PDF_ZOOM_MAX, s.zoom + PDF_ZOOM_STEP);
                else if (e.getDeltaY() < 0) s.zoom = Math.max(PDF_ZOOM_MIN, s.zoom - PDF_ZOOM_STEP);
                imageView.setScaleX(s.zoom);
                imageView.setScaleY(s.zoom);
                updateControls.run();
                e.consume();
            }
        });

        prev.setOnAction(e -> { s.page = Math.max(0, s.page - 1); render.run(); });
        next.setOnAction(e -> { s.page = Math.min(pageCount - 1, s.page + 1); render.run(); });
        zoomIn.setOnAction(e -> { s.fit = FitMode.NONE; s.zoom = Math.min(PDF_ZOOM_MAX, s.zoom + PDF_ZOOM_STEP); imageView.setScaleX(s.zoom); imageView.setScaleY(s.zoom); updateControls.run(); });
        zoomOut.setOnAction(e -> { s.fit = FitMode.NONE; s.zoom = Math.max(PDF_ZOOM_MIN, s.zoom - PDF_ZOOM_STEP); imageView.setScaleX(s.zoom); imageView.setScaleY(s.zoom); updateControls.run(); });
        fitW.setOnAction(e -> { s.fit = FitMode.WIDTH; Platform.runLater(applyFit); });
        fitP.setOnAction(e -> { s.fit = FitMode.PAGE; Platform.runLater(applyFit); });
        exit.setOnAction(e -> stage.close());

        stage.setOnCloseRequest(e -> {
            try { doc.close(); } catch (Exception ignored) {}
        });

        stage.setFullScreenExitHint("");
        stage.setFullScreen(true);
        stage.show();
        render.run();
    }

    @FXML
    public void handleReadAloud() {
        if (currentExam == null || currentExam.getFile() == null) return;
        java.io.File file = new java.io.File(currentExam.getFile());
        if (!file.exists()) return;

        if (mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            mediaPlayer.stop();
            if (ttsBtn != null) ttsBtn.setText("🔊");
            return;
        }

        if (ttsBtn != null) {
            ttsBtn.setDisable(true);
            ttsBtn.setText("⌛");
        }

        CompletableFuture.runAsync(() -> {
            try {
                // 1. Extract text if not cached
                String textToRead = cachedExamText;
                if (textToRead == null || textToRead.isBlank()) {
                    textToRead = PdfTextExtractor.extractText(file, 5000); // Read first 5k chars
                }
                
                if (textToRead == null || textToRead.isBlank()) {
                    throw new Exception("Could not extract text from PDF.");
                }

                // 2. Call VoiceRSS
                // Detect language or default to French/English
                String lang = detectLanguage(textToRead);
                java.io.File audioFile = ttsService.speak(textToRead, lang);

                // 3. Play audio
                Platform.runLater(() -> {
                    try {
                        Media hit = new Media(audioFile.toURI().toString());
                        mediaPlayer = new MediaPlayer(hit);
                        mediaPlayer.setOnEndOfMedia(() -> {
                            ttsBtn.setText("🔊");
                            ttsBtn.setDisable(false);
                        });
                        mediaPlayer.setOnError(() -> {
                            ttsBtn.setText("🔊");
                            ttsBtn.setDisable(false);
                        });
                        mediaPlayer.play();
                        if (ttsBtn != null) {
                            ttsBtn.setText("⏹");
                            ttsBtn.setDisable(false);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (ttsBtn != null) {
                            ttsBtn.setText("🔊");
                            ttsBtn.setDisable(false);
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    if (ttsBtn != null) {
                        ttsBtn.setText("🔊");
                        ttsBtn.setDisable(false);
                    }
                });
            }
        });
    }

    private String detectLanguage(String text) {
        // Very simple detection logic: check for common French words
        String lower = text.toLowerCase();
        if (lower.contains(" le ") || lower.contains(" la ") || lower.contains(" et ") || lower.contains(" est ")) {
            return "fr-fr";
        }
        return "en-us";
    }

    private void applyZoom() {
        if (pdfPageImageView.getImage() == null) return;
        pdfPageImageView.setFitWidth(pdfPageImageView.getImage().getWidth() * pdfZoom);
        pdfPageImageView.setFitHeight(pdfPageImageView.getImage().getHeight() * pdfZoom);
        updatePdfControls();
    }

    private void updatePdfControls() {
        if (pdfPageIndicator != null) pdfPageIndicator.setText(String.format("Page %d of %d", pdfCurrentPage + 1, pdfPageCount));
        if (pdfZoomLabel != null) pdfZoomLabel.setText(String.format("%d%%", (int)(pdfZoom * 100)));
        if (pdfPrevBtn != null) pdfPrevBtn.setDisable(pdfCurrentPage <= 0);
        if (pdfNextBtn != null) pdfNextBtn.setDisable(pdfCurrentPage >= pdfPageCount - 1);
    }

    @FXML
    public void handleDownloadFile() {
        if (currentExam == null || currentExam.getFile() == null) return;
        java.io.File src = new java.io.File(currentExam.getFile());
        if (!src.exists()) return;
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setInitialFileName(src.getName());
        java.io.File dest = fc.showSaveDialog(rootPane.getScene().getWindow());
        if (dest != null) {
            try { java.nio.file.Files.copy(src.toPath(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING); }
            catch (Exception e) { e.printStackTrace(); }
        }
    }

    private static String fileBasename(String path) {
        if (path == null) return "";
        int idx = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return idx >= 0 ? path.substring(idx + 1) : path;
    }

    private static String extensionOf(String path) {
        if (path == null) return "";
        int idx = path.lastIndexOf('.');
        return idx >= 0 ? path.substring(idx + 1) : "";
    }

    private static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        return String.format("%.1f %cB", bytes / Math.pow(1024, exp), "KMGTPE".charAt(exp - 1));
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
            boolean visible = !previewContentArea.isVisible();
            previewContentArea.setVisible(visible);
            previewContentArea.setManaged(visible);
            
            if (pdfControlsBox != null) {
                // Only show controls if the preview is visible AND we are using PDFBox (pdfFxScroll is visible)
                boolean usingPdfBox = pdfFxScroll != null && pdfFxScroll.isVisible();
                pdfControlsBox.setVisible(visible && usingPdfBox);
                pdfControlsBox.setManaged(visible && usingPdfBox);
            }
            
            if (visible) {
                togglePreviewBtn.setText("Hide preview");
                updatePreview(currentExam);
            } else {
                togglePreviewBtn.setText("Show preview");
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

    // Handlers referenced by frontend_exam_detail.fxml delete modal
    @FXML
    public void closeDeleteModal() {
        if (deleteModalOverlay == null) return;
        deleteModalOverlay.setVisible(false);
        deleteModalOverlay.setManaged(false);
    }

    @FXML
    public void confirmDelete() {
        if (currentExam == null) return;
        try {
            examService.supprimer(currentExam.getId());
            closeDeleteModal();
            handleBack();
        } catch (SQLException ex) {
            ex.printStackTrace();
            if (rootPane != null) {
                showErrorNotification(rootPane, "Delete failed", ex.getMessage() != null ? ex.getMessage() : ex.toString());
            }
        }
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

    // --- AI CHATBOT LOGIC ---

    @FXML
    public void toggleChatPanel() {
        if (chatPanel == null) return;
        boolean show = !chatPanel.isVisible();
        if (show) openChatAnimated();
        else closeChatAnimated();
        if (show) {
            ensureChatWelcome();
            if (chatInputField != null) Platform.runLater(() -> chatInputField.requestFocus());
        }
    }

    @FXML
    public void minimizeChat() { closeChatAnimated(); }
    @FXML
    public void closeChat() { closeChatAnimated(); }

    @FXML
    public void handleSendChat() {
        if (chatInputField == null || chatListView == null) return;
        if (!chatService.hasApiKeyConfigured()) {
            chatMessages.add(new ChatMessage(ChatMessage.Role.AI, "AI Tutor is not configured. Add your API key."));
            return;
        }
        String msg = chatInputField.getText() == null ? "" : chatInputField.getText().trim();
        if (msg.isEmpty()) return;
        chatInputField.clear();

        chatMessages.add(new ChatMessage(ChatMessage.Role.USER, msg));
        scrollChatToBottom();

        if (!isEducationRelated(msg)) {
            chatMessages.add(new ChatMessage(
                    ChatMessage.Role.AI,
                    "Je peux répondre uniquement aux questions **éducatives** liées au cours/examen (explication, correction, méthode, concepts). " +
                    "Pose-moi une question sur une notion, un exercice, ou une question de l’examen."
            ));
            scrollChatToBottom();
            return;
        }

        setChatBusy(true, "Thinking...");

        String systemPrompt = buildExamSystemPrompt();
        pendingAiBubble = new ChatMessage(ChatMessage.Role.AI, "…");
        chatMessages.add(pendingAiBubble);
        scrollChatToBottom();

        String userPrompt = buildExamUserPrompt(msg);
        chatInFlight = CompletableFuture.supplyAsync(() -> {
            try { return chatService.chat(systemPrompt, userPrompt); }
            catch (Exception e) { return "Error: " + e.getMessage(); }
        }).thenAccept(answer -> Platform.runLater(() -> {
            replacePendingAi(answer);
            setChatBusy(false, "");
        }));
    }

    private String buildExamUserPrompt(String userMessage) {
        String context = cachedExamText == null ? "" : cachedExamText.trim();
        if (!context.isEmpty()) {
            return "Student question:\n" + userMessage + "\n\n" +
                   "EXAM TEXT (reference only, may be truncated):\n" + context;
        }
        return userMessage;
    }

    @FXML
    public void handleExplainExam() {
        if (currentExam == null || currentExam.getFile() == null || currentExam.getFile().isEmpty()) {
            chatMessages.add(new ChatMessage(ChatMessage.Role.AI, "No exam paper attached to analyze."));
            return;
        }
        java.io.File f = new java.io.File(currentExam.getFile());
        if (!f.exists()) {
            chatMessages.add(new ChatMessage(ChatMessage.Role.AI, "Exam file not found on this device."));
            return;
        }

        setChatBusy(true, "Analyzing Paper...");
        ChatMessage pending = new ChatMessage(ChatMessage.Role.AI, "Analyzing your exam paper…");
        chatMessages.add(pending);
        pendingAiBubble = pending;
        scrollChatToBottom();

        String systemPrompt = buildExamSystemPrompt();
        CompletableFuture.supplyAsync(() -> {
            try {
                String text = PdfTextExtractor.extractText(f, 35_000);
                if (text.isBlank()) return "I couldn't read the exam text. Is it a scanned image?";
                String prompt = "Act as an Expert Tutor. Analyze this exam paper and provide a professional explanation.\n\n" +
                               "1) **Key Topics**: What are the main areas tested?\n" +
                               "2) **Difficulty Analysis**: How complex is this paper?\n" +
                               "3) **Strategic Approach**: How should I solve this efficiently?\n" +
                               "4) **Sample Explanations**: Briefly explain 2 key questions from the text.\n\n" +
                               "EXAM TEXT:\n" + text;
                return chatService.chat(systemPrompt, prompt);
            } catch (Exception e) { return "Error: " + e.getMessage(); }
        }).thenAccept(answer -> Platform.runLater(() -> {
            replacePendingAi(answer);
            setChatBusy(false, "");
        }));
    }

    private String buildExamSystemPrompt() {
        String examTitleStr = currentExam == null ? "—" : safeText(currentExam.getTitle(), "—");
        String courseNameStr = currentCourse == null ? "—" : safeText(currentCourse.getName(), "—");
        return "You are Studly Exam AI Tutor, a strict education-only assistant.\n" +
               "Allowed topics: education, course concepts, exam questions, corrections, step-by-step solutions, study strategy.\n" +
               "Disallowed topics: politics, adult content, hacking, personal/medical/legal advice, general chit-chat not related to studying.\n" +
               "If the user asks something outside education/exam scope, politely refuse and ask them to rephrase as a study/exam question.\n" +
               "\n" +
               "Mission: Help students understand exam questions, provide step-by-step solutions, and explain core concepts.\n" +
               "When correcting: show method, key steps, and final answer. If missing info, ask 1-2 clarifying questions.\n" +
               "If you use the provided exam text, quote small relevant snippets (no long copy).\n" +
               "\n" +
               "Context: Exam title: " + examTitleStr + ", Course: " + courseNameStr + ".\n" +
               "Tone: Formal, pedagogical, and encouraging. Use **bold** for key terms and *italics* for formulas or definitions.\n" +
               "Language: Reply in the same language as the student.";
    }

    private static boolean isEducationRelated(String msg) {
        if (msg == null) return false;
        String m = msg.toLowerCase();
        // quick allowlist of typical study/exam intents (FR/EN)
        String[] ok = {
                "examen", "exam", "quiz", "course", "cours", "leçon", "lesson",
                "corrig", "correction", "solution", "résoudre", "solve",
                "expliquer", "explain", "méthode", "method",
                "question", "exercise", "exercice",
                "java", "algorithm", "algorithme", "math", "sql", "oop", "poo"
        };
        for (String k : ok) {
            if (m.contains(k)) return true;
        }
        // also allow if message looks like a real problem statement (has numbers/operators)
        return m.matches(".*[0-9=+\\-*/^].*");
    }

    private void ensureChatWelcome() {
        if (chatMessages.isEmpty()) {
            chatMessages.add(new ChatMessage(ChatMessage.Role.AI, "Hello! I am your Exam AI Tutor. Need help solving a question or explaining a concept from this paper?"));
            scrollChatToBottom();
        }
    }

    private void replacePendingAi(String answer) {
        int idx = pendingAiBubble == null ? -1 : chatMessages.indexOf(pendingAiBubble);
        if (idx >= 0) chatMessages.set(idx, new ChatMessage(ChatMessage.Role.AI, answer));
        else chatMessages.add(new ChatMessage(ChatMessage.Role.AI, answer));
        pendingAiBubble = null;
        scrollChatToBottom();
    }

    private void setChatBusy(boolean busy, String status) {
        if (chatSendButton != null) chatSendButton.setDisable(busy);
        if (chatInputField != null) chatInputField.setDisable(busy);
        if (chatStatusLabel != null) chatStatusLabel.setText(busy ? status : "");
    }

    private void scrollChatToBottom() {
        if (chatListView != null && !chatMessages.isEmpty()) {
            Platform.runLater(() -> chatListView.scrollTo(chatMessages.size() - 1));
        }
    }

    private void openChatAnimated() {
        if (chatPanel == null) return;
        chatPanel.setManaged(true); chatPanel.setVisible(true);
        FadeTransition ft = new FadeTransition(Duration.millis(200), chatPanel);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    private void closeChatAnimated() {
        if (chatPanel == null) return;
        FadeTransition ft = new FadeTransition(Duration.millis(150), chatPanel);
        ft.setFromValue(1); ft.setToValue(0);
        ft.setOnFinished(e -> { chatPanel.setVisible(false); chatPanel.setManaged(false); });
        ft.play();
    }

    private final class ChatBubbleCell extends ListCell<ChatMessage> {
        @Override
        protected void updateItem(ChatMessage msg, boolean empty) {
            super.updateItem(msg, empty);
            if (empty || msg == null) { setGraphic(null); return; }
            TextFlow flow = new TextFlow();
            flow.setMaxWidth(260);
            parseMarkdownToFlow(msg.getContent(), flow, msg.getRole() == ChatMessage.Role.USER);
            VBox bubble = new VBox(flow);
            bubble.getStyleClass().add(msg.getRole() == ChatMessage.Role.USER ? "studly-bubble-user" : "studly-bubble-ai");
            HBox row = new HBox(bubble);
            row.setAlignment(msg.getRole() == ChatMessage.Role.USER ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            setGraphic(row);
        }

        private void parseMarkdownToFlow(String raw, TextFlow flow, boolean isUser) {
            if (raw == null) return;
            String[] lines = raw.replace("\r", "").split("\n");
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                if (line.trim().startsWith("* ") || line.trim().startsWith("- ")) {
                    Text b = new Text("• "); b.getStyleClass().add(isUser ? "studly-bubble-text-user" : "studly-bubble-text");
                    b.setStyle("-fx-font-weight: bold;"); flow.getChildren().add(b);
                    line = line.trim().substring(2);
                }
                processLineStyles(line, flow, isUser);
                if (i < lines.length - 1) flow.getChildren().add(new Text("\n"));
            }
        }

        private void processLineStyles(String line, TextFlow flow, boolean isUser) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("(\\*\\*.*?\\*\\*|\\*.*?\\*)");
            java.util.regex.Matcher m = p.matcher(line);
            int last = 0;
            while (m.find()) {
                if (m.start() > last) add(line.substring(last, m.start()), flow, isUser, false, false);
                String mt = m.group();
                if (mt.startsWith("**")) add(mt.substring(2, mt.length()-2), flow, isUser, true, false);
                else add(mt.substring(1, mt.length()-1), flow, isUser, false, true);
                last = m.end();
            }
            if (last < line.length()) add(line.substring(last), flow, isUser, false, false);
        }

        private void add(String s, TextFlow f, boolean u, boolean b, boolean it) {
            Text t = new Text(s); t.getStyleClass().add(u ? "studly-bubble-text-user" : "studly-bubble-text");
            if (b) t.setStyle("-fx-font-weight: 900;"); if (it) t.setStyle("-fx-font-style: italic;");
            f.getChildren().add(t);
        }
    }
}
