package models.quiz;

public final class QuizResult {
    private final int correct;
    private final int total;

    public QuizResult(int correct, int total) {
        this.correct = Math.max(0, correct);
        this.total = Math.max(0, total);
    }

    public int getCorrect() {
        return correct;
    }

    public int getTotal() {
        return total;
    }

    public int getPercentRounded() {
        if (total <= 0) return 0;
        return (int) Math.round((correct * 100.0) / total);
    }
}

