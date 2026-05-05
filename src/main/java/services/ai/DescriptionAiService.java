package services.ai;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DescriptionAiService {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public String generateProjectDescription(String title,
                                            String type,
                                            String status,
                                            LocalDate deadline,
                                            String resource,
                                            String groupName) {
        String prompt = buildProjectPrompt(title, type, status, deadline, resource, groupName);
        String ai = tryGenerateViaChatApi(prompt);
        if (ai != null && !ai.trim().isEmpty()) {
            return ai.trim();
        }
        return fallbackProjectTemplate(title, type, status, deadline, resource, groupName);
    }

    public String generateTaskDescription(String taskTitle,
                                         String projectTitle,
                                         String groupName,
                                         String status,
                                         LocalDate deadline) {
        String prompt = buildTaskPrompt(taskTitle, projectTitle, groupName, status, deadline);
        String ai = tryGenerateViaChatApi(prompt);
        if (ai != null && !ai.trim().isEmpty()) {
            return ai.trim();
        }
        return fallbackTaskTemplate(taskTitle, projectTitle, groupName, status, deadline);
    }

    private static String buildProjectPrompt(String title,
                                            String type,
                                            String status,
                                            LocalDate deadline,
                                            String resource,
                                            String groupName) {
        StringBuilder sb = new StringBuilder();
        sb.append("Tu es un assistant qui redige une description courte et claire pour un projet d'equipe.\n");
        sb.append("Ecris en francais, ton professionnel, 5 a 10 lignes max.\n");
        sb.append("Structure: objectif, perimetre, livrables, criteres de succes, et etapes.\n\n");
        sb.append("Contexte:\n");
        if (!safe(title).isEmpty()) sb.append("- Titre: ").append(safe(title)).append("\n");
        if (!safe(type).isEmpty()) sb.append("- Type: ").append(safe(type)).append("\n");
        if (!safe(status).isEmpty()) sb.append("- Statut: ").append(safe(status)).append("\n");
        if (deadline != null) sb.append("- Date limite: ").append(deadline.format(DATE_FMT)).append("\n");
        if (!safe(resource).isEmpty()) sb.append("- Ressources: ").append(safe(resource)).append("\n");
        if (!safe(groupName).isEmpty()) sb.append("- Groupe: ").append(safe(groupName)).append("\n");
        sb.append("\nNe mentionne pas que tu es une IA.");
        return sb.toString();
    }

    private static String buildTaskPrompt(String taskTitle,
                                         String projectTitle,
                                         String groupName,
                                         String status,
                                         LocalDate deadline) {
        StringBuilder sb = new StringBuilder();
        sb.append("Tu es un assistant qui redige une description / instructions pour une tache.\n");
        sb.append("Ecris en francais, 4 a 8 lignes max, concret et actionnable.\n");
        sb.append("Structure: objectif, etapes, criteres d'acceptation.\n\n");
        sb.append("Contexte:\n");
        if (!safe(taskTitle).isEmpty()) sb.append("- Titre tache: ").append(safe(taskTitle)).append("\n");
        if (!safe(projectTitle).isEmpty()) sb.append("- Projet: ").append(safe(projectTitle)).append("\n");
        if (!safe(groupName).isEmpty()) sb.append("- Groupe: ").append(safe(groupName)).append("\n");
        if (!safe(status).isEmpty()) sb.append("- Statut initial: ").append(safe(status)).append("\n");
        if (deadline != null) sb.append("- Date limite: ").append(deadline.format(DATE_FMT)).append("\n");
        sb.append("\nNe mentionne pas que tu es une IA.");
        return sb.toString();
    }

    private String tryGenerateViaChatApi(String prompt) {
        String apiKey = env("AI_API_KEY");
        String model = env("AI_MODEL");
        if (apiKey.isEmpty() || model.isEmpty()) {
            return null;
        }

        String baseUrl = env("AI_BASE_URL");
        if (baseUrl.isEmpty()) {
            baseUrl = "https://api.openai.com/v1";
        }
        String url = baseUrl.replaceAll("/+$", "") + "/chat/completions";

        String body = "{"
                + "\"model\":\"" + jsonEscape(model) + "\","
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":\"" + jsonEscape("Tu reponds uniquement avec le texte final, sans markdown.") + "\"},"
                + "{\"role\":\"user\",\"content\":\"" + jsonEscape(prompt) + "\"}"
                + "],"
                + "\"temperature\":0.7"
                + "}";

        HttpClient client = HttpClient.newBuilder().build();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() / 100 != 2) {
                return null;
            }
            return extractFirstMessageContent(resp.body());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return null;
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    // Very small JSON extraction (no deps). If format changes, returns null and we fallback to templates.
    private static String extractFirstMessageContent(String json) {
        if (json == null) {
            return null;
        }
        // Typical shape: choices[0].message.content
        int idx = json.indexOf("\"content\"");
        if (idx < 0) {
            return null;
        }
        int colon = json.indexOf(':', idx);
        if (colon < 0) {
            return null;
        }
        int startQuote = json.indexOf('"', colon + 1);
        if (startQuote < 0) {
            return null;
        }
        StringBuilder out = new StringBuilder();
        boolean escaping = false;
        for (int i = startQuote + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaping) {
                switch (c) {
                    case 'n':
                        out.append('\n');
                        break;
                    case 'r':
                        out.append('\r');
                        break;
                    case 't':
                        out.append('\t');
                        break;
                    case '"':
                        out.append('"');
                        break;
                    case '\\':
                        out.append('\\');
                        break;
                    case 'u':
                        if (i + 4 < json.length()) {
                            String hex = json.substring(i + 1, i + 5);
                            try {
                                int code = Integer.parseInt(hex, 16);
                                out.append((char) code);
                                i += 4;
                            } catch (NumberFormatException nfe) {
                                return null;
                            }
                        } else {
                            return null;
                        }
                        break;
                    default:
                        out.append(c);
                }
                escaping = false;
                continue;
            }
            if (c == '\\') {
                escaping = true;
                continue;
            }
            if (c == '"') {
                return out.toString();
            }
            out.append(c);
        }
        return null;
    }

    private static String fallbackProjectTemplate(String title,
                                                 String type,
                                                 String status,
                                                 LocalDate deadline,
                                                 String resource,
                                                 String groupName) {
        String t = safe(title);
        String ty = safe(type);
        String st = safe(status);
        String dl = deadline == null ? "" : deadline.format(DATE_FMT);
        String res = safe(resource);
        String g = safe(groupName);

        String header = !t.isEmpty() ? t : "Nouveau projet";
        StringBuilder sb = new StringBuilder();
        sb.append("Objectif: definir et realiser \"").append(header).append("\"");
        if (!ty.isEmpty()) sb.append(" (").append(ty).append(")");
        sb.append(".\n");
        if (!g.isEmpty()) sb.append("Equipe / groupe: ").append(g).append(".\n");
        if (!st.isEmpty()) sb.append("Statut: ").append(st.toUpperCase(Locale.ROOT)).append(".\n");
        if (!dl.isEmpty()) sb.append("Echeance: ").append(dl).append(".\n");
        sb.append("Perimetre: cadrer les besoins, planifier les taches, executer et valider les livrables.\n");
        sb.append("Livrables: plan d'action, elements produits, et un bref compte-rendu d'avancement.\n");
        sb.append("Criteres de succes: objectifs atteints, qualite verifiee, et livraison dans les delais.\n");
        if (!res.isEmpty()) sb.append("Ressources: ").append(res).append(".\n");
        sb.append("Etapes: 1) cadrage 2) execution 3) revue 4) livraison.");
        return sb.toString();
    }

    private static String fallbackTaskTemplate(String taskTitle,
                                              String projectTitle,
                                              String groupName,
                                              String status,
                                              LocalDate deadline) {
        String tt = safe(taskTitle);
        String pt = safe(projectTitle);
        String g = safe(groupName);
        String st = safe(status);
        String dl = deadline == null ? "" : deadline.format(DATE_FMT);

        String header = !tt.isEmpty() ? tt : "Nouvelle tache";
        StringBuilder sb = new StringBuilder();
        sb.append("Objectif: realiser \"").append(header).append("\".\n");
        if (!pt.isEmpty()) sb.append("Contexte projet: ").append(pt).append(".\n");
        if (!g.isEmpty()) sb.append("Groupe: ").append(g).append(".\n");
        if (!st.isEmpty()) sb.append("Statut initial: ").append(st.toUpperCase(Locale.ROOT)).append(".\n");
        if (!dl.isEmpty()) sb.append("Date limite: ").append(dl).append(".\n");
        sb.append("Etapes: analyser les besoins, implementer, tester, puis documenter.\n");
        sb.append("Criteres d'acceptation: fonctionnalite conforme, tests OK, et pas de regressions.");
        return sb.toString();
    }

    private static String env(String name) {
        try {
            String v = System.getenv(name);
            return v == null ? "" : v.trim();
        } catch (SecurityException se) {
            return "";
        }
    }

    private static String safe(String v) {
        return v == null ? "" : v.trim();
    }

    private static String jsonEscape(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\':
                    sb.append("\\\\");
                    break;
                case '"':
                    sb.append("\\\"");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}

