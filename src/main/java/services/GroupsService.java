package services;

import models.Groups;
import utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GroupsService implements IService<Groups> {
    private Connection connection;
    public GroupsService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Groups entity) throws SQLException {
        String sql = "insert into `groups` (capacity, group_photo, category, created_at, creator_id) values(?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, entity.getCapacity());
        ps.setString(2, entity.getGroup_photo());
        ps.setString(3, entity.getCategory());
        ps.setTimestamp(4, entity.getCreated_at());
        ps.setInt(5, entity.getCreator_id());
        ps.executeUpdate();
    }

    @Override
    public void modifier(Groups entity) throws SQLException {
        String sql = "update `groups` set capacity = ?, group_photo = ?, category = ?, created_at = ?, creator_id = ? where id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, entity.getCapacity());
        ps.setString(2, entity.getGroup_photo());
        ps.setString(3, entity.getCategory());
        ps.setTimestamp(4, entity.getCreated_at());
        ps.setInt(5, entity.getCreator_id());
        ps.setInt(6, entity.getId());
        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "delete from `groups` where id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<Groups> recuperer() throws SQLException {
        String sql = "select * from `groups`";
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sql);
        List<Groups> list = new ArrayList<>();
        while (rs.next()) {
            Groups entity = new Groups();
            entity.setId(rs.getInt("id"));
            entity.setCapacity(rs.getInt("capacity"));
            entity.setGroup_photo(rs.getString("group_photo"));
            entity.setCategory(rs.getString("category"));
            entity.setCreated_at(rs.getTimestamp("created_at"));
            entity.setCreator_id(rs.getInt("creator_id"));
            list.add(entity);
        }
        return list;
    }
}
