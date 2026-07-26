package com.knowledge.base.document.controller;

import com.knowledge.base.common.result.Result;
import com.knowledge.base.document.service.UserFavoriteService;
import com.knowledge.base.document.utils.UserContext;
import com.knowledge.base.document.vo.UserFavoriteVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * User favorite Controller
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/favorite")
@Tag(name = "User Favorites", description = "User favorite management endpoints")
public class UserFavoriteController {

    @Resource
    private UserFavoriteService userFavoriteService;

    /**
     * Toggles favorite status
     *
     * @param documentId document ID
     * @return favorite status (true-favorited, false-not favorited)
     */
    @PostMapping("/toggle/{documentId}")
    @Operation(summary = "Toggle favorite status", description = "Toggles the favorite status of a document")
    public Result<Boolean> toggleFavorite(
            @Parameter(description = "Document ID", required = true)
            @PathVariable Long documentId) {
        log.info("Toggle favorite status request: documentId={}", documentId);

        Long userId = UserContext.getCurrentUserId();
        Boolean isFavorited = userFavoriteService.toggleFavorite(userId, documentId);
        return Result.success(isFavorited);
    }

    /**
     * Adds a favorite
     *
     * @param documentId document ID
     * @return whether successful
     */
    @PostMapping("/add/{documentId}")
    @Operation(summary = "Add favorite", description = "Adds a document to favorites")
    public Result<Boolean> addFavorite(
            @Parameter(description = "Document ID", required = true)
            @PathVariable Long documentId) {
        log.info("Add favorite request: documentId={}", documentId);

        Long userId = UserContext.getCurrentUserId();
        Boolean result = userFavoriteService.addFavorite(userId, documentId);
        return Result.success("Favorite added successfully", result);
    }

    /**
     * Removes a favorite
     *
     * @param documentId document ID
     * @return whether successful
     */
    @DeleteMapping("/remove/{documentId}")
    @Operation(summary = "Remove favorite", description = "Removes a document from favorites")
    public Result<Boolean> removeFavorite(
            @Parameter(description = "Document ID", required = true)
            @PathVariable Long documentId) {
        log.info("Remove favorite request: documentId={}", documentId);

        Long userId = UserContext.getCurrentUserId();
        Boolean result = userFavoriteService.removeFavorite(userId, documentId);
        return Result.success("Favorite removed successfully", result);
    }

    /**
     * Checks whether a document is favorited
     *
     * @param documentId document ID
     * @return whether favorited
     */
    @GetMapping("/check/{documentId}")
    @Operation(summary = "Check favorite status", description = "Checks whether a document has been favorited")
    public Result<Boolean> checkFavorite(
            @Parameter(description = "Document ID", required = true)
            @PathVariable Long documentId) {
        log.info("Check favorite status request: documentId={}", documentId);

        Long userId = UserContext.getCurrentUserId();
        Boolean isFavorited = userFavoriteService.isFavorited(userId, documentId);
        return Result.success(isFavorited);
    }

    /**
     * Gets the current user's favorite list
     *
     * @return favorite list
     */
    @GetMapping("/list")
    @Operation(summary = "Get favorite list", description = "Gets the current user's favorite list")
    public Result<List<UserFavoriteVO>> getUserFavorites() {
        log.info("Get user favorite list request");

        Long userId = UserContext.getCurrentUserId();
        List<UserFavoriteVO> favorites = userFavoriteService.getUserFavorites(userId);
        return Result.success(favorites);
    }

    /**
     * Gets the favorite count for a document
     *
     * @param documentId document ID
     * @return favorite count
     */
    @GetMapping("/count/{documentId}")
    @Operation(summary = "Get favorite count", description = "Gets the favorite count for a document")
    public Result<Long> getFavoriteCount(
            @Parameter(description = "Document ID", required = true)
            @PathVariable Long documentId) {
        log.info("Get favorite count request: documentId={}", documentId);

        Long count = userFavoriteService.getFavoriteCount(documentId);
        return Result.success(count);
    }
}
