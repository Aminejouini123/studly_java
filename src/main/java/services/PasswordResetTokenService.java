package services;

import models.PasswordResetToken;
import utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PasswordResetTokenService implements IService<PasswordResetToken> {
    private Connection connection;
    public PasswordResetTokenService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(PasswordResetToken entity) throws SQLException {
        String sql = "insert into `password_reset_token` (token, created_at, expires_at, user_id) values(?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, entity.getToken());
        ps.setTimestamp(2, entity.getCreated_at());
        ps.setTimestamp(3, entity.getExpires_at());
        ps.setInt(4, entity.getUser_id());
        ps.executeUpdate();
    }

    @Override
    public void modifier(PasswordResetToken entity) throws SQLException {
        String sql = "update `password_reset_token` set token = ?, created_at = ?, expires_at = ?, user_id = ? where id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, entity.getToken());
        ps.setTimestamp(2, entity.getCreated_at());
        ps.setTimestamp(3, entity.getExpires_at());
        ps.setInt(4, entity.getUser_id());
        ps.setInt(5, entity.getId());
        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "delete from `password_reset_token` where id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<PasswordResetToken> recuperer() throws SQLException {
        String sql = "select * from `password_reset_token`";
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sql);
        List<PasswordResetToken> list = new ArrayList<>();
        while (rs.next()) {
            PasswordResetToken entity = new PasswordResetToken();
            entity.setId(rs.getInt("id"));
            entity.setToken(rs.getString("token"));
            entity.setCreated_at(rs.getTimestamp("created_at"));
            entity.setExpires_at(rs.getTimestamp("expires_at"));
            entity.setUser_id(rs.getInt("user_id"));
            list.add(entity);
        }
        return list;
    }
}
