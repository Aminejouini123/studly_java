package models.quiz;

import java.util.Collections;
import java.util.List;

public final class QuizQuestion {
    private QuizConfig.QuestionType type;
    private String prompt;
    private List<String> options;
    private String correctAnswer;
    private String explanation;

    public QuizQuestion() {}

    public QuizQuestion(QuizConfig.QuestionType type,
                        String prompt,
                        List<String> options,
                        String correctAnswer,
                        String explanation) {
        this.type = type == null ? QuizConfig.QuestionType.MCQ : type;
        this.prompt = prompt == null ? "" : prompt.trim();
        this.options = options == null ? Collections.emptyList() : List.copyOf(options);
        this.correctAnswer = correctAnswer == null ? "" : correctAnswer.trim();
        this.explanation = explanation == null ? "" : explanation.trim();
    }

    public QuizConfig.QuestionType getType() {
        return type;
    }

    public String getPrompt() {
        return prompt;
    }

    public List<String> getOptions() {
        return options;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public String getExplanation() {
        return explanation;
    }
}

