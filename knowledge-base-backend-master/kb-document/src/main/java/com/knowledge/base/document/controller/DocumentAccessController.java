package com.knowledge.base.document.controller;

import com.knowledge.base.common.result.Result;
import com.knowledge.base.document.service.DocumentAccessService;
import com.knowledge.base.document.utils.UserContext;
import com.knowledge.base.document.vo.DocumentAccessVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Document access record Controller
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/access")
@Tag(name = "Document Access Records", description = "Document access record management endpoints")
@RequiredArgsConstructor
public class DocumentAccessController {

    private final DocumentAccessService documentAccessService;

    /**
     * Records a document access
     *
     * @param documentId document ID
     * @param documentTitle document title
     * @return whether successful
     */
    @PostMapping("/record")
    @Operation(summary = "Record document access", description = "Records a user's document access activity")
    public Result<Boolean> recordAccess(
            @Parameter(description = "Document ID", required = true)
            @RequestParam Long documentId,
            @Parameter(description = "Document title", required = true)
            @RequestParam String documentTitle) {
        log.info("Record document access request: documentId={}, documentTitle={}", documentId, documentTitle);

        Long userId = UserContext.getCurrentUserId();
        documentAccessService.recordAccess(userId, documentId, documentTitle);
        return Result.success(true);
    }

    /**
     * Gets the user's recent access records
     *
     * @param limit query result limit (default 20)
     * @return access record list
     */
    @GetMapping("/recent")
    @Operation(summary = "Get recent access records", description = "Gets the current user's recently accessed document list")
    public Result<List<DocumentAccessVO>> getRecentAccess(
            @Parameter(description = "Query result limit")
            @RequestParam(required = false) Integer limit) {
        log.info("Get recent access records request: limit={}", limit);

        List<DocumentAccessVO> accessList = documentAccessService.getRecentAccess(limit);
        return Result.success(accessList);
    }

    /**
     * Deletes a single access record
     *
     * @param documentId document ID
     * @return whether successful
     */
    @DeleteMapping("/remove/{documentId}")
    @Operation(summary = "Delete access record", description = "Deletes a single access record")
    public Result<Boolean> deleteAccess(
            @Parameter(description = "Document ID", required = true)
            @PathVariable Long documentId) {
        log.info("Delete access record request: documentId={}", documentId);

        documentAccessService.deleteAccess(documentId);
        return Result.success("Deleted successfully", true);
    }

    /**
     * Clears all of the user's access records
     *
     * @return whether successful
     */
    @DeleteMapping("/clear")
    @Operation(summary = "Clear access records", description = "Clears all of the current user's access records")
    public Result<Boolean> clearAllAccess() {
        log.info("Clear access records request");

        documentAccessService.clearAllAccess();
        return Result.success("Cleared successfully", true);
    }
}
