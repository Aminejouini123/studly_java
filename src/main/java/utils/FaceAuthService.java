package utils;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.util.List;

import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

public class FaceAuthService {

    public static final double CONFIDENCE_THRESHOLD = 65.0;
    public static final int FACE_IMG_SIZE = 100;

    static {
        // Explicitly trigger native library loading
        try {
            Loader.load(org.bytedeco.opencv.global.opencv_core.class);
            Loader.load(org.bytedeco.opencv.global.opencv_imgproc.class);
            Loader.load(org.bytedeco.opencv.global.opencv_objdetect.class);
            Loader.load(org.bytedeco.opencv.global.opencv_face.class);
        } catch (Throwable t) {
            System.err.println("Face ID: Failed to load native libraries: " + t.getMessage());
        }
    }

    private static final String MODELS_DIR =
        System.getProperty("user.home") + File.separator + ".studly" + File.separator + "faces";

    private final CascadeClassifier faceDetector;

    public FaceAuthService() {
        try {
            Files.createDirectories(Paths.get(MODELS_DIR));
            faceDetector = loadCascade();
        } catch (Throwable t) {
            throw new RuntimeException("FaceAuthService init failed: " + t.getMessage(), t);
        }
    }

    // ---- public API ----

    /**
     * Detects the largest face in a BGR frame.
     * Returns a 100×100 grayscale Mat ready for LBPH, or null if nothing found.
     */
    public Mat extractFace(Mat frame) {
        if (frame == null || frame.empty()) return null;

        Mat gray = new Mat();
        cvtColor(frame, gray, org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGR2GRAY);
        equalizeHist(gray, gray);

        RectVector faces = new RectVector();
        faceDetector.detectMultiScale(gray, faces, 1.1, 3, 0,
            new Size(FACE_IMG_SIZE, FACE_IMG_SIZE), new Size());

        if (faces.size() == 0) return null;

        Rect largest = faces.get(0);
        for (int i = 1; i < faces.size(); i++) {
            Rect r = faces.get(i);
            if (r.width() * r.height() > largest.width() * largest.height()) largest = r;
        }

        Mat faceCrop = new Mat(gray, largest);
        Mat resized  = new Mat();
        resize(faceCrop, resized, new Size(FACE_IMG_SIZE, FACE_IMG_SIZE));
        return resized;
    }

    /** Draws green rectangles around detected faces on the BGR frame (in-place). */
    public void drawFaceRects(Mat frame) {
        if (frame == null || frame.empty()) return;
        Mat gray = new Mat();
        cvtColor(frame, gray, org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGR2GRAY);
        equalizeHist(gray, gray);

        RectVector faces = new RectVector();
        faceDetector.detectMultiScale(gray, faces, 1.1, 3, 0,
            new Size(FACE_IMG_SIZE, FACE_IMG_SIZE), new Size());

        for (int i = 0; i < faces.size(); i++) {
            rectangle(frame, faces.get(i), new Scalar(0, 255, 0, 0), 2, 8, 0);
        }
    }

    /**
     * Trains an LBPH model from {@code faceImages} (each labeled as {@code userId})
     * and persists it to disk.
     */
    public void registerFace(int userId, List<Mat> faceImages) {
        if (faceImages == null || faceImages.isEmpty())
            throw new IllegalArgumentException("No face images provided");

        MatVector images = new MatVector(faceImages.size());
        Mat labels = new Mat(faceImages.size(), 1, org.bytedeco.opencv.global.opencv_core.CV_32SC1);
        for (int i = 0; i < faceImages.size(); i++) {
            images.put(i, faceImages.get(i));
            labels.ptr(i, 0).putInt(userId);
        }

        LBPHFaceRecognizer recognizer = LBPHFaceRecognizer.create(1, 8, 8, 8, 200.0);
        recognizer.train(images, labels);
        recognizer.save(modelPath(userId));
    }

    /**
     * Scans every saved user model and returns the userId whose model best matches
     * {@code face} (confidence below threshold). Returns -1 if no confident match.
     */
    public int recognizeUser(Mat face) {
        if (face == null || face.empty()) return -1;

        File dir = new File(MODELS_DIR);
        File[] models = dir.listFiles((d, n) -> n.endsWith(".yml"));
        if (models == null || models.length == 0) return -1;

        double bestConf   = Double.MAX_VALUE;
        int    bestUserId = -1;

        for (File model : models) {
            try {
                int uid = Integer.parseInt(model.getName().replace(".yml", ""));
                LBPHFaceRecognizer rec = LBPHFaceRecognizer.create();
                rec.read(model.getAbsolutePath());

                int[]    label = {-1};
                double[] conf  = {0.0};
                rec.predict(face, label, conf);

                if (conf[0] < bestConf) {
                    bestConf   = conf[0];
                    bestUserId = uid;
                }
            } catch (NumberFormatException ignored) {}
        }

        return bestConf < CONFIDENCE_THRESHOLD ? bestUserId : -1;
    }

    public boolean hasRegisteredFace(int userId) {
        return new File(modelPath(userId)).exists();
    }

    public void deleteFaceModel(int userId) {
        File f = new File(modelPath(userId));
        if (f.exists()) f.delete();
    }

    // ---- private helpers ----

    private String modelPath(int userId) {
        return MODELS_DIR + File.separator + userId + ".yml";
    }

    private CascadeClassifier loadCascade() throws IOException {
        // JavaCV bundles OpenCV native data files per platform.
        // We try several common locations where the haarcascade might be located in the JARs,
        // prioritizing our own bundled resource for reliability.
        String platform = Loader.getPlatform();
        String[] candidates = {
            "/haarcascade_frontalface_default.xml", // Our own bundled resource
            "/org/bytedeco/opencv/" + platform + "/share/opencv4/haarcascades/haarcascade_frontalface_default.xml",
            "/org/bytedeco/opencv/data/haarcascade_frontalface_default.xml",
            "haarcascade_frontalface_default.xml"
        };

        for (String candidate : candidates) {
            URL url = FaceAuthService.class.getResource(candidate);
            if (url == null) {
                // Try without leading slash just in case
                if (candidate.startsWith("/")) {
                    url = FaceAuthService.class.getResource(candidate.substring(1));
                }
            }
            
            if (url == null) continue;
            
            try {
                File cached = Loader.cacheResource(url);
                if (cached != null && cached.exists()) {
                    CascadeClassifier cc = new CascadeClassifier(cached.getAbsolutePath());
                    if (!cc.empty()) {
                        System.out.println("Face ID: Successfully loaded cascade from " + candidate);
                        return cc;
                    }
                }
            } catch (Exception e) {
                System.err.println("Face ID: Failed to load candidate " + candidate + ": " + e.getMessage());
            }
        }

        throw new IOException(
            "Haar cascade XML not found in any expected location. \n" +
            "Searched platform: " + platform + "\n" +
            "Ensure 'opencv-platform' or 'opencv' for your platform is in the pom.xml.");
    }
}
