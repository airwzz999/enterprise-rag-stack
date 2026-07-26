package com.knowledge.base.document.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.knowledge.base.common.result.Result;
import com.knowledge.base.document.dto.DocumentVersionRestoreDTO;
import com.knowledge.base.document.service.DocumentVersionService;
import com.knowledge.base.document.utils.UserContext;
import com.knowledge.base.document.vo.DocumentVersionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Document version management Controller
 *
 * <p>Designed following the Alibaba Java Coding Guidelines, provides document version management endpoints</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/documents/{documentId}/versions")
@Tag(name = "Document Version Management", description = "Document version management endpoints")
public class DocumentVersionController {

    @Resource
    private DocumentVersionService documentVersionService;

    /**
     * Gets the document version list
     *
     * @param documentId document ID
     * @param current    current page
     * @param size       page size
     * @return paginated version information
     */
    @GetMapping
    @Operation(summary = "Get document version list", description = "Paginated query of the document version list")
    @PreAuthorize("hasAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_VERSION)")
    public Result<IPage<DocumentVersionVO>> getVersions(
        @Parameter(description = "Document ID", required = true)
        @PathVariable Long documentId,
        @Parameter(description = "Current page") @RequestParam(defaultValue = "1") Long current,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "10") Long size) {
        log.info("Get document version list request: documentId={}, current={}, size={}", documentId, current, size);

        IPage<DocumentVersionVO> page = documentVersionService.getVersionList(documentId, current, size);
        return Result.success(page);
    }

    /**
     * Gets document version details
     *
     * @param documentId document ID
     * @param versionId  version ID
     * @return version details
     */
    @GetMapping("/{versionId}")
    @Operation(summary = "Get document version details", description = "Gets version details by version ID")
    @PreAuthorize("hasAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_VERSION)")
    public Result<DocumentVersionVO> getVersionDetail(
        @Parameter(description = "Document ID", required = true)
        @PathVariable Long documentId,
        @Parameter(description = "Version ID", required = true)
        @PathVariable Long versionId) {
        log.info("Get document version details request: documentId={}, versionId={}", documentId, versionId);

        DocumentVersionVO versionVO = documentVersionService.getVersionDetail(versionId);
        return Result.success(versionVO);
    }

    /**
     * Restores a document version
     *
     * @param documentId document ID
     * @param dto        version restore DTO
     * @return whether successful
     */
    @PostMapping("/restore")
    @Operation(summary = "Restore document version", description = "Restores a document to the specified version")
    @PreAuthorize("hasAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_VERSION)")
    public Result<Boolean> restoreVersion(
        @Parameter(description = "Document ID", required = true)
        @PathVariable Long documentId,
        @Valid @RequestBody DocumentVersionRestoreDTO dto) {
        log.info("Restore document version request: documentId={}, versionId={}", documentId, dto.getVersionId());

        Long userId = UserContext.getCurrentUserId();
        Boolean success = documentVersionService.restoreVersion(documentId, dto, userId);
        return Result.success("Version restored successfully", success);
    }

    /**
     * Compares two versions
     *
     * @param documentId document ID
     * @param versionId1 version 1 ID
     * @param versionId2 version 2 ID
     * @return comparison result
     */
    @GetMapping("/compare")
    @Operation(summary = "Compare document versions", description = "Compares the differences between two document versions")
    @PreAuthorize("hasAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_VERSION)")
    public Result<String> compareVersions(
        @Parameter(description = "Document ID", required = true)
        @PathVariable Long documentId,
        @Parameter(description = "Version 1 ID", required = true)
        @RequestParam Long versionId1,
        @Parameter(description = "Version 2 ID", required = true)
        @RequestParam Long versionId2) {
        log.info("Compare document versions request: documentId={}, versionId1={}, versionId2={}",
            documentId, versionId1, versionId2);

        String diff = documentVersionService.compareVersions(versionId1, versionId2);
        return Result.success(diff);
    }
}
