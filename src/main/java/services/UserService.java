package services;

import models.User;
import utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService implements IService<User> {
    
    public UserService() {
        // No longer caching connection in constructor
    }

    @Override
    public void ajouter(User entity) throws SQLException {
        String sql = "insert into `users` (google_id, is_verified, verification_code, email, roles, password, first_name, last_name, date_of_birth, phone_number, address, created_at, updated_at, statut, profile_picture, education_level, job_title, website, bio, skills, score, google_access_token, google_refresh_token, google_token_expires_at) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = MyDatabase.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, entity.getGoogle_id());
            ps.setInt(2, entity.getIs_verified());
            ps.setString(3, entity.getVerification_code());
            ps.setString(4, entity.getEmail());
            ps.setString(5, entity.getRoles());
            ps.setString(6, entity.getPassword());
            ps.setString(7, entity.getFirst_name());
            ps.setString(8, entity.getLast_name());
            ps.setDate(9, entity.getDate_of_birth());
            ps.setString(10, entity.getPhone_number());
            ps.setString(11, entity.getAddress());
            ps.setTimestamp(12, entity.getCreated_at());
            ps.setTimestamp(13, entity.getUpdated_at());
            ps.setString(14, entity.getStatut());
            ps.setString(15, entity.getProfile_picture());
            ps.setString(16, entity.getEducation_level());
            ps.setString(17, entity.getJob_title());
            ps.setString(18, entity.getWebsite());
            ps.setString(19, entity.getBio());
            ps.setString(20, entity.getSkills());
            ps.setInt(21, entity.getScore());
            ps.setString(22, entity.getGoogle_access_token());
            ps.setString(23, entity.getGoogle_refresh_token());
            ps.setTimestamp(24, entity.getGoogle_token_expires_at());
            ps.executeUpdate();
        }
    }

    @Override
    public void modifier(User entity) throws SQLException {
        String sql = "update `users` set google_id = ?, is_verified = ?, verification_code = ?, email = ?, roles = ?, password = ?, first_name = ?, last_name = ?, date_of_birth = ?, phone_number = ?, address = ?, created_at = ?, updated_at = ?, statut = ?, profile_picture = ?, education_level = ?, job_title = ?, website = ?, bio = ?, skills = ?, score = ?, google_access_token = ?, google_refresh_token = ?, google_token_expires_at = ? where id = ?";
        try (PreparedStatement ps = MyDatabase.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, entity.getGoogle_id());
            ps.setInt(2, entity.getIs_verified());
            ps.setString(3, entity.getVerification_code());
            ps.setString(4, entity.getEmail());
            ps.setString(5, entity.getRoles());
            ps.setString(6, entity.getPassword());
            ps.setString(7, entity.getFirst_name());
            ps.setString(8, entity.getLast_name());
            ps.setDate(9, entity.getDate_of_birth());
            ps.setString(10, entity.getPhone_number());
            ps.setString(11, entity.getAddress());
            ps.setTimestamp(12, entity.getCreated_at());
            ps.setTimestamp(13, entity.getUpdated_at());
            ps.setString(14, entity.getStatut());
            ps.setString(15, entity.getProfile_picture());
            ps.setString(16, entity.getEducation_level());
            ps.setString(17, entity.getJob_title());
            ps.setString(18, entity.getWebsite());
            ps.setString(19, entity.getBio());
            ps.setString(20, entity.getSkills());
            ps.setInt(21, entity.getScore());
            ps.setString(22, entity.getGoogle_access_token());
            ps.setString(23, entity.getGoogle_refresh_token());
            ps.setTimestamp(24, entity.getGoogle_token_expires_at());
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

    public User authenticateUser(String email, String password) throws SQLException {
        String sql = "select * from `users` where email = ? and password = ?";
        try (PreparedStatement ps = MyDatabase.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return extractUserFromResultSet(rs);
                }
            }
        }
        return null;
    }

    private User extractUserFromResultSet(ResultSet rs) throws SQLException {
        User entity = new User();
        entity.setId(rs.getInt("id"));
        entity.setGoogle_id(rs.getString("google_id"));
        entity.setIs_verified(rs.getInt("is_verified"));
        entity.setVerification_code(rs.getString("verification_code"));
        entity.setEmail(rs.getString("email"));
        entity.setRoles(rs.getString("roles"));
        entity.setPassword(rs.getString("password"));
        entity.setFirst_name(rs.getString("first_name"));
        entity.setLast_name(rs.getString("last_name"));
        entity.setDate_of_birth(rs.getDate("date_of_birth"));
        entity.setPhone_number(rs.getString("phone_number"));
        entity.setAddress(rs.getString("address"));
        entity.setCreated_at(rs.getTimestamp("created_at"));
        entity.setUpdated_at(rs.getTimestamp("updated_at"));
        entity.setStatut(rs.getString("statut"));
        entity.setProfile_picture(rs.getString("profile_picture"));
        entity.setEducation_level(rs.getString("education_level"));
        entity.setJob_title(rs.getString("job_title"));
        entity.setWebsite(rs.getString("website"));
        entity.setBio(rs.getString("bio"));
        entity.setSkills(rs.getString("skills"));
        entity.setScore(rs.getInt("score"));
        entity.setGoogle_access_token(rs.getString("google_access_token"));
        entity.setGoogle_refresh_token(rs.getString("google_refresh_token"));
        entity.setGoogle_token_expires_at(rs.getTimestamp("google_token_expires_at"));
        return entity;
    }
}
