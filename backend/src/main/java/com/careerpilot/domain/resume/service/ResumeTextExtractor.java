package com.careerpilot.domain.resume.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
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
 * <p>
 * Supports:
 * - PDF via Apache PDFBox 3.0.1
 * - DOCX via Apache POI 5.2.5
 * - Fallback: raw plain-text read (works for .txt files)
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
    // PDF extraction — Apache PDFBox 3.0.1
    // ----------------------------------------------------------------
    private String extractFromPdf(MultipartFile file) throws IOException {
        try (InputStream is = file.getInputStream();
             PDDocument document = Loader.loadPDF(is.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document).trim();
            log.debug("Extracted {} characters from PDF: {}", text.length(), file.getOriginalFilename());
            return text;
        }
    }

    // ----------------------------------------------------------------
    // DOCX extraction — Apache POI 5.2.5
    // ----------------------------------------------------------------
    private String extractFromDocx(MultipartFile file) throws IOException {
        // Legacy .doc files cannot be parsed by POI XWPF — fall back to plain-text
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        if (name.endsWith(".doc") && !name.endsWith(".docx")) {
            log.debug("Legacy .doc format — using plain-text fallback for: {}", file.getOriginalFilename());
            return readAsPlainText(file);
        }
        try (InputStream is = file.getInputStream();
             XWPFDocument document = new XWPFDocument(is);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            String text = extractor.getText().trim();
            log.debug("Extracted {} characters from DOCX: {}", text.length(), file.getOriginalFilename());
            return text;
        }
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
