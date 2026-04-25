package controllers.courses;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Arc;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.util.Duration;
import javafx.scene.input.ScrollEvent;
import javafx.scene.web.WebView;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.Node;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import services.audio.TtsService;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.FileChooser;
import models.Course;
import models.chat.ChatMessage;
import services.CourseService;
import services.chat.ChatService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import utils.PdfTextExtractor;
import models.Activity;
import services.ActivityService;
import java.util.List;

import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.concurrent.CompletableFuture;

public class CourseDetailController extends BaseCourseController {

    @FXML
    private StackPane rootPane;
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
    private ScrollPane pdfFxScroll;
    @FXML
    private StackPane pdfCenterPane;
    @FXML
    private ImageView pdfPageImageView;
    @FXML
    private Button pdfPrevBtn;
    @FXML
    private Button pdfNextBtn;
    @FXML
    private Label pdfPageIndicator;
    @FXML
    private Button pdfZoomOutBtn;
    @FXML
    private Button pdfZoomInBtn;
    @FXML
    private Label pdfZoomLabel;
    @FXML
    private Button pdfFitWidthBtn;
    @FXML
    private Button pdfFitPageBtn;
    @FXML
    private Button pdfFullscreenBtn;
    @FXML
    private HBox pdfControlsBox;
    @FXML
    private Button ttsBtn;
    @FXML
    private VBox previewContentArea;
    @FXML
    private Button togglePreviewBtn;
    @FXML
    private Button openFileToolbarBtn;
    @FXML
    private Button downloadFileToolbarBtn;
    @FXML
    private VBox filePlaceholderLabel;
    @FXML
    private VBox filePreviewBox;
    @FXML
    private StackPane fileTypeBadge;
    @FXML
    private Label fileTypeBadgeLabel;
    @FXML
    private Label detailFileMeta;
    @FXML
    private Label placeholderTitleLabel;
    @FXML
    private Label placeholderSubtitleLabel;
    @FXML
    private Button placeholderOpenBtn;

    @FXML
    private VBox quizListContainer;
    @FXML
    private Label noQuizLabel;

    private Course displayedCourse;
    private final ActivityService activityService = new ActivityService();

    // ---- AI chat ----
    @FXML
    private ListView<ChatMessage> chatListView;
    @FXML
    private javafx.scene.control.TextField chatInputField;
    @FXML
    private Button chatSendButton;
    @FXML
    private Label chatStatusLabel;
    @FXML
    private VBox chatPanel;
    @FXML
    private Button chatFabButton;
    @FXML
    private StackPane chatOverlay;

    private final ObservableList<ChatMessage> chatMessages = FXCollections.observableArrayList();
    private boolean chatFullscreen = false;
    private ChatMessage pendingAiBubble;

    private final ChatService chatService = new ChatService();
    private volatile CompletableFuture<?> chatInFlight;

    // ---- PDF viewer state (single-page viewer) ----
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

    // TTS Support
    private final TtsService ttsService = new TtsService();
    private MediaPlayer mediaPlayer;

    @FXML
    public void handleBackToCoursesAction(javafx.event.ActionEvent event) {
        if (fromBackend && backendController != null) {
            backendController.restoreDashboard();
        } else if (fromBackend) {
            loadScene("/TEMPLATE/backend_courses.fxml", null, (javafx.scene.Node) event.getSource());
        } else {
            navigateToFrontendCourseList((javafx.scene.Node) event.getSource());
        }
    }

    @FXML
    public void populateCourseDetails(Course course) {
        this.displayedCourse = course;
        if (course == null) {
            return;
        }
        loadQuizzes();

        if (detailTitle != null) {
            detailTitle.setText(course.getName() != null ? course.getName() : "—");
        }
        if (detailType != null) {
            detailType.setText(displayUpper(course.getType()));
        }
        if (detailStatus != null) {
            detailStatus.setText(displayUpper(course.getStatus()));
        }
        if (detailPriority != null) {
            detailPriority.setText(displayUpper(course.getPriority()));
        }
        if (detailDifficulty != null) {
            detailDifficulty.setText(displayUpper(course.getDifficulty_level()));
        }
        if (detailDuration != null) {
            detailDuration.setText(course.getDuration() + " hours");
        }
        if (detailSemester != null) {
            String sem = course.getSemester();
            if (sem == null || sem.isEmpty()) {
                detailSemester.setText("—");
            } else if (sem.toLowerCase().startsWith("semester")) {
                detailSemester.setText(sem);
            } else {
                detailSemester.setText("Semester " + sem);
            }
        }
        if (detailCoefficient != null) {
            detailCoefficient.setText(String.valueOf((int) course.getCoefficient()));
        }

        if (coefficientArc != null) {
            double targetAngle = (course.getCoefficient() / 10.0) * 360.0;
            coefficientArc.setLength(-Math.min(targetAngle, 360));
        }

        if (resourceLinkLabel != null) {
            String link = course.getCourse_link();
            resourceLinkLabel.setText(link == null || link.isEmpty() ? "No link available" : link);
        }
        String courseFilePath = course.getCourse_file();
        boolean hasCourseFile = courseFilePath != null && !courseFilePath.trim().isEmpty();
        if (filePreviewBox != null) {
            filePreviewBox.setVisible(hasCourseFile);
            filePreviewBox.setManaged(hasCourseFile);
        }
        if (hasCourseFile) {
            File attachment = resolveCourseFile(courseFilePath);
            String displayName = fileBasename(courseFilePath);
            if (detailFileName != null) {
                detailFileName.setText(displayName.isEmpty() ? "Attached file" : displayName);
            }
            applyFileTypeBadge(courseFilePath);
            if (detailFileMeta != null) {
                if (attachment == null || !attachment.exists()) {
                    detailFileMeta.setText("Not found on this device");
                } else {
                    String kind = humanReadableKind(extensionOf(courseFilePath));
                    detailFileMeta.setText(kind + " · " + formatFileSize(attachment.length()));
                }
            }
            boolean openOk = attachment != null && attachment.exists();
            if (openFileToolbarBtn != null) {
                openFileToolbarBtn.setDisable(!openOk);
            }
            if (downloadFileToolbarBtn != null) {
                downloadFileToolbarBtn.setDisable(!openOk);
            }
            if (placeholderOpenBtn != null) {
                placeholderOpenBtn.setDisable(!openOk);
            }
        }
        if (detailComment != null) {
            String comment = course.getComment();
            detailComment.setText(comment == null || comment.isEmpty() ? "No additional notes provided." : comment);
        }
        if (detailTeacher != null) {
            detailTeacher.setText(course.getTeacher_email() != null ? course.getTeacher_email() : "—");
        }
        if (detailCreatedAt != null) {
            if (course.getCreated_at() != null) {
                detailCreatedAt.setText(new SimpleDateFormat("dd MMM yyyy").format(course.getCreated_at()));
            } else {
                detailCreatedAt.setText("—");
            }
        }

        updateTeacherInitials(course.getTeacher_email());

        try {
            updateFilePreview(course);
        } catch (RuntimeException ex) {
            ex.printStackTrace();
        }

        ensureChatWelcome();
    }

    @FXML
    public void initialize() {
        // Smooth-ish trackpad scrolling + Ctrl+wheel zoom
        if (pdfFxScroll != null) {
            pdfFxScroll.setPannable(true);
            pdfFxScroll.setHvalue(0);
            pdfFxScroll.setVvalue(0);
            pdfFxScroll.addEventFilter(ScrollEvent.SCROLL, e -> {
                if (e.isControlDown()) {
                    if (e.getDeltaY() > 0) {
                        handlePdfZoomIn();
                    } else if (e.getDeltaY() < 0) {
                        handlePdfZoomOut();
                    }
                    e.consume();
                }
            });

            pdfFxScroll.viewportBoundsProperty().addListener((obs, oldV, newV) -> {
                if (pdfFitMode != FitMode.NONE) {
                    Platform.runLater(this::applyFitModeNow);
                }
            });
        }

        if (pdfPageImageView != null) {
            pdfPageImageView.setPreserveRatio(true);
            pdfPageImageView.setSmooth(true);
            pdfPageImageView.setCache(true);
        }
        updatePdfControls();

        if (chatListView != null) {
            chatListView.setItems(chatMessages);
            chatListView.setFocusTraversable(false);
            chatListView.setCellFactory(lv -> new ChatBubbleCell());
        }
        if (chatStatusLabel != null) {
            chatStatusLabel.setText("");
        }
        if (chatSendButton != null) {
            chatSendButton.setDisable(false);
        }
        if (chatPanel != null) {
            chatPanel.setVisible(false);
            chatPanel.setManaged(false);
        }
        ensureChatWelcome();
    }

    @FXML
    public void toggleChatPanel() {
        if (chatPanel == null) {
            return;
        }
        boolean show = !chatPanel.isVisible();
        if (show) {
            openChatAnimated();
        } else {
            closeChatAnimated();
        }
        if (show) {
            ensureChatWelcome();
            if (chatInputField != null) {
                Platform.runLater(() -> chatInputField.requestFocus());
            }
        }
    }

    @FXML
    public void minimizeChat() {
        if (chatPanel == null || !chatPanel.isVisible()) return;
        closeChatAnimated();
    }

    @FXML
    public void closeChat() {
        if (chatPanel == null) return;
        closeChatAnimated();
    }

    @FXML
    public void toggleChatFullscreen() {
        if (chatPanel == null) return;
        chatFullscreen = !chatFullscreen;
        applyChatSizeMode();
    }

    @FXML
    public void handleSendChat() {
        if (chatInputField == null || chatListView == null) {
            return;
        }
        if (!chatService.hasApiKeyConfigured()) {
            chatMessages.add(new ChatMessage(
                    ChatMessage.Role.AI,
                    "Studly AI is not configured yet.\n" +
                    "Add your OpenRouter key in: src/main/resources/openrouter.properties\n" +
                    "Example: openrouter.apiKey=sk-or-..."
            ));
            scrollChatToBottom();
            return;
        }
        String msg = chatInputField.getText() == null ? "" : chatInputField.getText().trim();
        if (msg.isEmpty()) {
            return;
        }
        chatInputField.clear();

        chatMessages.add(new ChatMessage(ChatMessage.Role.USER, msg));
        scrollChatToBottom();
        setChatBusy(true, "Thinking...");

        String systemPrompt = buildSystemPrompt(displayedCourse);
        pendingAiBubble = new ChatMessage(ChatMessage.Role.AI, "…");
        chatMessages.add(pendingAiBubble);
        scrollChatToBottom();

        CompletableFuture<?> fut = CompletableFuture.supplyAsync(() -> {
            try {
                return chatService.chat(systemPrompt, msg);
            } catch (Exception e) {
                return "Error: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            }
        }).thenAccept(answer -> Platform.runLater(() -> {
            replacePendingAi(answer);
            setChatBusy(false, "");
        }));

        chatInFlight = fut;
    }

    private void ensureChatWelcome() {
        if (chatMessages.isEmpty()) {
            String title = displayedCourse != null && displayedCourse.getName() != null ? displayedCourse.getName().trim() : "";
            String welcome = title.isEmpty()
                    ? "Welcome! I’m Studly AI. Ask me anything about this course."
                    : "Welcome! I’m Studly AI. Ask me anything about \"" + title + "\".";
            chatMessages.add(new ChatMessage(ChatMessage.Role.AI, welcome));
            scrollChatToBottom();
        }
    }

    private void replacePendingAi(String answer) {
        String safe = answer == null ? "" : answer.trim();
        if (safe.isEmpty()) safe = "I didn’t get a response. Please try again.";
        int idx = pendingAiBubble == null ? -1 : chatMessages.indexOf(pendingAiBubble);
        if (idx >= 0) {
            chatMessages.set(idx, new ChatMessage(ChatMessage.Role.AI, safe));
        } else {
            chatMessages.add(new ChatMessage(ChatMessage.Role.AI, safe));
        }
        pendingAiBubble = null;
        scrollChatToBottom();
    }

    private void setChatBusy(boolean busy, String status) {
        if (chatSendButton != null) {
            chatSendButton.setDisable(busy);
        }
        if (chatInputField != null) {
            chatInputField.setDisable(busy);
        }
        if (chatStatusLabel != null) {
            chatStatusLabel.setText(status == null ? "" : status);
        }
        if (chatFabButton != null) {
            chatFabButton.setDisable(busy);
            chatFabButton.setOpacity(busy ? 0.85 : 1.0);
        }
    }

    private void scrollChatToBottom() {
        if (chatListView == null) return;
        int size = chatMessages.size();
        if (size <= 0) return;
        Platform.runLater(() -> chatListView.scrollTo(size - 1));
    }

    private void openChatAnimated() {
        if (chatPanel == null) return;
        applyChatSizeMode();
        chatPanel.setManaged(true);
        chatPanel.setVisible(true);
        chatPanel.setOpacity(0);
        chatPanel.setTranslateY(16);

        FadeTransition fade = new FadeTransition(Duration.millis(170), chatPanel);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(Duration.millis(170), chatPanel);
        slide.setFromY(16);
        slide.setToY(0);

        fade.play();
        slide.play();
    }

    private void closeChatAnimated() {
        if (chatPanel == null) return;
        FadeTransition fade = new FadeTransition(Duration.millis(140), chatPanel);
        fade.setFromValue(chatPanel.getOpacity());
        fade.setToValue(0);

        TranslateTransition slide = new TranslateTransition(Duration.millis(140), chatPanel);
        slide.setFromY(chatPanel.getTranslateY());
        slide.setToY(16);

        fade.setOnFinished(e -> {
            chatPanel.setVisible(false);
            chatPanel.setManaged(false);
            chatPanel.setOpacity(1);
            chatPanel.setTranslateY(0);
        });

        fade.play();
        slide.play();
    }

    private void applyChatSizeMode() {
        if (chatPanel == null) return;
        if (chatOverlay != null) {
            StackPane.setAlignment(chatPanel, chatFullscreen ? Pos.CENTER : Pos.BOTTOM_RIGHT);
            StackPane.setAlignment(chatFabButton, Pos.BOTTOM_RIGHT);
        }
        if (chatFullscreen) {
            chatPanel.setPrefWidth(720);
            chatPanel.setPrefHeight(720);
            chatPanel.setMaxWidth(900);
            chatPanel.setMaxHeight(900);
        } else {
            chatPanel.setPrefWidth(380);
            chatPanel.setPrefHeight(520);
            chatPanel.setMaxWidth(420);
            chatPanel.setMaxHeight(620);
        }
    }

    private final class ChatBubbleCell extends ListCell<ChatMessage> {
        @Override
        protected void updateItem(ChatMessage msg, boolean empty) {
            super.updateItem(msg, empty);
            if (empty || msg == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            TextFlow flow = new TextFlow();
            double max = 320;
            if (getListView() != null) {
                max = Math.max(220, getListView().getWidth() * 0.78);
            }
            flow.setMaxWidth(max);
            
            // Professional Parsing (Bold, Italic, Bullets)
            parseMarkdownToFlow(msg.getContent(), flow, msg.getRole() == ChatMessage.Role.USER);

            VBox bubble = new VBox(flow);
            bubble.getStyleClass().add(msg.getRole() == ChatMessage.Role.USER ? "studly-bubble-user" : "studly-bubble-ai");

            HBox row = new HBox(bubble);
            row.setAlignment(msg.getRole() == ChatMessage.Role.USER ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            HBox.setHgrow(bubble, Priority.NEVER);
            setGraphic(row);
        }

        private void parseMarkdownToFlow(String raw, TextFlow flow, boolean isUser) {
            if (raw == null) return;
            String text = raw.replace("\r\n", "\n").trim();
            
            // Simple regex based parsing for **bold**, *italic*, and bullets
            String[] lines = text.split("\n");
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                if (line.trim().startsWith("* ") || line.trim().startsWith("- ")) {
                    Text bullet = new Text("• ");
                    bullet.getStyleClass().add(isUser ? "studly-bubble-text-user" : "studly-bubble-text");
                    bullet.setStyle("-fx-font-weight: bold;");
                    flow.getChildren().add(bullet);
                    line = line.trim().substring(2);
                }
                
                processLineWithStyles(line, flow, isUser);
                
                if (i < lines.length - 1) {
                    flow.getChildren().add(new Text("\n"));
                }
            }
        }

        private void processLineWithStyles(String line, TextFlow flow, boolean isUser) {
            // Regex for **bold** and *italic*
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("(\\*\\*.*?\\*\\*|\\*.*?\\*)");
            java.util.regex.Matcher m = p.matcher(line);
            int lastEnd = 0;
            while (m.find()) {
                if (m.start() > lastEnd) {
                    addText(line.substring(lastEnd, m.start()), flow, isUser, false, false);
                }
                String match = m.group();
                if (match.startsWith("**")) {
                    addText(match.substring(2, match.length() - 2), flow, isUser, true, false);
                } else {
                    addText(match.substring(1, match.length() - 1), flow, isUser, false, true);
                }
                lastEnd = m.end();
            }
            if (lastEnd < line.length()) {
                addText(line.substring(lastEnd), flow, isUser, false, false);
            }
        }

        private void addText(String content, TextFlow flow, boolean isUser, boolean bold, boolean italic) {
            Text t = new Text(content);
            t.getStyleClass().add(isUser ? "studly-bubble-text-user" : "studly-bubble-text");
            if (bold) t.setStyle(t.getStyle() + "-fx-font-weight: 900;");
            if (italic) t.setStyle(t.getStyle() + "-fx-font-style: italic;");
            flow.getChildren().add(t);
        }
    }

    private static String formatForDisplay(String raw) {
        if (raw == null) return "";
        String s = raw.replace("\r\n", "\n").trim();

        // Make markdown-ish headings/bullets look cleaner in a plain Label.
        s = s.replaceAll("(?m)^#{2,6}\\s*", "");      // remove ### headings markers
        s = s.replaceAll("(?m)^\\*\\s+", "• ");       // * bullet -> •
        s = s.replaceAll("(?m)^-\\s+", "• ");         // - bullet -> •

        // Prevent horizontal scrolling caused by very long tokens (URLs, code, etc.)
        s = insertSoftBreaks(s, 26);

        return s;
    }

    private static String insertSoftBreaks(String s, int chunk) {
        if (s == null || s.isEmpty()) return "";
        StringBuilder out = new StringBuilder(s.length() + 64);
        int run = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean breakable = Character.isWhitespace(c) || c == '/' || c == '-' || c == '_' || c == '.' || c == ',' || c == ';' || c == ':';
            if (breakable) run = 0;
            else run++;
            out.append(c);
            if (!breakable && run >= chunk) {
                out.append('\u200B'); // zero-width space
                run = 0;
            }
        }
        return out.toString();
    }

    @FXML
    public void summarizeCoursePdf() {
        if (chatListView == null) return;
        if (!chatService.hasApiKeyConfigured()) {
            chatMessages.add(new ChatMessage(
                    ChatMessage.Role.AI,
                    "Studly AI is not configured yet.\n" +
                    "Add your OpenRouter key in: src/main/resources/openrouter.properties\n" +
                    "Example: openrouter.apiKey=sk-or-..."
            ));
            scrollChatToBottom();
            return;
        }
        if (displayedCourse == null || displayedCourse.getCourse_file() == null || displayedCourse.getCourse_file().trim().isEmpty()) {
            chatMessages.add(new ChatMessage(ChatMessage.Role.AI, "No course file attached. Add a PDF to this course to summarize it."));
            scrollChatToBottom();
            return;
        }
        File f = new File(displayedCourse.getCourse_file().trim());
        if (!f.exists() || !f.isFile()) {
            chatMessages.add(new ChatMessage(ChatMessage.Role.AI, "Course file not found on this device:\n" + f.getAbsolutePath()));
            scrollChatToBottom();
            return;
        }
        String name = f.getName().toLowerCase();
        if (!name.endsWith(".pdf")) {
            chatMessages.add(new ChatMessage(ChatMessage.Role.AI, "Summary is supported for PDF files only."));
            scrollChatToBottom();
            return;
        }

        setChatBusy(true, "Reading PDF...");
        ChatMessage pending = new ChatMessage(ChatMessage.Role.AI, "Summarizing your PDF…");
        chatMessages.add(pending);
        pendingAiBubble = pending;
        scrollChatToBottom();

        String systemPrompt = buildSystemPrompt(displayedCourse);
        CompletableFuture.supplyAsync(() -> {
            try {
                String text = PdfTextExtractor.extractText(f, 35_000);
                if (text.isBlank()) {
                    return "I couldn’t extract text from this PDF. It looks like a scanned PDF (images). " +
                           "To summarize scanned PDFs, we need OCR (e.g., Tesseract).";
                }
                String userPrompt =
                        "Act as a Senior Academic Researcher and Pedagogical Expert. " +
                        "Synthesize a highly professional, structured academic summary from the PDF content below.\n\n" +
                        "Use the following professional structure:\n" +
                        "1) **EXECUTIVE SUMMARY**: A high-level 2-3 sentence academic overview.\n" +
                        "2) **STRATEGIC LEARNING OBJECTIVES**: What the student should master (3-5 items).\n" +
                        "3) **CORE THEORETICAL FRAMEWORK**: Key concepts and their relationships.\n" +
                        "4) **CRITICAL VOCABULARY**: Definitions of essential terms.\n" +
                        "5) **EVALUATION PREPARATION**: 5 sophisticated exam questions ranging from application to synthesis.\n" +
                        "6) **ACCELERATED REVISION SHEET**: A condensed 10-line mastery guide.\n\n" +
                        "Maintain a formal, authoritative, and educational tone throughout. Use **bolding** for emphasis and *italics* for technical terms.\n\n" +
                        "PDF content:\n" + text;

                return chatService.chat(systemPrompt, userPrompt);
            } catch (Exception e) {
                return "Error: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            }
        }).thenAccept(answer -> Platform.runLater(() -> {
            replacePendingAi(answer);
            setChatBusy(false, "");
        }));
    }

    private static String buildSystemPrompt(Course course) {
        String base =
                "You are Studly AI, a highly capable academic assistant for students.\n" +
                "Scope: Professional education, research, and study assistance.\n" +
                "Core Directive: Always be helpful, detailed, and professional. If the user asks for a summary, synthesize one using the provided course data (notes, title, link).\n" +
                "If the user asks something completely unrelated to education (e.g., politics, gossip), politely redirect them to academic topics.\n" +
                "Language: Always reply in the same language as the user (e.g., French if they ask in French).";
        if (course == null) {
            return base;
        }
        String name = course.getName() != null ? course.getName().trim() : "";
        String comment = course.getComment() != null ? course.getComment().trim() : "";
        String link = course.getCourse_link() != null ? course.getCourse_link().trim() : "";

        StringBuilder sb = new StringBuilder(base);
        sb.append(" You are the dedicated assistant for this course.");
        if (!name.isEmpty()) sb.append(" [COURSE TITLE]: ").append(name).append(".");
        if (!comment.isEmpty()) sb.append(" [COURSE NOTES/CONTENT]: ").append(comment).append(".");
        if (!link.isEmpty()) sb.append(" [REFERENCE LINK]: ").append(link).append(".");
        sb.append(" You can use these notes to provide summaries, explain concepts, or answer questions. If the information is missing, use your general knowledge to assist the student while staying relevant to the course theme.");
        return sb.toString();
    }

    private static String displayUpper(String value) {
        return (value == null || value.isEmpty()) ? "—" : value.toUpperCase();
    }

    private static String fileBasename(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        String normalized = path.replace('\\', '/').trim();
        int slash = normalized.lastIndexOf('/');
        String name = slash >= 0 ? normalized.substring(slash + 1) : normalized;
        return name.isEmpty() ? normalized : name;
    }

    private static File resolveCourseFile(String rawPath) {
        if (rawPath == null || rawPath.trim().isEmpty()) {
            return null;
        }
        String p = rawPath.trim();
        File f = new File(p);
        if (f.exists()) {
            return f;
        }
        if (!f.isAbsolute()) {
            String base = System.getProperty("user.dir");
            if (base != null && !base.isEmpty()) {
                File alt = new File(base, p);
                if (alt.exists()) {
                    return alt;
                }
            }
        }
        return f;
    }

    private static void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }

    private static String extensionOf(String path) {
        String name = fileBasename(path);
        int dot = name.lastIndexOf('.');
        if (dot <= 0 || dot >= name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase();
    }

    private static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return String.format(java.util.Locale.US, "%.1f KB", kb);
        }
        double mb = kb / 1024.0;
        if (mb < 1024) {
            return String.format(java.util.Locale.US, "%.1f MB", mb);
        }
        return String.format(java.util.Locale.US, "%.2f GB", mb / 1024.0);
    }

    private static String humanReadableKind(String ext) {
        if (ext == null || ext.isEmpty()) {
            return "File";
        }
        if ("pdf".equals(ext)) {
            return "PDF";
        }
        if ("doc".equals(ext) || "docx".equals(ext)) {
            return "Word";
        }
        if ("ppt".equals(ext) || "pptx".equals(ext)) {
            return "PowerPoint";
        }
        if ("xls".equals(ext) || "xlsx".equals(ext)) {
            return "Excel";
        }
        if ("txt".equals(ext) || "md".equals(ext) || "csv".equals(ext)) {
            return "Text";
        }
        if ("zip".equals(ext) || "rar".equals(ext) || "7z".equals(ext)) {
            return "Archive";
        }
        if ("png".equals(ext) || "jpg".equals(ext) || "jpeg".equals(ext) || "gif".equals(ext)
                || "webp".equals(ext) || "bmp".equals(ext) || "svg".equals(ext)) {
            return "Image";
        }
        if ("mp4".equals(ext) || "webm".equals(ext) || "mov".equals(ext)) {
            return "Video";
        }
        if ("mp3".equals(ext) || "wav".equals(ext) || "ogg".equals(ext)) {
            return "Audio";
        }
        return ext.length() > 6 ? "File" : ext.toUpperCase();
    }

    private void applyFileTypeBadge(String path) {
        if (fileTypeBadge == null || fileTypeBadgeLabel == null) {
            return;
        }
        String ext = extensionOf(path);
        String badge = ext.isEmpty() ? "FILE" : (ext.length() > 4 ? ext.substring(0, 4).toUpperCase() : ext.toUpperCase());
        fileTypeBadgeLabel.setText(badge);
        String bg;
        if ("pdf".equals(ext)) {
            bg = "#DC2626";
        } else if ("doc".equals(ext) || "docx".equals(ext)) {
            bg = "#2563EB";
        } else if ("ppt".equals(ext) || "pptx".equals(ext)) {
            bg = "#EA580C";
        } else if ("xls".equals(ext) || "xlsx".equals(ext)) {
            bg = "#16A34A";
        } else if ("png".equals(ext) || "jpg".equals(ext) || "jpeg".equals(ext) || "gif".equals(ext)
                || "webp".equals(ext) || "bmp".equals(ext) || "svg".equals(ext)) {
            bg = "#7C3AED";
        } else if ("zip".equals(ext) || "rar".equals(ext) || "7z".equals(ext)) {
            bg = "#CA8A04";
        } else if ("txt".equals(ext) || "md".equals(ext) || "csv".equals(ext)) {
            bg = "#64748B";
        } else if ("mp4".equals(ext) || "webm".equals(ext) || "mov".equals(ext)
                || "mp3".equals(ext) || "wav".equals(ext) || "ogg".equals(ext)) {
            bg = "#0D9488";
        } else {
            bg = "#475569";
        }
        fileTypeBadge.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 12;");
    }

    private void updateTeacherInitials(String email) {
        if (teacherInitials == null || email == null || email.isEmpty()) {
            return;
        }
        String initials = "UC";
        if (email.contains("@")) {
            String namePart = email.split("@")[0];
            if (namePart.contains(".")) {
                String[] parts = namePart.split("\\.");
                if (parts.length >= 2 && !parts[0].isEmpty() && !parts[1].isEmpty()) {
                    initials = (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
                } else if (!namePart.isEmpty()) {
                    initials = namePart.substring(0, Math.min(2, namePart.length())).toUpperCase();
                }
            } else if (!namePart.isEmpty()) {
                initials = namePart.substring(0, Math.min(2, namePart.length())).toUpperCase();
            }
        }
        teacherInitials.setText(initials);
    }

    private void resetPdfViewer() {
        if (pdfPageImageView != null) {
            pdfPageImageView.setImage(null);
            pdfPageImageView.setScaleX(1.0);
            pdfPageImageView.setScaleY(1.0);
        }
        if (pdfFxScroll != null) {
            pdfFxScroll.setVisible(false);
            pdfFxScroll.setManaged(false);
            pdfFxScroll.setHvalue(0);
            pdfFxScroll.setVvalue(0);
        }
        pdfPageCount = 0;
        pdfCurrentPage = 0;
        pdfZoom = 1.0;
        pdfFitMode = FitMode.WIDTH;
        updatePdfControls();
    }

    private void closePdfDocument() {
        try {
            if (pdfDoc != null) {
                pdfDoc.close();
            }
        } catch (Exception ignored) {
        } finally {
            pdfDoc = null;
            pdfRenderer = null;
            pdfLoadedPath = null;
        }
    }

    private boolean openPdf(File file) {
        if (file == null || !file.exists()) {
            return false;
        }
        String path = file.getAbsolutePath();
        if (path.equals(pdfLoadedPath) && pdfDoc != null && pdfRenderer != null && pdfPageCount > 0) {
            return true;
        }
        closePdfDocument();
        try {
            pdfDoc = PDDocument.load(file);
            pdfRenderer = new PDFRenderer(pdfDoc);
            pdfLoadedPath = path;
            pdfPageCount = pdfDoc.getNumberOfPages();
            pdfCurrentPage = 0;
            pdfZoom = 1.0;
            pdfFitMode = FitMode.WIDTH;
            renderPdfPage();
            if (pdfFxScroll != null) {
                pdfFxScroll.setHvalue(0.5);
                pdfFxScroll.setVvalue(0.0);
            }
            Platform.runLater(this::applyFitModeNow);
            return pdfPageCount > 0;
        } catch (Exception ex) {
            ex.printStackTrace();
            closePdfDocument();
            return false;
        }
    }

    private void renderPdfPage() {
        if (pdfRenderer == null || pdfPageImageView == null || pdfPageCount <= 0) {
            return;
        }
        int page = Math.max(0, Math.min(pdfCurrentPage, pdfPageCount - 1));
        pdfCurrentPage = page;
        try {
            BufferedImage bim = pdfRenderer.renderImageWithDPI(page, PDF_RENDER_DPI, ImageType.RGB);
            Image fx = SwingFXUtils.toFXImage(bim, null);
            pdfPageImageView.setImage(fx);
            applyZoom(pdfZoom);
            updatePdfControls();
            if (pdfFitMode != FitMode.NONE) {
                Platform.runLater(this::applyFitModeNow);
            }
            if (pdfFxScroll != null) {
                pdfFxScroll.setHvalue(0.5);
                pdfFxScroll.setVvalue(0.0);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void updatePdfControls() {
        if (pdfPrevBtn != null) {
            pdfPrevBtn.setDisable(pdfPageCount <= 1 || pdfCurrentPage <= 0);
        }
        if (pdfNextBtn != null) {
            pdfNextBtn.setDisable(pdfPageCount <= 1 || pdfCurrentPage >= pdfPageCount - 1);
        }
        if (pdfPageIndicator != null) {
            if (pdfPageCount <= 0) {
                pdfPageIndicator.setText("— / —");
            } else {
                pdfPageIndicator.setText((pdfCurrentPage + 1) + " / " + pdfPageCount);
            }
        }
        if (pdfZoomLabel != null) {
            pdfZoomLabel.setText((int) Math.round(pdfZoom * 100) + "%");
        }
    }

    private void applyZoom(double newZoom) {
        double z = Math.max(PDF_ZOOM_MIN, Math.min(PDF_ZOOM_MAX, newZoom));
        pdfZoom = z;
        if (pdfPageImageView != null) {
            pdfPageImageView.setScaleX(z);
            pdfPageImageView.setScaleY(z);
        }
        updatePdfControls();
    }

    private void applyFitModeNow() {
        if (pdfFitMode == FitMode.NONE) {
            return;
        }
        if (pdfFxScroll == null || pdfPageImageView == null || pdfPageImageView.getImage() == null) {
            return;
        }
        double viewportW = pdfFxScroll.getViewportBounds().getWidth();
        double viewportH = pdfFxScroll.getViewportBounds().getHeight();
        if (viewportW <= 20 || viewportH <= 20) {
            return;
        }
        if (pdfCenterPane != null) {
            pdfCenterPane.setMinWidth(viewportW);
            pdfCenterPane.setMinHeight(viewportH);
        }
        double imgW = pdfPageImageView.getImage().getWidth();
        double imgH = pdfPageImageView.getImage().getHeight();
        if (imgW <= 0 || imgH <= 0) {
            return;
        }

        // Account for padding/card inside the center pane (rough but stable)
        double usableW = Math.max(100, viewportW - 60);
        double usableH = Math.max(100, viewportH - 60);
        double z;
        if (pdfFitMode == FitMode.WIDTH) {
            z = usableW / imgW;
        } else { // PAGE
            z = Math.min(usableW / imgW, usableH / imgH);
        }
        applyZoom(z);
        // Center horizontally; start at top for reading
        pdfFxScroll.setHvalue(0.5);
        pdfFxScroll.setVvalue(0.0);
    }

    private void updateFilePreview(Course course) {
        if (pdfWebView == null || previewContentArea == null) {
            return;
        }

        resetPdfViewer();
        if (pdfWebView.getEngine() != null) {
            pdfWebView.getEngine().loadContent("<html><body style='margin:0;background:#0F172A;'></body></html>");
        }

        String filePath = course.getCourse_file();
        String link = course.getCourse_link();
        boolean hasPreviewable = false;
        boolean usePdfViewer = false;

        if (filePath != null && !filePath.isEmpty()) {
            File file = new File(filePath);
            if (file.exists()) {
                String pathLower = filePath.toLowerCase();
                if (pathLower.endsWith(".png") || pathLower.endsWith(".jpg") || pathLower.endsWith(".jpeg")
                        || pathLower.endsWith(".gif") || pathLower.endsWith(".webp") || pathLower.endsWith(".bmp")) {
                    pdfWebView.getEngine().load(file.toURI().toString());
                    hasPreviewable = true;
                } else if (pathLower.endsWith(".pdf")) {
                    if (openPdf(file)) {
                        hasPreviewable = true;
                        usePdfViewer = true;
                    }
                }
            }
        }

        if (!hasPreviewable && link != null && link.startsWith("http")) {
            pdfWebView.getEngine().load(link);
            hasPreviewable = true;
        }

        if (hasPreviewable) {
            if (filePlaceholderLabel != null) {
                filePlaceholderLabel.setVisible(false);
                filePlaceholderLabel.setManaged(false);
            }
            if (usePdfViewer && pdfFxScroll != null) {
                pdfWebView.setVisible(false);
                pdfWebView.setManaged(false);
                pdfFxScroll.setVisible(true);
                pdfFxScroll.setManaged(true);
                if (pdfControlsBox != null) {
                    pdfControlsBox.setVisible(true);
                    pdfControlsBox.setManaged(true);
                }
            } else {
                pdfWebView.setVisible(true);
                pdfWebView.setManaged(true);
                if (pdfFxScroll != null) {
                    pdfFxScroll.setVisible(false);
                    pdfFxScroll.setManaged(false);
                }
                if (pdfControlsBox != null) {
                    pdfControlsBox.setVisible(false);
                    pdfControlsBox.setManaged(false);
                }
            }
        } else {
            pdfWebView.setVisible(false);
            pdfWebView.setManaged(false);
            if (pdfFxScroll != null) {
                pdfFxScroll.setVisible(false);
                pdfFxScroll.setManaged(false);
            }
            if (pdfControlsBox != null) {
                pdfControlsBox.setVisible(false);
                pdfControlsBox.setManaged(false);
            }
            if (filePlaceholderLabel != null) {
                filePlaceholderLabel.setVisible(true);
                filePlaceholderLabel.setManaged(true);
                applyNoPreviewPlaceholder(filePath);
            }
            closePdfDocument();
        }

        if (togglePreviewBtn != null) {
            boolean showToggle = hasPreviewable;
            togglePreviewBtn.setVisible(showToggle);
            togglePreviewBtn.setManaged(showToggle);
            if (showToggle && previewContentArea.isVisible()) {
                togglePreviewBtn.setText("Hide preview");
            } else if (showToggle) {
                togglePreviewBtn.setText("Show preview");
            }
        }
    }

    // ---- PDF toolbar handlers ----
    @FXML
    public void handlePdfPrevPage() {
        if (pdfPageCount <= 0) {
            return;
        }
        pdfCurrentPage = Math.max(0, pdfCurrentPage - 1);
        renderPdfPage();
        Platform.runLater(this::applyFitModeNow);
    }

    @FXML
    public void handlePdfNextPage() {
        if (pdfPageCount <= 0) {
            return;
        }
        pdfCurrentPage = Math.min(pdfPageCount - 1, pdfCurrentPage + 1);
        renderPdfPage();
        Platform.runLater(this::applyFitModeNow);
    }

    @FXML
    public void handlePdfZoomIn() {
        pdfFitMode = FitMode.NONE;
        applyZoom(pdfZoom + PDF_ZOOM_STEP);
    }

    @FXML
    public void handlePdfZoomOut() {
        pdfFitMode = FitMode.NONE;
        applyZoom(pdfZoom - PDF_ZOOM_STEP);
    }

    @FXML
    public void handlePdfFitWidth() {
        pdfFitMode = FitMode.WIDTH;
        Platform.runLater(this::applyFitModeNow);
    }

    @FXML
    public void handlePdfFitPage() {
        pdfFitMode = FitMode.PAGE;
        Platform.runLater(this::applyFitModeNow);
    }

    @FXML
    public void handlePdfFullscreen() {
        if (displayedCourse == null || displayedCourse.getCourse_file() == null || displayedCourse.getCourse_file().isEmpty()) {
            return;
        }
        File file = new File(displayedCourse.getCourse_file());
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

        final Stage stage = new Stage(StageStyle.DECORATED);
        stage.setTitle(fileBasename(file.getAbsolutePath()));

        final ImageView imageView = new ImageView();
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setCache(true);

        final StackPane centerPane = new StackPane();
        centerPane.setAlignment(Pos.CENTER);
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

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        final HBox toolbar = new HBox(10, prev, next, pageLabel, spacer, zoomOut, zoomLabel, zoomIn, fitW, fitP, exit);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(10, 14, 10, 14));
        toolbar.setStyle("-fx-background-color: rgba(15,23,42,0.85); -fx-border-color: rgba(148,163,184,0.18); -fx-border-width: 0 0 1 0;");

        final BorderPane root = new BorderPane(scroll);
        root.setTop(toolbar);
        root.setStyle("-fx-background-color: #0B1220;");

        final Scene scene = new Scene(root, 1200, 800);
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
            Image img = imageView.getImage();
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
                Image fx = SwingFXUtils.toFXImage(bim, null);
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
        scroll.addEventFilter(ScrollEvent.SCROLL, e -> {
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

    private void applyNoPreviewPlaceholder(String filePath) {
        if (placeholderTitleLabel == null || placeholderSubtitleLabel == null) {
            return;
        }
        if (filePath == null || filePath.isEmpty()) {
            placeholderTitleLabel.setText("No file attached");
            placeholderSubtitleLabel.setText("Add a course file when editing this course to see it here.");
            return;
        }
        File f = new File(filePath);
        if (!f.exists()) {
            placeholderTitleLabel.setText("File not found");
            placeholderSubtitleLabel.setText(
                "The saved path no longer points to a file on this computer. Check the file location or re-upload from course settings.");
        } else {
            placeholderTitleLabel.setText("No preview for this format");
            placeholderSubtitleLabel.setText(
                "Inline preview supports PDFs and common images. If preview fails, use Open file to view the document on your device.");
        }
    }


    @FXML
    public void handleOpenExternally() {
        if (displayedCourse == null || displayedCourse.getCourse_file() == null || displayedCourse.getCourse_file().isEmpty()) {
            return;
        }
        File file = resolveCourseFile(displayedCourse.getCourse_file());
        if (file == null || !file.exists()) {
            showAlert(Alert.AlertType.WARNING, "File not found", "The course file cannot be found on this device.");
            return;
        }
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(file);
                return;
            }
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("win")) {
                new ProcessBuilder("cmd", "/c", "start", "", file.getAbsolutePath()).start();
            } else if (os.contains("mac")) {
                new ProcessBuilder("open", file.getAbsolutePath()).start();
            } else {
                new ProcessBuilder("xdg-open", file.getAbsolutePath()).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleDownloadFile() {
        if (displayedCourse == null || displayedCourse.getCourse_file() == null || displayedCourse.getCourse_file().isEmpty()) {
            return;
        }
        File source = resolveCourseFile(displayedCourse.getCourse_file());
        if (source == null || !source.exists() || !source.isFile()) {
            showAlert(Alert.AlertType.WARNING, "File not found", "The course file cannot be found on this device.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save course file");
        chooser.setInitialFileName(fileBasename(source.getAbsolutePath()));
        String srcName = source.getName();
        String srcExt = extensionOf(srcName);
        if ("pdf".equalsIgnoreCase(srcExt)) {
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Documents", "*.pdf"));
        }
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files", "*.*"));

        Stage owner = null;
        if (rootPane != null && rootPane.getScene() != null) {
            owner = (Stage) rootPane.getScene().getWindow();
        } else if (detailTitle != null && detailTitle.getScene() != null) {
            owner = (Stage) detailTitle.getScene().getWindow();
        }

        File dest = chooser.showSaveDialog(owner);
        if (dest == null) {
            return;
        }
        try {
            // If user saved without extension, preserve the original file extension (esp. .pdf)
            String destName = dest.getName();
            boolean hasDot = destName.lastIndexOf('.') > 0;
            if (!hasDot && srcExt != null && !srcExt.isBlank()) {
                dest = new File(dest.getParentFile(), destName + "." + srcExt.toLowerCase());
            }
            if (dest.getParentFile() != null) {
                Files.createDirectories(dest.getParentFile().toPath());
            }
            Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            showAlert(Alert.AlertType.INFORMATION, "Download complete", "Saved to:\n" + dest.getAbsolutePath());
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Download failed", ex.getMessage() != null ? ex.getMessage() : "Unable to save the file.");
        }
    }

    @FXML
    public void handleTogglePreview() {
        if (previewContentArea != null && togglePreviewBtn != null) {
            boolean visible = !previewContentArea.isVisible();
            previewContentArea.setVisible(visible);
            previewContentArea.setManaged(visible);
            
            if (pdfControlsBox != null) {
                // Show controls if preview is visible AND we are using the PDFBox viewer
                boolean usingPdfBox = pdfFxScroll != null && pdfFxScroll.isVisible();
                pdfControlsBox.setVisible(visible && usingPdfBox);
                pdfControlsBox.setManaged(visible && usingPdfBox);
            }
            
            if (visible) {
                togglePreviewBtn.setText("Hide preview");
                updateFilePreview(displayedCourse);
            } else {
                togglePreviewBtn.setText("Show preview");
            }
        }
    }



    @FXML
    public void handleReadAloud() {
        if (displayedCourse == null || displayedCourse.getCourse_file() == null) return;
        File file = new File(displayedCourse.getCourse_file());
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
                // 1. Extract text
                String text = PdfTextExtractor.extractText(file, 5000);
                if (text == null || text.isBlank()) throw new Exception("No text found");

                // 2. Call VoiceRSS
                String lang = detectLanguage(text);
                File audio = ttsService.speak(text, lang);

                // 3. Play
                Platform.runLater(() -> {
                    try {
                        Media hit = new Media(audio.toURI().toString());
                        mediaPlayer = new MediaPlayer(hit);
                        mediaPlayer.setOnEndOfMedia(() -> {
                            if (ttsBtn != null) ttsBtn.setText("🔊");
                            if (ttsBtn != null) ttsBtn.setDisable(false);
                        });
                        mediaPlayer.play();
                        if (ttsBtn != null) ttsBtn.setText("⏹");
                        if (ttsBtn != null) ttsBtn.setDisable(false);
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (ttsBtn != null) ttsBtn.setText("🔊");
                        if (ttsBtn != null) ttsBtn.setDisable(false);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    if (ttsBtn != null) ttsBtn.setText("🔊");
                    if (ttsBtn != null) ttsBtn.setDisable(false);
                });
            }
        });
    }

    private String detectLanguage(String text) {
        String lower = text.toLowerCase();
        if (lower.contains(" le ") || lower.contains(" la ") || lower.contains(" et ")) return "fr-fr";
        return "en-us";
    }

    @FXML
    public void handleEditAction() {
        if (displayedCourse != null) {
            CourseEditController.startEdit(displayedCourse, (Stage) detailTitle.getScene().getWindow());
        }
    }

    @FXML
    public void openDeleteModal() {
        if (deleteModalOverlay != null) {
            deleteModalOverlay.setVisible(true);
            deleteModalOverlay.setManaged(true);
            deleteModalOverlay.toFront();
        }
    }

    @FXML
    public void closeDeleteModal() {
        if (deleteModalOverlay != null) {
            deleteModalOverlay.setVisible(false);
            deleteModalOverlay.setManaged(false);
        }
    }

    @FXML
    public void confirmDelete() {
        if (displayedCourse == null) {
            closeDeleteModal();
            return;
        }
        try {
            new CourseService().supprimer(displayedCourse.getId());
            closeDeleteModal();
            javafx.scene.Node anchor = detailTitle != null ? detailTitle : rootPane;
            navigateToFrontendCourseList(anchor);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    public void goToExams(javafx.event.ActionEvent event) {
        if (displayedCourse == null) {
            return;
        }
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/gestion_examen/frontend_exams.fxml"));
            javafx.scene.Parent root = loader.load();
            controllers.exams.ExamListController controller = loader.getController();
            controller.setCourse(displayedCourse);
            if (controllers.FrontendController.getInstance() != null) {
                controllers.FrontendController.getInstance().loadContentNode(root);
            } else {
                javafx.stage.Stage stage = (javafx.stage.Stage) detailTitle.getScene().getWindow();
                stage.getScene().setRoot(root);
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    private void loadQuizzes() {
        if (displayedCourse == null || quizListContainer == null) return;
        
        try {
            List<Activity> quizzes = activityService.recupererParCours(displayedCourse.getId());
            // Filter only type "Quiz"
            quizzes.removeIf(a -> !"Quiz".equalsIgnoreCase(a.getType()));
            
            Platform.runLater(() -> renderQuizCards(quizzes));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void renderQuizCards(List<Activity> quizzes) {
        quizListContainer.getChildren().clear();
        
        if (quizzes.isEmpty()) {
            if (noQuizLabel != null) quizListContainer.getChildren().add(noQuizLabel);
            return;
        }
        
        for (Activity q : quizzes) {
            HBox card = new HBox(20);
            card.setAlignment(Pos.CENTER_LEFT);
            card.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 12; -fx-padding: 15 20; -fx-border-color: rgba(255,255,255,0.05); -fx-border-radius: 12;");
            
            VBox info = new VBox(5);
            Label title = new Label(q.getTitle());
            title.setStyle("-fx-text-fill: #F8FAFC; -fx-font-weight: 800; -fx-font-size: 14px;");
            
            HBox meta = new HBox(10);
            meta.setAlignment(Pos.CENTER_LEFT);
            Label statusBadge = new Label(q.getStatus().toUpperCase());
            String statusColor = "Completed".equalsIgnoreCase(q.getStatus()) ? "#10B981" : "#F59E0B";
            statusBadge.setStyle("-fx-text-fill: " + statusColor + "; -fx-font-size: 10px; -fx-font-weight: 900; -fx-background-color: " + statusColor + "22; -fx-padding: 2 8; -fx-background-radius: 4;");
            
            Label score = new Label("Completed".equalsIgnoreCase(q.getStatus()) ? ("Score: " + q.getExpected_output()) : "Pending");
            score.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px; -fx-font-weight: 600;");
            
            meta.getChildren().addAll(statusBadge, score);
            info.getChildren().addAll(title, meta);
            
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            
            Button actionBtn = new Button("Completed".equalsIgnoreCase(q.getStatus()) ? "View Correction" : "Resume Quiz");
            actionBtn.setStyle("-fx-background-color: " + ("Completed".equalsIgnoreCase(q.getStatus()) ? "rgba(99, 102, 241, 0.15)" : "#6366F1") + "; -fx-text-fill: " + ("Completed".equalsIgnoreCase(q.getStatus()) ? "#A5B4FC" : "white") + "; -fx-font-weight: 800; -fx-font-size: 12px; -fx-padding: 8 16; -fx-background-radius: 8; -fx-cursor: hand;");
            
            actionBtn.setOnAction(e -> openQuizActivity(q));
            
            card.getChildren().addAll(info, spacer, actionBtn);
            
            // Hover effect
            card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-background-radius: 12; -fx-padding: 15 20; -fx-border-color: rgba(99, 102, 241, 0.3); -fx-border-radius: 12; -fx-cursor: hand;"));
            card.setOnMouseExited(e -> card.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 12; -fx-padding: 15 20; -fx-border-color: rgba(255,255,255,0.05); -fx-border-radius: 12;"));

            quizListContainer.getChildren().add(card);
        }
    }

    private void openQuizActivity(Activity q) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/gestion_activites/frontend_quiz_generator.fxml"));
            javafx.scene.Parent root = loader.load();
            controllers.activities.QuizController controller = loader.getController();
            controller.setQuizActivity(q);
            
            if (controllers.FrontendController.getInstance() != null) {
                controllers.FrontendController.getInstance().loadContentNode(root);
            } else {
                rootPane.getScene().setRoot(root);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
