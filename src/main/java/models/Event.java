package models;

public class Event {
    private int id;
    private String title;
    private String description;
    private String type;
    private int duration;
    private String location;
    private String status;
    private String priority;
    private int difficulty;
    private java.sql.Date date;
    private java.sql.Timestamp start_time;
    private java.sql.Timestamp end_time;
    private String color;
    private String category;
    private String notes;
    private int all_day;
    private int reminder_minutes;
    private String google_event_id;
    private int motivation_id;
    private int user_id;

    public Event() {}

    public Event(String title, String description, String type, int duration, String location, String status, String priority, int difficulty, java.sql.Date date, java.sql.Timestamp start_time, java.sql.Timestamp end_time, String color, String category, String notes, int all_day, int reminder_minutes, String google_event_id, int motivation_id, int user_id) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.duration = duration;
        this.location = location;
        this.status = status;
        this.priority = priority;
        this.difficulty = difficulty;
        this.date = date;
        this.start_time = start_time;
        this.end_time = end_time;
        this.color = color;
        this.category = category;
        this.notes = notes;
        this.all_day = all_day;
        this.reminder_minutes = reminder_minutes;
        this.google_event_id = google_event_id;
        this.motivation_id = motivation_id;
        this.user_id = user_id;
    }

    public Event(int id, String title, String description, String type, int duration, String location, String status, String priority, int difficulty, java.sql.Date date, java.sql.Timestamp start_time, java.sql.Timestamp end_time, String color, String category, String notes, int all_day, int reminder_minutes, String google_event_id, int motivation_id, int user_id) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.type = type;
        this.duration = duration;
        this.location = location;
        this.status = status;
        this.priority = priority;
        this.difficulty = difficulty;
        this.date = date;
        this.start_time = start_time;
        this.end_time = end_time;
        this.color = color;
        this.category = category;
        this.notes = notes;
        this.all_day = all_day;
        this.reminder_minutes = reminder_minutes;
        this.google_event_id = google_event_id;
        this.motivation_id = motivation_id;
        this.user_id = user_id;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public int getDifficulty() { return difficulty; }
    public void setDifficulty(int difficulty) { this.difficulty = difficulty; }
    public java.sql.Date getDate() { return date; }
    public void setDate(java.sql.Date date) { this.date = date; }
    public java.sql.Timestamp getStart_time() { return start_time; }
    public void setStart_time(java.sql.Timestamp start_time) { this.start_time = start_time; }
    public java.sql.Timestamp getEnd_time() { return end_time; }
    public void setEnd_time(java.sql.Timestamp end_time) { this.end_time = end_time; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public int getAll_day() { return all_day; }
    public void setAll_day(int all_day) { this.all_day = all_day; }
    public int getReminder_minutes() { return reminder_minutes; }
    public void setReminder_minutes(int reminder_minutes) { this.reminder_minutes = reminder_minutes; }
    public String getGoogle_event_id() { return google_event_id; }
    public void setGoogle_event_id(String google_event_id) { this.google_event_id = google_event_id; }
    public int getMotivation_id() { return motivation_id; }
    public void setMotivation_id(int motivation_id) { this.motivation_id = motivation_id; }
    public int getUser_id() { return user_id; }
    public void setUser_id(int user_id) { this.user_id = user_id; }

    @Override
    public String toString() {
        return "Event{" +
                "id=" + id
 + ", title=" + title
 + ", description=" + description
 + ", type=" + type
 + ", duration=" + duration
 + ", location=" + location
 + ", status=" + status
 + ", priority=" + priority
 + ", difficulty=" + difficulty
 + ", date=" + date
 + ", start_time=" + start_time
 + ", end_time=" + end_time
 + ", color=" + color
 + ", category=" + category
 + ", notes=" + notes
 + ", all_day=" + all_day
 + ", reminder_minutes=" + reminder_minutes
 + ", google_event_id=" + google_event_id
 + ", motivation_id=" + motivation_id
 + ", user_id=" + user_id
                + '}';
    }
}
