package models;

public class ProjectTask {
    private int id;
    private String title;
    private String description;
    private String status;
    private java.sql.Timestamp deadline;
    private java.sql.Timestamp completed_at;
    private String deliverable;
    private int grade;
    private String attachment;
    private String resource_path;
    private int project_id;
    private int assigned_user_id;

    public ProjectTask() {}

    public ProjectTask(String title, String description, String status, java.sql.Timestamp deadline, java.sql.Timestamp completed_at, String deliverable, int grade, String attachment, String resource_path, int project_id, int assigned_user_id) {
        this.title = title;
        this.description = description;
        this.status = status;
        this.deadline = deadline;
        this.completed_at = completed_at;
        this.deliverable = deliverable;
        this.grade = grade;
        this.attachment = attachment;
        this.resource_path = resource_path;
        this.project_id = project_id;
        this.assigned_user_id = assigned_user_id;
    }

    public ProjectTask(int id, String title, String description, String status, java.sql.Timestamp deadline, java.sql.Timestamp completed_at, String deliverable, int grade, String attachment, String resource_path, int project_id, int assigned_user_id) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.deadline = deadline;
        this.completed_at = completed_at;
        this.deliverable = deliverable;
        this.grade = grade;
        this.attachment = attachment;
        this.resource_path = resource_path;
        this.project_id = project_id;
        this.assigned_user_id = assigned_user_id;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public java.sql.Timestamp getDeadline() { return deadline; }
    public void setDeadline(java.sql.Timestamp deadline) { this.deadline = deadline; }
    public java.sql.Timestamp getCompleted_at() { return completed_at; }
    public void setCompleted_at(java.sql.Timestamp completed_at) { this.completed_at = completed_at; }
    public String getDeliverable() { return deliverable; }
    public void setDeliverable(String deliverable) { this.deliverable = deliverable; }
    public int getGrade() { return grade; }
    public void setGrade(int grade) { this.grade = grade; }
    public String getAttachment() { return attachment; }
    public void setAttachment(String attachment) { this.attachment = attachment; }
    public String getResource_path() { return resource_path; }
    public void setResource_path(String resource_path) { this.resource_path = resource_path; }
    public int getProject_id() { return project_id; }
    public void setProject_id(int project_id) { this.project_id = project_id; }
    public int getAssigned_user_id() { return assigned_user_id; }
    public void setAssigned_user_id(int assigned_user_id) { this.assigned_user_id = assigned_user_id; }

    @Override
    public String toString() {
        return "ProjectTask{" +
                "id=" + id
 + ", title=" + title
 + ", description=" + description
 + ", status=" + status
 + ", deadline=" + deadline
 + ", completed_at=" + completed_at
 + ", deliverable=" + deliverable
 + ", grade=" + grade
 + ", attachment=" + attachment
 + ", resource_path=" + resource_path
 + ", project_id=" + project_id
 + ", assigned_user_id=" + assigned_user_id
                + '}';
    }
}
