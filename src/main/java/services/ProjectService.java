package services;

import models.Project;
import utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProjectService implements IService<Project> {
    private final Connection connection;
    public ProjectService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    /**
     * Returns projects for a given group.
     * Uses PreparedStatement (requested).
     */
    public List<Project> getProjectsByGroup(int groupId) throws SQLException {
        String sql = "select * from `project` where group_id = ? order by deadline asc, id asc";
        List<Project> list = new ArrayList<>();
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
    public void ajouter(Project entity) throws SQLException {
        String sql = "insert into `project` (title, description, status, resource, deadline, type, group_id) values(?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, entity.getTitle());
            ps.setString(2, entity.getDescription());
            ps.setString(3, entity.getStatus());
            ps.setString(4, entity.getResource());
            ps.setDate(5, entity.getDeadline());
            ps.setString(6, entity.getType());
            ps.setInt(7, entity.getGroup_id());
            ps.executeUpdate();
        }
    }

    @Override
    public void modifier(Project entity) throws SQLException {
        String sql = "update `project` set title = ?, description = ?, status = ?, resource = ?, deadline = ?, type = ?, group_id = ? where id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, entity.getTitle());
            ps.setString(2, entity.getDescription());
            ps.setString(3, entity.getStatus());
            ps.setString(4, entity.getResource());
            ps.setDate(5, entity.getDeadline());
            ps.setString(6, entity.getType());
            ps.setInt(7, entity.getGroup_id());
            ps.setInt(8, entity.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "delete from `project` where id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Project> recuperer() throws SQLException {
        String sql = "select * from `project`";
        List<Project> list = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    private static Project mapRow(ResultSet rs) throws SQLException {
        Project entity = new Project();
        entity.setId(rs.getInt("id"));
        entity.setTitle(rs.getString("title"));
        entity.setDescription(rs.getString("description"));
        entity.setStatus(rs.getString("status"));
        entity.setResource(rs.getString("resource"));
        entity.setDeadline(rs.getDate("deadline"));
        entity.setType(rs.getString("type"));
        entity.setGroup_id(rs.getInt("group_id"));
        return entity;
    }
}
