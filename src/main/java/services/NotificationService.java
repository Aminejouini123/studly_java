package services;

import models.Notification;
import utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificationService implements IService<Notification> {
    private Connection connection;
    public NotificationService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Notification entity) throws SQLException {
        String sql = "insert into `notification` (content, link, is_read, created_at, user_id) values(?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, entity.getContent());
        ps.setString(2, entity.getLink());
        ps.setInt(3, entity.getIs_read());
        ps.setTimestamp(4, entity.getCreated_at());
        ps.setInt(5, entity.getUser_id());
        ps.executeUpdate();
    }

    @Override
    public void modifier(Notification entity) throws SQLException {
        String sql = "update `notification` set content = ?, link = ?, is_read = ?, created_at = ?, user_id = ? where id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, entity.getContent());
        ps.setString(2, entity.getLink());
        ps.setInt(3, entity.getIs_read());
        ps.setTimestamp(4, entity.getCreated_at());
        ps.setInt(5, entity.getUser_id());
        ps.setInt(6, entity.getId());
        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "delete from `notification` where id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<Notification> recuperer() throws SQLException {
        String sql = "select * from `notification`";
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sql);
        List<Notification> list = new ArrayList<>();
        while (rs.next()) {
            Notification entity = new Notification();
            entity.setId(rs.getInt("id"));
            entity.setContent(rs.getString("content"));
            entity.setLink(rs.getString("link"));
            entity.setIs_read(rs.getInt("is_read"));
            entity.setCreated_at(rs.getTimestamp("created_at"));
            entity.setUser_id(rs.getInt("user_id"));
            list.add(entity);
        }
        return list;
    }
}
