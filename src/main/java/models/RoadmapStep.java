package models;

import java.util.List;

public class RoadmapStep {

    private int            id;
    private int            stepNumber;
    private String         title;
    private String         description;
    private List<Resource> resources;
    private boolean        completed;

    public RoadmapStep() {}

    public int            getId()           { return id; }
    public void           setId(int id)     { this.id = id; }
    public int            getStepNumber()   { return stepNumber; }
    public void           setStepNumber(int stepNumber) { this.stepNumber = stepNumber; }
    public String         getTitle()        { return title; }
    public void           setTitle(String title) { this.title = title; }
    public String         getDescription()  { return description; }
    public void           setDescription(String description) { this.description = description; }
    public List<Resource> getResources()    { return resources; }
    public void           setResources(List<Resource> resources) { this.resources = resources; }
    public boolean        isCompleted()     { return completed; }
    public void           setCompleted(boolean completed) { this.completed = completed; }
}
