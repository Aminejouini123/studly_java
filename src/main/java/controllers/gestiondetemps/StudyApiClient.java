package controllers.gestiondetemps;

import models.Event;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.Duration;
import java.time.LocalDate;

public class StudyApiClient {

    private static final String API_BASE = "http://127.0.0.1:8000";
    
    private HttpClient createClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public JSONArray getLearningStyleQuestions() throws Exception {
        JSONObject response = sendGet("/study/quiz/learning-style/questions");
        if (response.has("questions") && response.optJSONArray("questions") != null) {
            return response.getJSONArray("questions");
        }
        if (response.has("items") && response.optJSONArray("items") != null) {
            return response.getJSONArray("items");
        }
        return new JSONArray();
    }

    public JSONObject analyzeLearningStyle(Event event, JSONArray answers) throws Exception {
        return sendPost("/study/quiz/learning-style/analyze", new JSONObject()
                .put("event_info", buildEventInfo(event))
                .put("answers", answers));
    }

    public JSONObject getLevelQuestion(String subject, String difficulty) throws Exception {
        String encodedSubject = URLEncoder.encode(subject, StandardCharsets.UTF_8);
        String encodedDifficulty = URLEncoder.encode(difficulty, StandardCharsets.UTF_8);
        return sendGet("/study/quiz/level/question?subject=" + encodedSubject + "&difficulty=" + encodedDifficulty);
    }

    public JSONObject estimateLevel(Event event, JSONArray answers) throws Exception {
        return sendPost("/study/quiz/level/estimate", new JSONObject()
                .put("event_info", buildEventInfo(event))
                .put("answers", answers));
    }

    public JSONObject generateStudyPlan(Event event, String learningStyle, String level) throws Exception {
        return sendPost("/study/plan/generate", new JSONObject()
                .put("event_info", buildEventInfo(event))
                .put("learning_style", learningStyle)
                .put("level", level));
    }

    private JSONObject sendGet(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE + path))
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        HttpClient client = createClient();
        return parseResponse(client.send(request, HttpResponse.BodyHandlers.ofString()), path);
    }

    private JSONObject sendPost(String path, JSONObject payload) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE + path))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();
        HttpClient client = createClient();
        return parseResponse(client.send(request, HttpResponse.BodyHandlers.ofString()), path);
    }

    private JSONObject parseResponse(HttpResponse<String> response, String path) throws Exception {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + response.statusCode() + " recu depuis " + path + "\n" + response.body());
        }

        String body = response.body() == null ? "" : response.body().trim();
        if (body.isEmpty()) {
            return new JSONObject();
        }
        if (body.startsWith("[")) {
            return new JSONObject().put("items", new JSONArray(body));
        }
        return new JSONObject(body);
    }

    private JSONObject buildEventInfo(Event event) {
        String subject = event.getTitle() == null || event.getTitle().isBlank() ? "Etude" : event.getTitle().trim();
        Date eventDate = event.getDate();
        return new JSONObject()
                .put("title", subject)
                .put("subject", subject)
                .put("duration_minutes", Math.max(60, event.getDuration()))
                .put("date", eventDate == null ? LocalDate.now().toString() : eventDate.toString());
    }
}
