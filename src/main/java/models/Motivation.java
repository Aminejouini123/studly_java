package models;

public class Motivation {
    private int id;
    private int motivation_level;
    private String emotion;
    private String preparation;
    private String reward;
    private int user_id;

    public Motivation() {}

    public Motivation(int motivation_level, String emotion, String preparation, String reward, int user_id) {
        this.motivation_level = motivation_level;
        this.emotion = emotion;
        this.preparation = preparation;
        this.reward = reward;
        this.user_id = user_id;
    }

    public Motivation(int id, int motivation_level, String emotion, String preparation, String reward, int user_id) {
        this.id = id;
        this.motivation_level = motivation_level;
        this.emotion = emotion;
        this.preparation = preparation;
        this.reward = reward;
        this.user_id = user_id;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getMotivation_level() { return motivation_level; }
    public void setMotivation_level(int motivation_level) { this.motivation_level = motivation_level; }
    public String getEmotion() { return emotion; }
    public void setEmotion(String emotion) { this.emotion = emotion; }
    public String getPreparation() { return preparation; }
    public void setPreparation(String preparation) { this.preparation = preparation; }
    public String getReward() { return reward; }
    public void setReward(String reward) { this.reward = reward; }
    public int getUser_id() { return user_id; }
    public void setUser_id(int user_id) { this.user_id = user_id; }

    @Override
    public String toString() {
        return "Motivation{" +
                "id=" + id
 + ", motivation_level=" + motivation_level
 + ", emotion=" + emotion
 + ", preparation=" + preparation
 + ", reward=" + reward
 + ", user_id=" + user_id
                + '}';
    }
}
