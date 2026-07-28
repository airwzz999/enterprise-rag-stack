package com.knowledge.base.document.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.http.HttpUtil;
import com.knowledge.base.common.exception.BusinessException;
import com.knowledge.base.document.entity.Category;
import com.knowledge.base.document.entity.Document;
import com.knowledge.base.document.entity.mongodb.DocumentContent;
import com.knowledge.base.document.mapper.CategoryMapper;
import com.knowledge.base.document.service.DocumentContentService;
import com.knowledge.base.document.service.DocumentService;
import com.knowledge.base.document.service.FileUploadService;
import com.knowledge.base.document.service.PdfExportService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * PDF export service implementation class
 *
 * <p>Uses Apache PDFBox to convert document content into PDF format</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Service
public class PdfExportServiceImpl implements PdfExportService {

    @Resource
    private DocumentService documentService;

    @Resource
    private DocumentContentService documentContentService;

    @Resource
    private FileUploadService fileUploadService;

    @Resource
    private CategoryMapper categoryMapper;

    @Value("${file.upload.path:/data/knowledge-base/uploads}")
    private String uploadPath;

    private static final float MARGIN = 50;
    private static final float LINE_HEIGHT = 20;
    private static final float FONT_SIZE = 12;
    private static final float TITLE_FONT_SIZE = 18;
    private static final float SUBTITLE_FONT_SIZE = 10;
    private static final float CODE_FONT_SIZE = 9;
    private static final float CODE_LINE_HEIGHT = 14;
    // One Dark Pro code editor colors
    private static final Color CODE_BG = new Color(40, 44, 52);
    private static final Color CODE_BORDER = new Color(62, 68, 81);
    private static final Color CODE_HEADER_BG = new Color(33, 37, 43);
    private static final Color CODE_TEXT = new Color(171, 178, 191);
    private static final Color TABLE_HEADER_BG = new Color(240, 242, 245);
    private static final Color TABLE_BORDER = new Color(200, 200, 200);

    private static final Pattern MARKDOWN_HEADING = Pattern.compile("^(#{1,6})\\s+(.+)$");
    private static final Pattern MARKDOWN_CODE = Pattern.compile("^```(\\w*)\\s*$");
    private static final Pattern MARKDOWN_BOLD = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern MARKDOWN_ITALIC = Pattern.compile("\\*(.+?)\\*");
    private static final Pattern MARKDOWN_LINK = Pattern.compile("\\[(.+?)\\]\\((.+?)\\)");
    private static final Pattern MARKDOWN_LIST = Pattern.compile("^[-*+]\\s+(.+)$");
    private static final Pattern MARKDOWN_NUMBER_LIST = Pattern.compile("^\\d+\\.\\s+(.+)$");
    private static final Pattern MARKDOWN_IMAGE = Pattern.compile("!\\[([^\\]]*)\\]\\(([^)]+)\\)");
    private static final Pattern MARKDOWN_IMAGE_TITLE = Pattern.compile("!\\[([^\\]]*)\\]\\(([^)]+)\\s+\"([^\"]+)\"\\)");

    private static final float DEFAULT_IMAGE_WIDTH = 400;
    private static final float DEFAULT_IMAGE_HEIGHT = 300;

    /**
     * System Chinese font paths (in priority order)
     */
    private static final String[] CHINESE_FONT_PATHS = {
            "/System/Library/Fonts/Supplemental/Arial Unicode.ttf",  // macOS Arial Unicode (supports Chinese)
            "/Library/Fonts/Arial Unicode.ttf",  // macOS Arial Unicode symlink
            "/Library/Fonts/Songti.ttc",      // macOS Songti
            "/Library/Fonts/STSong.ttf",       // macOS Songti
            "/Library/Fonts/Hiragino Sans GB W3.ttc", // macOS Hiragino Sans GB
            "/usr/share/fonts/wqy/wqy-zenhei.ttc",   // Linux WenQuanYi Zen Hei
            "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",
            "/usr/share/fonts/truetype/simsun/simsun.ttc",  // Linux SimSun
            "C:\\Windows\\Fonts\\simsun.ttc",  // Windows SimSun
            "C:\\Windows\\Fonts\\simhei.ttf"   // Windows SimHei
    };

    @Override
    public String exportDocumentToPdf(Long documentId) {
        log.info("Export document to PDF: documentId={}", documentId);

        Document document = documentService.getById(documentId);
        if (document == null) {
            throw new BusinessException("Document does not exist");
        }

        DocumentContent documentContent = documentContentService.getContentById(document.getContentId());
        String content = documentContent != null ? documentContent.getContent() : "";
        if (content == null || content.isEmpty()) {
            content = "";
        }

        String categoryName = resolveCategoryName(document.getCategoryId());
        byte[] pdfBytes = generatePdf(document.getTitle(), content, document.getAuthorName(),
                categoryName, document.getSummary(), document.getPublishTime());

        String fileName = generatePdfFileName(documentId, document.getTitle());

        String pdfUrl = fileUploadService.uploadBytes(pdfBytes, fileName, "application/pdf");
        log.info("PDF exported successfully: documentId={}, pdfUrl={}", documentId, pdfUrl);

        return pdfUrl;
    }

    @Override
    public byte[] exportDocumentToPdfBytes(Long documentId) {
        log.info("Export document to PDF byte array: documentId={}", documentId);

        Document document = documentService.getById(documentId);
        if (document == null) {
            throw new BusinessException("Document does not exist");
        }

        DocumentContent documentContent = documentContentService.getContentById(document.getContentId());
        String content = documentContent != null ? documentContent.getContent() : "";
        if (content == null || content.isEmpty()) {
            content = "";
        }

        String categoryName = resolveCategoryName(document.getCategoryId());
        return generatePdf(document.getTitle(), content, document.getAuthorName(),
                categoryName, document.getSummary(), document.getPublishTime());
    }

    @Override
    public byte[] batchExportDocuments(List<String> documentIds, String format) {
        log.info("Batch export documents: documentIds={}, format={}", documentIds, format);

        // Convert String IDs to Long, avoiding JavaScript precision loss
        List<Long> longIds = documentIds.stream()
                .map(Long::parseLong)
                .collect(Collectors.toList());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {

            List<Document> documents = documentService.listByIds(longIds);
            log.info("Found {} documents (requested {})", documents.size(), documentIds.size());
            for (Document document : documents) {
                DocumentContent documentContent = documentContentService.getContentById(document.getContentId());
                String content = documentContent != null ? documentContent.getContent() : "";
                if (content == null) content = "";

                String fileName;
                byte[] fileBytes;

                if ("markdown".equalsIgnoreCase(format)) {
                    fileName = sanitizeFileName(document.getTitle()) + ".md";
                    fileBytes = content.getBytes(StandardCharsets.UTF_8);
                } else {
                    String categoryName = resolveCategoryName(document.getCategoryId());
                    byte[] pdfBytes = generatePdf(document.getTitle(), content,
                            document.getAuthorName(), categoryName, document.getSummary(),
                            document.getPublishTime());
                    fileName = generatePdfFileName(document.getId(), document.getTitle());
                    fileBytes = pdfBytes;
                }

                ZipEntry entry = new ZipEntry(fileName);
                zos.putNextEntry(entry);
                zos.write(fileBytes);
                zos.closeEntry();
            }

            zos.finish();
        } catch (IOException e) {
            log.error("Batch export failed", e);
            throw new RuntimeException("Batch export failed: " + e.getMessage());
        }

        log.info("Batch export successful: {} documents in total", documentIds.size());
        return baos.toByteArray();
    }

    private String sanitizeFileName(String title) {
        if (title == null || title.isEmpty()) {
            return "untitled";
        }
        return title.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }

    @Override
    public String generatePdfFileName(Long documentId, String title) {
        String safeTitle = FileUtil.mainName(FileUtil.cleanInvalid(title));
        if (safeTitle == null || safeTitle.isEmpty()) {
            safeTitle = "document";
        }
        if (safeTitle.length() > 50) {
            safeTitle = safeTitle.substring(0, 50);
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return String.format("%s_%s.pdf", safeTitle, timestamp);
    }

    private byte[] generatePdf(String title, String content, String author,
            String categoryName, String summary, LocalDateTime publishTime) {

        // Remove characters unsupported by the PDF font (emoji, etc.)
        title = stripUnsupportedCharacters(title);
        content = stripUnsupportedCharacters(content);
        author = stripUnsupportedCharacters(author);
        categoryName = stripUnsupportedCharacters(categoryName);
        summary = stripUnsupportedCharacters(summary);

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            float yPosition = page.getMediaBox().getHeight() - MARGIN;
            float pageWidth = page.getMediaBox().getWidth() - 2 * MARGIN;
            boolean isFirstPage = true;

            // Try to load a Chinese font, falling back to the default font on failure
            PDType0Font chineseFont = null;
            try {
                chineseFont = loadChineseFont(document);
                log.info("Chinese font loaded successfully");
            } catch (Exception e) {
                log.warn("Could not load a Chinese font, falling back to the default font (Chinese may not be supported): {}", e.getMessage());
            }

            PDPageContentStream contentStream = null;
            try {
                contentStream = new PDPageContentStream(document, page,
                        PDPageContentStream.AppendMode.OVERWRITE, true, true);

                if (isFirstPage) {
                    // ===== Title (supports automatic wrapping) =====
                    String displayTitle = title != null ? title : "Untitled Document";
                    List<String> wrappedTitle = wrapTextByWidth(displayTitle, pageWidth,
                            chineseFont, TITLE_FONT_SIZE);
                    for (String titleLine : wrappedTitle) {
                        contentStream.beginText();
                        if (chineseFont != null) {
                            contentStream.setFont(chineseFont, TITLE_FONT_SIZE);
                        } else {
                            contentStream.setFont(PDType1Font.HELVETICA_BOLD, TITLE_FONT_SIZE);
                        }
                        contentStream.newLineAtOffset(MARGIN, yPosition);
                        contentStream.showText(titleLine);
                        contentStream.endText();
                        yPosition -= LINE_HEIGHT * 1.5f;
                    }
                    yPosition -= LINE_HEIGHT * 0.5f;

                    // ===== Subtitle (author + publish time, supports automatic wrapping) =====
                    String subtitle = buildSubtitle(author, categoryName, publishTime);
                    List<String> wrappedSubtitle = wrapTextByWidth(subtitle, pageWidth,
                            chineseFont, SUBTITLE_FONT_SIZE);
                    for (String subLine : wrappedSubtitle) {
                        contentStream.beginText();
                        if (chineseFont != null) {
                            contentStream.setFont(chineseFont, SUBTITLE_FONT_SIZE);
                        } else {
                            contentStream.setFont(PDType1Font.HELVETICA, SUBTITLE_FONT_SIZE);
                        }
                        contentStream.newLineAtOffset(MARGIN, yPosition);
                        contentStream.showText(subLine);
                        contentStream.endText();
                        yPosition -= LINE_HEIGHT;
                    }
                    yPosition -= LINE_HEIGHT;

                    // ===== Summary =====
                    if (summary != null && !summary.isEmpty()) {
                        String shortSummary = summary.length() > 200 ? summary.substring(0, 200) + "..." : summary;
                        // Summary is auto-wrapped by width to prevent overflowing the page
                        List<String> wrappedSummary = wrapTextByWidth(shortSummary, pageWidth,
                                chineseFont, SUBTITLE_FONT_SIZE);
                        for (String summaryLine : wrappedSummary) {
                            if (yPosition < MARGIN + LINE_HEIGHT) {
                                contentStream.close();
                                page = new PDPage(PDRectangle.A4);
                                document.addPage(page);
                                contentStream = new PDPageContentStream(document, page,
                                        PDPageContentStream.AppendMode.OVERWRITE, true, true);
                                yPosition = page.getMediaBox().getHeight() - MARGIN;
                            }
                            contentStream.beginText();
                            if (chineseFont != null) {
                                contentStream.setFont(chineseFont, SUBTITLE_FONT_SIZE);
                            } else {
                                contentStream.setFont(PDType1Font.HELVETICA_OBLIQUE, SUBTITLE_FONT_SIZE);
                            }
                            contentStream.newLineAtOffset(MARGIN, yPosition);
                            contentStream.showText(summaryLine);
                            contentStream.endText();
                            yPosition -= LINE_HEIGHT;
                        }
                        yPosition -= LINE_HEIGHT;
                    }

                    yPosition -= LINE_HEIGHT;

                    isFirstPage = false;
                }

                String[] lines = content.split("\n");
                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i];

                    // ===== Code block detection =====
                    if (line.trim().startsWith("```")) {
                        String codeLang = line.trim().substring(3).trim();
                        List<String> codeLines = new ArrayList<>();
                        i++;
                        while (i < lines.length && !lines[i].trim().startsWith("```")) {
                            codeLines.add(lines[i]);
                            i++;
                        }
                        // Render the code block
                        if (!codeLines.isEmpty()) {
                            RenderResult result = renderCodeBlock(
                                    contentStream, document, page, codeLines, codeLang,
                                    chineseFont, yPosition, pageWidth, MARGIN);
                            yPosition = result.yPosition;
                            contentStream = result.contentStream;
                            page = result.page;
                        }
                        continue;
                    }

                    // ===== Table detection =====
                    if (i + 2 < lines.length && isTableRow(line.trim()) && isTableSeparator(lines[i + 1].trim())) {
                        List<String[]> tableData = new ArrayList<>();
                        tableData.add(parseTableRow(line.trim())); // header row
                        i++; // skip the separator row
                        i++; // move to the first data row
                        while (i < lines.length && isTableRow(lines[i].trim())) {
                            tableData.add(parseTableRow(lines[i].trim()));
                            i++;
                        }
                        i--; // step back one line
                        // Render the table
                        RenderResult result = renderTable(
                                contentStream, document, page, tableData,
                                chineseFont, yPosition, pageWidth, MARGIN);
                        yPosition = result.yPosition;
                        contentStream = result.contentStream;
                        page = result.page;
                        continue;
                    }

                    // ===== Page break detection =====
                    if (yPosition < MARGIN + LINE_HEIGHT * 2) {
                        contentStream.close();
                        page = new PDPage(PDRectangle.A4);
                        document.addPage(page);
                        contentStream = new PDPageContentStream(document, page,
                                PDPageContentStream.AppendMode.OVERWRITE, true, true);
                        yPosition = page.getMediaBox().getHeight() - MARGIN;
                        isFirstPage = false;
                    }

                    Matcher headingMatcher = MARKDOWN_HEADING.matcher(line.trim());
                    if (headingMatcher.matches()) {
                        int level = headingMatcher.group(1).length();
                        float headingFontSize = TITLE_FONT_SIZE - (level - 1) * 2;
                        String headingText = headingMatcher.group(2);

                        // Headings are also auto-wrapped by width to prevent long headings from overflowing the page
                        List<String> wrappedHeading = wrapTextByWidth(headingText, pageWidth,
                                chineseFont, headingFontSize);
                        for (String headingLine : wrappedHeading) {
                            // Check whether a page break is needed when wrapping a heading
                            if (yPosition < MARGIN + LINE_HEIGHT * 1.5f) {
                                contentStream.close();
                                page = new PDPage(PDRectangle.A4);
                                document.addPage(page);
                                contentStream = new PDPageContentStream(document, page,
                                        PDPageContentStream.AppendMode.OVERWRITE, true, true);
                                yPosition = page.getMediaBox().getHeight() - MARGIN;
                                isFirstPage = false;
                            }
                            contentStream.beginText();
                            if (chineseFont != null) {
                                contentStream.setFont(chineseFont, headingFontSize);
                            } else {
                                contentStream.setFont(PDType1Font.HELVETICA_BOLD, headingFontSize);
                            }
                            contentStream.newLineAtOffset(MARGIN, yPosition);
                            contentStream.showText(headingLine);
                            contentStream.endText();
                            yPosition -= LINE_HEIGHT * 1.3f;
                        }
                        yPosition -= LINE_HEIGHT * 0.2f; // extra spacing between heading and body
                        continue;
                    }

                    Matcher imageMatcher = MARKDOWN_IMAGE.matcher(line);
                    if (imageMatcher.find()) {
                        String imageUrl = imageMatcher.group(2);
                        String imageAlt = imageMatcher.group(1);
                        log.info("Found image: alt={}, url={}", imageAlt, imageUrl);

                        RenderResult result = renderImage(
                                contentStream, document, page, imageUrl, imageAlt,
                                yPosition, pageWidth, MARGIN);
                        yPosition = result.yPosition;
                        contentStream = result.contentStream;
                        page = result.page;

                        String remainingText = MARKDOWN_IMAGE.matcher(line).replaceAll("").trim();
                        if (!remainingText.isEmpty()) {
                            List<String> wrappedLines = wrapTextByWidth(remainingText, pageWidth, chineseFont, FONT_SIZE);
                            for (String wrappedLine : wrappedLines) {
                                if (yPosition < MARGIN + LINE_HEIGHT) {
                                    contentStream.close();
                                    page = new PDPage(PDRectangle.A4);
                                    document.addPage(page);
                                    contentStream = new PDPageContentStream(document, page,
                                            PDPageContentStream.AppendMode.OVERWRITE, true, true);
                                    yPosition = page.getMediaBox().getHeight() - MARGIN;
                                }

                                contentStream.beginText();
                                if (chineseFont != null) {
                                    contentStream.setFont(chineseFont, FONT_SIZE);
                                } else {
                                    contentStream.setFont(PDType1Font.HELVETICA, FONT_SIZE);
                                }
                                contentStream.newLineAtOffset(MARGIN, yPosition);
                                contentStream.showText(wrappedLine);
                                contentStream.endText();
                                yPosition -= LINE_HEIGHT;
                            }
                        }
                        continue;
                    }

                    if (line.trim().matches("^[-*+]\\s.*") || line.trim().matches("^\\d+\\.\\s.*")) {
                        String itemPrefix = line.trim().matches("^\\d+\\.\\s.*") ? "• " : "• ";
                        String itemText = itemPrefix + extractPlainText(line.trim().substring(2));
                        // List items are auto-wrapped by width to prevent overflowing the page
                        float listMaxWidth = pageWidth - 20; // subtract list indentation
                        List<String> wrappedList = wrapTextByWidth(itemText, listMaxWidth,
                                chineseFont, FONT_SIZE);
                        for (String listLine : wrappedList) {
                            if (yPosition < MARGIN + LINE_HEIGHT) {
                                contentStream.close();
                                page = new PDPage(PDRectangle.A4);
                                document.addPage(page);
                                contentStream = new PDPageContentStream(document, page,
                                        PDPageContentStream.AppendMode.OVERWRITE, true, true);
                                yPosition = page.getMediaBox().getHeight() - MARGIN;
                            }
                            contentStream.beginText();
                            if (chineseFont != null) {
                                contentStream.setFont(chineseFont, FONT_SIZE);
                            } else {
                                contentStream.setFont(PDType1Font.HELVETICA, FONT_SIZE);
                            }
                            contentStream.newLineAtOffset(MARGIN + 20, yPosition);
                            contentStream.showText(listLine);
                            contentStream.endText();
                            yPosition -= LINE_HEIGHT;
                        }
                        continue;
                    }

                    if (line.trim().isEmpty()) {
                        yPosition -= LINE_HEIGHT * 0.5f;
                        continue;
                    }

                    String plainText = extractPlainText(line);
                    if (plainText.length() > 0) {
                        // Wrap according to actual font width, avoiding Chinese characters overflowing the page
                        List<String> wrappedLines = wrapTextByWidth(plainText, pageWidth,
                                chineseFont, FONT_SIZE);
                        for (String wrappedLine : wrappedLines) {
                            if (yPosition < MARGIN + LINE_HEIGHT) {
                                contentStream.close();
                                page = new PDPage(PDRectangle.A4);
                                document.addPage(page);
                                contentStream = new PDPageContentStream(document, page,
                                        PDPageContentStream.AppendMode.OVERWRITE, true, true);
                                yPosition = page.getMediaBox().getHeight() - MARGIN;
                            }

                            contentStream.beginText();
                            if (chineseFont != null) {
                                contentStream.setFont(chineseFont, FONT_SIZE);
                            } else {
                                contentStream.setFont(PDType1Font.HELVETICA, FONT_SIZE);
                            }
                            contentStream.newLineAtOffset(MARGIN, yPosition);
                            contentStream.showText(wrappedLine);
                            contentStream.endText();
                            yPosition -= LINE_HEIGHT;
                        }
                    }
                }
            } finally {
                if (contentStream != null) {
                    contentStream.close();
                }
            }

            document.save(outputStream);
            return outputStream.toByteArray();

        } catch (IOException e) {
            log.error("Failed to generate PDF", e);
            throw new BusinessException("Failed to generate PDF: " + e.getMessage());
        }
    }

    /**
     * Attempts to load a system Chinese font (preferring loading from classpath resources for cross-platform consistency)
     */
    private PDType0Font loadChineseFont(PDDocument document) throws IOException {
        // Prefer loading from a classpath resource file (ensures it works when deployed to any server)
        try (InputStream is = getClass().getResourceAsStream("/fonts/Arial Unicode.ttf")) {
            if (is != null) {
                log.info("Loaded Chinese font from classpath resource: Arial Unicode.ttf");
                return PDType0Font.load(document, is);
            }
        }
        // Fallback: try other classpath resources
        try (InputStream is = getClass().getResourceAsStream("/fonts/simsun.ttc")) {
            if (is != null) {
                log.info("Loaded Chinese font from classpath resource: simsun.ttc");
                return PDType0Font.load(document, is);
            }
        }

        // Fallback: try loading from system font paths
        for (String fontPath : CHINESE_FONT_PATHS) {
            Path path = Paths.get(fontPath);
            if (Files.exists(path)) {
                log.info("Loaded Chinese font from system path: {}", fontPath);
                return PDType0Font.load(document, path.toFile());
            }
        }

        // Throw if none is found; the caller will handle it
        throw new IOException("No usable Chinese font found");
    }

    private String buildSubtitle(String author, String categoryName, LocalDateTime publishTime) {
        StringBuilder sb = new StringBuilder();
        if (author != null && !author.isEmpty()) {
            sb.append("Author: ").append(author);
        }
        if (categoryName != null && !categoryName.isEmpty()) {
            if (sb.length() > 0) {
                sb.append("  |  ");
            }
            sb.append("Category: ").append(categoryName);
        }
        if (sb.length() > 0) {
            sb.append("  |  ");
        }
        sb.append("Published: ").append(publishTime != null
                ? publishTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                : "Unknown");
        return sb.toString();
    }

    /**
     * Queries the category name by category ID
     */
    private String resolveCategoryName(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        try {
            Category category = categoryMapper.selectById(categoryId);
            return category != null ? category.getCategoryName() : null;
        } catch (Exception e) {
            log.warn("Failed to query category name: categoryId={}", categoryId, e);
            return null;
        }
    }

    /**
     * Render result: wraps contentStream, page, yPosition
     */
    private static class RenderResult {
        final PDPageContentStream contentStream;
        final PDPage page;
        final float yPosition;

        RenderResult(PDPageContentStream contentStream, PDPage page, float yPosition) {
            this.contentStream = contentStream;
            this.page = page;
            this.yPosition = yPosition;
        }
    }

    // ==================== Code block rendering ====================

    /**
     * Renders a code block: One Dark Pro style dark background
     */
    private RenderResult renderCodeBlock(PDPageContentStream contentStream, PDDocument document,
             PDPage page, List<String> codeLines, String language, PDType0Font chineseFont,
             float yPosition, float pageWidth, float marginX) throws IOException {

        float headerHeight = 28;
        float paddingTop = 8;
        float paddingBottom = 12;
        float lineHeight = CODE_LINE_HEIGHT;

        // Compute the actual number of lines needed (accounting for wrapping)
        float codeMaxWidth = pageWidth - 24;
        int totalLines = 0;
        for (String codeLine : codeLines) {
            String replacedLine = codeLine.replace("\t", "    ");
            List<String> wrapped = wrapTextByWidth(replacedLine, codeMaxWidth, chineseFont, CODE_FONT_SIZE);
            totalLines += wrapped.size();
        }

        float totalHeight = headerHeight + paddingTop + (totalLines * lineHeight) + paddingBottom;

        // Break to a new page if the code block does not fit
        if (yPosition - totalHeight < marginX) {
            contentStream.close();
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            contentStream = new PDPageContentStream(document, page,
                    PDPageContentStream.AppendMode.OVERWRITE, true, true);
            yPosition = page.getMediaBox().getHeight() - marginX;
        }

        float blockBottom = yPosition - totalHeight;

        // Dark background
        contentStream.setNonStrokingColor(CODE_BG);
        contentStream.addRect(marginX, blockBottom, pageWidth, totalHeight);
        contentStream.fill();

        // Border
        contentStream.setStrokingColor(CODE_BORDER);
        contentStream.setLineWidth(0.5f);
        contentStream.addRect(marginX, blockBottom, pageWidth, totalHeight);
        contentStream.stroke();

        // Top language label bar
        contentStream.setNonStrokingColor(CODE_HEADER_BG);
        contentStream.addRect(marginX, yPosition - headerHeight, pageWidth, headerHeight);
        contentStream.fill();

        String langLabel = (language != null && !language.isEmpty()) ? language.toUpperCase() : "CODE";
        contentStream.beginText();
        contentStream.setNonStrokingColor(CODE_TEXT);
        if (chineseFont != null) {
            contentStream.setFont(chineseFont, 8);
        } else {
            contentStream.setFont(PDType1Font.HELVETICA, 8);
        }
        contentStream.newLineAtOffset(marginX + 12, yPosition - 19);
        contentStream.showText(langLabel);
        contentStream.endText();

        // Code lines (with automatic wrapping)
        float textY = yPosition - headerHeight - paddingTop - lineHeight;
        for (String codeLine : codeLines) {
            String replacedLine = codeLine.replace("\t", "    ");
            List<String> wrappedLines = wrapTextByWidth(replacedLine, codeMaxWidth, chineseFont, CODE_FONT_SIZE);

            for (String wrappedLine : wrappedLines) {
                contentStream.beginText();
                contentStream.setNonStrokingColor(CODE_TEXT);
                if (chineseFont != null) {
                    contentStream.setFont(chineseFont, CODE_FONT_SIZE);
                } else {
                    contentStream.setFont(PDType1Font.COURIER, CODE_FONT_SIZE);
                }
                contentStream.newLineAtOffset(marginX + 12, textY);
                contentStream.showText(wrappedLine);
                contentStream.endText();
                textY -= lineHeight;
            }
        }

        contentStream.setStrokingColor(0, 0, 0);
        contentStream.setNonStrokingColor(0, 0, 0);

        return new RenderResult(contentStream, page, blockBottom - 8);
    }

    // ==================== Table rendering ====================

    /**
     * Renders a table: dynamic row height + automatic cell wrapping, ensuring all content is displayed
     */
    private RenderResult renderTable(PDPageContentStream contentStream, PDDocument document,
             PDPage page, List<String[]> rows, PDType0Font chineseFont,
             float yPosition, float pageWidth, float marginX) throws IOException {

        if (rows.isEmpty()) {
            return new RenderResult(contentStream, page, yPosition);
        }

        int colCount = rows.get(0).length;
        float colWidth = pageWidth / colCount;
        float cellMaxWidth = colWidth - 10;
        float cellFontSize = FONT_SIZE - 1;
        float cellLineHeight = LINE_HEIGHT - 2;
        float cellPaddingTop = 5;
        float cellPaddingBottom = 5;

        // ---- First pass: compute each row's wrapped text and actual height ----
        List<List<List<String>>> allWrappedRows = new ArrayList<>();
        List<Float> rowHeights = new ArrayList<>();

        for (String[] row : rows) {
            List<List<String>> wrappedRow = new ArrayList<>();
            int maxLines = 1;
            for (int col = 0; col < Math.min(row.length, colCount); col++) {
                String cell = row[col] != null ? row[col].trim() : "";
                List<String> wrappedLines = wrapTextByWidth(cell, cellMaxWidth, chineseFont, cellFontSize);
                if (wrappedLines.isEmpty()) {
                    wrappedLines = new ArrayList<>();
                    wrappedLines.add("");
                }
                wrappedRow.add(wrappedLines);
                maxLines = Math.max(maxLines, wrappedLines.size());
            }
            allWrappedRows.add(wrappedRow);
            rowHeights.add(maxLines * cellLineHeight + cellPaddingTop + cellPaddingBottom);
        }

        // Compute the total height
        float totalHeight = 0;
        for (float h : rowHeights) {
            totalHeight += h;
        }

        // Break to a new page if the entire table does not fit
        if (yPosition - totalHeight < marginX) {
            contentStream.close();
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            contentStream = new PDPageContentStream(document, page,
                    PDPageContentStream.AppendMode.OVERWRITE, true, true);
            yPosition = page.getMediaBox().getHeight() - marginX;
        }

        float tableBottom = yPosition - totalHeight;
        float currentY = yPosition;

        // ---- Second pass: draw row by row ----
        for (int r = 0; r < rows.size(); r++) {
            List<List<String>> wrappedRow = allWrappedRows.get(r);
            float rowHeight = rowHeights.get(r);
            boolean isHeader = (r == 0);

            // Row background
            if (isHeader) {
                contentStream.setNonStrokingColor(TABLE_HEADER_BG);
                contentStream.addRect(marginX, currentY - rowHeight, pageWidth, rowHeight);
                contentStream.fill();
            }

            // Draw cell text
            for (int col = 0; col < wrappedRow.size(); col++) {
                List<String> lines = wrappedRow.get(col);
                float textY = currentY - cellPaddingTop - cellLineHeight;
                for (String line : lines) {
                    contentStream.beginText();
                    contentStream.setNonStrokingColor(0, 0, 0);
                    if (chineseFont != null) {
                        contentStream.setFont(chineseFont, cellFontSize);
                    } else {
                        if (isHeader) {
                            contentStream.setFont(PDType1Font.HELVETICA_BOLD, cellFontSize);
                        } else {
                            contentStream.setFont(PDType1Font.HELVETICA, cellFontSize);
                        }
                    }
                    contentStream.newLineAtOffset(marginX + col * colWidth + 5, textY);
                    contentStream.showText(line);
                    contentStream.endText();
                    textY -= cellLineHeight;
                }
            }

            currentY -= rowHeight;
        }

        // ---- Draw grid lines ----
        contentStream.setStrokingColor(TABLE_BORDER);
        contentStream.setLineWidth(0.5f);
        // Horizontal lines
        float lineY = yPosition;
        for (float h : rowHeights) {
            contentStream.moveTo(marginX, lineY);
            contentStream.lineTo(marginX + pageWidth, lineY);
            lineY -= h;
        }
        // Bottom closing line
        contentStream.moveTo(marginX, lineY);
        contentStream.lineTo(marginX + pageWidth, lineY);
        // Vertical lines
        for (int col = 0; col <= colCount; col++) {
            contentStream.moveTo(marginX + col * colWidth, yPosition);
            contentStream.lineTo(marginX + col * colWidth, tableBottom);
        }
        contentStream.stroke();

        contentStream.setStrokingColor(0, 0, 0);
        contentStream.setNonStrokingColor(0, 0, 0);

        return new RenderResult(contentStream, page, tableBottom - 12);
    }

    // ==================== Image rendering ====================

    /**
     * Renders an image: downloads from a URL and embeds it into the PDF
     */
    private RenderResult renderImage(PDPageContentStream contentStream, PDDocument document,
             PDPage page, String imageUrl, String imageAlt,
             float yPosition, float pageWidth, float marginX) throws IOException {

        float maxImageWidth = pageWidth - 20;
        float maxImageHeight = 400;
        float imageY = yPosition;

        try {
            byte[] imageBytes = downloadImage(imageUrl);
            if (imageBytes == null || imageBytes.length == 0) {
                log.warn("Image download failed or is empty: url={}", imageUrl);
                return new RenderResult(contentStream, page, yPosition - LINE_HEIGHT);
            }

            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (bufferedImage == null) {
                log.warn("Could not parse image format: url={}", imageUrl);
                return new RenderResult(contentStream, page, yPosition - LINE_HEIGHT);
            }

            int imgWidth = bufferedImage.getWidth();
            int imgHeight = bufferedImage.getHeight();

            float ratio = Math.min(maxImageWidth / imgWidth, maxImageHeight / imgHeight);
            float drawWidth = imgWidth * ratio;
            float drawHeight = imgHeight * ratio;

            if (imageY - drawHeight < marginX) {
                contentStream.close();
                page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                contentStream = new PDPageContentStream(document, page,
                        PDPageContentStream.AppendMode.OVERWRITE, true, true);
                imageY = page.getMediaBox().getHeight() - marginX;
            }

            float imageX = marginX + (pageWidth - drawWidth) / 2;
            PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, imageBytes, imageAlt);

            contentStream.drawImage(pdImage, imageX, imageY - drawHeight, drawWidth, drawHeight);

            log.info("Image rendered successfully: url={}, width={}, height={}", imageUrl, drawWidth, drawHeight);

            return new RenderResult(contentStream, page, imageY - drawHeight - LINE_HEIGHT);

        } catch (Exception e) {
            log.error("Image rendering failed: url={}, error={}", imageUrl, e.getMessage());
            return new RenderResult(contentStream, page, yPosition - LINE_HEIGHT);
        }
    }

    /**
     * Downloads the image byte array from a URL
     *
     * <p>Only http(s) URLs are supported. Document content (and therefore image URLs) is
     * user-authored, so treating a non-http value as a local filesystem path would let an
     * attacker embed something like {@code ![x](/etc/passwd)} in a document and have this
     * service read arbitrary local files into the exported PDF.</p>
     */
    private byte[] downloadImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return null;
        }

        if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
            log.warn("Ignoring non-http(s) image URL: url={}", imageUrl);
            return null;
        }

        try {
            return HttpUtil.downloadBytes(imageUrl);
        } catch (Exception e) {
            log.error("Failed to download image: url={}", imageUrl, e);
        }

        return null;
    }

    // ==================== Table detection helper methods ====================

    private boolean isTableRow(String line) {
        return line.startsWith("|") && line.endsWith("|") && line.length() > 2;
    }

    private boolean isTableSeparator(String line) {
        return line.matches("^\\|[\\s\\-:]+\\|([\\s\\-:]+\\|)+$");
    }

    private String[] parseTableRow(String line) {
        // Strip the leading and trailing |, then split on |
        String inner = line.substring(1, line.length() - 1);
        return inner.split("\\|");
    }

    private String truncateText(String text, float maxWidth, PDType0Font chineseFont, float fontSize) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        // Measure the actual width
        float textWidth = measureTextWidth(text, chineseFont, fontSize);
        if (textWidth <= maxWidth) {
            return text;
        }
        // Trim from the end until the width fits
        int len = text.length();
        while (len > 0 && measureTextWidth(text.substring(0, len) + "…", chineseFont, fontSize) > maxWidth) {
            len--;
        }
        return len > 0 ? text.substring(0, len) + "…" : "";
    }

    /**
     * Wraps text into lines based on actual font width, ensuring no line exceeds pageWidth
     */
    private List<String> wrapTextByWidth(String text, float maxWidth, PDType0Font chineseFont, float fontSize) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return result;
        }

        // Replace tabs with 4 spaces
        text = text.replace("\t", "    ");

        StringBuilder currentLine = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            String testLine = currentLine.toString() + c;
            float lineWidth = measureTextWidth(testLine, chineseFont, fontSize);

            if (lineWidth > maxWidth && currentLine.length() > 0) {
                result.add(currentLine.toString());
                currentLine = new StringBuilder();
                // Skip a leading space at the start of a line
                if (c == ' ') {
                    continue;
                }
            }
            currentLine.append(c);
        }

        if (currentLine.length() > 0) {
            result.add(currentLine.toString());
        }

        return result;
    }

    /**
     * Measures the actual text width using the font (unit: pt).
     * When chineseFont is null, CJK character widths are automatically estimated to avoid
     * Chinese content overflowing the page and being truncated
     */
    private float measureTextWidth(String text, PDType0Font chineseFont, float fontSize) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        try {
            if (chineseFont != null) {
                return chineseFont.getStringWidth(text) / 1000f * fontSize;
            } else {
                // The fallback font does not support Chinese, so CJK character widths must be estimated manually
                float totalWidth = 0;
                for (int i = 0; i < text.length(); i++) {
                    char c = text.charAt(i);
                    if (isCJKCharacter(c)) {
                        // CJK full-width character: approximately 1em wide
                        totalWidth += fontSize;
                    } else {
                        // Half-width character: use HELVETICA's actual width
                        try {
                            totalWidth += PDType1Font.HELVETICA.getStringWidth(String.valueOf(c)) / 1000f * fontSize;
                        } catch (IOException e2) {
                            totalWidth += fontSize * 0.55f;
                        }
                    }
                }
                return totalWidth;
            }
        } catch (IOException e) {
            // Fallback on measurement failure: estimate each CJK character as approximately 1em, regular characters as approximately 0.55em
            float width = 0;
            for (int i = 0; i < text.length(); i++) {
                width += isCJKCharacter(text.charAt(i)) ? fontSize : fontSize * 0.55f;
            }
            return width;
        }
    }

    /**
     * Determines whether a character is a CJK (Chinese/Japanese/Korean) full-width character
     */
    private boolean isCJKCharacter(char c) {
        // CJK Radicals Supplement
        if (c >= 0x2E80 && c <= 0x2EFF) return true;
        // Kangxi Radicals
        if (c >= 0x2F00 && c <= 0x2FDF) return true;
        // CJK Symbols and Punctuation
        if (c >= 0x3000 && c <= 0x303F) return true;
        // CJK Unified Ideographs Extension A
        if (c >= 0x3400 && c <= 0x4DBF) return true;
        // CJK Unified Ideographs
        if (c >= 0x4E00 && c <= 0x9FFF) return true;
        // CJK Compatibility Ideographs
        if (c >= 0xF900 && c <= 0xFAFF) return true;
        // Fullwidth Forms
        if (c >= 0xFF00 && c <= 0xFFEF) return true;
        // Halfwidth and Fullwidth Forms (fullwidth)
        if (c >= 0xFF01 && c <= 0xFF60) return true;
        // CJK Extension B-F ranges (supplementary, check char not surrogate)
        // Chinese punctuation
        if (c == '‘' || c == '’' || c == '“' || c == '”') return true; // Chinese quotation marks
        if (c == '—' || c == '―') return true; // em dash
        return false;
    }

    /**
     * Removes characters unsupported by the PDF font (emoji, special symbols, etc.)
     * Retains common Chinese, English, digits, and punctuation
     */
    private String stripUnsupportedCharacters(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            int codePoint = text.codePointAt(i);
            // Skip the high surrogate of a pair (already handled at the codepoint level)
            if (Character.isSupplementaryCodePoint(codePoint)) {
                i++; // skip the low surrogate
                continue; // skip all supplementary-plane characters (including emoji U+1Fxxx)
            }
            // Skip common unsupported symbol ranges: miscellaneous symbols (U+2600-27BF), dingbats (U+2700-27BF)
            if (codePoint >= 0x2600 && codePoint <= 0x27BF) {
                continue;
            }
            // Skip other special symbols: U+2300-23FF (miscellaneous technical symbols)
            if (codePoint >= 0x2300 && codePoint <= 0x23FF) {
                continue;
            }
            sb.append((char) codePoint);
        }
        return sb.toString();
    }

    private String extractPlainText(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return "";
        }

        String text = markdown;

        text = MARKDOWN_LINK.matcher(text).replaceAll("$1: $2");

        text = MARKDOWN_BOLD.matcher(text).replaceAll("$1");

        text = MARKDOWN_ITALIC.matcher(text).replaceAll("$1");

        text = text.replaceAll("`([^`]+)`", "$1");

        text = text.replaceAll("#+\\s*", "");

        text = text.replaceAll("!\\[.*?\\]\\(.*?\\)", "");

        text = text.replaceAll("\\s*[-*_]{3,}\\s*", "\n");

        text = text.trim();

        return text;
    }
}
