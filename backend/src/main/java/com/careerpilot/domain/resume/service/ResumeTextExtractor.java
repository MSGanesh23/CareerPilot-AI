package com.careerpilot.domain.resume.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Extracts plain text from uploaded resume files.
 *
 * Production note:
 * - For PDF: integrate Apache PDFBox (org.apache.pdfbox:pdfbox)
 * - For DOCX: integrate Apache POI (org.apache.poi:poi-ooxml)
 *
 * This implementation provides a clean integration point with
 * graceful fallback. Add the dependencies and uncomment the
 * extraction logic below when ready.
 */
@Slf4j
@Component
public class ResumeTextExtractor {

    /**
     * Attempts to extract text from the uploaded file.
     * Returns empty string on failure (non-fatal — AI analysis
     * is degraded but upload still succeeds).
     */
    public String extractText(MultipartFile file) {
        if (file == null || file.isEmpty()) return "";

        String contentType = file.getContentType() != null ? file.getContentType() : "";
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";

        try {
            if (contentType.contains("pdf") || originalName.toLowerCase().endsWith(".pdf")) {
                return extractFromPdf(file);
            } else if (contentType.contains("word") || originalName.toLowerCase().endsWith(".docx")
                    || originalName.toLowerCase().endsWith(".doc")) {
                return extractFromDocx(file);
            }
        } catch (Exception e) {
            log.warn("Text extraction failed for file={}, continuing without parsed text: {}",
                    originalName, e.getMessage());
        }

        return "";
    }

    // ----------------------------------------------------------------
    // PDF extraction — requires: org.apache.pdfbox:pdfbox:3.0.1
    // ----------------------------------------------------------------
    private String extractFromPdf(MultipartFile file) throws IOException {
        /*
         * Uncomment when pdfbox is added to pom.xml:
         *
         * try (InputStream is = file.getInputStream();
         *      PDDocument document = Loader.loadPDF(is.readAllBytes())) {
         *     PDFTextStripper stripper = new PDFTextStripper();
         *     return stripper.getText(document).trim();
         * }
         */
        log.debug("PDF text extraction placeholder — add pdfbox dependency to enable");
        return readAsPlainText(file);
    }

    // ----------------------------------------------------------------
    // DOCX extraction — requires: org.apache.poi:poi-ooxml:5.2.5
    // ----------------------------------------------------------------
    private String extractFromDocx(MultipartFile file) throws IOException {
        /*
         * Uncomment when poi-ooxml is added to pom.xml:
         *
         * try (InputStream is = file.getInputStream();
         *      XWPFDocument document = new XWPFDocument(is)) {
         *     XWPFWordExtractor extractor = new XWPFWordExtractor(document);
         *     return extractor.getText().trim();
         * }
         */
        log.debug("DOCX text extraction placeholder — add poi-ooxml dependency to enable");
        return readAsPlainText(file);
    }

    // ----------------------------------------------------------------
    // Fallback: try reading as plain text (works for .txt uploads)
    // ----------------------------------------------------------------
    private String readAsPlainText(MultipartFile file) {
        try (InputStream is = file.getInputStream();
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            log.debug("Plain text fallback also failed: {}", e.getMessage());
            return "";
        }
    }
}
