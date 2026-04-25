package utils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PdfTextExtractor {
    private PdfTextExtractor() {}

    public static String extractText(File pdfFile, int maxChars) throws Exception {
        if (pdfFile == null || !pdfFile.exists()) {
            return "";
        }
        byte[] bytes = Files.readAllBytes(pdfFile.toPath());
        if (bytes.length == 0) {
            return "";
        }

        String raw = new String(bytes, StandardCharsets.ISO_8859_1).replace("\u0000", " ");
        Pattern pattern = Pattern.compile("[\\p{L}\\p{N}][\\p{L}\\p{N}\\p{Punct}\\p{Space}]{8,}");
        Matcher matcher = pattern.matcher(raw);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String chunk = matcher.group().replaceAll("\\s+", " ").trim();
            if (!chunk.isEmpty()) {
                if (out.length() > 0) out.append('\n');
                out.append(chunk);
            }
            if (maxChars > 0 && out.length() >= maxChars) {
                break;
            }
        }
        String cleaned = out.length() > 0
                ? out.toString().trim()
                : raw.replaceAll("[^\\p{Print}\\n\\r\\t]", " ").replaceAll("\\s+", " ").trim();
        if (maxChars > 0 && cleaned.length() > maxChars) {
            return cleaned.substring(0, maxChars);
        }
        return cleaned;
    }
}

