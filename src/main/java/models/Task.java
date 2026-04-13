package models;

public class Task {
    private int id;
    private String title;
    private String description;
    private int repeat_count;
    private String status;
    private int difficulty;
    private double impact;
    private java.sql.Timestamp deadline;
    private java.sql.Timestamp completed_at;
    private int objective_id;
    private int assigned_user_id;

    public Task() {}

    public Task(String title, String description, int repeat_count, String status, int difficulty, double impact, java.sql.Timestamp deadline, java.sql.Timestamp completed_at, int objective_id, int assigned_user_id) {
        this.title = title;
        this.description = description;
        this.repeat_count = repeat_count;
        this.status = status;
        this.difficulty = difficulty;
        this.impact = impact;
        this.deadline = deadline;
        this.completed_at = completed_at;
        this.objective_id = objective_id;
        this.assigned_user_id = assigned_user_id;
    }

    public Task(int id, String title, String description, int repeat_count, String status, int difficulty, double impact, java.sql.Timestamp deadline, java.sql.Timestamp completed_at, int objective_id, int assigned_user_id) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.repeat_count = repeat_count;
        this.status = status;
        this.difficulty = difficulty;
        this.impact = impact;
        this.deadline = deadline;
        this.completed_at = completed_at;
        this.objective_id = objective_id;
        this.assigned_user_id = assigned_user_id;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getRepeat_count() { return repeat_count; }
    public void setRepeat_count(int repeat_count) { this.repeat_count = repeat_count; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getDifficulty() { return difficulty; }
    public void setDifficulty(int difficulty) { this.difficulty = difficulty; }
    public double getImpact() { return impact; }
    public void setImpact(double impact) { this.impact = impact; }
    public java.sql.Timestamp getDeadline() { return deadline; }
    public void setDeadline(java.sql.Timestamp deadline) { this.deadline = deadline; }
    public java.sql.Timestamp getCompleted_at() { return completed_at; }
    public void setCompleted_at(java.sql.Timestamp completed_at) { this.completed_at = completed_at; }
    public int getObjective_id() { return objective_id; }
    public void setObjective_id(int objective_id) { this.objective_id = objective_id; }
    public int getAssigned_user_id() { return assigned_user_id; }
    public void setAssigned_user_id(int assigned_user_id) { this.assigned_user_id = assigned_user_id; }

    @Override
    public String toString() {
        return "Task{" +
                "id=" + id
 + ", title=" + title
 + ", description=" + description
 + ", repeat_count=" + repeat_count
 + ", status=" + status
 + ", difficulty=" + difficulty
 + ", impact=" + impact
 + ", deadline=" + deadline
 + ", completed_at=" + completed_at
 + ", objective_id=" + objective_id
 + ", assigned_user_id=" + assigned_user_id
                + '}';
    }
}
