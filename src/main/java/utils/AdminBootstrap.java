package utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Optional bootstrap for creating an admin account in the local DB.
 *
 * This is intentionally opt-in: it only runs when both environment variables are set:
 * - STUDLY_ADMIN_EMAIL
 * - STUDLY_ADMIN_PASSWORD
 *
 * If the user exists, it is upgraded to admin and its password is set to the provided value.
 * If it doesn't exist, it's inserted with safe defaults.
 */
public final class AdminBootstrap {
    private AdminBootstrap() {
    }

    public static void ensureAdminFromEnv() {
        String email = getenvTrimmed("STUDLY_ADMIN_EMAIL");
        String password = getenvTrimmed("STUDLY_ADMIN_PASSWORD");

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return; // not configured
        }

        Connection c = MyDatabase.getInstance().getConnection();
        if (c == null) {
            System.err.println("AdminBootstrap: no DB connection; cannot bootstrap admin.");
            return;
        }

        try {
            Integer id = findUserIdByEmail(c, email);
            if (id != null) {
                upgradeToAdmin(c, id, password);
                System.out.println("AdminBootstrap: admin updated: id=" + id + ", email=" + email);
            } else {
                int newId = insertAdmin(c, email, password);
                System.out.println("AdminBootstrap: admin created: id=" + newId + ", email=" + email);
            }
        } catch (SQLException e) {
            System.err.println("AdminBootstrap: failed: " + e.getMessage());
        }
    }

    private static String getenvTrimmed(String key) {
        String v = System.getenv(key);
        return v == null ? null : v.trim();
    }

    private static Integer findUserIdByEmail(Connection c, String email) throws SQLException {
        String sql = "select id from `users` where email = ? limit 1";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return null;
            }
        }
    }

    private static void upgradeToAdmin(Connection c, int id, String password) throws SQLException {
        String sql = "update `users` set roles = ?, password = ?, is_verified = 1, updated_at = CURRENT_TIMESTAMP where id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "[\"ROLE_ADMIN\"]");
            ps.setString(2, password);
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    private static int insertAdmin(Connection c, String email, String password) throws SQLException {
        String sql = "insert into `users` ("
                + "google_id, is_verified, verification_code, email, roles, password, first_name, last_name, "
                + "date_of_birth, phone_number, address, created_at, updated_at, statut, profile_picture, "
                + "education_level, job_title, website, bio, skills, score, google_access_token, google_refresh_token, google_token_expires_at"
                + ") values ("
                + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?"
                + ")";

        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int i = 1;
            ps.setString(i++, null);                 // google_id
            ps.setInt(i++, 1);                       // is_verified
            ps.setString(i++, null);                 // verification_code
            ps.setString(i++, email);                // email
            ps.setString(i++, "[\"ROLE_ADMIN\"]");   // roles
            ps.setString(i++, password);             // password (plaintext, matches current authenticateUser)
            ps.setString(i++, "Admin");              // first_name
            ps.setString(i++, "Studly");             // last_name
            ps.setObject(i++, null);                 // date_of_birth
            ps.setString(i++, null);                 // phone_number
            ps.setString(i++, null);                 // address
            ps.setString(i++, "ACTIVE");             // statut
            ps.setString(i++, null);                 // profile_picture
            ps.setString(i++, null);                 // education_level
            ps.setString(i++, null);                 // job_title
            ps.setString(i++, null);                 // website
            ps.setString(i++, null);                 // bio
            ps.setString(i++, "[]");                 // skills
            ps.setInt(i++, 0);                       // score
            ps.setString(i++, null);                 // google_access_token
            ps.setString(i++, null);                 // google_refresh_token
            ps.setObject(i++, null);                 // google_token_expires_at

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return -1;
    }
}

