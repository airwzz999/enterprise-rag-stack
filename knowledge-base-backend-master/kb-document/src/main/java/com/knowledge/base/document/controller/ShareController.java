package com.knowledge.base.document.controller;

import com.knowledge.base.common.result.Result;
import com.knowledge.base.document.service.DocumentService;
import com.knowledge.base.document.service.DocumentShareService;
import com.knowledge.base.document.vo.DocumentVO;
import com.knowledge.base.document.vo.ShareVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * Public share access controller
 *
 * <p>Provides share link access endpoints that do not require login</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/share")
@Tag(name = "Public Share Access", description = "Share link access endpoints that do not require login")
public class ShareController {

    @Resource
    private DocumentShareService documentShareService;

    @Resource
    private DocumentService documentService;

    @GetMapping("/{shareId}")
    @Operation(summary = "Get share info", description = "Gets basic share link information without incrementing the access count")
    public Result<ShareVO> getShareInfo(
            @Parameter(description = "Share ID", required = true)
            @PathVariable String shareId) {
        log.info("Public access get share info: shareId={}", shareId);
        ShareVO shareVO = documentShareService.getShareById(shareId);
        // Hide sensitive fields
        shareVO.setDocumentId(null);
        return Result.success(shareVO);
    }

    @PostMapping("/{shareId}/verify")
    @Operation(summary = "Verify share access", description = "Verifies the password without incrementing the access count")
    public Result<Boolean> verifyShare(
            @Parameter(description = "Share ID", required = true)
            @PathVariable String shareId,
            @Parameter(description = "Access password (optional)")
            @RequestParam(required = false) String password) {
        log.info("Public access verify share: shareId={}", shareId);
        boolean valid = documentShareService.verifyShareAccess(shareId, password);
        return Result.success(valid);
    }

    @PostMapping("/{shareId}/access")
    @Operation(summary = "Access share", description = "Verifies and accesses the share link, incrementing the access count, and returns the document content")
    public Result<DocumentVO> accessShare(
            @Parameter(description = "Share ID", required = true)
            @PathVariable String shareId,
            @Parameter(description = "Access password (optional)")
            @RequestParam(required = false) String password) {
        log.info("Public access share link: shareId={}", shareId);

        Long documentId = documentShareService.accessShare(shareId, password);
        DocumentVO document = documentService.viewDocument(documentId);

        log.info("Public share access successful: shareId={}, documentId={}", shareId, documentId);
        return Result.success(document);
    }
}
