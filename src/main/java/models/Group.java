package models;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.sql.Timestamp;

public class Group {
    private final IntegerProperty id = new SimpleIntegerProperty(this, "id");
    private final IntegerProperty capacity = new SimpleIntegerProperty(this, "capacity");
    private final StringProperty groupPhoto = new SimpleStringProperty(this, "groupPhoto");
    private final StringProperty category = new SimpleStringProperty(this, "category");
    private final IntegerProperty creatorId = new SimpleIntegerProperty(this, "creatorId");
    private final ObjectProperty<Timestamp> createdAt = new SimpleObjectProperty<>(this, "createdAt");

    public Group() {
    }

    public Group(int id, int capacity, String groupPhoto, String category, int creatorId) {
        setId(id);
        setCapacity(capacity);
        setGroupPhoto(groupPhoto);
        setCategory(category);
        setCreatorId(creatorId);
    }

    public Group(int capacity, String groupPhoto, String category, int creatorId) {
        setCapacity(capacity);
        setGroupPhoto(groupPhoto);
        setCategory(category);
        setCreatorId(creatorId);
    }

    public Group(int id, int capacity, String groupPhoto, String category, int creatorId, Timestamp createdAt) {
        this(id, capacity, groupPhoto, category, creatorId);
        setCreatedAt(createdAt);
    }

    public int getId() {
        return id.get();
    }

    public IntegerProperty idProperty() {
        return id;
    }

    public void setId(int id) {
        this.id.set(id);
    }

    public int getCapacity() {
        return capacity.get();
    }

    public IntegerProperty capacityProperty() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity.set(capacity);
    }

    public String getGroupPhoto() {
        return groupPhoto.get();
    }

    public StringProperty groupPhotoProperty() {
        return groupPhoto;
    }

    public void setGroupPhoto(String groupPhoto) {
        this.groupPhoto.set(groupPhoto);
    }

    public String getCategory() {
        return category.get();
    }

    public StringProperty categoryProperty() {
        return category;
    }

    public void setCategory(String category) {
        this.category.set(category);
    }

    public int getCreatorId() {
        return creatorId.get();
    }

    public IntegerProperty creatorIdProperty() {
        return creatorId;
    }

    public void setCreatorId(int creatorId) {
        this.creatorId.set(creatorId);
    }

    public Timestamp getCreatedAt() {
        return createdAt.get();
    }

    public ObjectProperty<Timestamp> createdAtProperty() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt.set(createdAt);
    }

    @Override
    public String toString() {
        return "Group{" +
                "id=" + getId() +
                ", capacity=" + getCapacity() +
                ", groupPhoto='" + getGroupPhoto() + '\'' +
                ", category='" + getCategory() + '\'' +
                ", creatorId=" + getCreatorId() +
                ", createdAt=" + getCreatedAt() +
                '}';
    }
}
