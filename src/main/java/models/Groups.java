package models;

public class Groups {
    private int id;
    private int capacity;
    private String group_photo;
    private String category;
    private java.sql.Timestamp created_at;
    private int creator_id;

    public Groups() {}

    public Groups(int capacity, String group_photo, String category, java.sql.Timestamp created_at, int creator_id) {
        this.capacity = capacity;
        this.group_photo = group_photo;
        this.category = category;
        this.created_at = created_at;
        this.creator_id = creator_id;
    }

    public Groups(int id, int capacity, String group_photo, String category, java.sql.Timestamp created_at, int creator_id) {
        this.id = id;
        this.capacity = capacity;
        this.group_photo = group_photo;
        this.category = category;
        this.created_at = created_at;
        this.creator_id = creator_id;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public String getGroup_photo() { return group_photo; }
    public void setGroup_photo(String group_photo) { this.group_photo = group_photo; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public java.sql.Timestamp getCreated_at() { return created_at; }
    public void setCreated_at(java.sql.Timestamp created_at) { this.created_at = created_at; }
    public int getCreator_id() { return creator_id; }
    public void setCreator_id(int creator_id) { this.creator_id = creator_id; }

    @Override
    public String toString() {
        return "Groups{" +
                "id=" + id
 + ", capacity=" + capacity
 + ", group_photo=" + group_photo
 + ", category=" + category
 + ", created_at=" + created_at
 + ", creator_id=" + creator_id
                + '}';
    }
}
