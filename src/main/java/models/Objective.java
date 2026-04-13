package models;

public class Objective {
    private int id;
    private String title;
    private String description;
    private String estimated_duration;
    private int real_duration;
    private String priority;
    private String status;
    private String reason;

    public Objective() {}

    public Objective(String title, String description, String estimated_duration, int real_duration, String priority, String status, String reason) {
        this.title = title;
        this.description = description;
        this.estimated_duration = estimated_duration;
        this.real_duration = real_duration;
        this.priority = priority;
        this.status = status;
        this.reason = reason;
    }

    public Objective(int id, String title, String description, String estimated_duration, int real_duration, String priority, String status, String reason) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.estimated_duration = estimated_duration;
        this.real_duration = real_duration;
        this.priority = priority;
        this.status = status;
        this.reason = reason;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getEstimated_duration() { return estimated_duration; }
    public void setEstimated_duration(String estimated_duration) { this.estimated_duration = estimated_duration; }
    public int getReal_duration() { return real_duration; }
    public void setReal_duration(int real_duration) { this.real_duration = real_duration; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    @Override
    public String toString() {
        return "Objective{" +
                "id=" + id
 + ", title=" + title
 + ", description=" + description
 + ", estimated_duration=" + estimated_duration
 + ", real_duration=" + real_duration
 + ", priority=" + priority
 + ", status=" + status
 + ", reason=" + reason
                + '}';
    }
}
