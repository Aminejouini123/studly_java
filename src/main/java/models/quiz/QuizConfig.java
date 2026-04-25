package models.quiz;

public final class QuizConfig {
    public enum QuestionType { MCQ, TRUE_FALSE, SHORT_ANSWER }
    public enum Difficulty { EASY, MEDIUM, HARD }

    private final String activityName;
    private final int numberOfQuestions;
    private final QuestionType questionType;
    private final Difficulty difficulty;
    private final String topic;

    public QuizConfig(String activityName,
                      int numberOfQuestions,
                      QuestionType questionType,
                      Difficulty difficulty,
                      String topic) {
        this.activityName = activityName == null ? "" : activityName.trim();
        this.numberOfQuestions = numberOfQuestions;
        this.questionType = questionType == null ? QuestionType.MCQ : questionType;
        this.difficulty = difficulty == null ? Difficulty.MEDIUM : difficulty;
        this.topic = topic == null ? "" : topic.trim();
    }

    public String getActivityName() {
        return activityName;
    }

    public int getNumberOfQuestions() {
        return numberOfQuestions;
    }

    public QuestionType getQuestionType() {
        return questionType;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public String getTopic() {
        return topic;
    }
}

