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
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Camera dialog for Face ID login.
 *
 * Continuously captures frames and tries to recognize the user's face.
 * After MAX_FAILURES non-match attempts (or if camera is unavailable),
 * the {@code onFallback} callback fires so the caller can prompt for a password.
 */
public class FaceLoginController {

    private static final int  MAX_FAILURES         = 3;
    private static final long RECOGNITION_DELAY_MS = 1200; // ms between recognition attempts
    private static final int  CAMERA_FPS_PERIOD_MS = 33;   // ~30 fps display

    private final FaceAuthService   faceAuth;
    private final Consumer<Integer> onSuccess;   // receives recognized userId
    private final Runnable          onFallback;  // called when fallback to password is triggered

    // UI
    private Stage     stage;
    private ImageView cameraView;
    private Label     statusLabel;
    private Label     attemptsLabel;

    // Camera
    private ScheduledExecutorService executor;
    private OpenCVFrameGrabber       grabber;
    private final OpenCVFrameConverter.ToMat matConv = new OpenCVFrameConverter.ToMat();
    private final Java2DFrameConverter       biConv  = new Java2DFrameConverter();

    // State
    private volatile boolean done        = false;
    private int              failures    = 0;
    private long             lastAttempt = 0;

    public FaceLoginController(FaceAuthService faceAuth,
                                Consumer<Integer> onSuccess,
                                Runnable onFallback) {
        this.faceAuth   = faceAuth;
        this.onSuccess  = onSuccess;
        this.onFallback = onFallback;
    }

    public void show(Stage owner) {
        stage = new Stage();
        stage.setTitle("Face ID Login");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(owner);
        stage.setResizable(false);
        stage.setOnCloseRequest(e -> triggerFallback());

        cameraView = new ImageView();
        cameraView.setFitWidth(560);
        cameraView.setFitHeight(420);
        cameraView.setPreserveRatio(true);
        StackPane cameraPane = new StackPane(cameraView);
        cameraPane.setStyle("-fx-background-color: black;");

        statusLabel = new Label("Scanning... look at the camera");
        statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #eee; -fx-font-weight: bold;");

        attemptsLabel = new Label("Attempts remaining: " + MAX_FAILURES);
        attemptsLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #aaa;");

        Button fallbackBtn = new Button("Use Password Instead");
        fallbackBtn.setStyle("-fx-background-color: #444; -fx-text-fill: white; "
            + "-fx-font-weight: bold; -fx-padding: 10 24; -fx-background-radius: 8; -fx-cursor: hand;");
        fallbackBtn.setOnAction(e -> triggerFallback());

        VBox bottom = new VBox(10, statusLabel, attemptsLabel, fallbackBtn);
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
            r -> { Thread t = new Thread(r, "face-login-camera"); t.setDaemon(true); return t; });
        executor.execute(() -> {
            try {
                grabber = new OpenCVFrameGrabber(0);
                grabber.setImageWidth(640);
                grabber.setImageHeight(480);
                grabber.start();
                executor.scheduleAtFixedRate(this::tick, 0, CAMERA_FPS_PERIOD_MS, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("⚠ Camera unavailable – switching to password login");
                    executor.schedule(() -> Platform.runLater(this::triggerFallback),
                        1500, TimeUnit.MILLISECONDS);
                });
            }
        });
    }

    private void tick() {
        if (done) return;
        try {
            Frame frame = grabber.grab();
            if (frame == null) return;

            Mat mat = matConv.convert(frame);
            if (mat == null || mat.empty()) return;

            // Annotate and display
            Mat display = mat.clone();
            faceAuth.drawFaceRects(display);
            updateCameraView(display);

            // Rate-limit recognition attempts
            long now = System.currentTimeMillis();
            if (now - lastAttempt < RECOGNITION_DELAY_MS) return;
            lastAttempt = now;

            Mat face = faceAuth.extractFace(mat);
            if (face == null) {
                Platform.runLater(() -> statusLabel.setText("No face detected — face the camera"));
                return;
            }

            int recognizedId = faceAuth.recognizeUser(face);

            if (recognizedId != -1) {
                // Success
                done = true;
                Platform.runLater(() -> statusLabel.setText("✓ Face recognized! Logging you in…"));
                executor.schedule(() -> Platform.runLater(() -> {
                    shutdownCamera();
                    stage.close();
                    if (onSuccess != null) onSuccess.accept(recognizedId);
                }), 900, TimeUnit.MILLISECONDS);
            } else {
                failures++;
                int remaining = MAX_FAILURES - failures;
                Platform.runLater(() -> {
                    attemptsLabel.setText(remaining > 0
                        ? "Attempts remaining: " + remaining
                        : "No attempts remaining");
                    statusLabel.setText(remaining > 0
                        ? "Not recognized — stay still and look at the camera"
                        : "Face ID failed — switching to password login…");
                });
                if (failures >= MAX_FAILURES) {
                    done = true;
                    executor.schedule(() -> Platform.runLater(this::triggerFallback),
                        1500, TimeUnit.MILLISECONDS);
                }
            }
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

    // ---- fallback / cleanup ----

    private void triggerFallback() {
        done = true;
        shutdownCamera();
        if (stage.isShowing()) stage.close();
        if (onFallback != null) onFallback.run();
    }

    private void shutdownCamera() {
        if (executor != null) executor.shutdownNow();
        if (grabber != null) {
            try { grabber.stop(); grabber.release(); } catch (Exception ignored) {}
        }
    }
}