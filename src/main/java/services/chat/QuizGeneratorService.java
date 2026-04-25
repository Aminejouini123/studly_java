package services.chat;

import models.quiz.QuizConfig;
import models.quiz.QuizQuestion;
import utils.PdfTextExtractor;
import utils.JsonUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates a quiz from a course PDF using OpenRouter.
 */
public final class QuizGeneratorService {
    private final ChatService chatService;

    public QuizGeneratorService() {
        this(new ChatService());
    }

    public QuizGeneratorService(ChatService chatService) {
        this.chatService = chatService;
    }

    public boolean isConfigured() {
        return chatService.hasApiKeyConfigured();
    }

    public String serializeQuestions(List<QuizQuestion> questions) throws Exception {
        List<Map<String, Object>> payload = new ArrayList<>();
        if (questions != null) {
            for (QuizQuestion q : questions) {
                if (q == null) continue;
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("type", q.getType() == null ? QuizConfig.QuestionType.MCQ.name() : q.getType().name());
                item.put("prompt", q.getPrompt());
                item.put("options", q.getOptions() == null ? List.of() : q.getOptions());
                item.put("correctAnswer", q.getCorrectAnswer());
                item.put("explanation", q.getExplanation());
                payload.add(item);
            }
        }
        return JsonUtils.stringify(payload);
    }

    public List<QuizQuestion> deserializeQuestions(String rawJson) throws Exception {
        return parseQuestions(rawJson, QuizConfig.QuestionType.MCQ, Integer.MAX_VALUE);
    }

    public List<QuizQuestion> generateQuizFromPdf(File pdf, String courseName, QuizConfig config) throws Exception {
        String text = PdfTextExtractor.extractText(pdf, 35_000);
        if (text.isBlank()) {
            throw new IllegalStateException(
                    "I couldn’t extract text from this PDF. It looks like a scanned PDF (images). " +
                    "To generate a quiz from scanned PDFs, we need OCR (e.g., Tesseract)."
            );
        }

        QuizConfig cfg = config == null
                ? new QuizConfig("Quiz", 10, QuizConfig.QuestionType.MCQ, QuizConfig.Difficulty.MEDIUM, "")
                : config;

        String systemPrompt =
                "You are Studly AI, an education assistant.\n" +
                "Generate quizzes only about the provided course material.\n" +
                "STRICT REQUIREMENT: You MUST generate questions ONLY of the requested type. If TRUE_FALSE is requested, do not generate MCQ.\n" +
                "Language: match the language of the content.\n" +
                "Be precise and avoid unrelated topics.\n" +
                "Return ONLY valid JSON. No markdown. No code fences.";

        String title = (courseName == null || courseName.isBlank()) ? "this course" : courseName.trim();
        String topic = cfg.getTopic() == null || cfg.getTopic().isBlank() ? title : cfg.getTopic().trim();
        int n = Math.max(1, Math.min(30, cfg.getNumberOfQuestions()));
        String qType = cfg.getQuestionType().name();
        String diff = cfg.getDifficulty().name();

        String userPrompt =
                "Create a quiz for topic: " + topic + "\n" +
                "Constraints:\n" +
                "- numberOfQuestions: " + n + "\n" +
                "- questionType: " + qType + " (MCQ | TRUE_FALSE | SHORT_ANSWER)\n" +
                "- difficulty: " + diff + " (EASY | MEDIUM | HARD)\n" +
                "\n" +
                "Return JSON with this exact schema:\n" +
                "{\n" +
                "  \"title\": string,\n" +
                "  \"questions\": [\n" +
                "    {\n" +
                "      \"type\": \"MCQ\"|\"TRUE_FALSE\"|\"SHORT_ANSWER\",\n" +
                "      \"prompt\": string,\n" +
                "      \"options\": [string],\n" +
                "      \"correctAnswer\": string,\n" +
                "      \"explanation\": string\n" +
                "    }\n" +
                "  ]\n" +
                "}\n" +
                "Rules:\n" +
                "- ALL questions in the 'questions' array MUST have type=\"" + qType + "\".\n" +
                "- For MCQ: options MUST be 4 items, correctAnswer MUST equal one of the options.\n" +
                "- For TRUE_FALSE: options MUST be [\"True\",\"False\"] and correctAnswer one of them.\n" +
                "- For SHORT_ANSWER: options MUST be [] and correctAnswer a short expected answer.\n" +
                "- explanation: 1 short line.\n" +
                "- DO NOT mix types. If the requested type is " + qType + ", every single question must be " + qType + ".\n" +
                "\n" +
                "PDF content:\n" + text;

        String raw = chatService.chat(systemPrompt, userPrompt);
        return parseQuestions(raw, cfg.getQuestionType(), n);
    }

    private List<QuizQuestion> parseQuestions(String rawJson, QuizConfig.QuestionType expectedType, int n) throws Exception {
        Object parsed = JsonUtils.parse(rawJson == null ? "" : rawJson);
        List<Object> qs;
        if (parsed instanceof Map<?, ?>) {
            Object questionsNode = ((Map<?, ?>) parsed).get("questions");
            qs = JsonUtils.asArray(questionsNode);
        } else if (parsed instanceof List<?>) {
            qs = JsonUtils.asArray(parsed);
        } else {
            qs = List.of();
        }
        if (qs.isEmpty()) {
            throw new IllegalStateException("Invalid quiz JSON: missing questions[]");
        }
        List<QuizQuestion> out = new ArrayList<>();
        for (Object item : qs) {
            Map<String, Object> q = JsonUtils.asObject(item);
            String typeStr = JsonUtils.asString(q.get("type"));
            QuizConfig.QuestionType type = parseType(typeStr, expectedType);
            String prompt = JsonUtils.asString(q.get("prompt"));
            List<String> options = new ArrayList<>();
            for (Object opt : JsonUtils.asArray(q.get("options"))) {
                options.add(JsonUtils.asString(opt));
            }
            String correct = JsonUtils.asString(q.get("correctAnswer"));
            String expl = JsonUtils.asString(q.get("explanation"));
            if (prompt.isBlank()) continue;

            // normalize constraints
            List<String> normalizedOptions;
            if (type == QuizConfig.QuestionType.MCQ) {
                if (options.size() != 4) continue;
                final String correctFinal = correct;
                if (options.stream().noneMatch(o -> o.equalsIgnoreCase(correctFinal))) continue;
                normalizedOptions = List.copyOf(options);
            } else if (type == QuizConfig.QuestionType.TRUE_FALSE) {
                normalizedOptions = List.of("True", "False");
                if (!"true".equalsIgnoreCase(correct) && !"false".equalsIgnoreCase(correct)) continue;
                correct = "true".equalsIgnoreCase(correct) ? "True" : "False";
            } else {
                normalizedOptions = List.of();
            }

            out.add(new QuizQuestion(type, prompt, normalizedOptions, correct, expl));
            if (out.size() >= n) break;
        }
        if (out.isEmpty()) {
            throw new IllegalStateException("Quiz generation returned no usable questions.");
        }
        return out;
    }

    private static QuizConfig.QuestionType parseType(String s, QuizConfig.QuestionType fallback) {
        if (s == null) return fallback;
        String t = s.trim().toUpperCase();
        if (t.equals("MCQ")) return QuizConfig.QuestionType.MCQ;
        if (t.equals("TRUE_FALSE") || t.equals("TRUE/FALSE") || t.equals("TRUEFALSE")) return QuizConfig.QuestionType.TRUE_FALSE;
        if (t.equals("SHORT_ANSWER") || t.equals("SHORTANSWER")) return QuizConfig.QuestionType.SHORT_ANSWER;
        return fallback;
    }
}

