package models;

import java.sql.Timestamp;

public class Message {
    private int id;
    private String content;
    private int user_id;
    private int group_id;
    private Timestamp timestamp;

    public Message() {}

    public Message(String content, Timestamp timestamp, int user_id, int group_id) {
        this.content = content;
        this.timestamp = timestamp;
        this.user_id = user_id;
        this.group_id = group_id;
    }

    public Message(int id, String content, Timestamp timestamp, int user_id, int group_id) {
        this.id = id;
        this.content = content;
        this.timestamp = timestamp;
        this.user_id = user_id;
        this.group_id = group_id;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public int getUser_id() { return user_id; }
    public void setUser_id(int user_id) { this.user_id = user_id; }
    public int getGroup_id() { return group_id; }
    public void setGroup_id(int group_id) { this.group_id = group_id; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    // Backward compatible aliases for existing codebase / DB column naming.
    public Timestamp getCreated_at() { return getTimestamp(); }
    public void setCreated_at(Timestamp created_at) { setTimestamp(created_at); }
    public int getSender_id() { return getUser_id(); }
    public void setSender_id(int sender_id) { setUser_id(sender_id); }

    @Override
    public String toString() {
        return "Message{" +
                "id=" + id
 + ", content=" + content
 + ", user_id=" + user_id
 + ", group_id=" + group_id
 + ", timestamp=" + timestamp
                + '}';
    }
}
