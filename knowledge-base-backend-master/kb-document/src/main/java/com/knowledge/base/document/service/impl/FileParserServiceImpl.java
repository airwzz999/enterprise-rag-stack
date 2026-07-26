package com.knowledge.base.document.service.impl;

import com.knowledge.base.document.service.FileParserService;
import com.knowledge.base.document.service.FileUploadService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.poi.xslf.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;

/**
 * File parsing Service implementation
 *
 * <p>PDF/DOCX/PPTX/XLSX -> native Java parsing (PDFBox / Apache POI).</p>
 * <p>Plain text (.txt) and Markdown (.md) are read directly.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Service
public class FileParserServiceImpl implements FileParserService {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "pdf", "docx", "xlsx", "pptx", "txt", "md"
    );

    @Resource
    private TikaConverterService tikaConverter;

    @Resource
    private FileUploadService fileUploadService;

    @Override
    public boolean isSupported(String extension) {
        return extension != null && SUPPORTED_EXTENSIONS.contains(extension.toLowerCase());
    }

    @Override
    public String parse(MultipartFile file) throws Exception {
        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);

        if (!isSupported(extension)) {
            throw new IllegalArgumentException("Unsupported file format: " + extension);
        }

        // Defensive check: ensure the file stream is readable (the stream may have already been consumed after a Feign upload)
        if (file.getSize() > 0) {
            try (InputStream testStream = file.getInputStream()) {
                byte[] firstBytes = new byte[16];
                int read = testStream.read(firstBytes);
                log.debug("File readability check: name={}, size={}, firstRead={}bytes",
                        originalFilename, file.getSize(), read);
            } catch (Exception e) {
                log.error("File stream is not readable: name={}, error={}", originalFilename, e.getMessage());
                throw new IllegalArgumentException("File stream is not readable, cannot parse: " + e.getMessage());
            }
        }

        long startTime = System.currentTimeMillis();

        String result;
        switch (extension.toLowerCase()) {
            case "docx":
                // DOCX: parsed directly via Java POI, preserving heading levels as # Heading format
                result = parseDocx(file);
                break;
            case "pdf":
                // PDF: parsed directly via Java PDFBox
                result = parsePdf(file);
                break;
            case "pptx":
                // PPTX: parsed directly via Java POI, preserving the ## Slide N structure
                result = parsePptx(file);
                break;
            case "xlsx":
                // XLSX: Tika is preferred (Markdown table + separator row), falling back to Java POI on failure
                result = withFallback(file, extension,
                        () -> tikaConverter.parseXlsx(file),
                        this::parseXlsx);
                break;
            case "txt":
            case "md":
                result = parsePlainText(file);
                break;
            default:
                throw new IllegalArgumentException("Unsupported file format: " + extension);
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("File parsing complete: name={}, format={}, chars={}, elapsed={}ms",
                originalFilename, extension, result.length(), elapsed);

        return result;
    }

    /**
     * Prefers Tika for conversion, falling back to Java parsing on failure.
     */
    @FunctionalInterface
    private interface Converter {
        String convert() throws Exception;
    }

    @FunctionalInterface
    private interface Fallback {
        String parse(MultipartFile file) throws Exception;
    }

    private String withFallback(MultipartFile file, String extension,
                                 Converter converter, Fallback fallback) throws Exception {
        try {
            long start = System.currentTimeMillis();
            String result = converter.convert();
            long elapsed = System.currentTimeMillis() - start;
            log.info("Tika parsing succeeded: format={}, chars={}, elapsed={}ms",
                    extension, result.length(), elapsed);
            return result;
        } catch (Exception e) {
            log.warn("Tika conversion failed, falling back to Java parsing: format={}, error={}",
                    extension, e.getMessage());
        }

        return fallback.parse(file);
    }

    // ── PDF parsing ──────────────────────────────────────────

    /**
     * Line information with font metadata
     */
    /** Position information for an image on the page */
    private static class ImagePlacement {
        final float y;       // Y coordinate (top-down from the page top, consistent with getYDirAdj)
        final String url;    // Image URL

        ImagePlacement(float y, String url) {
            this.y = y;
            this.url = url;
        }
    }

    private static class PdfLine {
        final String text;
        final float fontSize;
        final boolean isBold;
        final boolean isMono;
        /** Y coordinate (top-down from the page top); Float.NaN indicates a non-text line (e.g. a PAGE_BREAK marker) */
        final float y;
        /**
         * When non-null, indicates that table columns were detected on this line, storing each column's text.
         * Also stores the starting X coordinate of each column for cross-row alignment matching.
         */
        String[] columns;
        float[] columnX;
        /** X coordinates and text of positions after the original X-sort (used for template column reassignment) */
        float[] posXs;
        String[] posTexts;

        PdfLine(String text, float fontSize, boolean isBold, boolean isMono) {
            this(text, fontSize, isBold, isMono, Float.NaN);
        }

        PdfLine(String text, float fontSize, boolean isBold, boolean isMono, float y) {
            this.text = text;
            this.fontSize = fontSize;
            this.isBold = isBold;
            this.isMono = isMono;
            this.y = y;
        }
    }

    /**
     * Custom PDFTextStripper: intercepts text that PDFBox has already split into lines by
     * Y coordinate by overriding {@code writeString(text, positions)}, extracts font metadata
     * from TextPosition, and finally formats it uniformly into Markdown via {@code buildMarkdown()}.
     *
     * <p><b>Core design:</b> does not override {@code processTextPosition} —
     * lets the parent class fully perform its line detection logic (based on Y coordinate changes),
     * intercepting the result only at the {@code writeString / writeLineSeparator} exit points.
     * If {@code processTextPosition} were overridden without calling super,
     * the parent class would be unable to detect line breaks, merging all text into a single blob.</p>
     */
    private static class CollectingPDFStripper extends PDFTextStripper {
        private final List<PdfLine> lines = new ArrayList<>();
        /** Consecutive writeLineSeparator count -> paragraph spacing */
        private int consecutiveSeparators = 0;
        /** Y coordinate of the previous (already flushed) line, used for paragraph spacing detection */
        private float prevLineY = Float.NaN;
        /** Exponential moving average of line spacing */
        private float runningLineGap = 0;
        /** Per-page image position information, injected by parsePdf via setImagePlacements */
        private Map<Integer, List<ImagePlacement>> pageImages;

        // ── Current line accumulator (PDFBox writeString is called word by word, requiring accumulation of a full line before column detection) ──
        private final StringBuilder curText = new StringBuilder();
        private final List<TextPosition> curPositions = new ArrayList<>();
        private float curMaxFontSize = 0;
        private boolean curIsBold = false;
        private boolean curIsMono = false;
        private float curY = Float.NaN;

        CollectingPDFStripper() throws IOException {
            super();
            setSortByPosition(true);
            setAddMoreFormatting(false);
            setSuppressDuplicateOverlappingText(true);
        }

        void setImagePlacements(Map<Integer, List<ImagePlacement>> pageImages) {
            this.pageImages = pageImages;
        }

        /**
         * PDFBox calls this method once for each "word" (contiguous text separated by
         * spaces/large gaps) within the same line. We don't generate a PdfLine directly —
         * text and TextPosition are accumulated first, and flushed only when
         * writeLineSeparator() signals the end of the line, allowing column detection to be
         * performed on the complete line.
         */
        @Override
        protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
            String cleaned = text.stripTrailing();
            if (cleaned.isEmpty()) return;

            // Defensive: if the Y coordinate changes noticeably (> 2pt), PDFBox started a new line without inserting writeLineSeparator
            float maxY = Float.NaN;
            for (TextPosition tp : textPositions) {
                float y = tp.getYDirAdj();
                if (Float.isNaN(maxY) || y > maxY) maxY = y;
            }
            if (!Float.isNaN(curY) && !Float.isNaN(maxY) && Math.abs(maxY - curY) > 2f) {
                flushCurrentLine();
            }

            // Accumulate text (with a space between words)
            if (curText.length() > 0) curText.append(' ');
            curText.append(cleaned);
            curPositions.addAll(textPositions);

            // Accumulate font metadata
            for (TextPosition tp : textPositions) {
                float fs = tp.getFontSizeInPt();
                if (fs > curMaxFontSize) curMaxFontSize = fs;
                float y = tp.getYDirAdj();
                if (Float.isNaN(curY) || y > curY) curY = y;
                if (tp.getFont() != null) {
                    String fn = tp.getFont().getName().toLowerCase();
                    if (fn.contains("bold") || fn.contains("semibold")
                            || fn.contains("heavy") || fn.contains("black")) {
                        curIsBold = true;
                    }
                    if (isMonospacePdfFont(fn)) {
                        curIsMono = true;
                    }
                }
            }
        }

        /**
         * Detects table columns based on TextPosition's X coordinates.
         * If multiple clear horizontal gaps (> 12pt) are detected within the same line,
         * it is judged to be a table row, and the column text is split out.
         */
        private void detectTableColumns(PdfLine line, List<TextPosition> positions) {
            if (positions.size() < 3) return; // Too few fragments to form a table

            // Sort by X coordinate
            List<TextPosition> sorted = new ArrayList<>(positions);
            sorted.sort(Comparator.comparingDouble(TextPosition::getXDirAdj));

            // Always store the original sorted position data, for template column reassignment in detectTableGroups later
            line.posXs = new float[sorted.size()];
            line.posTexts = new String[sorted.size()];
            for (int i = 0; i < sorted.size(); i++) {
                line.posXs[i] = sorted.get(i).getXDirAdj();
                line.posTexts[i] = sorted.get(i).getUnicode();
            }

            // First pass: find column gaps (positions with X spacing > 12pt)
            List<Integer> gapBefore = new ArrayList<>();
            float prevEndX = -Float.MAX_VALUE;
            for (int i = 0; i < sorted.size(); i++) {
                TextPosition tp = sorted.get(i);
                float x = tp.getXDirAdj();
                if (i > 0 && prevEndX > 0 && x - prevEndX > 12f) {
                    gapBefore.add(i);
                }
                prevEndX = Math.max(prevEndX, x + tp.getWidthDirAdj());
            }

            // At least 1 gap is required -> at least 2 columns
            if (gapBefore.isEmpty()) return;

            // Second pass: extract each column's text
            int colCount = gapBefore.size() + 1;
            StringBuilder[] colTexts = new StringBuilder[colCount];
            float[] colX = new float[colCount];
            Arrays.setAll(colTexts, i -> new StringBuilder());

            int col = 0;
            for (int i = 0; i < sorted.size(); i++) {
                while (col < gapBefore.size() && i == gapBefore.get(col)) {
                    col++;
                }
                if (colTexts[col].length() == 0) {
                    colX[col] = sorted.get(i).getXDirAdj();
                }
                colTexts[col].append(sorted.get(i).getUnicode());
            }

            // Clean up each column's text
            String[] cols = new String[colCount];
            for (int c = 0; c < colCount; c++) {
                cols[c] = colTexts[c].toString().strip();
            }

            line.columns = cols;
            line.columnX = colX;
        }

        /**
         * Checks whether the previous non-empty line ends with a Chinese sentence-ending
         * punctuation mark (。！？) — these are the actual punctuation characters that may
         * appear in the source document being parsed, and are matched literally, not translated.
         */
        private boolean prevLineEndsWithSentencePunct() {
            for (int i = lines.size() - 1; i >= 0; i--) {
                String t = lines.get(i).text;
                if (t.isEmpty() || t.startsWith("__PAGE_BREAK__")) continue;
                return t.matches(".*[。！？]$");
            }
            return false;
        }

        /**
         * Checks whether the current line looks like the start of a new section.
         * These are common markers for the start of a new paragraph/subsection in Chinese PDFs;
         * the patterns below match literal Chinese characters found in the source document
         * being parsed, and must not be translated.
         */
        private boolean looksLikeSectionStart(String text) {
            if (text == null || text.isEmpty()) return false;
            // Chinese numbering: 一、二、三...
            if (text.matches("^[一二三四五六七八九十]+[、，\\s].*")) return true;
            // Numeric numbering: 1. 2. 3. or 1、2、3、
            if (text.matches("^\\d+[.、)]\\s.*")) return true;
            // Common new-section keywords (in Chinese, matched against the source document's language)
            if (text.matches("^(推荐|使用|示例|注意|提示|说明|总结|总之|因此|所以|另外|此外|同时|然而|但是|首先|其次|最后|然后|接着|接下来|配置|方案|步骤|问题|优势|特点|定义|背景|目标|需求|实现|测试|部署|运维|监控|参考).*")) return true;
            return false;
        }

        /**
         * Checks whether the previous line is a "short standalone line" (&lt;= 15 characters
         * ending with 。！？). Lines like this are usually mini-headings or standalone
         * statements, and should be followed by a new paragraph.
         * For example: "Built the index online with the tool.", "Configuration changed.", "Preface"
         * (the patterns match literal Chinese punctuation found in the source document being parsed).
         */
        private boolean isPrevLineShortStandalone() {
            PdfLine prev = null;
            for (int i = lines.size() - 1; i >= 0; i--) {
                String t = lines.get(i).text;
                if (t.isEmpty() || t.startsWith("__PAGE_BREAK__")) continue;
                prev = lines.get(i);
                break;
            }
            if (prev == null) return false;
            String t = prev.text.strip();
            // Short line (<= 15 characters) ending with sentence-ending punctuation -> standalone line
            if (t.length() <= 15 && t.matches(".*[。！？]$")) {
                return true;
            }
            // Short line (<= 10 characters) may be standalone even without punctuation
            // (e.g. truncation in the PDF leaves no trailing punctuation, but the content itself is a standalone statement)
            if (t.length() <= 10 && (t.matches(".*[。！？]$") || t.endsWith("）"))) {
                return true;
            }
            return false;
        }

        /**
         * Called by PDFBox after each line ends.
         * First flushes the accumulated current line, then increments the separator count.
         */
        @Override
        protected void writeLineSeparator() throws IOException {
            flushCurrentLine();
            consecutiveSeparators++;
        }

        /**
         * Flushes the currently accumulated line (all words + TextPosition across multiple
         * writeString calls) into a single PdfLine. At this point curPositions contains all
         * character positions for the full line -> column detection can work correctly.
         */
        private void flushCurrentLine() {
            if (curText.length() == 0) return;

            String text = curText.toString();
            float lineY = curY;

            // ── Paragraph boundary detection (same logic as the old writeString, just applied to the fully accumulated line) ──
            boolean isParagraphBreak = false;

            // Method A: Y-spacing detection
            if (!Float.isNaN(prevLineY) && !Float.isNaN(lineY)) {
                float gap = lineY - prevLineY;
                if (gap > 0 && runningLineGap > 0) {
                    if (gap > runningLineGap * 1.8f) {
                        isParagraphBreak = true;
                    } else if (gap > runningLineGap * 1.25f && prevLineEndsWithSentencePunct()) {
                        isParagraphBreak = true;
                    }
                }
                if (gap > 0 && runningLineGap > 0 && gap <= runningLineGap * 1.5f) {
                    runningLineGap = runningLineGap * 0.7f + gap * 0.3f;
                } else if (gap > 0 && runningLineGap == 0) {
                    runningLineGap = gap;
                }
            }
            prevLineY = lineY;

            // Method B: text feature detection
            if (!isParagraphBreak && prevLineEndsWithSentencePunct()) {
                String ct = text.strip();
                if (ct.length() <= 15 && !ct.matches("^[\\d•\\-].*")) {
                    isParagraphBreak = true;
                } else if (looksLikeSectionStart(ct)) {
                    isParagraphBreak = true;
                }
            }

            // Method C: the previous line is a short standalone line
            if (!isParagraphBreak && isPrevLineShortStandalone()) {
                isParagraphBreak = true;
            }

            // Consecutive blank lines / paragraph separation -> insert a blank line
            if ((consecutiveSeparators >= 2 || isParagraphBreak) && !lines.isEmpty()) {
                lines.add(new PdfLine("", 0, false, false));
            }
            consecutiveSeparators = 0;

            // Column detection: curPositions contains all characters in the line -> can detect across column gaps
            PdfLine pdfLine = new PdfLine(text, curMaxFontSize, curIsBold, curIsMono, lineY);
            detectTableColumns(pdfLine, curPositions);
            lines.add(pdfLine);

            // Reset the current line accumulator
            curText.setLength(0);
            curPositions.clear();
            curMaxFontSize = 0;
            curIsBold = false;
            curIsMono = false;
            curY = Float.NaN;
        }

        /**
         * Called by PDFBox after each page ends.
         * Flushes the current line -> inserts a page boundary marker -> resets Y tracking.
         */
        @Override
        protected void writePageEnd() throws IOException {
            super.writePageEnd();
            flushCurrentLine();
            int pageNum = getCurrentPageNo();
            lines.add(new PdfLine("__PAGE_BREAK__" + pageNum, 0, false, false));
            prevLineY = Float.NaN;
            runningLineGap = 0;
        }

        @Override
        public String getText(PDDocument doc) throws IOException {
            lines.clear();
            consecutiveSeparators = 0;
            curText.setLength(0);
            curPositions.clear();
            curMaxFontSize = 0;
            curIsBold = false;
            curIsMono = false;
            curY = Float.NaN;

            // Use the parent class's full pipeline (including processTextPosition line detection)
            try (StringWriter sw = new StringWriter()) {
                writeText(doc, sw);
            }

            if (lines.isEmpty()) return "";

            // Always merge page boundaries: when images are present, interleave ![](url) by Y; otherwise at least clean up the __PAGE_BREAK__ markers
            mergeImagesByPosition();

            // Build Markdown based on the collected lines and font metadata
            return buildMarkdown();
        }

        /**
         * Inserts each page's images into the corresponding page's text lines by Y coordinate.
         * Images and text are merged after being uniformly sorted by Y (top to bottom).
         */
        private void mergeImagesByPosition() {
            List<PdfLine> merged = new ArrayList<>();
            int currentPage = -1;
            int pageStart = 0;

            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).text.startsWith("__PAGE_BREAK__")) {
                    // Merge the current page's text and images
                    mergePageLines(currentPage + 1, pageStart, i, merged);
                    currentPage++;
                    pageStart = i + 1; // skip the PAGE_BREAK marker
                }
            }
            // Last page
            if (pageStart < lines.size()) {
                mergePageLines(currentPage + 1, pageStart, lines.size(), merged);
            }

            lines.clear();
            lines.addAll(merged);
        }

        /**
         * Merges a single page's text lines and images by Y coordinate.
         */
        private void mergePageLines(int pageNum, int fromIdx, int toIdx, List<PdfLine> output) {
            List<ImagePlacement> images = (pageImages != null) ? pageImages.get(pageNum) : null;

            if (images == null || images.isEmpty()) {
                // No images on this page, copy the text lines directly (the PAGE_BREAK marker was already skipped in the loop)
                for (int i = fromIdx; i < toIdx; i++) {
                    output.add(lines.get(i));
                }
                return;
            }

            // Images are already sorted by Y (in extractAndUploadImages)
            int imgIdx = 0;
            for (int i = fromIdx; i < toIdx; i++) {
                PdfLine line = lines.get(i);

                // Insert all images whose Y coordinate is <= the current text line's
                while (imgIdx < images.size()
                        && !Float.isNaN(line.y)
                        && images.get(imgIdx).y <= line.y) {
                    ImagePlacement img = images.get(imgIdx);
                    output.add(new PdfLine("![](" + img.url + ")", 0, false, false));
                    output.add(new PdfLine("", 0, false, false)); // blank line after the image for separation
                    imgIdx++;
                }

                output.add(line);
            }

            // Remaining images (with a Y coordinate after all of the page's text)
            while (imgIdx < images.size()) {
                ImagePlacement img = images.get(imgIdx);
                output.add(new PdfLine("![](" + img.url + ")", 0, false, false));
                output.add(new PdfLine("", 0, false, false));
                imgIdx++;
            }
        }

        /**
         * Preprocesses the entire lines list:
         * 1. Splits multiple list items that PDFBox merged together into separate lines
         * 2. Converts • to "- " (standard Markdown list syntax)
         *
         * <p>Must run before buildMarkdown's code detection / formatting,
         * ensuring the split lines participate in later processing as independent units.</p>
         */
        private void normalizeBulletLists() {
            List<PdfLine> normalized = new ArrayList<>();
            for (PdfLine line : lines) {
                if (line.text.isEmpty()) {
                    normalized.add(line);
                    continue;
                }

                // Split on • (only splits when preceded by at least 1 non-• character, avoiding an empty string from a leading •)
                String[] parts = line.text.split("(?<=[^•])(?=•)");
                if (parts.length <= 1) {
                    // No merged items: convert • -> - directly
                    normalized.add(convertBullet(line));
                } else {
                    for (String part : parts) {
                        String trimmed = part.strip();
                        if (!trimmed.isEmpty()) {
                            normalized.add(convertBullet(
                                    new PdfLine(trimmed, line.fontSize, line.isBold, false)));
                        }
                    }
                }
            }
            lines.clear();
            lines.addAll(normalized);
        }

        /** Converts a line starting with • into a standard Markdown list item starting with "- " */
        private static PdfLine convertBullet(PdfLine line) {
            String t = line.text;
            if (t.startsWith("• ") || t.startsWith("•\t")) {
                return new PdfLine("- " + t.substring(2), line.fontSize, line.isBold, false);
            } else if (t.startsWith("•")) {
                return new PdfLine("- " + t.substring(1).stripLeading(), line.fontSize, line.isBold, false);
            }
            return line;
        }

        /**
         * Builds formatted Markdown from the collected line information and font metadata
         */
        private String buildMarkdown() {
            // Step 0: preprocessing -- split list items merged by PDFBox, convert • -> - Markdown list syntax
            normalizeBulletLists();

            float baseFontSize = computeBaseFontSize();

            // Step 1: precompute the code flag for each line
            boolean[] isCodeLine = new boolean[lines.size()];
            for (int i = 0; i < lines.size(); i++) {
                PdfLine line = lines.get(i);
                if (!line.text.isEmpty()) {
                    isCodeLine[i] = line.isMono || looksLikeCodeLine(line.text);
                }
            }

            // Step 1.5: fill gaps of <=1 line between code segments
            fillCodeSegmentGaps(isCodeLine);

            // Step 2: merge consecutive code lines into a code block (at least 2 lines to form a block)
            boolean[] inCodeBlock = new boolean[lines.size()];
            int i = 0;
            while (i < isCodeLine.length) {
                if (isCodeLine[i]) {
                    // Find the start and end of a consecutive code segment
                    int start = i;
                    while (i < isCodeLine.length && isCodeLine[i]) i++;
                    int end = i;
                    // At least 2 consecutive code lines -> mark as a code block
                    if (end - start >= 2) {
                        for (int j = start; j < end; j++) {
                            inCodeBlock[j] = true;
                        }
                    }
                } else {
                    i++;
                }
            }

            // Step 2.5: also promote isolated single lines with strong code features into a code block
            // (e.g. a single-line SQL statement: "ALTER TABLE ... ;")
            for (i = 0; i < lines.size(); i++) {
                if (isCodeLine[i] && !inCodeBlock[i]
                        && isStrongIsolatedCodeLine(lines.get(i).text)) {
                    inCodeBlock[i] = true;
                }
            }

            // Step 2.6: detect and format tables
            // key=starting line index, value=pre-formatted Markdown table text
            Map<Integer, String> tableBlocks = detectTableGroups();

            // Step 3: build the Markdown output
            StringBuilder md = new StringBuilder();
            boolean opened = false;
            boolean prevWasBullet = false; // whether the previous non-empty line is a list item (starts with -)
            int tableSkipUntil = -1;       // skip table rows (already pre-formatted)

            for (i = 0; i < lines.size(); i++) {
                // Table block: output the pre-formatted Markdown table, skipping the original rows
                if (tableBlocks.containsKey(i)) {
                    if (opened) { md.append("```\n"); opened = false; }
                    md.append('\n').append(tableBlocks.get(i)).append('\n');
                    // Skip to the line after the table ends
                    // detectTableGroups recorded endIdx; we can infer it from the tableBlocks value
                    // Simple approach: keep skipping lines with columns set until a non-table line is reached
                    while (i + 1 < lines.size() && lines.get(i + 1).columns != null) {
                        i++;
                    }
                    prevWasBullet = false;
                    continue;
                }

                PdfLine line = lines.get(i);

                // Blank line handling: blank lines inside a code block stay within the fence; blank lines outside close the fence
                if (line.text.isEmpty()) {
                    if (inCodeBlock[i]) {
                        // Blank line inside a code block -> output a blank line to preserve code spacing, without closing the code block
                        md.append('\n');
                        continue;
                    }
                    if (opened) {
                        md.append("```\n");
                        opened = false;
                    }
                    md.append('\n');
                    prevWasBullet = false;
                    continue;
                }

                // Code block output
                if (inCodeBlock[i]) {
                    // Skip standalone language label lines inside the code block ("java", "text", etc.)
                    if (isLangLabelWord(line.text)) continue;

                    if (!opened) {
                        String lang = guessCodeLanguage(inCodeBlock, i);
                        md.append("\n```").append(lang).append('\n');
                        opened = true;
                    }
                    md.append(line.text).append('\n');
                    continue;
                } else if (opened) {
                    md.append("```\n");
                    opened = false;
                }

                // Non-code line: check whether it's a standalone language label before a code block (e.g. "JAVA", "XML")
                // If so, skip this line (the language label has already been incorporated into the fence)
                if (isStandaloneLangLabel(line.text, i, inCodeBlock)) {
                    continue;
                }

                // Heading detection: skip lines that are purely symbols (a Chinese heading may be only 2-4 characters)
                int headingLevel = 0;
                if (line.text.length() >= 2 && !line.text.matches("^[\\d•\\-+\\s]+$")) {
                    headingLevel = computeHeadingLevel(line.fontSize, baseFontSize, line.isBold);
                }
                // Same-size bold heading (level 6): only applies to short lines (<= 20 characters)
                // Bold long lines are more likely emphasized text (such as a slogan), not a heading
                if (headingLevel == 6 && line.text.length() > 20) {
                    headingLevel = 0;
                }
                if (headingLevel > 0) {
                    md.append("#".repeat(headingLevel)).append(' ');
                }

                // Inline styling
                String formatted = line.text;
                if (line.isBold && headingLevel == 0) {
                    formatted = "**" + formatted + "**";
                }

                // List item -> non-list-item transition: requires a blank line separator, otherwise Markdown will absorb the following content into the list
                boolean isBullet = formatted.startsWith("- ");
                if (prevWasBullet && !isBullet && headingLevel == 0) {
                    md.append('\n');
                }

                md.append(formatted).append('\n');
                prevWasBullet = (headingLevel == 0 && isBullet);
            }

            if (opened) {
                md.append("```\n");
            }

            return cleanMarkdown(md.toString());
        }

        /**
         * Scans all lines to detect consecutive table row groups (matching column counts,
         * column X coordinates aligned), and pre-formats them as a Markdown table.
         *
         * @return starting line index -> pre-formatted Markdown table text
         */
        private Map<Integer, String> detectTableGroups() {
            Map<Integer, String> result = new LinkedHashMap<>();

            for (int i = 0; i < lines.size(); i++) {
                PdfLine line = lines.get(i);
                if (line.columns == null || line.columns.length < 2) continue;
                float[] templateColX = line.columnX; // the first row's column X positions serve as the template

                // Find the end of the consecutive table rows
                int start = i;
                int end = i + 1;
                while (end < lines.size()) {
                    PdfLine nextLine = lines.get(end);

                    // Attempt 1: direct match (same column count + X aligned)
                    if (nextLine.columns != null
                            && nextLine.columns.length == line.columns.length
                            && columnsAligned(line.columns, line.columnX,
                                              nextLine.columns, nextLine.columnX)) {
                        end++;
                        continue;
                    }

                    // Attempt 2: template reassignment -- handles rows where commas/semicolons within
                    // a cell produce small gaps misjudged as column boundaries, or empty cells cause
                    // a mismatched column count
                    if (nextLine.posXs != null && nextLine.posTexts != null && templateColX != null) {
                        applyColumnTemplate(nextLine, templateColX);
                        if (nextLine.columns != null
                                && nextLine.columns.length == line.columns.length
                                && columnsAligned(line.columns, line.columnX,
                                                  nextLine.columns, nextLine.columnX)) {
                            end++;
                            continue;
                        }
                    }

                    break;
                }

                int rowCount = end - start;
                if (rowCount < 2) { i = end - 1; continue; } // A single row does not count as a table

                // Ensure all rows use the same column template (the first row's columnX)
                for (int r = start; r < end; r++) {
                    PdfLine row = lines.get(r);
                    if (row.columnX != templateColX) {
                        row.columnX = templateColX;
                    }
                }

                // Format as a Markdown table
                StringBuilder table = new StringBuilder();

                for (int r = start; r < end; r++) {
                    PdfLine row = lines.get(r);
                    table.append("|");
                    for (String col : row.columns) {
                        table.append(' ').append(col.isEmpty() ? " " : col).append(" |");
                    }
                    table.append('\n');

                    // Add a separator row after the header
                    if (r == start) {
                        table.append("|");
                        for (int c = 0; c < row.columns.length; c++) {
                            table.append(" --- |");
                        }
                        table.append('\n');
                    }
                }

                result.put(start, table.toString());
                i = end - 1; // skip to the end of the table group
            }

            return result;
        }

        /**
         * Uses the first row's column X coordinate template to reassign this row's original
         * TextPosition entries into columns. Used to fix the following scenarios:
         * <ul>
         *   <li>A small gap produced by a comma within a cell being misjudged as a column boundary</li>
         *   <li>An empty cell causing the row to be missing a text fragment at that X coordinate -> too few columns</li>
         * </ul>
         * <p>Algorithm: compares each TextPosition's X coordinate against the template
         * column midpoints, assigning it to the nearest column. Empty columns naturally
         * produce an empty string.</p>
         */
        private void applyColumnTemplate(PdfLine line, float[] templateColX) {
            if (line.posXs == null || line.posTexts == null || templateColX == null) return;
            if (templateColX.length < 2) return;

            // Compute column boundary midpoints (used to assign each TextPosition to the nearest column)
            float[] midpoints = new float[templateColX.length - 1];
            for (int i = 0; i < templateColX.length - 1; i++) {
                midpoints[i] = (templateColX[i] + templateColX[i + 1]) / 2;
            }

            StringBuilder[] colTexts = new StringBuilder[templateColX.length];
            Arrays.setAll(colTexts, i -> new StringBuilder());

            for (int i = 0; i < line.posXs.length; i++) {
                float x = line.posXs[i];
                String text = line.posTexts[i];

                // Binary-search-style lookup of the owning column: find the first midpoint > x, column index = midpoint index + 1
                int col = templateColX.length - 1; // default to the last column
                for (int m = 0; m < midpoints.length; m++) {
                    if (x <= midpoints[m]) {
                        col = m;
                        break;
                    }
                }
                colTexts[col].append(text);
            }

            String[] cols = new String[templateColX.length];
            for (int c = 0; c < templateColX.length; c++) {
                cols[c] = colTexts[c].toString().strip();
            }

            line.columns = cols;
            line.columnX = templateColX;
        }

        /**
         * Checks whether two rows' column X coordinates are aligned (tolerance <= 5pt).
         */
        private boolean columnsAligned(String[] colsA, float[] xA, String[] colsB, float[] xB) {
            if (xA == null || xB == null) return true; // lenient handling when no X data is available
            if (xA.length != xB.length) return false;
            for (int i = 0; i < xA.length; i++) {
                if (Math.abs(xA[i] - xB[i]) > 5f) return false;
            }
            return true;
        }

        /**
         * Determines whether a line is a code line based on content features (used for PDFs
         * where the font is not monospaced)
         *
         * <p>Does not check patterns containing a colon (such as YAML / task descriptions), to
         * avoid false positives.</p>
         */
        private boolean looksLikeCodeLine(String text) {
            String t = text.strip();

            // A standalone brace is a strong code signal (must be placed before the length check)
            if (t.equals("{") || t.equals("}")) return true;

            if (t.length() < 3) return false;

            // Starts with a Java/C# keyword or annotation
            if (t.startsWith("public ") || t.startsWith("private ") || t.startsWith("protected ")
                    || t.startsWith("class ") || t.startsWith("interface ") || t.startsWith("import ")
                    || t.startsWith("package ") || t.startsWith("return ") || t.startsWith("throw ")
                    || t.matches("^(@\\w+.*)")) {
                return true;
            }

            // Ends with { } ; is a strong code signal
            if (t.endsWith("{") || t.endsWith("}") || t.endsWith(";")) {
                return true;
            }

            // Method chain call: starts with . (e.g. .antMatchers() / .permitAll())
            if (t.startsWith(".") && t.length() > 2) {
                return true;
            }

            // Indented line + contains ( ) = or a keyword
            if (t.matches("^\\s{2,}.*") && (t.contains("(") || t.contains("=")
                    || t.matches(".*\\b(new|if|for|while|try|catch|throw|throws|extends|implements)\\b.*"))) {
                return true;
            }

            // XML / HTML tags
            if (t.startsWith("<") && t.contains(">")) return true;    // <tag>...</tag>
            if (t.startsWith("</")) return true;                       // </tag>
            if (t.startsWith("<?xml")) return true;                    // <?xml ...?>

            // Starts with an SQL keyword (uppercase)
            if (t.matches("^(SELECT|INSERT|UPDATE|DELETE|CREATE|ALTER|DROP|FROM|WHERE|ORDER BY|GROUP BY|HAVING|JOIN|LEFT JOIN|RIGHT JOIN)\\b.*")) {
                return true;
            }

            // Comment line
            if (t.startsWith("//") || t.startsWith("/*") || t.startsWith("* ") || t.startsWith("<!--")) {
                return true;
            }

            // String literal (a String argument to a code method)
            if (t.startsWith("\"") && (t.endsWith("\"") || t.endsWith("\","))) return true;
            // Enum constant / static reference (HttpMethod.GET, etc.)
            if (t.matches("^\\w+\\.\\w+,?$") && t.length() > 5) return true;
            // URL path / wildcard (code-like arguments such as "/job/**", "/*.html")
            if (t.matches("^[ \"']*[/#.*\\-!\\w]+[ \"']*[,)]?$") && t.length() <= 60 && t.contains("/")) return true;

            // Command line / shell command: contains --flag (a CLI long option) or a pattern with an = assignment and quotes
            if (t.matches(".*\\s--\\w+.*")) return true;                     // pt-online-schema-change --alter "..." D=... --execute
            if (t.contains("\"") && t.contains("=") && !t.contains("：")     // contains a quote + equals sign + no Chinese colon -> command/config
                    && !t.matches(".*[\\u4e00-\\u9fff].*")) return true;

            return false;
        }

        /**
         * Determines whether an isolated code line is "strong" enough to be worth wrapping in
         * its own code block.
         * <p>Used to handle scenarios like a single-line SQL statement / single-line command
         * (which would normally require >= 2 lines to form a block).</p>
         */
        private boolean isStrongIsolatedCodeLine(String text) {
            String t = text.strip();

            // A language-label line mixed with code content (e.g. "SQL -- comment\nALTER TABLE ...;")
            // After stripping the leading language label, check whether the remaining content looks like code
            String afterLabel = stripLeadingLangLabel(t);
            if (afterLabel != null && !afterLabel.isEmpty()) {
                // The remaining content starts with an SQL keyword -> a strong signal
                if (afterLabel.matches("^(SELECT|INSERT|UPDATE|DELETE|CREATE|ALTER|DROP)\\b.*")) return true;
                // Starts with a comment marker + followed by an SQL keyword
                if (afterLabel.startsWith("--") || afterLabel.startsWith("//")) return true;
            }

            // The line contains SQL DDL (ALTER TABLE, CREATE TABLE, ADD INDEX, etc.) and ends with ;
            if (t.endsWith(";") && t.matches(".*\\b(ALTER TABLE|CREATE TABLE|DROP TABLE|ADD INDEX|ADD COLUMN|CREATE INDEX)\\b.*")) return true;

            // Starts with a language label + ends with ; or has code features
            if (afterLabel != null && (t.endsWith(";") || t.endsWith("{") || t.endsWith("}"))) return true;

            // The line contains a config/assignment (=) + quotes + no Chinese colon -> a command/config line
            if (t.contains("=") && t.contains("\"") && !t.contains("：")
                    && !t.matches(".*[\\u4e00-\\u9fff].*")) return true;

            return false;
        }

        /**
         * If the text starts with a known language label (case-insensitive), strips the label
         * and returns the remaining content; otherwise returns null.
         */
        private String stripLeadingLangLabel(String text) {
            String t = text.strip();
            String lower = t.toLowerCase();
            for (String label : LANG_LABELS) {
                if (lower.startsWith(label)) {
                    int end = label.length();
                    // The label must be followed by a space, newline, or comment marker
                    if (end < t.length()) {
                        char next = t.charAt(end);
                        if (next == ' ' || next == '\t' || next == '-' || next == '/' || next == '\n') {
                            return t.substring(end).stripLeading();
                        }
                    }
                }
            }
            return null;
        }

        /**
         * Scans the first few lines of a code block to infer the programming language for the Markdown fence.
         */
        private String guessCodeLanguage(boolean[] inCodeBlock, int fromIdx) {
            int end = fromIdx;
            while (end < inCodeBlock.length && inCodeBlock[end]) end++;

            int javaScore = 0, sqlScore = 0, xmlScore = 0;
            int checkLines = Math.min(end - fromIdx, 8);

            for (int i = fromIdx; i < fromIdx + checkLines; i++) {
                String t = lines.get(i).text.strip();
                if (t.matches(".*\\b(public|private|protected|class|void|import|package|super|this|return|static|final|new|try|catch|throw|throws|extends|implements|@Override|@Autowired|@Service|@Component|@Configuration|@Bean)\\b.*")) javaScore++;
                if (t.endsWith(";")) javaScore++;
                // SQL keyword at the start of the line
                if (t.matches("^\\s*(SELECT|INSERT|UPDATE|DELETE|CREATE|ALTER|DROP|FROM|WHERE|ORDER BY|GROUP BY|HAVING|JOIN|LEFT JOIN|RIGHT JOIN)\\b.*")) sqlScore += 2;
                // SQL DDL keyword within the line (e.g. "SQL -- comment\nALTER TABLE ...")
                if (t.matches(".*\\b(ALTER TABLE|CREATE TABLE|DROP TABLE|ADD INDEX|ADD COLUMN|CREATE INDEX|DROP INDEX)\\b.*")) sqlScore += 3;
                if (t.startsWith("<?xml")) xmlScore += 3;
                else if (t.startsWith("</")) xmlScore += 2;
                else if (t.startsWith("<") && t.endsWith(">") && t.length() > 5) xmlScore++;
            }

            if (xmlScore > javaScore && xmlScore > sqlScore && xmlScore >= 2) return "xml";
            if (sqlScore > javaScore && sqlScore > xmlScore && sqlScore >= 2) return "sql";
            if (javaScore > 0) return "java";
            return "text";
        }

        /**
         * Determines whether the current line is a standalone language label before a code
         * block (such as "JAVA", "XML", "SQL"). When the condition is met, the line should be
         * absorbed into the fence's language label rather than output as visible text.
         */
        /** Common programming language label keywords */
        private static final Set<String> LANG_LABELS = Set.of(
                "java", "xml", "sql", "python", "yaml", "json", "text", "bash", "shell",
                "javascript", "typescript", "html", "css", "kotlin", "groovy", "scala",
                "rust", "go", "c", "cpp", "ruby", "php", "swift", "markdown", "properties"
        );

        /**
         * Determines whether a line of text is a standalone language label word (used for filtering within code blocks).
         */
        private boolean isLangLabelWord(String text) {
            String t = text.strip().toLowerCase();
            if (t.length() < 2 || t.length() > 20) return false;
            if (!t.matches("^[a-z#]+$")) return false;
            return LANG_LABELS.contains(t.startsWith("#") ? t.substring(1) : t);
        }

        private boolean isStandaloneLangLabel(String text, int idx, boolean[] inCodeBlock) {
            // Find the next non-empty line
            int peek = idx + 1;
            while (peek < lines.size() && lines.get(peek).text.isEmpty()) peek++;
            // The next non-empty line is not code -> not a label
            // Relaxed condition: accept even outside a code block if the next line looks like a code line
            if (peek >= lines.size()) return false;
            if (!inCodeBlock[peek] && !looksLikeCodeLine(lines.get(peek).text)) return false;

            // Whether the current line is a standalone language label (case-insensitive)
            String t = text.strip().toLowerCase();
            if (t.length() < 2 || t.length() > 20) return false;
            // Strip a possible # prefix
            if (t.startsWith("#")) t = t.substring(1);
            return LANG_LABELS.contains(t);
        }

        /**
         * Fills gaps between code segments.
         * <ul>
         *   <li>Gap &lt;= 2 lines and all trivial -> merge directly</li>
         *   <li>The code context suggests a continuation (the previous line ends with . ( , or
         *       the next line starts with . )) -> allows merging gaps of &lt;= 20 lines</li>
         *   <li>Otherwise, do not merge</li>
         * </ul>
         */
        private void fillCodeSegmentGaps(boolean[] isCode) {
            for (int i = 0; i < isCode.length; i++) {
                if (!isCode[i] || lines.get(i).text.isEmpty()) continue;

                // Find the end of the current code segment
                int segmentEnd = i;
                while (segmentEnd + 1 < isCode.length && isCode[segmentEnd + 1]
                        && !lines.get(segmentEnd + 1).text.isEmpty()) {
                    segmentEnd++;
                }

                // Skip genuinely blank lines
                int gapStart = segmentEnd + 1;
                while (gapStart < isCode.length && lines.get(gapStart).text.isEmpty()) {
                    gapStart++;
                }
                if (gapStart >= isCode.length) break;

                // Find the start of the next code segment
                int nextSegmentStart = gapStart;
                while (nextSegmentStart < isCode.length && !isCode[nextSegmentStart]) {
                    nextSegmentStart++;
                }
                if (nextSegmentStart >= isCode.length) break;

                int gapLength = nextSegmentStart - gapStart;
                boolean isContinuation = isCodeContinuation(segmentEnd, nextSegmentStart);

                if (isContinuation && gapLength <= 20 && allTrivialGapLines(gapStart, nextSegmentStart)) {
                    // Code continuation: allow a larger merge, marking all lines in the gap (including blank lines) as code
                    for (int j = segmentEnd + 1; j < nextSegmentStart; j++) {
                        isCode[j] = true;
                    }
                    i = nextSegmentStart - 1;
                } else if (!isContinuation && gapLength <= 2 && allTrivialGapLines(gapStart, nextSegmentStart)) {
                    // Short gap merge, marking all lines in the gap (including blank lines) as code
                    for (int j = segmentEnd + 1; j < nextSegmentStart; j++) {
                        isCode[j] = true;
                    }
                    i = nextSegmentStart - 1;
                } else {
                    i = segmentEnd;
                }
            }
        }

        /**
         * Determines whether two code segments are in a continuation relationship.
         * The preceding segment ends with . ( , (an unfinished method chain), or the
         * following segment starts with . ) (a method chain continuing).
         */
        private boolean isCodeContinuation(int beforeIdx, int afterIdx) {
            String before = lines.get(beforeIdx).text.strip();
            String after = lines.get(afterIdx).text.strip();
            return before.endsWith(".") || before.endsWith("(") || before.endsWith(",")
                    || after.startsWith(".") || after.startsWith(")");
        }

        /**
         * Determines whether a gap line is a "mergeable" trivial line:
         * short text, no Chinese, no natural sentence punctuation -> likely a code
         * formatting artifact (line number / separator, etc.)
         * long text, containing Chinese, with sentence punctuation -> genuine body content,
         * should not be merged
         */
        private boolean allTrivialGapLines(int from, int to) {
            for (int j = from; j < to; j++) {
                String t = lines.get(j).text.strip();
                if (t.isEmpty()) continue; // let blank lines pass
                // Page boundary marker -> must not be merged into a code block
                if (t.startsWith("__PAGE_BREAK__")) return false;
                // Contains Chinese characters -> body content, must not be merged
                if (t.matches(".*[\\u4e00-\\u9fff].*")) return false;
                // Contains natural sentence punctuation -> body content, must not be merged
                if (t.matches(".*[。！？；：、，…].*")) return false;
                // Longer than 80 characters -> body content, must not be merged
                if (t.length() > 80) return false;
                // More than 8 words -> a body sentence, must not be merged
                if (t.split("\\s+").length > 8) return false;
            }
            return true;
        }

        /**
         * Computes the body text baseline font size: takes the 25th percentile font size.
         * Using the median is easily skewed by large heading font sizes, pushing the body
         * baseline too high and making heading detection fail.
         */
        private float computeBaseFontSize() {
            List<Float> sizes = new ArrayList<>();
            for (PdfLine line : lines) {
                if (!line.text.isEmpty() && line.fontSize > 0) {
                    sizes.add(line.fontSize);
                }
            }
            if (sizes.isEmpty()) return 12f;

            Collections.sort(sizes);
            // Take the 25th percentile (lower quartile): body lines make up the bulk, heading font sizes are toward the tail
            int p25Index = Math.max(0, sizes.size() / 4);
            return sizes.get(p25Index);
        }

        /**
         * Infers heading level (1-6) based on font size ratio; 0 means not a heading.
         * Chinese PDFs often use "same font size but bold" as a heading style -- this must
         * also be recognized.
         */
        private int computeHeadingLevel(float fontSize, float baseFontSize, boolean bold) {
            if (baseFontSize <= 0) return 0;
            float ratio = fontSize / baseFontSize;

            // Font size noticeably larger -> heading
            if (ratio >= 2.0) return 1;
            if (ratio >= 1.6) return 2;
            if (ratio >= 1.35) return 3;
            if (ratio >= 1.2) return bold ? 4 : 0;
            if (ratio >= 1.1 && bold) return 5;

            // Same font size but bold -> a common heading style in Chinese PDFs (e.g. "Preface", "1.", etc.)
            if (ratio >= 0.95 && bold && fontSize >= 10f) return 6;

            return 0;
        }

        private boolean isMonospacePdfFont(String fontName) {
            return fontName.contains("courier")
                    || fontName.contains("consolas")
                    || fontName.contains("monaco")
                    || fontName.contains("menlo")
                    || fontName.contains("monospace")
                    || fontName.contains("source code")
                    || fontName.contains("fira code")
                    || fontName.contains("jetbrains")
                    || fontName.contains("dejavu sans mono")
                    || fontName.contains("lucida console");
        }

        private static String cleanMarkdown(String text) {
            if (text == null) return "";
            return text.replaceAll("\\n{4,}", "\n\n\n").trim();
        }
    }

    private String parsePdf(MultipartFile file) throws Exception {
        try (InputStream is = file.getInputStream();
             PDDocument document = PDDocument.load(is)) {

            int pageCount = document.getNumberOfPages();
            log.info("PDF loaded successfully: pages={}", pageCount);

            // Step 1: extract images and upload them, recording each image's Y coordinate position
            Map<Integer, List<ImagePlacement>> pageImages = extractAndUploadImages(document);

            // Step 2: extract text (including page boundary markers + Y coordinates), and insert images by Y coordinate
            CollectingPDFStripper stripper = new CollectingPDFStripper();
            stripper.setImagePlacements(pageImages);
            String text = stripper.getText(document);

            // Fallback: if the custom stripper extracts nothing, fall back to the standard PDFTextStripper
            if (text == null || text.isBlank()) {
                log.warn("Custom PDFStripper extraction was empty, falling back to standard PDFTextStripper");
                PDFTextStripper fallbackStripper = new PDFTextStripper();
                fallbackStripper.setSortByPosition(true);
                fallbackStripper.setSuppressDuplicateOverlappingText(true);
                text = fallbackStripper.getText(document);
                if (text != null) {
                    text = cleanText(text);
                }
            }

            return text != null ? text : "";
        }
    }

    /**
     * Extracts images from each page of the PDF document, uploads them to the file server, and
     * records each image's Y coordinate on the page.
     *
     * <p>Skips images smaller than 50x50 pixels (usually decorative elements such as icons or backgrounds).</p>
     *
     * @return page index -> image position list (sorted by Y coordinate, top to bottom)
     */
    private Map<Integer, List<ImagePlacement>> extractAndUploadImages(PDDocument document) {
        // First find the Y coordinate position of every image on each page
        Map<Integer, Map<COSName, Float>> positionMap = findImagePositions(document);

        Map<Integer, List<ImagePlacement>> pageImages = new LinkedHashMap<>();

        for (int p = 0; p < document.getNumberOfPages(); p++) {
            PDPage page = document.getPage(p);
            PDResources resources;
            try {
                resources = page.getResources();
            } catch (Exception e) {
                log.debug("Could not get resources for page {}: {}", p + 1, e.getMessage());
                continue;
            }
            if (resources == null) continue;

            Map<COSName, Float> pagePositions = positionMap.getOrDefault(p, Collections.emptyMap());
            List<ImagePlacement> placements = new ArrayList<>();
            int imgIdx = 0;

            try {
                // Recursively collect all images (including those nested within Form XObjects)
                Map<COSName, PDImageXObject> allImages = new LinkedHashMap<>();
                collectImageXObjects(resources, allImages);

                for (Map.Entry<COSName, PDImageXObject> entry : allImages.entrySet()) {
                    COSName name = entry.getKey();
                    PDImageXObject image = entry.getValue();

                    // Skip small images (icons, backgrounds, etc.)
                    if (image.getWidth() < 50 || image.getHeight() < 50) continue;

                    try {
                        BufferedImage bufferedImage = image.getImage();
                        if (bufferedImage == null) continue;

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(bufferedImage, "PNG", baos);
                        byte[] imageBytes = baos.toByteArray();

                        String fileName = "pdf_img_p" + (p + 1) + "_" + (imgIdx++) + ".png";
                        String url = fileUploadService.uploadBytes(imageBytes, fileName, "image/png");

                        // Get the image's Y coordinate (parsed from the content stream), defaulting to the top of the page
                        float imageY = pagePositions.getOrDefault(name, 0f);
                        placements.add(new ImagePlacement(imageY, url));

                        log.debug("PDF image extracted and uploaded: page={}, image={}, size={}x{}, y={}, url={}",
                                p + 1, imgIdx, image.getWidth(), image.getHeight(), imageY, url);
                    } catch (Exception e) {
                        log.warn("Failed to extract/upload image {} on page {}: {}", imgIdx, p + 1, e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to iterate resources on page {}: {}", p + 1, e.getMessage());
            }

            if (!placements.isEmpty()) {
                // Sort by Y coordinate (top to bottom)
                placements.sort((a, b) -> Float.compare(a.y, b.y));
                pageImages.put(p, placements);
                log.info("Extracted {} images from page {}", placements.size(), p + 1);
            }
        }

        if (!pageImages.isEmpty()) {
            log.info("PDF image extraction complete: {} pages contain images, {} images in total",
                    pageImages.size(),
                    pageImages.values().stream().mapToInt(List::size).sum());
        }
        return pageImages;
    }

    /**
     * Recursively collects all images from PDResources and its Form XObject sub-resources.
     * Many PDF generators (PowerPoint export, LaTeX, etc.) nest images within Form XObjects;
     * iterating only the page's top-level getXObjectNames() would miss these images.
     */
    private void collectImageXObjects(PDResources resources, Map<COSName, PDImageXObject> images) {
        if (resources == null) return;
        try {
            for (COSName name : resources.getXObjectNames()) {
                PDXObject xobj;
                try {
                    xobj = resources.getXObject(name);
                } catch (Exception e) {
                    continue;
                }
                if (xobj instanceof PDImageXObject image) {
                    images.put(name, image);
                } else if (xobj instanceof PDFormXObject form) {
                    // Recurse into the Form XObject's sub-resources
                    try {
                        collectImageXObjects(form.getResources(), images);
                    } catch (Exception ignored) {
                        // Some Form XObjects have no separate Resources dictionary
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to recursively collect images: {}", e.getMessage());
        }
    }

    /**
     * Parses each page's content stream in the PDF to find the Y coordinate at which each image XObject is drawn.
     *
     * <p>By tracking the cm (transformation matrix) and Do (invoke XObject) operators,
     * computes each image's Y position on the page (top-down from the page top, consistent with getYDirAdj).</p>
     *
     * @return page index -> (image resource name -> Y coordinate)
     */
    private Map<Integer, Map<COSName, Float>> findImagePositions(PDDocument document) {
        Map<Integer, Map<COSName, Float>> allPositions = new LinkedHashMap<>();

        for (int p = 0; p < document.getNumberOfPages(); p++) {
            PDPage page = document.getPage(p);
            float pageHeight = page.getMediaBox().getHeight();
            Map<COSName, Float> pagePositions = new LinkedHashMap<>();

            try {
                PDFStreamParser parser = new PDFStreamParser(page);
                parser.parse();
                List<Object> tokens = parser.getTokens();
                if (tokens.isEmpty()) continue;

                float tx = 0, ty = 0;
                Deque<float[]> saved = new ArrayDeque<>();

                for (int i = 0; i < tokens.size(); i++) {
                    Object token = tokens.get(i);
                    String opName = getOperatorName(token);

                    if (opName != null) {
                        switch (opName) {
                            case "q" -> saved.push(new float[]{tx, ty});
                            case "Q" -> {
                                if (!saved.isEmpty()) {
                                    float[] prev = saved.pop();
                                    tx = prev[0]; ty = prev[1];
                                }
                            }
                            case "Do" -> {
                                // The previous token is the XObject name (COSName)
                                if (i > 0 && tokens.get(i - 1) instanceof COSName xobjName) {
                                    // Convert to page-top-down coordinates
                                    float yDirAdj = pageHeight - ty;
                                    pagePositions.put(xobjName, yDirAdj);
                                }
                            }
                        }
                    } else if (token instanceof COSNumber) {
                        // Check whether this is an "a b c d e f cm" transformation matrix
                        if (i + 6 < tokens.size()
                                && tokens.get(i + 1) instanceof COSNumber
                                && tokens.get(i + 2) instanceof COSNumber
                                && tokens.get(i + 3) instanceof COSNumber
                                && tokens.get(i + 4) instanceof COSNumber
                                && tokens.get(i + 5) instanceof COSNumber
                                && isOperatorName(tokens.get(i + 6), "cm")) {
                            float a = ((COSNumber) tokens.get(i + 0)).floatValue();
                            float b = ((COSNumber) tokens.get(i + 1)).floatValue();
                            float c = ((COSNumber) tokens.get(i + 2)).floatValue();
                            float d = ((COSNumber) tokens.get(i + 3)).floatValue();
                            float e = ((COSNumber) tokens.get(i + 4)).floatValue();
                            float f = ((COSNumber) tokens.get(i + 5)).floatValue();
                            // cm concatenates the matrix: CTM' = CTM × [a b c d e f]
                            float newTx = a * tx + c * ty + e;
                            float newTy = b * tx + d * ty + f;
                            tx = newTx;
                            ty = newTy;
                            i += 6; // skip the already-processed matrix values
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Failed to parse content stream on page {} to get image positions: {}", p + 1, e.getMessage());
            }

            if (!pagePositions.isEmpty()) {
                allPositions.put(p, pagePositions);
            }
        }

        return allPositions;
    }

    private static String getOperatorName(Object token) {
        if (token instanceof Operator op) return op.getName();
        return null;
    }

    private static boolean isOperatorName(Object token, String expected) {
        if (token instanceof Operator op) return expected.equals(op.getName());
        return false;
    }

    private String parseDocx(MultipartFile file) throws Exception {
        try (InputStream is = file.getInputStream();
             XWPFDocument document = new XWPFDocument(is)) {

            StringBuilder sb = new StringBuilder();

            // Use getBodyElements() to preserve the original order of paragraphs and tables
            for (IBodyElement element : document.getBodyElements()) {
                if (element instanceof XWPFTable table) {
                    sb.append(formatTable(table));
                } else if (element instanceof XWPFParagraph paragraph) {
                    String style = paragraph.getStyle();
                    String styleLower = (style != null) ? style.toLowerCase() : "";

                    // Empty paragraph
                    if (paragraph.getText() == null || paragraph.getText().isBlank()) {
                        sb.append('\n');
                        continue;
                    }

                    // Heading detection: outlineLvl > style name > "heading"/"标题" (the "标题" Chinese style name check is functional, matched against actual Word style metadata)
                    int headingLevel = detectHeadingLevel(paragraph, styleLower);
                    if (headingLevel > 0) {
                        sb.append("#".repeat(Math.min(headingLevel, 6)))
                                .append(' ')
                                .append(formatInlineRuns(paragraph))
                                .append('\n');
                        continue;
                    }

                    // List item
                    String listPrefix = getListPrefix(paragraph, styleLower);
                    if (!listPrefix.isEmpty()) {
                        sb.append(listPrefix).append(' ')
                                .append(formatInlineRuns(paragraph))
                                .append('\n');
                        continue;
                    }

                    // Code block detection
                    if (isCodeBlock(paragraph)) {
                        sb.append("```\n")
                                .append(extractCodeText(paragraph))
                                .append("\n```\n\n");
                        continue;
                    }

                    // Normal paragraph
                    sb.append(formatInlineRuns(paragraph)).append("\n\n");
                }
            }

            return cleanText(sb.toString());
        }
    }

    // ── DOCX helper methods ────────────────────────────────────

    /**
     * Parses inline formatting within a paragraph: bold / italic / inline code / hyperlink / line break
     *
     * <p>Line break handling: for a run containing w:br, its XML text is parsed with a regex,
     * extracting w:t and w:br in document order, avoiding the loss of line-break positions
     * that would occur if run.text() were concatenated directly.</p>
     */
    private String formatInlineRuns(XWPFParagraph paragraph) {
        StringBuilder sb = new StringBuilder();
        List<XWPFRun> runs = paragraph.getRuns();

        for (XWPFRun run : runs) {
            // Hyperlink run
            if (run instanceof XWPFHyperlinkRun) {
                String text = run.text();
                if (text != null && !text.isEmpty()) {
                    sb.append('[').append(text).append(']').append("()");
                }
                continue;
            }

            String text = run.text();
            if (text == null || text.isEmpty()) continue;

            // Detect whether this run contains a w:br
            if (runHasBreak(run)) {
                appendRunWithBreaks(sb, run);
            } else {
                sb.append(applyInlineFormat(run, text));
            }
        }

        return sb.toString();
    }

    // Matches the text within w:t, or the empty w:br / w:cr elements
    private static final Pattern RUN_BREAK_PATTERN =
            Pattern.compile("<w:t[^>]*>([^<]*)</w:t>|<w:br\\s*/>|<w:cr\\s*/>");

    /**
     * Determines whether an XWPFRun contains a line-break element (w:br)
     */
    private boolean runHasBreak(XWPFRun run) {
        try {
            return !run.getCTR().getBrList().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Parses a run's XML with a regex, outputting w:t / w:br in their original order
     */
    private void appendRunWithBreaks(StringBuilder sb, XWPFRun run) {
        try {
            String xml = run.getCTR().xmlText();
            Matcher m = RUN_BREAK_PATTERN.matcher(xml);
            while (m.find()) {
                String seg = m.group(1);
                if (seg != null) {
                    // w:t — a text segment
                    if (!seg.isEmpty()) {
                        sb.append(applyInlineFormat(run, seg));
                    }
                } else {
                    // w:br / w:cr — a line break
                    sb.append('\n');
                }
            }
        } catch (Exception e) {
            // Fallback: concatenate using run.text() directly
            String text = run.text();
            if (text != null && !text.isEmpty()) {
                sb.append(applyInlineFormat(run, text));
            }
        }
    }

    /**
     * Applies inline format markers to a single plain-text segment
     */
    private String applyInlineFormat(XWPFRun run, String text) {
        if (run == null) return text;

        boolean isBold = run.isBold();
        boolean isItalic = run.isItalic();
        boolean isMono = isMonospaceFont(run);

        if (isMono) return "`" + text + "`";
        if (isBold && isItalic) return "***" + text + "***";
        if (isBold) return "**" + text + "**";
        if (isItalic) return "*" + text + "*";
        return text;
    }

    /**
     * Table -> Markdown table (first row as header + separator row)
     */
    private String formatTable(XWPFTable table) {
        StringBuilder sb = new StringBuilder("\n");
        int numRows = table.getRows().size();
        if (numRows == 0) return "";

        // Compute the column count (the maximum across all rows)
        int maxCols = 0;
        for (XWPFTableRow row : table.getRows()) {
            if (row.getTableCells().size() > maxCols) {
                maxCols = row.getTableCells().size();
            }
        }
        if (maxCols == 0) return "";

        for (int r = 0; r < numRows; r++) {
            XWPFTableRow row = table.getRow(r);
            StringBuilder rowSb = new StringBuilder("|");
            for (int c = 0; c < maxCols; c++) {
                String cellText = "";
                if (c < row.getTableCells().size()) {
                    cellText = row.getCell(c).getText().trim()
                            .replace("\n", " ");
                }
                rowSb.append(' ').append(cellText).append(" |");
            }
            sb.append(rowSb).append('\n');

            // Add a separator row after the header
            if (r == 0) {
                StringBuilder sep = new StringBuilder("|");
                for (int c = 0; c < maxCols; c++) {
                    sep.append(" --- |");
                }
                sb.append(sep).append('\n');
            }
        }
        sb.append('\n');
        return sb.toString();
    }

    /**
     * Determines whether a paragraph is a list item, returning the Markdown list prefix (an empty string means it is not a list)
     */
    private String getListPrefix(XWPFParagraph paragraph, String styleLower) {
        // Determine via the style
        if (styleLower.contains("list") || styleLower.contains("bullet")) {
            int indent = getListIndent(paragraph);
            return "    ".repeat(indent) + "-";
        }
        if (styleLower.contains("number")) {
            int indent = getListIndent(paragraph);
            return "    ".repeat(indent) + "1.";
        }

        // Determine via whether the XML has numPr (cannot distinguish numbered/bullet, defaults to -)
        if (paragraph.getCTP() != null && paragraph.getCTP().getPPr() != null) {
            var numPr = paragraph.getCTP().getPPr().getNumPr();
            if (numPr != null && numPr.getNumId() != null) {
                int indent = getListIndent(paragraph);
                return "    ".repeat(indent) + "-";
            }
        }

        return "";
    }

    /**
     * Gets the list indentation level (0-based)
     */
    private int getListIndent(XWPFParagraph paragraph) {
        if (paragraph.getCTP() != null
                && paragraph.getCTP().getPPr() != null
                && paragraph.getCTP().getPPr().getNumPr() != null
                && paragraph.getCTP().getPPr().getNumPr().getIlvl() != null) {
            return paragraph.getCTP().getPPr().getNumPr().getIlvl().getVal().intValue();
        }
        return 0;
    }

    /**
     * Determines whether a paragraph is a code block:
     * 1. The style name contains code / html / pre / src / source / 源代码 (Chinese for "source code")
     * 2. The paragraph has shading (w:shd) and all runs use a monospace font
     * 3. All runs use a monospace font
     */
    private boolean isCodeBlock(XWPFParagraph paragraph) {
        // Determine via the style name
        String style = paragraph.getStyle();
        if (style != null) {
            String s = style.toLowerCase();
            if (s.contains("code") || s.contains("html")
                    || s.contains("pre") || s.contains("src")
                    || s.contains("source") || s.contains("源代码")
                    || s.contains("program")) {
                return true;
            }
        }

        List<XWPFRun> runs = paragraph.getRuns();
        if (runs.isEmpty()) return false;

        // Determine via paragraph shading (Word code blocks usually have gray/dark shading)
        boolean hasShading = false;
        try {
            if (paragraph.getCTP() != null
                    && paragraph.getCTP().getPPr() != null
                    && paragraph.getCTP().getPPr().getShd() != null) {
                hasShading = true;
            }
        } catch (Exception ignored) {
        }

        // Monospace font detection
        boolean allMono = true;
        boolean anyMono = false;
        for (XWPFRun run : runs) {
            if (run.text() != null && !run.text().isBlank()) {
                if (isMonospaceFont(run)) {
                    anyMono = true;
                } else {
                    allMono = false;
                }
            }
        }

        // Has shading + at least some runs are monospace -> code block
        if (hasShading && anyMono) return true;

        // All runs are monospace -> code block
        return allMono && anyMono;
    }

    /**
     * Determines whether a run uses a monospace font (Courier / Consolas / monospace / Menlo / Monaco)
     */
    private boolean isMonospaceFont(XWPFRun run) {
        String font = run.getFontFamily();
        if (font == null) return false;
        String f = font.toLowerCase();
        return f.contains("courier")
                || f.contains("consolas")
                || f.contains("monaco")
                || f.contains("menlo")
                || f.contains("monospace")
                || f.contains("source code")
                || f.contains("fira code")
                || f.contains("jetbrains");
    }

    /**
     * Extracts code block text, preserving inline line breaks (w:br). Parses each run's XML with a regex.
     */
    private String extractCodeText(XWPFParagraph paragraph) {
        StringBuilder sb = new StringBuilder();
        for (XWPFRun run : paragraph.getRuns()) {
            String text = run.text();
            if (text == null || text.isEmpty()) continue;

            if (runHasBreak(run)) {
                try {
                    String xml = run.getCTR().xmlText();
                    Matcher m = RUN_BREAK_PATTERN.matcher(xml);
                    while (m.find()) {
                        String seg = m.group(1);
                        if (seg != null) {
                            if (!seg.isEmpty()) sb.append(seg);
                        } else {
                            sb.append('\n');
                        }
                    }
                } catch (Exception e) {
                    sb.append(text);
                }
            } else {
                sb.append(text);
            }
        }
        return sb.toString().trim();
    }

    private String parseXlsx(MultipartFile file) throws Exception {
        try (InputStream is = file.getInputStream();
             XSSFWorkbook workbook = new XSSFWorkbook(is)) {

            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                var sheet = workbook.getSheetAt(i);
                String sheetName = sheet.getSheetName();
                sb.append("## ").append(sheetName).append("\n\n");

                // Compute this sheet's maximum column count
                int maxCols = 0;
                for (var row : sheet) {
                    if (row.getLastCellNum() > maxCols) {
                        maxCols = row.getLastCellNum();
                    }
                }
                if (maxCols == 0) {
                    sb.append('\n');
                    continue;
                }

                boolean firstRow = true;
                for (var row : sheet) {
                    StringBuilder rowSb = new StringBuilder("|");
                    for (int c = 0; c < maxCols; c++) {
                        var cell = row.getCell(c);
                        String cellValue = getCellStringValue(cell);
                        rowSb.append(' ').append(cellValue).append(" |");
                    }
                    sb.append(rowSb).append('\n');

                    // Add a separator row after the header
                    if (firstRow) {
                        StringBuilder sep = new StringBuilder("|");
                        for (int c = 0; c < maxCols; c++) {
                            sep.append(" --- |");
                        }
                        sb.append(sep).append('\n');
                        firstRow = false;
                    }
                }
                sb.append('\n');
            }

            return sb.toString();
        }
    }

    private String parsePptx(MultipartFile file) throws Exception {
        try (InputStream is = file.getInputStream();
             XMLSlideShow ppt = new XMLSlideShow(is)) {

            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < ppt.getSlides().size(); i++) {
                var slide = ppt.getSlides().get(i);
                sb.append("## Slide ").append(i + 1).append('\n');

                // Recursively process all shapes (including shapes within groups)
                processPptxShapes(slide.getShapes(), sb);
            }

            return cleanText(sb.toString());
        }
    }

    /**
     * Recursively processes the PPTX shape list
     */
    private void processPptxShapes(List<XSLFShape> shapes, StringBuilder sb) {
        for (XSLFShape shape : shapes) {
            if (shape instanceof XSLFGroupShape group) {
                // Group shape: process recursively
                processPptxShapes(group.getShapes(), sb);
            } else if (shape instanceof XSLFTable table) {
                // Table embedded in the slide
                sb.append(formatPptxTable(table));
            } else if (shape instanceof XSLFTextShape textShape) {
                String text = textShape.getText();
                if (text == null || text.isBlank()) continue;

                // Determine whether this is a title (Placeholder.TITLE or the text type is a title)
                boolean isTitle = false;
                try {
                    var placeholder = textShape.getTextType();
                    if (placeholder != null) {
                        String phName = placeholder.name();
                        isTitle = phName.contains("TITLE") || phName.contains("CENTER");
                    }
                } catch (Exception ignored) {
                    // Some shapes have no Placeholder
                }

                // Format the text (processing bold/italic/monospace per run)
                String formatted = formatPptxParagraphs(textShape);
                if (isTitle) {
                    sb.append("### ").append(formatted).append('\n');
                } else {
                    sb.append(formatted).append("\n\n");
                }
            }
        }
    }

    /**
     * Formats the paragraphs and runs within a PPTX text box (bold/italic/inline code)
     */
    private String formatPptxParagraphs(XSLFTextShape textShape) {
        StringBuilder sb = new StringBuilder();

        for (XSLFTextParagraph para : textShape.getTextParagraphs()) {
            boolean hasRuns = !para.getTextRuns().isEmpty();

            if (hasRuns) {
                for (XSLFTextRun run : para.getTextRuns()) {
                    String runText = run.getRawText();
                    if (runText == null || runText.isEmpty()) continue;

                    boolean isBold = run.isBold();
                    boolean isItalic = run.isItalic();
                    boolean isMono = isMonospaceFontPptx(run);

                    String formatted = runText;
                    if (isMono) {
                        formatted = "`" + runText + "`";
                    } else if (isBold && isItalic) {
                        formatted = "***" + runText + "***";
                    } else if (isBold) {
                        formatted = "**" + runText + "**";
                    } else if (isItalic) {
                        formatted = "*" + runText + "*";
                    }
                    sb.append(formatted);
                }
                sb.append('\n');
            } else {
                // No runs, fall back to plain text
                String plain = para.getText();
                if (plain != null && !plain.isBlank()) {
                    sb.append(plain.trim()).append('\n');
                }
            }
        }

        return sb.toString().trim();
    }

    /**
     * PPTX slide-embedded table -> Markdown table
     */
    private String formatPptxTable(XSLFTable table) {
        StringBuilder sb = new StringBuilder("\n");
        int numRows = table.getRows().size();
        if (numRows == 0) return "";

        // Compute the column count
        int maxCols = 0;
        for (XSLFTableRow row : table.getRows()) {
            if (row.getCells().size() > maxCols) {
                maxCols = row.getCells().size();
            }
        }
        if (maxCols == 0) return "";

        for (int r = 0; r < numRows; r++) {
            XSLFTableRow row = table.getRows().get(r);
            StringBuilder rowSb = new StringBuilder("|");
            for (int c = 0; c < maxCols; c++) {
                String cellText = "";
                if (c < row.getCells().size()) {
                    cellText = row.getCells().get(c).getText().trim()
                            .replace("\n", " ");
                }
                rowSb.append(' ').append(cellText).append(" |");
            }
            sb.append(rowSb).append('\n');

            if (r == 0) {
                StringBuilder sep = new StringBuilder("|");
                for (int c = 0; c < maxCols; c++) {
                    sep.append(" --- |");
                }
                sb.append(sep).append('\n');
            }
        }
        sb.append('\n');
        return sb.toString();
    }

    /**
     * PPTX run monospace font detection
     */
    private boolean isMonospaceFontPptx(XSLFTextRun run) {
        String font = run.getFontFamily();
        if (font == null) return false;
        String f = font.toLowerCase();
        return f.contains("courier")
                || f.contains("consolas")
                || f.contains("monaco")
                || f.contains("menlo")
                || f.contains("monospace")
                || f.contains("source code")
                || f.contains("fira code")
                || f.contains("jetbrains");
    }

    private String parsePlainText(MultipartFile file) throws Exception {
        return new String(file.getBytes(), StandardCharsets.UTF_8);
    }

    // ── Utility methods ─────────────────────────────────────────

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    /**
     * Detects the paragraph's heading level. Priority order:
     * 1. The paragraph's outlineLvl attribute (most reliable; Word heading styles always set this)
     * 2. Style name contains "heading" -> extract the number
     * 3. Style name contains "标题" (Chinese for "heading") -> extract the number (Chinese Word style)
     * 4. Numbering pattern + bold/large font -> automatically infer the level
     *    "5. xxx" (depth=1) -> ##
     *    "5.1 xxx" (depth=2) -> ###
     * Returns 1-6 for a heading level, 0 for a non-heading
     */
    private int detectHeadingLevel(XWPFParagraph paragraph, String styleLower) {
        // 1. Paragraph attribute outlineLvl (0-based -> 1-based)
        try {
            if (paragraph.getCTP() != null
                    && paragraph.getCTP().getPPr() != null
                    && paragraph.getCTP().getPPr().getOutlineLvl() != null) {
                int level = paragraph.getCTP().getPPr().getOutlineLvl().getVal().intValue() + 1;
                return Math.min(level, 6);
            }
        } catch (Exception ignored) {
        }

        // 2. Style name contains "heading" -> extract the number
        if (styleLower.contains("heading")) {
            try {
                return Integer.parseInt(styleLower.replaceAll("[^0-9]", ""));
            } catch (NumberFormatException e) {
                return 1;
            }
        }

        // 3. Style name contains "标题" -> extract the number (Chinese Word style)
        if (styleLower.contains("标题")) {
            try {
                return Integer.parseInt(styleLower.replaceAll("[^0-9]", ""));
            } catch (NumberFormatException e) {
                return 1;
            }
        }

        // 4. Numbering pattern inference -- only applies when the text is short (heading trait: usually < 200 characters)
        String plainText = paragraph.getText().trim();
        int patternLevel = detectNumberedHeadingLevel(plainText);
        if (patternLevel > 0 && plainText.length() < 200) {
            return patternLevel;
        }

        return 0;
    }

    /**
     * Infers the heading level from the text's numbering pattern.
     * "5. xxx" / "一、" -> depth 1 -> returns 2
     * "5.1 xxx" / "（一）" -> depth 2 -> returns 3
     * "5.1.1 xxx" -> depth 3 -> returns 4
     * Returns 0 if there is no match
     */
    private int detectNumberedHeadingLevel(String text) {
        if (text == null || text.isEmpty()) return 0;

        // Numeric numbering: 5. / 5.1 / 5.1.1 / 5.1.1.
        if (text.matches("^\\d+\\.[\\s\\u00A0].*")) return 2;
        if (text.matches("^\\d+\\.\\d+[.\\s].*")) return 3;
        if (text.matches("^\\d+\\.\\d+\\.\\d+[.\\s].*")) return 4;

        // Chinese numbering: 一、 / 二、 -> level 1; （一） / （二） -> level 2
        if (text.matches("^[一二三四五六七八九十]+[、\\s].*")) return 2;
        if (text.matches("^[（(][一二三四五六七八九十]+[）)].*")) return 3;

        return 0;
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                double val = cell.getNumericCellValue();
                yield val == Math.floor(val) && !Double.isInfinite(val)
                        ? String.valueOf((long) val)
                        : String.valueOf(val);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield cell.getStringCellValue();
                } catch (Exception e) {
                    yield String.valueOf(cell.getNumericCellValue());
                }
            }
            default -> "";
        };
    }

    private String cleanText(String text) {
        if (text == null) return "";
        return text.replaceAll("\\n{4,}", "\n\n\n").trim();
    }
}
