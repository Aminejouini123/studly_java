package models;

public class Invitation {
    private int id;
    private String status;
    private java.sql.Timestamp created_at;
    private int sender_id;
    private int receiver_id;
    private int group_id;

    public Invitation() {}

    public Invitation(String status, java.sql.Timestamp created_at, int sender_id, int receiver_id, int group_id) {
        this.status = status;
        this.created_at = created_at;
        this.sender_id = sender_id;
        this.receiver_id = receiver_id;
        this.group_id = group_id;
    }

    public Invitation(int id, String status, java.sql.Timestamp created_at, int sender_id, int receiver_id, int group_id) {
        this.id = id;
        this.status = status;
        this.created_at = created_at;
        this.sender_id = sender_id;
        this.receiver_id = receiver_id;
        this.group_id = group_id;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public java.sql.Timestamp getCreated_at() { return created_at; }
    public void setCreated_at(java.sql.Timestamp created_at) { this.created_at = created_at; }
    public int getSender_id() { return sender_id; }
    public void setSender_id(int sender_id) { this.sender_id = sender_id; }
    public int getReceiver_id() { return receiver_id; }
    public void setReceiver_id(int receiver_id) { this.receiver_id = receiver_id; }
    public int getGroup_id() { return group_id; }
    public void setGroup_id(int group_id) { this.group_id = group_id; }

    @Override
    public String toString() {
        return "Invitation{" +
                "id=" + id
 + ", status=" + status
 + ", created_at=" + created_at
 + ", sender_id=" + sender_id
 + ", receiver_id=" + receiver_id
 + ", group_id=" + group_id
                + '}';
    }
}
