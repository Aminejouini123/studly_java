package services;

import models.Message;
import utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageService implements IService<Message> {
    private final Connection connection;
    public MessageService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    /**
     * MVC-friendly alias (requested API).
     */
    public void addMessage(Message m) throws SQLException {
        ajouter(m);
    }

    /**
     * Returns messages for a given group, ordered from oldest to newest.
     * Uses PreparedStatement (requested).
     */
    public List<Message> getMessagesByGroup(int groupId) throws SQLException {
        String sql = "select * from `message` where group_id = ? order by created_at asc, id asc";
        List<Message> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    @Override
    public void ajouter(Message entity) throws SQLException {
        String sql = "insert into `message` (content, created_at, sender_id, group_id) values(?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, entity.getContent());
            ps.setTimestamp(2, entity.getTimestamp());
            ps.setInt(3, entity.getUser_id());
            ps.setInt(4, entity.getGroup_id());
            ps.executeUpdate();
        }
    }

    @Override
    public void modifier(Message entity) throws SQLException {
        String sql = "update `message` set content = ?, created_at = ?, sender_id = ?, group_id = ? where id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, entity.getContent());
            ps.setTimestamp(2, entity.getTimestamp());
            ps.setInt(3, entity.getUser_id());
            ps.setInt(4, entity.getGroup_id());
            ps.setInt(5, entity.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "delete from `message` where id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Message> recuperer() throws SQLException {
        String sql = "select * from `message`";
        List<Message> list = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    private static Message mapRow(ResultSet rs) throws SQLException {
        Message entity = new Message();
        entity.setId(rs.getInt("id"));
        entity.setContent(rs.getString("content"));
        entity.setTimestamp(rs.getTimestamp("created_at"));
        entity.setUser_id(rs.getInt("sender_id"));
        entity.setGroup_id(rs.getInt("group_id"));
        return entity;
    }
}
