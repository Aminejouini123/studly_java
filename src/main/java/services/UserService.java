package services;

import models.User;
import utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService implements IService<User> {
    
    public UserService() {
    }

    @Override
    public void ajouter(User entity) throws SQLException {
        String sql = "insert into `users` (google_id, is_verified, verification_code, email, roles, password, first_name, last_name, date_of_birth, phone_number, address, created_at, updated_at, statut, profile_picture, education_level, job_title, website, bio, skills, score, google_access_token, google_refresh_token, google_token_expires_at) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = MyDatabase.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, entity.getGoogleId());
            ps.setInt(2, entity.getIsVerified());
            ps.setString(3, entity.getVerificationCode());
            ps.setString(4, entity.getEmail());
            ps.setString(5, entity.getRoles());
            ps.setString(6, entity.getPassword());
            ps.setString(7, entity.getFirstName());
            ps.setString(8, entity.getLastName());
            ps.setDate(9, entity.getDateOfBirth());
            ps.setString(10, entity.getPhoneNumber());
            ps.setString(11, entity.getAddress());
            ps.setTimestamp(12, entity.getCreatedAt());
            ps.setTimestamp(13, entity.getUpdatedAt());
            ps.setString(14, entity.getStatut());
            ps.setString(15, entity.getProfilePicture());
            ps.setString(16, entity.getEducationLevel());
            ps.setString(17, entity.getJobTitle());
            ps.setString(18, entity.getWebsite());
            ps.setString(19, entity.getBio());
            ps.setString(20, entity.getSkills());
            ps.setInt(21, entity.getScore());
            ps.setString(22, entity.getGoogleAccessToken());
            ps.setString(23, entity.getGoogleRefreshToken());
            ps.setTimestamp(24, entity.getGoogleTokenExpiresAt());
            ps.executeUpdate();
        }
    }

    @Override
    public void modifier(User entity) throws SQLException {
        String sql = "update `users` set google_id = ?, is_verified = ?, verification_code = ?, email = ?, roles = ?, password = ?, first_name = ?, last_name = ?, date_of_birth = ?, phone_number = ?, address = ?, created_at = ?, updated_at = ?, statut = ?, profile_picture = ?, education_level = ?, job_title = ?, website = ?, bio = ?, skills = ?, score = ?, google_access_token = ?, google_refresh_token = ?, google_token_expires_at = ? where id = ?";
        try (PreparedStatement ps = MyDatabase.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, entity.getGoogleId());
            ps.setInt(2, entity.getIsVerified());
            ps.setString(3, entity.getVerificationCode());
            ps.setString(4, entity.getEmail());
            ps.setString(5, entity.getRoles());
            ps.setString(6, entity.getPassword());
            ps.setString(7, entity.getFirstName());
            ps.setString(8, entity.getLastName());
            ps.setDate(9, entity.getDateOfBirth());
            ps.setString(10, entity.getPhoneNumber());
            ps.setString(11, entity.getAddress());
            ps.setTimestamp(12, entity.getCreatedAt());
            ps.setTimestamp(13, entity.getUpdatedAt());
            ps.setString(14, entity.getStatut());
            ps.setString(15, entity.getProfilePicture());
            ps.setString(16, entity.getEducationLevel());
            ps.setString(17, entity.getJobTitle());
            ps.setString(18, entity.getWebsite());
            ps.setString(19, entity.getBio());
            ps.setString(20, entity.getSkills());
            ps.setInt(21, entity.getScore());
            ps.setString(22, entity.getGoogleAccessToken());
            ps.setString(23, entity.getGoogleRefreshToken());
            ps.setTimestamp(24, entity.getGoogleTokenExpiresAt());
            ps.setInt(25, entity.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "delete from `users` where id = ?";
        try (PreparedStatement ps = MyDatabase.getInstance().getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<User> recuperer() throws SQLException {
        String sql = "select * from `users`";
        List<User> list = new ArrayList<>();
        try (Statement statement = MyDatabase.getInstance().getConnection().createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                list.add(extractUserFromResultSet(rs));
            }
        }
        return list;
    }

    public void storeResetToken(String email, String token) throws SQLException {
        String sql = "UPDATE users SET verification_code = ?, updated_at = ? WHERE email = ?";
        try (PreparedStatement ps = MyDatabase.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, token);
            ps.setTimestamp(2, new java.sql.Timestamp(System.currentTimeMillis()));
            ps.setString(3, email);
            ps.executeUpdate();
        }
    }

    public User findByResetToken(String token) throws SQLException {
        String sql = "SELECT * FROM users WHERE verification_code = ?";
        try (PreparedStatement ps = MyDatabase.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return extractUserFromResultSet(rs);
            }
        }
        return null;
    }

    public void updatePassword(int userId, String hashedPassword) throws SQLException {
        String sql = "UPDATE users SET password = ?, verification_code = NULL, updated_at = ? WHERE id = ?";
        try (PreparedStatement ps = MyDatabase.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, hashedPassword);
            ps.setTimestamp(2, new java.sql.Timestamp(System.currentTimeMillis()));
            ps.setInt(3, userId);
            ps.executeUpdate();
        }
    }

    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (PreparedStatement ps = MyDatabase.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return extractUserFromResultSet(rs);
            }
        }
        return null;
    }

    public User findByGoogleId(String googleId) throws SQLException {
        String sql = "SELECT * FROM users WHERE google_id = ?";
        try (PreparedStatement ps = MyDatabase.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, googleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return extractUserFromResultSet(rs);
            }
        }
        return null;
    }

    public void linkGoogleAccount(int userId, String googleId, String accessToken,
                                  String refreshToken, java.sql.Timestamp expiresAt) throws SQLException {
        String sql = "UPDATE users SET google_id = ?, google_access_token = ?, "
            + "google_refresh_token = ?, google_token_expires_at = ?, updated_at = ? WHERE id = ?";
        try (PreparedStatement ps = MyDatabase.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, googleId);
            ps.setString(2, accessToken);
            ps.setString(3, refreshToken);
            ps.setTimestamp(4, expiresAt);
            ps.setTimestamp(5, new java.sql.Timestamp(System.currentTimeMillis()));
            ps.setInt(6, userId);
            ps.executeUpdate();
        }
    }

    public User authenticateUser(String email, String password) throws SQLException {
        String sql = "select * from `users` where email = ?";
        try (PreparedStatement ps = MyDatabase.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = extractUserFromResultSet(rs);
                    if (utils.PasswordUtil.verify(password, user.getPassword())) {
                        return user;
                    }
                }
            }
        }
        return null;
    }

    private User extractUserFromResultSet(ResultSet rs) throws SQLException {
        User entity = new User();
        entity.setId(rs.getInt("id"));
        entity.setGoogleId(rs.getString("google_id"));
        entity.setIsVerified(rs.getInt("is_verified"));
        entity.setVerificationCode(rs.getString("verification_code"));
        entity.setEmail(rs.getString("email"));
        entity.setRoles(rs.getString("roles"));
        entity.setPassword(rs.getString("password"));
        entity.setFirstName(rs.getString("first_name"));
        entity.setLastName(rs.getString("last_name"));
        entity.setDateOfBirth(rs.getDate("date_of_birth"));
        entity.setPhoneNumber(rs.getString("phone_number"));
        entity.setAddress(rs.getString("address"));
        entity.setCreatedAt(rs.getTimestamp("created_at"));
        entity.setUpdatedAt(rs.getTimestamp("updated_at"));
        entity.setStatut(rs.getString("statut"));
        entity.setProfilePicture(rs.getString("profile_picture"));
        entity.setEducationLevel(rs.getString("education_level"));
        entity.setJobTitle(rs.getString("job_title"));
        entity.setWebsite(rs.getString("website"));
        entity.setBio(rs.getString("bio"));
        entity.setSkills(rs.getString("skills"));
        entity.setScore(rs.getInt("score"));
        entity.setGoogleAccessToken(rs.getString("google_access_token"));
        entity.setGoogleRefreshToken(rs.getString("google_refresh_token"));
        entity.setGoogleTokenExpiresAt(rs.getTimestamp("google_token_expires_at"));
        return entity;
    }
}
