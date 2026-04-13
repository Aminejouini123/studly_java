package services;

import models.Message;
import utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageService implements IService<Message> {
    private Connection connection;
    public MessageService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Message entity) throws SQLException {
        String sql = "insert into `message` (content, created_at, sender_id, group_id) values(?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, entity.getContent());
        ps.setTimestamp(2, entity.getCreated_at());
        ps.setInt(3, entity.getSender_id());
        ps.setInt(4, entity.getGroup_id());
        ps.executeUpdate();
    }

    @Override
    public void modifier(Message entity) throws SQLException {
        String sql = "update `message` set content = ?, created_at = ?, sender_id = ?, group_id = ? where id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, entity.getContent());
        ps.setTimestamp(2, entity.getCreated_at());
        ps.setInt(3, entity.getSender_id());
        ps.setInt(4, entity.getGroup_id());
        ps.setInt(5, entity.getId());
        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "delete from `message` where id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<Message> recuperer() throws SQLException {
        String sql = "select * from `message`";
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sql);
        List<Message> list = new ArrayList<>();
        while (rs.next()) {
            Message entity = new Message();
            entity.setId(rs.getInt("id"));
            entity.setContent(rs.getString("content"));
            entity.setCreated_at(rs.getTimestamp("created_at"));
            entity.setSender_id(rs.getInt("sender_id"));
            entity.setGroup_id(rs.getInt("group_id"));
            list.add(entity);
        }
        return list;
    }
}
