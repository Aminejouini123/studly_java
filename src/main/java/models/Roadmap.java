package models;

import java.sql.Timestamp;
import java.util.List;

public class Roadmap {

    private int               id;
    private String            skill;
    private Timestamp         createdAt;
    private int               userId;
    private List<RoadmapStep> steps;

    public Roadmap() {}

    public Roadmap(String skill, int userId, List<RoadmapStep> steps) {
        this.skill  = skill;
        this.userId = userId;
        this.steps  = steps;
    }

    public int               getId()        { return id; }
    public void              setId(int id)  { this.id = id; }
    public String            getSkill()     { return skill; }
    public void              setSkill(String skill) { this.skill = skill; }
    public Timestamp         getCreatedAt() { return createdAt; }
    public void              setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public int               getUserId()    { return userId; }
    public void              setUserId(int userId) { this.userId = userId; }
    public List<RoadmapStep> getSteps()     { return steps; }
    public void              setSteps(List<RoadmapStep> steps) { this.steps = steps; }
}
