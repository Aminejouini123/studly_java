package models;

public class Message {
    private int id;
    private String content;
    private java.sql.Timestamp created_at;
    private int sender_id;
    private int group_id;

    public Message() {}

    public Message(String content, java.sql.Timestamp created_at, int sender_id, int group_id) {
        this.content = content;
        this.created_at = created_at;
        this.sender_id = sender_id;
        this.group_id = group_id;
    }

    public Message(int id, String content, java.sql.Timestamp created_at, int sender_id, int group_id) {
        this.id = id;
        this.content = content;
        this.created_at = created_at;
        this.sender_id = sender_id;
        this.group_id = group_id;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public java.sql.Timestamp getCreated_at() { return created_at; }
    public void setCreated_at(java.sql.Timestamp created_at) { this.created_at = created_at; }
    public int getSender_id() { return sender_id; }
    public void setSender_id(int sender_id) { this.sender_id = sender_id; }
    public int getGroup_id() { return group_id; }
    public void setGroup_id(int group_id) { this.group_id = group_id; }

    @Override
    public String toString() {
        return "Message{" +
                "id=" + id
 + ", content=" + content
 + ", created_at=" + created_at
 + ", sender_id=" + sender_id
 + ", group_id=" + group_id
                + '}';
    }
}
