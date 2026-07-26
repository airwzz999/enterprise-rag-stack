package com.knowledge.base.document.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.knowledge.base.common.result.Result;
import com.knowledge.base.document.dto.BatchExportRequest;
import com.knowledge.base.document.dto.AutoSaveDTO;
import com.knowledge.base.document.dto.DocumentDTO;
import com.knowledge.base.document.dto.ShareDTO;
import com.knowledge.base.document.service.DocumentService;
import com.knowledge.base.document.service.DocumentShareService;
import com.knowledge.base.document.service.PdfExportService;
import com.knowledge.base.document.service.AutoSaveHistoryService;
import com.knowledge.base.document.vo.AutoSaveHistoryVO;
import com.knowledge.base.document.dto.AutoSaveHistoryQueryDTO;
import com.knowledge.base.document.vo.DocumentNeighborVO;
import com.knowledge.base.document.vo.DocumentVO;
import com.knowledge.base.document.vo.ShareVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Document Controller
 *
 * <p>Designed following the Alibaba Java Coding Guidelines, provides document management endpoints</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/documents")
@Tag(name = "Document Management", description = "Document information management endpoints")
public class DocumentController {

    @Resource
    private DocumentService documentService;

    @Resource
    private PdfExportService pdfExportService;

    @Resource
    private DocumentShareService documentShareService;

    @Resource
    private AutoSaveHistoryService autoSaveHistoryService;

    /**
     * Creates a document
     *
     * @param documentDTO document information
     * @return document ID
     */
    @PostMapping
    @Operation(summary = "Create document", description = "Creates a new document")
    @PreAuthorize("hasAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_CREATE)")
    public Result<Long> createDocument(@Valid @RequestBody DocumentDTO documentDTO) {
        log.info("Create document request: title={}", documentDTO.getTitle());

        Long documentId = documentService.createDocument(documentDTO);
        return Result.success("Document created successfully", documentId);
    }

    /**
     * Updates a document
     *
     * @param documentDTO document information
     * @return whether successful
     */
    @PutMapping
    @Operation(summary = "Update document", description = "Updates document information")
    @PreAuthorize("hasAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_EDIT)")
    public Result<Boolean> updateDocument(@Valid @RequestBody DocumentDTO documentDTO) {
        log.info("Update document request: documentId={}", documentDTO.getId());

        Boolean success = documentService.updateDocument(documentDTO);
        return Result.success("Document updated successfully", success);
    }

    /**
     * Updates the document summary (partial update, not subject to full validation)
     *
     * @param documentId document ID
     * @param body       request body containing the summary field
     * @return whether successful
     */
    @PatchMapping("/{documentId}/summary")
    @Operation(summary = "Update document summary", description = "Updates only the document's summary field")
    @PreAuthorize("hasAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_EDIT)")
    public Result<Boolean> updateSummary(
            @Parameter(description = "Document ID", required = true)
            @PathVariable Long documentId,
            @RequestBody Map<String, String> body) {
        String summary = body.get("summary");
        if (summary == null || summary.isBlank()) {
            return Result.error("Summary content must not be blank");
        }
        if (summary.length() > 500) {
            summary = summary.substring(0, 500);
        }
        log.info("Update document summary request: documentId={}", documentId);
        Boolean success = documentService.updateSummary(documentId, summary);
        return Result.success("Summary updated successfully", success);
    }

    /**
     * Auto-saves a document (creates or updates a draft)
     *
     * <p>Allows an empty title, forces the status to draft, and does not trigger RAG/KAG/ES indexing.
     * Used by the frontend editor's auto-save scenario to prevent user data loss.</p>
     *
     * @param autoSaveDTO auto-save data
     * @return document ID
     */
    @PostMapping("/autosave")
    @Operation(summary = "Auto-save document", description = "Auto-saves a draft, allowing an empty title, without triggering indexing")
    @PreAuthorize("hasAnyAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_CREATE, T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_EDIT)")
    public Result<Long> autoSaveDocument(@Valid @RequestBody AutoSaveDTO autoSaveDTO) {
        log.info("Auto-save request: id={}, title={}", autoSaveDTO.getId(), autoSaveDTO.getTitle());
        Long documentId = documentService.autoSaveDocument(autoSaveDTO);
        return Result.success("Auto-save successful", documentId);
    }

    /**
     * Dismisses the auto-saved draft: marks all of the current user's drafts as acknowledged
     *
     * <p>Called after the user clicks "discard draft"; marks autoSaveDismissed=1,
     * so the restore prompt no longer appears when entering the new document page.</p>
     *
     * @return whether successful
     */
    @PutMapping("/autosave/dismiss")
    @Operation(summary = "Dismiss auto-saved drafts", description = "Marks all of the current user's drafts as acknowledged so the restore prompt no longer appears")
    @PreAuthorize("hasAnyAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_CREATE, T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_EDIT)")
    public Result<Boolean> dismissAutoSaveDrafts() {
        log.info("Dismiss auto-saved drafts request");
        documentService.dismissAutoSaveDrafts();
        return Result.success("Draft dismissal acknowledged", true);
    }

    /**
     * Deletes a document
     *
     * @param documentId document ID
     * @return whether successful
     */
    @DeleteMapping("/{documentId}")
    @Operation(summary = "Delete document", description = "Deletes a document by ID")
    @PreAuthorize("hasAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_DELETE)")
    public Result<Boolean> deleteDocument(
        @Parameter(description = "Document ID", required = true)
        @PathVariable Long documentId) {
        log.info("Delete document request: documentId={}", documentId);

        Boolean success = documentService.deleteDocument(documentId);
        return Result.success("Document deleted successfully", success);
    }

    /**
     * Queries a document by ID
     *
     * @param documentId document ID
     * @return document information
     */
    @GetMapping("/{documentId}")
    @Operation(summary = "Query document", description = "Queries document details by document ID")
    @PreAuthorize("hasAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_LIST)")
    public Result<DocumentVO> getDocumentById(
        @Parameter(description = "Document ID", required = true)
        @PathVariable Long documentId) {
        log.info("Query document request: documentId={}", documentId);

        DocumentVO documentVO = documentService.getDocumentById(documentId);
        return Result.success(documentVO);
    }

    /**
     * Views a document (increments the view count)
     *
     * @param documentId document ID
     * @return document information
     */
    @GetMapping("/{documentId}/view")
    @Operation(summary = "View document", description = "Views a document and increments its view count")
    @PreAuthorize("hasAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_LIST)")
    public Result<DocumentVO> viewDocument(
        @Parameter(description = "Document ID", required = true)
        @PathVariable Long documentId) {
        log.info("View document request: documentId={}", documentId);

        DocumentVO documentVO = documentService.viewDocument(documentId);
        return Result.success(documentVO);
    }

    /**
     * Paginated query of the document list
     *
     * @param current    current page
     * @param size       page size
     * @param categoryId category ID
     * @param keyword    search keyword
     * @param status     status
     * @param sortBy     sort field
     * @param sortOrder  sort direction
     * @return paginated document information
     */
    @GetMapping("/page")
    @Operation(summary = "Paginated document query", description = "Paginated query of the document list")
    @PreAuthorize("hasAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_LIST)")
    public Result<IPage<DocumentVO>> pageDocuments(
        @Parameter(description = "Current page") @RequestParam(defaultValue = "1") Long current,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "10") Long size,
        @Parameter(description = "Category ID") @RequestParam(required = false) Long categoryId,
        @Parameter(description = "Team space ID") @RequestParam(required = false) Long teamId,
        @Parameter(description = "Search keyword") @RequestParam(required = false) String keyword,
        @Parameter(description = "Status") @RequestParam(required = false) Integer status,
        @Parameter(description = "Sort field") @RequestParam(required = false) String sortBy,
        @Parameter(description = "Sort direction") @RequestParam(required = false) String sortOrder,
        @Parameter(description = "Author ID") @RequestParam(required = false) Long authorId) {
        log.info("Paginated document query request: current={}, size={}, categoryId={}, keyword={}, status={}, sortBy={}, sortOrder={}, authorId={}",
            current, size, categoryId, keyword, status, sortBy, sortOrder, authorId);

        IPage<DocumentVO> page = documentService.pageDocuments(current, size, categoryId, teamId, keyword, status, sortBy, sortOrder, authorId);
        return Result.success(page);
    }

    /**
     * Queries the previous and next document
     *
     * @param documentId document ID
     * @return neighboring document information
     */
    @GetMapping("/{documentId}/neighbors")
    @Operation(summary = "Query neighboring documents", description = "Queries the previous and next document, used for detail page navigation")
    @PreAuthorize("hasAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_LIST)")
    public Result<DocumentNeighborVO> getDocumentNeighbors(
        @Parameter(description = "Document ID", required = true)
        @PathVariable Long documentId) {
        log.info("Query neighboring documents request: documentId={}", documentId);
        DocumentNeighborVO result = documentService.getDocumentNeighbors(documentId);
        return Result.success(result);
    }

    /**
     * Uploads a document file
     *
     * @param file file
     * @return file path
     */
    @PostMapping("/upload")
    @Operation(summary = "Upload document file", description = "Uploads a document file and returns its file path")
    @PreAuthorize("hasAnyAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_CREATE, T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_EDIT)")
    public Result<String> uploadDocumentFile(
        @Parameter(description = "File", required = true)
        @RequestParam("file") MultipartFile file) {
        log.info("Upload document file request: fileName={}", file.getOriginalFilename());

        String filePath = documentService.uploadDocumentFile(file);
        return Result.success("File uploaded successfully", filePath);
    }

    /**
     * Uploads a file and parses it to create a document
     *
     * <p>Upload file → parse text content → automatically create a document record → publish through the pipeline</p>
     *
     * @param file file
     * @return document creation result (includes documentId / title / fileUrl / fileSize / contentLength / contentPreview)
     */
    @PostMapping("/upload/parse")
    @Operation(summary = "Upload and parse a file to create a document", description = "Automatically parses the content after uploading a file and creates a document record, publishing directly through the knowledge pipeline")
    @PreAuthorize("hasAnyAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_CREATE, T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_EDIT)")
    public Result<Map<String, Object>> uploadAndParseDocument(
            @Parameter(description = "File", required = true)
            @RequestParam("file") MultipartFile file) {
        log.info("Upload and parse document request: fileName={}, size={}", file.getOriginalFilename(), file.getSize());

        Map<String, Object> result = documentService.uploadAndCreateDocument(file);
        return Result.success("File parsed and document created successfully", result);
    }

    /**
     * Likes a document
     *
     * @param documentId document ID
     * @return whether successful
     */
    @PostMapping("/{documentId}/like")
    @Operation(summary = "Like document", description = "The user likes a document")
    public Result<Boolean> likeDocument(
        @Parameter(description = "Document ID", required = true)
        @PathVariable Long documentId) {
        log.info("Like document request: documentId={}", documentId);

        Boolean success = documentService.likeDocument(documentId);
        return Result.success("Liked successfully", success);
    }

    /**
     * Unlikes a document
     *
     * @param documentId document ID
     * @return whether successful
     */
    @DeleteMapping("/{documentId}/like")
    @Operation(summary = "Unlike document", description = "The user unlikes a document")
    public Result<Boolean> unlikeDocument(
        @Parameter(description = "Document ID", required = true)
        @PathVariable Long documentId) {
        log.info("Unlike document request: documentId={}", documentId);

        Boolean success = documentService.unlikeDocument(documentId);
        return Result.success("Unliked successfully", success);
    }

    /**
     * Favorites a document
     *
     * @param documentId document ID
     * @return whether successful
     */
    @PostMapping("/{documentId}/favorite")
    @Operation(summary = "Favorite document", description = "The user favorites a document")
    public Result<Boolean> favoriteDocument(
        @Parameter(description = "Document ID", required = true)
        @PathVariable Long documentId) {
        log.info("Favorite document request: documentId={}", documentId);

        Boolean success = documentService.favoriteDocument(documentId);
        return Result.success("Favorited successfully", success);
    }

    /**
     * Publishes a document (directly publishes or submits for review based on system configuration)
     *
     * @param documentId document ID
     * @return whether successful
     */
    @PutMapping("/{documentId}/publish")
    @Operation(summary = "Publish document", description = "Directly publishes or submits for review based on system configuration")
    @PreAuthorize("hasAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_EDIT)")
    public Result<Boolean> publishDocument(
        @Parameter(description = "Document ID", required = true)
        @PathVariable Long documentId) {
        log.info("Submit document for review request: documentId={}", documentId);

        Boolean success = documentService.publishDocument(documentId);
        return Result.success(success ? "Published successfully" : "Publish failed", success);
    }

    /**
     * Archives a document
     *
     * @param documentId document ID
     * @return whether successful
     */
    @PutMapping("/{documentId}/archive")
    @Operation(summary = "Archive document", description = "Archives a document")
    @PreAuthorize("hasAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_EDIT)")
    public Result<Boolean> archiveDocument(
        @Parameter(description = "Document ID", required = true)
        @PathVariable Long documentId) {
        log.info("Archive document request: documentId={}", documentId);

        Boolean success = documentService.archiveDocument(documentId);
        return Result.success("Archived successfully", success);
    }

    /**
     * Exports a document to PDF
     *
     * @param documentId document ID
     * @return PDF download URL
     */
    @GetMapping("/{documentId}/export-pdf")
    @Operation(summary = "Export PDF", description = "Exports a document to PDF and returns the download link")
    @PreAuthorize("hasAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_LIST)")
    public Result<String> exportDocumentToPdf(
        @Parameter(description = "Document ID", required = true)
        @PathVariable Long documentId) {
        log.info("Export PDF request: documentId={}", documentId);

        String pdfUrl = pdfExportService.exportDocumentToPdf(documentId);
        return Result.success("PDF exported successfully", pdfUrl);
    }

    /**
     * Downloads a document's PDF
     *
     * @param documentId document ID
     * @param response HTTP response
     */
    @GetMapping("/{documentId}/download-pdf")
    @Operation(summary = "Download PDF", description = "Directly downloads the document's PDF file")
    @PreAuthorize("hasAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_LIST)")
    public void downloadDocumentPdf(
        @Parameter(description = "Document ID", required = true)
        @PathVariable Long documentId,
        HttpServletResponse response) {
        log.info("Download PDF request: documentId={}", documentId);

        try {
            byte[] pdfBytes = pdfExportService.exportDocumentToPdfBytes(documentId);

            DocumentVO document = documentService.getDocumentById(documentId);
            String fileName = pdfExportService.generatePdfFileName(documentId, document.getTitle());

            response.setContentType("application/pdf");
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replace("+", "%20");
            response.setHeader("Content-Disposition",
                    "attachment; filename*=UTF-8''" + encodedFileName);
            response.setContentLength(pdfBytes.length);
            response.getOutputStream().write(pdfBytes);
            response.getOutputStream().flush();

            log.info("PDF downloaded successfully: documentId={}", documentId);
        } catch (IOException e) {
            log.error("PDF download failed: documentId={}", documentId, e);
            throw new RuntimeException("PDF download failed: " + e.getMessage());
        }
    }

    /**
     * Batch-exports documents
     *
     * @param request batch export request
     * @param response HTTP response
     */
    @PostMapping("/batch-export")
    @Operation(summary = "Batch export", description = "Batch-exports the selected documents as a ZIP file in PDF or Markdown format")
    @PreAuthorize("hasAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_LIST)")
    public void batchExportDocuments(
            @Parameter(description = "Batch export request", required = true)
            @Valid @RequestBody BatchExportRequest request,
            HttpServletResponse response) {
        log.info("Batch export request: documentIds={}, format={}", request.getDocumentIds(), request.getFormat());

        try {
            byte[] zipBytes = pdfExportService.batchExportDocuments(
                    request.getDocumentIds(), request.getFormat());

            String fileName = "documents_export_" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".zip";

            response.setContentType("application/zip");
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replace("+", "%20");
            response.setHeader("Content-Disposition",
                    "attachment; filename*=UTF-8''" + encodedFileName);
            response.setContentLength(zipBytes.length);
            response.getOutputStream().write(zipBytes);
            response.getOutputStream().flush();

            log.info("Batch export successful: {} documents in total", request.getDocumentIds().size());
        } catch (IOException e) {
            log.error("Batch export failed", e);
            throw new RuntimeException("Batch export failed: " + e.getMessage());
        }
    }

    /**
     * Creates a share link
     *
     * @param shareDTO share parameters
     * @return share information
     */
    @PostMapping("/share")
    @Operation(summary = "Create share", description = "Creates a document share link")
    @PreAuthorize("hasAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_EDIT)")
    public Result<ShareVO> createShare(@Valid @RequestBody ShareDTO shareDTO) {
        log.info("Create share link request: documentId={}", shareDTO.getDocumentId());

        ShareVO shareVO = documentShareService.createShare(shareDTO);
        return Result.success("Share link created successfully", shareVO);
    }

    /**
     * Gets share information
     *
     * @param shareId share ID
     * @return share information
     */
    @GetMapping("/share/{shareId}")
    @Operation(summary = "Get share info", description = "Gets share information by share ID")
    @PreAuthorize("hasAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_LIST)")
    public Result<ShareVO> getShareInfo(
        @Parameter(description = "Share ID", required = true)
        @PathVariable String shareId) {
        log.info("Get share info request: shareId={}", shareId);

        ShareVO shareVO = documentShareService.getShareById(shareId);
        return Result.success(shareVO);
    }

    /**
     * Accesses a share link
     *
     * @param shareId share ID
     * @param password access password (optional)
     * @return document ID
     */
    @PostMapping("/share/{shareId}/access")
    @Operation(summary = "Access share", description = "Accesses a share link and returns the document ID")
    @PreAuthorize("hasAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_LIST)")
    public Result<Long> accessShare(
        @Parameter(description = "Share ID", required = true)
        @PathVariable String shareId,
        @Parameter(description = "Access password")
        @RequestParam(required = false) String password) {
        log.info("Access share link request: shareId={}", shareId);

        Long documentId = documentShareService.accessShare(shareId, password);
        return Result.success(documentId);
    }

    /**
     * Gets all shares for a document
     *
     * @param documentId document ID
     * @return share list
     */
    @GetMapping("/{documentId}/shares")
    @Operation(summary = "Get document share list", description = "Gets all share links for a document")
    @PreAuthorize("hasAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_LIST)")
    public Result<List<ShareVO>> getDocumentShares(
        @Parameter(description = "Document ID", required = true)
        @PathVariable Long documentId) {
        log.info("Get document share list request: documentId={}", documentId);

        List<ShareVO> shares = documentShareService.getSharesByDocumentId(documentId);
        return Result.success(shares);
    }

    /**
     * Gets my share list
     *
     * @return share list
     */
    @GetMapping("/share/my")
    @Operation(summary = "Get my shares", description = "Gets the current user's share list")
    @PreAuthorize("hasAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_LIST)")
    public Result<List<ShareVO>> getMyShares() {
        log.info("Get my share list request");

        List<ShareVO> shares = documentShareService.getMyShares();
        return Result.success(shares);
    }

    /**
     * Deletes a share link
     *
     * @param shareId share ID
     * @return whether successful
     */
    @DeleteMapping("/share/{shareId}")
    @Operation(summary = "Delete share", description = "Deletes a share link")
    @PreAuthorize("hasAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_EDIT)")
    public Result<Boolean> deleteShare(
        @Parameter(description = "Share ID", required = true)
        @PathVariable String shareId) {
        log.info("Delete share link request: shareId={}", shareId);

        boolean success = documentShareService.deleteShare(shareId);
        return Result.success("Share deleted", success);
    }

    /**
     * Updates share settings
     *
     * @param shareId share ID
     * @param shareDTO update parameters
     * @return whether successful
     */
    @PutMapping("/share/{shareId}")
    @Operation(summary = "Update share settings", description = "Updates the settings of a share link")
    @PreAuthorize("hasAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_EDIT)")
    public Result<Boolean> updateShare(
        @Parameter(description = "Share ID", required = true)
        @PathVariable String shareId,
        @RequestBody ShareDTO shareDTO) {
        log.info("Update share settings request: shareId={}", shareId);

        boolean success = documentShareService.updateShare(shareId, shareDTO);
        return Result.success("Share settings updated", success);
    }

    /**
     * Cleans up stale knowledge graph nodes
     *
     * <p>Deletes graph nodes in Neo4j for documents that no longer exist in MySQL, resolving stale
     * data caused by asynchronous deletion failures.</p>
     *
     * @return operation result
     */
    @PostMapping("/graph/cleanup")
    @Operation(summary = "Clean up stale knowledge graph nodes", description = "Deletes document graph nodes that remain in Neo4j after being deleted in MySQL")
    @PreAuthorize("hasAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_EDIT)")
    public Result<String> cleanupGraphGhostNodes() {
        log.info("Clean up stale knowledge graph nodes request");
        int count = documentService.cleanupGraphGhostNodes();
        return Result.success("Synchronized " + count + " valid document IDs, stale node cleanup request sent");
    }

    /**
     * Batch-rebuilds the knowledge graph for all published documents
     *
     * <p>Iterates over all published documents and triggers KAG graph construction, used for initial
     * graph building or a full rebuild.</p>
     *
     * @return operation result
     */
    @PostMapping("/graph/rebuild")
    @Operation(summary = "Batch rebuild knowledge graph", description = "Rebuilds the knowledge graph for all published documents")
    @PreAuthorize("hasAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_EDIT)")
    public Result<String> rebuildAllGraphs() {
        log.info("Batch rebuild knowledge graph request");
        int count = documentService.rebuildAllGraphs();
        return Result.success("Triggered knowledge graph rebuild for " + count + " documents, please check the knowledge graph page shortly");
    }

    /**
     * Gets the auto-save history for a document
     *
     * @param documentId document ID
     * @param current    current page
     * @param size       page size
     * @return paginated history snapshots
     */
    @GetMapping("/{documentId}/autosave-history")
    @Operation(summary = "Get auto-save history", description = "Paginated query of the auto-save snapshot history for the specified document")
    @PreAuthorize("hasAnyAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_LIST)")
    public Result<IPage<AutoSaveHistoryVO>> getAutoSaveHistory(
            @Parameter(description = "Document ID", required = true)
            @PathVariable Long documentId,
            @Parameter(description = "Current page", example = "1")
            @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") Long size) {
        log.info("Query auto-save history: documentId={}, current={}, size={}", documentId, current, size);
        AutoSaveHistoryQueryDTO query = new AutoSaveHistoryQueryDTO();
        query.setDocumentId(documentId);
        query.setCurrent(current);
        query.setSize(size);
        IPage<AutoSaveHistoryVO> result = autoSaveHistoryService.pageHistory(query);
        return Result.success(result);
    }

    /**
     * Gets a single auto-save snapshot's details (including full Markdown content)
     *
     * @param documentId document ID
     * @param snapshotId snapshot ID (MongoDB _id)
     * @return snapshot details
     */
    @GetMapping("/{documentId}/autosave-history/{snapshotId}")
    @Operation(summary = "Get auto-save snapshot details", description = "Gets the full Markdown content by snapshot ID")
    @PreAuthorize("hasAnyAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_LIST)")
    public Result<AutoSaveHistoryVO> getAutoSaveSnapshot(
            @Parameter(description = "Document ID", required = true)
            @PathVariable Long documentId,
            @Parameter(description = "Snapshot ID (MongoDB _id)", required = true)
            @PathVariable String snapshotId) {
        log.info("Query auto-save snapshot details: documentId={}, snapshotId={}", documentId, snapshotId);
        AutoSaveHistoryVO vo = autoSaveHistoryService.getSnapshot(snapshotId, documentId);
        return Result.success(vo);
    }
}
