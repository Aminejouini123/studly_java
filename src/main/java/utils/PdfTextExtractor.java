package utils;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;

public final class PdfTextExtractor {
    private PdfTextExtractor() {}

    public static String extractText(File pdfFile, int maxChars) throws Exception {
        if (pdfFile == null || !pdfFile.exists()) {
            return "";
        }
        
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            
            // Optimization: we don't need to read a 1000-page book if we only need a few thousand chars.
            // But PDFTextStripper streams it, so we can just extract it and substring.
            // For extreme optimization, we could setEndPage() but it's fine for typical course PDFs.
            
            String text = stripper.getText(document);
            if (text == null || text.trim().isEmpty()) {
                return "";
            }
            
            text = text.trim();
            if (maxChars > 0 && text.length() > maxChars) {
                return text.substring(0, maxChars);
            }
            return text;
        }
    }
}

