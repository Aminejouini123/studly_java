package models;

public class Project {
    private int id;
    private String title;
    private String description;
    private String status;
    private String resource;
    private java.sql.Date deadline;
    private String type;
    private int group_id;

    public Project() {}

    public Project(String title, String description, String status, String resource, java.sql.Date deadline, String type, int group_id) {
        this.title = title;
        this.description = description;
        this.status = status;
        this.resource = resource;
        this.deadline = deadline;
        this.type = type;
        this.group_id = group_id;
    }

    public Project(int id, String title, String description, String status, String resource, java.sql.Date deadline, String type, int group_id) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.resource = resource;
        this.deadline = deadline;
        this.type = type;
        this.group_id = group_id;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getResource() { return resource; }
    public void setResource(String resource) { this.resource = resource; }
    public java.sql.Date getDeadline() { return deadline; }
    public void setDeadline(java.sql.Date deadline) { this.deadline = deadline; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public int getGroup_id() { return group_id; }
    public void setGroup_id(int group_id) { this.group_id = group_id; }

    @Override
    public String toString() {
        return "Project{" +
                "id=" + id
 + ", title=" + title
 + ", description=" + description
 + ", status=" + status
 + ", resource=" + resource
 + ", deadline=" + deadline
 + ", type=" + type
 + ", group_id=" + group_id
                + '}';
    }
}
