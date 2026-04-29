package services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import models.RoadmapStep;
import services.chat.ChatService;

import java.util.List;

public class RoadmapService {

    private static final String SYSTEM_PROMPT = """
            You are an expert learning path designer and curriculum architect.
            You ALWAYS respond with ONLY valid JSON — no markdown fences, no explanations, no extra text.
            Your entire response must start with [ and end with ].
            """;

    private static final String USER_TEMPLATE = """
            Generate a practical 6-step learning roadmap for the skill: "%s"

            Respond with ONLY this JSON array (no other text):
            [
              {
                "stepNumber": 1,
                "title": "Concise step title",
                "description": "2-3 sentences explaining what to learn and why it matters.",
                "resources": [
                  { "label": "Resource Name", "url": "https://real-url.com" },
                  { "label": "Resource Name 2", "url": "https://real-url2.com" }
                ]
              }
            ]

            Rules:
            - Exactly 6 steps, ordered from beginner to advanced.
            - Each step has 2-3 real, working resource URLs (docs, tutorials, or courses).
            - Descriptions are practical, not generic.
            """;

    private final ChatService  chatService;
    private final ObjectMapper mapper;

    public RoadmapService() {
        this.chatService = new ChatService();
        this.mapper      = new ObjectMapper();
    }

    /** Returns true when an OpenRouter API key is configured. */
    public boolean isConfigured() {
        return chatService.hasApiKeyConfigured();
    }

    /**
     * Calls the AI and returns a parsed list of roadmap steps.
     * Must be called off the JavaFX Application Thread.
     */
    public List<RoadmapStep> generateRoadmap(String skill) throws Exception {
        String prompt      = String.format(USER_TEMPLATE, skill);
        String rawResponse = chatService.chat(SYSTEM_PROMPT, prompt);
        return parseResponse(rawResponse);
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private List<RoadmapStep> parseResponse(String raw) throws Exception {
        if (raw == null || raw.isBlank()) {
            throw new Exception("AI returned an empty response.");
        }
        // Tolerate leading/trailing prose: find the JSON array bounds
        int start = raw.indexOf('[');
        int end   = raw.lastIndexOf(']');
        if (start == -1 || end == -1 || end < start) {
            throw new Exception("No valid JSON array found in AI response:\n" + raw);
        }
        String json = raw.substring(start, end + 1);
        return mapper.readValue(json, new TypeReference<List<RoadmapStep>>() {});
    }
}
