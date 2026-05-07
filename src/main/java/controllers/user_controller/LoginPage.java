package controllers.user_controller;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.Student;
import models.User;
import services.UserService;
import utils.EmailService;
import utils.FaceAuthService;
import utils.GitHubOAuthService;
import utils.GoogleOAuthService;
import utils.GoogleOAuthService.UserProfile;
import utils.SessionManager;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class LoginPage {

    @FXML private TextField   emailInput;
    @FXML private PasswordField passwordInput;
    @FXML private Button      loginButton;
    @FXML private Button      googleLoginButton;
    @FXML private Button      githubLoginButton;
    @FXML private Button      faceLoginButton;
    @FXML private TextField   passwordVisibleInput;
    @FXML private Button      showPasswordBtn;
    @FXML private CheckBox    rememberMeCheckbox;   // may be null if not in FXML
    @FXML private Hyperlink   resetPasswordLink;
    @FXML private Hyperlink   signUpLink;

    private FaceAuthService faceAuth;

    private static final String PREFS_NODE = "studly/auth";
    private static final String PREF_EMAIL = "email";
    private static final String PREF_PWD   = "password";
    private static final String PREF_REMEMBER = "remember_me";

    private final UserService         userService       = new UserService();
    private final GoogleOAuthService  oauthService      = new GoogleOAuthService();
    private final GitHubOAuthService  githubOAuthService = new GitHubOAuthService();

    @FXML
    public void initialize() {
        loginButton.setOnAction(e -> handleLogin());
        signUpLink.setOnAction(e -> navigateTo("/getion_user/signUp_page.fxml", "Sign Up – Studly"));
        resetPasswordLink.setOnAction(e -> handleForgotPassword());
        loadRememberedCredentials();

        // Lazy-init FaceAuthService so startup isn't blocked if OpenCV is unavailable
        try {
            faceAuth = new FaceAuthService();
        } catch (Throwable t) {
            if (faceLoginButton != null) {
                faceLoginButton.setDisable(true);
                String errorMsg = t.getMessage();
                if (errorMsg == null) errorMsg = t.getClass().getSimpleName();
                faceLoginButton.setText("Face ID: " + errorMsg);
            }
        }
        
        setupShowPassword();
    }

    private void setupShowPassword() {
        if (showPasswordBtn == null) return;
        
        showPasswordBtn.setOnAction(e -> {
            if (passwordInput.isVisible()) {
                passwordVisibleInput.setText(passwordInput.getText());
                passwordVisibleInput.setVisible(true);
                passwordVisibleInput.setManaged(true);
                passwordInput.setVisible(false);
                passwordInput.setManaged(false);
                showPasswordBtn.setText("🙈");
            } else {
                passwordInput.setText(passwordVisibleInput.getText());
                passwordInput.setVisible(true);
                passwordInput.setManaged(true);
                passwordVisibleInput.setVisible(false);
                passwordVisibleInput.setManaged(false);
                showPasswordBtn.setText("👁");
            }
        });
        
        // Sync text as user types
        passwordInput.textProperty().addListener((obs, old, val) -> {
            if (passwordInput.isVisible()) passwordVisibleInput.setText(val);
        });
        passwordVisibleInput.textProperty().addListener((obs, old, val) -> {
            if (passwordVisibleInput.isVisible()) passwordInput.setText(val);
        });
    }

    // ---- Remember Me ----

    private void loadRememberedCredentials() {
        Preferences prefs = Preferences.userRoot().node(PREFS_NODE);
        if (prefs.getBoolean(PREF_REMEMBER, false)) {
            if (rememberMeCheckbox != null) rememberMeCheckbox.setSelected(true);
            emailInput.setText(prefs.get(PREF_EMAIL, ""));
            String encoded = prefs.get(PREF_PWD, "");
            if (!encoded.isEmpty()) {
                try {
                    passwordInput.setText(new String(Base64.getDecoder().decode(encoded)));
                } catch (IllegalArgumentException ignored) { /* corrupt pref → skip */ }
            }
        }
    }

    private void saveCredentials(String email, String password) {
        Preferences prefs = Preferences.userRoot().node(PREFS_NODE);
        prefs.putBoolean(PREF_REMEMBER, true);
        prefs.put(PREF_EMAIL, email);
        prefs.put(PREF_PWD, Base64.getEncoder().encodeToString(password.getBytes()));
    }

    private void clearSavedCredentials() {
        try {
            Preferences prefs = Preferences.userRoot().node(PREFS_NODE);
            prefs.remove(PREF_REMEMBER);
            prefs.remove(PREF_EMAIL);
            prefs.remove(PREF_PWD);
            prefs.flush();
        } catch (BackingStoreException ignored) {}
    }

    // ---- Standard email/password login ----

    private void handleLogin() {
        String email    = emailInput.getText().trim();
        String password = passwordInput.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing Fields",
                "Please enter both email and password.");
            return;
        }

        try {
            User user = userService.authenticateUser(email, password);
            if (user != null) {
                boolean remember = rememberMeCheckbox != null && rememberMeCheckbox.isSelected();
                if (remember) saveCredentials(email, password);
                else          clearSavedCredentials();
                SessionManager.setCurrentUser(user);
                redirectByRole(user);
            } else {
                showAlert(Alert.AlertType.ERROR, "Login Failed",
                    "Invalid email or password. Please try again.");
            }
        } catch (UserService.BannedException ex) {
            String reason = ex.getReason();
            showAlert(Alert.AlertType.ERROR, "Access Denied",
                "Your account has been banned.\nReason: "
                + (reason != null && !reason.isBlank() ? reason : "No reason provided."));
        } catch (SQLException ex) {
            showAlert(Alert.AlertType.ERROR, "Database Error",
                "Could not reach the database: " + ex.getMessage());
        }
    }

    // ---- Forgot Password ----

    private void handleForgotPassword() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Forgot Password");
        dialog.setHeaderText("Reset your password");
        dialog.setContentText("Enter your account email address:");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) return;

        String email = result.get().trim();
        if (email.isEmpty() || !email.matches("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$")) {
            showAlert(Alert.AlertType.WARNING, "Invalid Email", "Please enter a valid email address.");
            return;
        }

        try {
            User user = userService.findByEmail(email);
            if (user == null) {
                // Don't reveal whether the email exists (security best practice)
                showAlert(Alert.AlertType.INFORMATION, "Code Sent",
                    "If an account with that email exists, a reset code has been sent.");
                return;
            }

            String code = EmailService.generateCode();
            userService.storeResetToken(email, code);
            long sentAt = System.currentTimeMillis();

            resetPasswordLink.setDisable(true);

            Task<Void> sendTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    EmailService.sendPasswordResetCode(email, code);
                    return null;
                }
            };

            sendTask.setOnSucceeded(e -> {
                resetPasswordLink.setDisable(false);
                showAlert(Alert.AlertType.INFORMATION, "Code Sent",
                    "A 6-digit reset code was sent to " + email + ".\n"
                    + "Check your inbox and enter it in the next screen.");
                navigateToResetPassword(email, sentAt);
            });

            sendTask.setOnFailed(e -> {
                resetPasswordLink.setDisable(false);
                showAlert(Alert.AlertType.ERROR, "Email Error",
                    "Could not send reset code: " + sendTask.getException().getMessage());
            });

            new Thread(sendTask, "email-reset").start();

        } catch (SQLException ex) {
            showAlert(Alert.AlertType.ERROR, "Database Error", ex.getMessage());
        }
    }

    private void navigateToResetPassword(String email, long sentAt) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/getion_user/reset_password.fxml"));
            Parent root = loader.load();
            ResetPasswordController ctrl = loader.getController();
            ctrl.initData(email, sentAt);

            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Reset Password – Studly");
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error", ex.getMessage());
        }
    }

    // ---- Face ID login ----

    @FXML
    private void handleFaceLogin() {
        if (faceAuth == null) {
            showAlert(Alert.AlertType.WARNING, "Face ID Unavailable",
                "OpenCV could not be initialized. Check that opencv-platform is in the classpath.");
            return;
        }

        Stage owner = (Stage) loginButton.getScene().getWindow();

        new FaceLoginController(
            faceAuth,
            // onSuccess: userId recognized → look up user in DB and login
            recognizedId -> {
                try {
                    User user = findUserById(recognizedId);
                    if (user != null) {
                        if (!checkNotBanned(user)) return;
                        SessionManager.setCurrentUser(user);
                        redirectByRole(user);
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Face ID Error",
                            "Recognized face but user not found in database.");
                    }
                } catch (SQLException ex) {
                    showAlert(Alert.AlertType.ERROR, "Database Error", ex.getMessage());
                }
            },
            // onFallback: show a prompt with the email field focused
            () -> showAlert(Alert.AlertType.INFORMATION, "Face ID Fallback",
                "Face ID could not verify you. Please enter your email and password.")
        ).show(owner);
    }

    private User findUserById(int userId) throws SQLException {
        for (User u : userService.recuperer()) {
            if (u.getId() == userId) return u;
        }
        return null;
    }

    // ---- Google OAuth2 login ----

    @FXML
    private void handleGoogleLogin() {
        if (!oauthService.isConfigured()) {
            showAlert(Alert.AlertType.WARNING, "Not Configured",
                "Google OAuth credentials are not set up yet.\n"
                + "Edit src/main/resources/google-oauth.properties.");
            return;
        }

        googleLoginButton.setDisable(true);
        googleLoginButton.setText("…");

        Task<UserProfile> task = new Task<>() {
            @Override
            protected UserProfile call() throws Exception {
                return oauthService.authenticate();
            }
        };

        task.setOnSucceeded(e -> {
            googleLoginButton.setDisable(false);
            googleLoginButton.setText("G");
            UserProfile profile = task.getValue();
            if (profile == null) {
                showAlert(Alert.AlertType.ERROR, "Cancelled",
                    "Google login was cancelled or timed out.");
                return;
            }
            try {
                processGoogleProfile(profile);
            } catch (SQLException ex) {
                showAlert(Alert.AlertType.ERROR, "Database Error", ex.getMessage());
            }
        });

        task.setOnFailed(e -> {
            googleLoginButton.setDisable(false);
            googleLoginButton.setText("G");
            Throwable ex = task.getException();
            showAlert(Alert.AlertType.ERROR, "Google Login Failed",
                ex != null ? ex.getMessage() : "Unknown error.");
        });

        new Thread(task, "google-oauth").start();
    }

    private void processGoogleProfile(UserProfile profile) throws SQLException {
        User user = userService.findByGoogleId(profile.googleId);
        if (user == null) user = userService.findByEmail(profile.email);

        if (user != null) {
            if (!checkNotBanned(user)) return;
            if (user.getGoogleId() == null || user.getGoogleId().isEmpty()) {
                userService.linkGoogleAccount(user.getId(), profile.googleId,
                    profile.accessToken, profile.refreshToken, profile.tokenExpiresAt);
                user.setGoogleId(profile.googleId);
            }
            SessionManager.setCurrentUser(user);
            redirectByRole(user);
        } else {
            registerNewGoogleUser(profile);
        }
    }

    private void registerNewGoogleUser(UserProfile profile) {
        String code = EmailService.generateCode();
        long sentAt = System.currentTimeMillis();

        Task<Void> sendTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                EmailService.sendVerificationCode(profile.email, code);
                return null;
            }
        };

        sendTask.setOnSucceeded(e -> showGoogleVerificationDialog(profile, code, sentAt));
        sendTask.setOnFailed(e -> showAlert(Alert.AlertType.ERROR, "Email Error",
            "Could not send verification code: " + sendTask.getException().getMessage()));

        new Thread(sendTask, "email-sender").start();
    }

    private void showGoogleVerificationDialog(UserProfile profile, String code, long sentAt) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Email Verification");
        dialog.setHeaderText("One last step – verify your email");
        dialog.setContentText("Enter the 6-digit code sent to " + profile.email + ":");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) return;

        String entered = result.get().trim();

        if (System.currentTimeMillis() - sentAt > 10 * 60 * 1000L) {
            showAlert(Alert.AlertType.ERROR, "Code Expired",
                "The verification code has expired. Please try Google login again.");
            return;
        }
        if (!entered.equals(code)) {
            showAlert(Alert.AlertType.ERROR, "Wrong Code",
                "Incorrect verification code. Please try again.");
            return;
        }

        try {
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            User newUser = new User();
            newUser.setGoogleId(profile.googleId);
            newUser.setEmail(profile.email);
            newUser.setFirstName(profile.firstName);
            newUser.setLastName(profile.lastName.isEmpty() ? "-" : profile.lastName);
            newUser.setProfilePicture(profile.pictureUrl);
            newUser.setGoogleAccessToken(profile.accessToken);
            newUser.setGoogleRefreshToken(profile.refreshToken);
            newUser.setGoogleTokenExpiresAt(profile.tokenExpiresAt);
            newUser.setRole(new Student());
            newUser.setIsVerified(1);
            newUser.setStatut("active");
            newUser.setScore(0);
            newUser.setPassword("");
            newUser.setCreatedAt(now);
            newUser.setUpdatedAt(now);

            userService.ajouter(newUser);
            User created = userService.findByEmail(profile.email);
            SessionManager.setCurrentUser(created);
            navigateTo("/TEMPLATE/frontend_dashboard.fxml", "Dashboard – Studly");
        } catch (SQLException ex) {
            showAlert(Alert.AlertType.ERROR, "Database Error",
                "Could not create account: " + ex.getMessage());
        }
    }

    // ---- GitHub OAuth2 login ----

    @FXML
    private void handleGitHubLogin() {
        if (!githubOAuthService.isConfigured()) {
            showAlert(Alert.AlertType.WARNING, "Not Configured",
                "GitHub OAuth credentials are not set up yet.\n"
                + "Edit src/main/resources/github-oauth.properties.");
            return;
        }

        githubLoginButton.setDisable(true);
        githubLoginButton.setText("…");

        Task<GitHubOAuthService.UserProfile> task = new Task<>() {
            @Override
            protected GitHubOAuthService.UserProfile call() throws Exception {
                return githubOAuthService.authenticate();
            }
        };

        task.setOnSucceeded(e -> {
            githubLoginButton.setDisable(false);
            githubLoginButton.setText("git");
            GitHubOAuthService.UserProfile profile = task.getValue();
            if (profile == null) {
                showAlert(Alert.AlertType.ERROR, "Cancelled",
                    "GitHub login was cancelled or timed out.");
                return;
            }
            try {
                processGitHubProfile(profile);
            } catch (SQLException ex) {
                showAlert(Alert.AlertType.ERROR, "Database Error", ex.getMessage());
            }
        });

        task.setOnFailed(e -> {
            githubLoginButton.setDisable(false);
            githubLoginButton.setText("git");
            Throwable ex = task.getException();
            showAlert(Alert.AlertType.ERROR, "GitHub Login Failed",
                ex != null ? ex.getMessage() : "Unknown error.");
        });

        new Thread(task, "github-oauth").start();
    }

    private void processGitHubProfile(GitHubOAuthService.UserProfile profile) throws SQLException {
        // Prefer looking up by GitHub ID so returning users are found even if email changes
        User user = userService.findByGitHubId(profile.githubId);
        if (user == null) user = userService.findByEmail(profile.email);

        if (user != null) {
            if (!checkNotBanned(user)) return;
            if (user.getGithubId() == null || user.getGithubId().isEmpty()) {
                userService.linkGitHubAccount(user.getId(), profile.githubId);
                user.setGithubId(profile.githubId);
            }
            SessionManager.setCurrentUser(user);
            redirectByRole(user);
        } else {
            registerNewGitHubUser(profile);
        }
    }

    private void registerNewGitHubUser(GitHubOAuthService.UserProfile profile) {
        // GitHub has already verified the email — no verification step required
        try {
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            User newUser = new User();
            newUser.setGithubId(profile.githubId);
            newUser.setEmail(profile.email);
            newUser.setFirstName(profile.firstName.isEmpty() ? profile.email.split("@")[0] : profile.firstName);
            newUser.setLastName(profile.lastName.isEmpty() ? "-" : profile.lastName);
            newUser.setProfilePicture(profile.avatarUrl);
            newUser.setRole(new Student());
            newUser.setIsVerified(1);
            newUser.setStatut("active");
            newUser.setScore(0);
            newUser.setPassword("");
            newUser.setCreatedAt(now);
            newUser.setUpdatedAt(now);

            userService.ajouter(newUser);
            User created = userService.findByEmail(profile.email);
            if (created != null) {
                SessionManager.setCurrentUser(created);
                navigateTo("/TEMPLATE/frontend_dashboard.fxml", "Dashboard – Studly");
            }
        } catch (SQLException ex) {
            showAlert(Alert.AlertType.ERROR, "Database Error",
                "Could not create account: " + ex.getMessage());
        }
    }

    // ---- Utilities ----

    /** Returns false and shows an alert when the user is banned. */
    private boolean checkNotBanned(User user) {
        if (!"BANNED".equalsIgnoreCase(user.getStatut())) return true;
        String reason = user.getBanReason();
        showAlert(Alert.AlertType.ERROR, "Access Denied",
            "Your account has been banned.\nReason: "
            + (reason != null && !reason.isBlank() ? reason : "No reason provided."));
        return false;
    }

    private void redirectByRole(User user) {
        if (user.isAdmin()) {
            navigateTo("/TEMPLATE/backend_management.fxml", "Admin Dashboard – Studly");
        } else {
            navigateTo("/TEMPLATE/frontend_dashboard.fxml", "Dashboard – Studly");
        }
    }

    private void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
        } catch (Exception ex) {
            ex.printStackTrace();
            String msg = ex.getClass().getSimpleName()
                + (ex.getCause() != null ? " caused by " + ex.getCause().getClass().getSimpleName() : "");
            showAlert(Alert.AlertType.ERROR, "Navigation Error",
                msg + "\nCheck console for details.");
        }
    }

    private void showAlert(Alert.AlertType type, String header, String content) {
        Alert alert = new Alert(type);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
