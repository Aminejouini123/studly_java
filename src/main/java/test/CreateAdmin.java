package test;

import utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Utility to create/update an admin user in the local MySQL database.
 *
 * Usage:
 *   java ... test.CreateAdmin [email] [password]
 *
 * Defaults:
 *   email=admin@studly.local
 *   password=admin123
 */
public final class CreateAdmin {
    public static void main(String[] args) throws Exception {
        String email = (args.length >= 1 && args[0] != null && !args[0].trim().isEmpty())
                ? args[0].trim()
                : "admin@studly.local";
        String password = (args.length >= 2 && args[1] != null && !args[1].trim().isEmpty())
                ? args[1].trim()
                : "admin123";

        try (Connection c = MyDatabase.getInstance().getConnection()) {
            if (c == null) {
                throw new IllegalStateException("No DB connection. Check utils.MyDatabase settings and that MySQL is running.");
            }

            Integer existingId = findUserIdByEmail(c, email);
            if (existingId != null) {
                upgradeToAdmin(c, existingId, password);
                System.out.println("Admin updated: id=" + existingId + ", email=" + email);
                return;
            }

            int id = insertAdmin(c, email, password);
            System.out.println("Admin created: id=" + id + ", email=" + email);
        }
    }

    private static Integer findUserIdByEmail(Connection c, String email) throws SQLException {
        String sql = "select id from `users` where email = ? limit 1";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
                return null;
            }
        }
    }

    private static void upgradeToAdmin(Connection c, int id, String password) throws SQLException {
        // Make sure roles contains ROLE_ADMIN. We keep it simple: overwrite roles.
        String sql = "update `users` set roles = ?, password = ?, is_verified = 1, updated_at = CURRENT_TIMESTAMP where id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "[\"ROLE_ADMIN\"]");
            ps.setString(2, password);
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    private static int insertAdmin(Connection c, String email, String password) throws SQLException {
        // Match the columns used by UserService. Many columns are nullable; we provide safe defaults
        // for common NOT NULL fields (is_verified, roles, created_at/updated_at, score).
        String sql = "insert into `users` (" +
                "google_id, is_verified, verification_code, email, roles, password, first_name, last_name, " +
                "date_of_birth, phone_number, address, created_at, updated_at, statut, profile_picture, " +
                "education_level, job_title, website, bio, skills, score, google_access_token, google_refresh_token, google_token_expires_at" +
                ") values (" +
                "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?" +
                ")";

        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int i = 1;
            ps.setString(i++, null);                 // google_id
            ps.setInt(i++, 1);                       // is_verified
            ps.setString(i++, null);                 // verification_code
            ps.setString(i++, email);                // email
            ps.setString(i++, "[\"ROLE_ADMIN\"]");   // roles (DB check constraint expects JSON-like array)
            ps.setString(i++, password);             // password (plaintext, matches authenticateUser)
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
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }
}
