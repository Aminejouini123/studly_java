package services;

import models.ProjectTask;
import utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProjectTaskService implements IService<ProjectTask> {
    private Connection connection;
    public ProjectTaskService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(ProjectTask entity) throws SQLException {
        String sql = "insert into `project_task` (title, description, status, deadline, completed_at, deliverable, grade, attachment, resource_path, project_id, assigned_user_id) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, entity.getTitle());
        ps.setString(2, entity.getDescription());
        ps.setString(3, entity.getStatus());
        ps.setTimestamp(4, entity.getDeadline());
        ps.setTimestamp(5, entity.getCompleted_at());
        ps.setString(6, entity.getDeliverable());
        ps.setInt(7, entity.getGrade());
        ps.setString(8, entity.getAttachment());
        ps.setString(9, entity.getResource_path());
        ps.setInt(10, entity.getProject_id());
        ps.setInt(11, entity.getAssigned_user_id());
        ps.executeUpdate();
    }

    @Override
    public void modifier(ProjectTask entity) throws SQLException {
        String sql = "update `project_task` set title = ?, description = ?, status = ?, deadline = ?, completed_at = ?, deliverable = ?, grade = ?, attachment = ?, resource_path = ?, project_id = ?, assigned_user_id = ? where id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, entity.getTitle());
        ps.setString(2, entity.getDescription());
        ps.setString(3, entity.getStatus());
        ps.setTimestamp(4, entity.getDeadline());
        ps.setTimestamp(5, entity.getCompleted_at());
        ps.setString(6, entity.getDeliverable());
        ps.setInt(7, entity.getGrade());
        ps.setString(8, entity.getAttachment());
        ps.setString(9, entity.getResource_path());
        ps.setInt(10, entity.getProject_id());
        ps.setInt(11, entity.getAssigned_user_id());
        ps.setInt(12, entity.getId());
        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "delete from `project_task` where id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<ProjectTask> recuperer() throws SQLException {
        String sql = "select * from `project_task`";
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sql);
        List<ProjectTask> list = new ArrayList<>();
        while (rs.next()) {
            ProjectTask entity = new ProjectTask();
            entity.setId(rs.getInt("id"));
            entity.setTitle(rs.getString("title"));
            entity.setDescription(rs.getString("description"));
            entity.setStatus(rs.getString("status"));
            entity.setDeadline(rs.getTimestamp("deadline"));
            entity.setCompleted_at(rs.getTimestamp("completed_at"));
            entity.setDeliverable(rs.getString("deliverable"));
            entity.setGrade(rs.getInt("grade"));
            entity.setAttachment(rs.getString("attachment"));
            entity.setResource_path(rs.getString("resource_path"));
            entity.setProject_id(rs.getInt("project_id"));
            entity.setAssigned_user_id(rs.getInt("assigned_user_id"));
            list.add(entity);
        }
        return list;
    }
}
