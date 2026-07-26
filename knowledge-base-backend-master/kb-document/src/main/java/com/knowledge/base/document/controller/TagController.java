package com.knowledge.base.document.controller;

import com.knowledge.base.common.annotation.OperationLog;
import com.knowledge.base.common.result.PageResult;
import com.knowledge.base.common.result.Result;
import com.knowledge.base.document.dto.TagCreateDTO;
import com.knowledge.base.document.dto.TagQueryDTO;
import com.knowledge.base.document.dto.TagUpdateDTO;
import com.knowledge.base.document.service.TagService;
import com.knowledge.base.document.vo.TagVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Tag management Controller
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
@Tag(name = "Tag Management", description = "Tag management related endpoints")
public class TagController {

    private final TagService tagService;

    /**
     * Creates a tag
     */
    @PostMapping
    @Operation(summary = "Create tag", description = "Creates a new tag")
    @OperationLog(module = "Tag Management", operation = "Create Tag", description = "Creates a new tag")
    @PreAuthorize("hasAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_TAG)")
    public Result<Long> createTag(@Valid @RequestBody TagCreateDTO dto) {
        Long tagId = tagService.createTag(dto);
        return Result.success(tagId);
    }

    /**
     * Updates a tag
     */
    @PutMapping
    @Operation(summary = "Update tag", description = "Updates tag information")
    @OperationLog(module = "Tag Management", operation = "Update Tag", description = "Updates tag information")
    @PreAuthorize("hasAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_TAG)")
    public Result<Boolean> updateTag(@Valid @RequestBody TagUpdateDTO dto) {
        Boolean result = tagService.updateTag(dto);
        return Result.success(result);
    }

    /**
     * Deletes a tag
     */
    @DeleteMapping("/{tagId}")
    @Operation(summary = "Delete tag", description = "Deletes the specified tag")
    @OperationLog(module = "Tag Management", operation = "Delete Tag", description = "Deletes a tag")
    @PreAuthorize("hasAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_TAG)")
    public Result<Boolean> deleteTag(@PathVariable Long tagId) {
        Boolean result = tagService.deleteTag(tagId);
        return Result.success(result);
    }

    /**
     * Gets tag details
     */
    @GetMapping("/{tagId}")
    @Operation(summary = "Get tag details", description = "Gets tag details by ID")
    public Result<TagVO> getTagDetail(@PathVariable Long tagId) {
        TagVO tagVO = tagService.getTagDetail(tagId);
        return Result.success(tagVO);
    }

    /**
     * Paginated tag query
     */
    @PostMapping("/page")
    @Operation(summary = "Paginated tag query", description = "Paginated query of the tag list")
    public Result<PageResult<TagVO>> pageTags(@RequestBody TagQueryDTO dto) {
        PageResult<TagVO> pageResult = tagService.pageTags(dto);
        return Result.success(pageResult);
    }

    /**
     * Gets popular tags
     */
    @GetMapping("/hot")
    @Operation(summary = "Get popular tags", description = "Gets the most-used tags")
    public Result<List<TagVO>> getHotTags(
            @RequestParam(defaultValue = "10") Integer limit) {
        List<TagVO> hotTags = tagService.getHotTags(limit);
        return Result.success(hotTags);
    }

    /**
     * Gets tags by category
     */
    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Get tags by category", description = "Gets the tags under the specified category")
    public Result<List<TagVO>> getTagsByCategory(@PathVariable Long categoryId) {
        List<TagVO> tags = tagService.getTagsByCategory(categoryId);
        return Result.success(tags);
    }
}
