package controllers.user_controller;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.bytedeco.javacv.*;
import org.bytedeco.opencv.opencv_core.Mat;
import utils.FaceAuthService;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Camera dialog for registering a user's face.
 * Auto-captures REQUIRED_CAPTURES frames when a face is detected,
 * trains an LBPH model, and saves it to disk.
 */
public class FaceRegistrationController {

    private static final int    REQUIRED_CAPTURES     = 30;
    private static final long   CAPTURE_INTERVAL_MS   = 200;
    private static final int    CAMERA_FPS_PERIOD_MS  = 33; // ~30 fps

    private final int            userId;
    private final FaceAuthService faceAuth;
    private final Runnable        onSuccess;

    // UI
    private Stage     stage;
    private ImageView cameraView;
    private Label     statusLabel;
    private ProgressBar progressBar;
    private Button    startBtn;

    // Camera
    private ScheduledExecutorService executor;
    private OpenCVFrameGrabber       grabber;
    private final OpenCVFrameConverter.ToMat  matConv  = new OpenCVFrameConverter.ToMat();
    private final Java2DFrameConverter        biConv   = new Java2DFrameConverter();

    // State
    private final List<Mat> capturedFaces = new ArrayList<>();
    private volatile boolean capturing     = false;
    private long             lastCapture   = 0;

    public FaceRegistrationController(int userId, FaceAuthService faceAuth, Runnable onSuccess) {
        this.userId    = userId;
        this.faceAuth  = faceAuth;
        this.onSuccess = onSuccess;
    }

    public void show(Stage owner) {
        stage = new Stage();
        stage.setTitle("Register Face ID");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(owner);
        stage.setResizable(false);
        stage.setOnCloseRequest(e -> shutdownCamera());

        // Camera feed
        cameraView = new ImageView();
        cameraView.setFitWidth(560);
        cameraView.setFitHeight(420);
        cameraView.setPreserveRatio(true);
        StackPane cameraPane = new StackPane(cameraView);
        cameraPane.setStyle("-fx-background-color: black;");

        // Status area
        statusLabel = new Label("Click 'Start' — position your face in the camera");
        statusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #ddd;");
        statusLabel.setWrapText(true);

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(320);
        progressBar.setVisible(false);

        startBtn = new Button("▶  Start Registration");
        startBtn.setStyle("-fx-background-color: #004fb0; -fx-text-fill: white; "
            + "-fx-font-weight: bold; -fx-padding: 10 24; -fx-background-radius: 8; -fx-cursor: hand;");
        startBtn.setOnAction(e -> beginCapture());

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-padding: 10 24; -fx-background-radius: 8; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> { shutdownCamera(); stage.close(); });

        HBox btns = new HBox(15, startBtn, cancelBtn);
        btns.setAlignment(Pos.CENTER);

        VBox bottom = new VBox(10, statusLabel, progressBar, btns);
        bottom.setAlignment(Pos.CENTER);
        bottom.setPadding(new Insets(16));
        bottom.setStyle("-fx-background-color: #1a1a2e;");

        BorderPane root = new BorderPane(cameraPane);
        root.setBottom(bottom);

        stage.setScene(new Scene(root, 580, 570));
        stage.show();

        openCamera();
    }

    // ---- camera lifecycle ----

    private void openCamera() {
        executor = Executors.newSingleThreadScheduledExecutor(
            r -> { Thread t = new Thread(r, "face-reg-camera"); t.setDaemon(true); return t; });
        executor.execute(() -> {
            try {
                grabber = new OpenCVFrameGrabber(0);
                grabber.setImageWidth(640);
                grabber.setImageHeight(480);
                grabber.start();
                executor.scheduleAtFixedRate(this::tick, 0, CAMERA_FPS_PERIOD_MS, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("⚠ Camera not found. Check your webcam.");
                    startBtn.setDisable(true);
                });
            }
        });
    }

    private void tick() {
        try {
            Frame frame = grabber.grab();
            if (frame == null) return;

            Mat mat = matConv.convert(frame);
            if (mat == null || mat.empty()) return;

            // Annotate and display
            Mat display = mat.clone();
            faceAuth.drawFaceRects(display);
            updateCameraView(display);

            // Auto-capture when active
            if (!capturing) return;
            long now = System.currentTimeMillis();
            if (now - lastCapture < CAPTURE_INTERVAL_MS) return;

            Mat face = faceAuth.extractFace(mat);
            if (face == null) return;

            capturedFaces.add(face);
            lastCapture = now;
            int count = capturedFaces.size();

            Platform.runLater(() -> {
                progressBar.setProgress((double) count / REQUIRED_CAPTURES);
                statusLabel.setText("Capturing sample " + count + "/" + REQUIRED_CAPTURES
                    + " — move your head slightly for better results");
                if (count >= REQUIRED_CAPTURES) {
                    capturing = false;
                    trainModel();
                }
            });

        } catch (Exception ignored) {}
    }

    private void updateCameraView(Mat mat) {
        try {
            Frame f = matConv.convert(mat);
            BufferedImage bi = biConv.getBufferedImage(f);
            if (bi == null) return;
            WritableImage img = SwingFXUtils.toFXImage(bi, null);
            Platform.runLater(() -> cameraView.setImage(img));
        } catch (Exception ignored) {}
    }

    // ---- registration logic ----

    private void beginCapture() {
        capturedFaces.clear();
        lastCapture = 0;
        capturing = true;
        startBtn.setDisable(true);
        progressBar.setVisible(true);
        progressBar.setProgress(0);
        statusLabel.setText("Capturing samples... face the camera and hold still");
    }

    private void trainModel() {
        Platform.runLater(() -> {
            statusLabel.setText("Processing face data...");
            progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        });
        executor.execute(() -> {
            try {
                faceAuth.registerFace(userId, capturedFaces);
                Platform.runLater(() -> {
                    statusLabel.setText("✓ Face ID registered successfully!");
                    progressBar.setProgress(1.0);
                    if (onSuccess != null) onSuccess.run();
                    executor.schedule(() -> Platform.runLater(() -> {
                        shutdownCamera();
                        stage.close();
                    }), 2, TimeUnit.SECONDS);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Registration failed: " + e.getMessage());
                    startBtn.setDisable(false);
                    progressBar.setVisible(false);
                });
            }
        });
    }

    private void shutdownCamera() {
        if (executor != null) executor.shutdownNow();
        if (grabber != null) {
            try { grabber.stop(); grabber.release(); } catch (Exception ignored) {}
        }
    }
}