package com.knowledge.base.foundation.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.knowledge.base.common.result.Result;
import com.knowledge.base.foundation.entity.Dict;
import com.knowledge.base.foundation.entity.DictData;
import com.knowledge.base.foundation.service.DictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Dictionary Controller
 *
 * <p>Designed according to the Alibaba Java Development Guidelines; provides
 * dictionary management endpoints</p>
 *
 * <p>Admin-only: dictionary entries are shared, system-wide lookup data (used to
 * populate dropdowns/enums across the app), and only the admin dictionary-management
 * page ({@code admin/dictionary}, gated {@code requireAdmin} in the frontend) uses this
 * API, so any authenticated user being able to create/edit/delete entries directly
 * would let them corrupt shared data for everyone.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/dicts")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Dictionary Management", description = "Dictionary data management endpoints")
public class DictController {

    @Resource
    private DictService dictService;

    /**
     * Paginated query of the dictionary type list
     *
     * @param current current page
     * @param size    page size
     * @param keyword search keyword
     * @return paginated dictionary information
     */
    @GetMapping
    @Operation(summary = "Paginated query of dictionaries", description = "Paginated query of the dictionary type list")
    public Result<IPage<Dict>> pageDicts(
        @Parameter(description = "Current page") @RequestParam(defaultValue = "1") Long current,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "10") Long size,
        @Parameter(description = "Search keyword") @RequestParam(required = false) String keyword) {
        log.info("Paginated dictionary query request: current={}, size={}, keyword={}", current, size, keyword);

        IPage<Dict> page = dictService.pageDicts(current, size, keyword);
        return Result.success(page);
    }

    /**
     * Query dictionary details by dictionary code
     *
     * @param code dictionary code
     * @return dictionary details
     */
    @GetMapping("/{code}")
    @Operation(summary = "Query dictionary details", description = "Query dictionary details by dictionary code")
    public Result<Dict> getDictByCode(
        @Parameter(description = "Dictionary code", required = true)
        @PathVariable String code) {
        log.info("Query dictionary details request: code={}", code);

        Dict dict = dictService.getDictByCode(code);
        return Result.success(dict);
    }

    /**
     * Create a dictionary
     *
     * @param dict dictionary information
     * @return whether it succeeded
     */
    @PostMapping
    @Operation(summary = "Create dictionary", description = "Create a new dictionary type")
    public Result<Boolean> createDict(@Valid @RequestBody Dict dict) {
        log.info("Create dictionary request: code={}, name={}", dict.getDictCode(), dict.getDictName());

        Boolean success = dictService.createDict(dict);
        return Result.success("Dictionary created successfully", success);
    }

    /**
     * Update a dictionary
     *
     * @param code dictionary code
     * @param dict dictionary information
     * @return whether it succeeded
     */
    @PutMapping("/{code}")
    @Operation(summary = "Update dictionary", description = "Update dictionary type information")
    public Result<Boolean> updateDict(
        @Parameter(description = "Dictionary code", required = true)
        @PathVariable String code,
        @Valid @RequestBody Dict dict) {
        log.info("Update dictionary request: code={}", code);

        Boolean success = dictService.updateDict(code, dict);
        return Result.success("Dictionary updated successfully", success);
    }

    /**
     * Delete a dictionary
     *
     * @param code dictionary code
     * @return whether it succeeded
     */
    @DeleteMapping("/{code}")
    @Operation(summary = "Delete dictionary", description = "Delete a dictionary by dictionary code")
    public Result<Boolean> deleteDict(
        @Parameter(description = "Dictionary code", required = true)
        @PathVariable String code) {
        log.info("Delete dictionary request: code={}", code);

        Boolean success = dictService.deleteDict(code);
        return Result.success("Dictionary deleted successfully", success);
    }

    /**
     * Get the dictionary data list
     *
     * @param code dictionary code
     * @return dictionary data list
     */
    @GetMapping("/{code}/data")
    @Operation(summary = "Get dictionary data", description = "Get the dictionary data list by dictionary code")
    public Result<List<DictData>> getDictData(
        @Parameter(description = "Dictionary code", required = true)
        @PathVariable String code) {
        log.info("Get dictionary data request: code={}", code);

        List<DictData> dataList = dictService.getDictData(code);
        return Result.success(dataList);
    }

    /**
     * Add dictionary data
     *
     * @param code     dictionary code
     * @param dictData dictionary data
     * @return whether it succeeded
     */
    @PostMapping("/{code}/data")
    @Operation(summary = "Add dictionary data", description = "Add a data item to the specified dictionary")
    public Result<Boolean> addDictData(
        @Parameter(description = "Dictionary code", required = true)
        @PathVariable String code,
        @Valid @RequestBody DictData dictData) {
        log.info("Add dictionary data request: code={}, label={}", code, dictData.getDictLabel());

        Boolean success = dictService.addDictData(code, dictData);
        return Result.success("Dictionary data added successfully", success);
    }

    /**
     * Update dictionary data
     *
     * @param code     dictionary code
     * @param dictData dictionary data
     * @return whether it succeeded
     */
    @PutMapping("/{code}/data")
    @Operation(summary = "Update dictionary data", description = "Update a dictionary data item")
    public Result<Boolean> updateDictData(
        @Parameter(description = "Dictionary code", required = true)
        @PathVariable String code,
        @Valid @RequestBody DictData dictData) {
        log.info("Update dictionary data request: code={}, id={}", code, dictData.getId());

        Boolean success = dictService.updateDictData(code, dictData);
        return Result.success("Dictionary data updated successfully", success);
    }

    /**
     * Delete dictionary data
     *
     * @param code dictionary code
     * @param id   data ID
     * @return whether it succeeded
     */
    @DeleteMapping("/{code}/data/{id}")
    @Operation(summary = "Delete dictionary data", description = "Delete the specified dictionary data item")
    public Result<Boolean> deleteDictData(
        @Parameter(description = "Dictionary code", required = true)
        @PathVariable String code,
        @Parameter(description = "Data ID", required = true)
        @PathVariable Long id) {
        log.info("Delete dictionary data request: code={}, id={}", code, id);

        Boolean success = dictService.deleteDictData(code, id);
        return Result.success("Dictionary data deleted successfully", success);
    }
}
