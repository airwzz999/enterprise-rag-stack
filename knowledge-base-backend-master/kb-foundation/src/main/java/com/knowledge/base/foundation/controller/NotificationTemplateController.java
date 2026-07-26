package com.knowledge.base.foundation.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.knowledge.base.common.result.PageResult;
import com.knowledge.base.common.result.Result;
import com.knowledge.base.foundation.entity.NotificationTemplate;
import com.knowledge.base.foundation.service.NotificationTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Notification template management Controller
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/notifications/templates")
@Tag(name = "Notification Template Management", description = "Notification template management endpoints")
public class NotificationTemplateController {

    @Resource
    private NotificationTemplateService templateService;

    /**
     * Paginated query of the template list
     */
    @GetMapping
    @Operation(summary = "Query template list", description = "Paginated query of the notification template list")
    public Result<PageResult<NotificationTemplate>> listTemplates(
            @Parameter(description = "Current page")
            @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "Notification type (optional)")
            @RequestParam(required = false) String notificationType) {
        log.info("Query template list: current={}, size={}, notificationType={}", current, size, notificationType);

        IPage<NotificationTemplate> page = templateService.pageTemplates(current, size, notificationType);
        PageResult<NotificationTemplate> pageResult = new PageResult<>();
        pageResult.setRecords(page.getRecords());
        pageResult.setTotal(page.getTotal());
        pageResult.setCurrent(page.getCurrent());
        pageResult.setSize(page.getSize());
        return Result.success(pageResult);
    }

    /**
     * Get all active templates
     */
    @GetMapping("/active")
    @Operation(summary = "Get active templates", description = "Get the list of all active templates")
    public Result<List<NotificationTemplate>> listActiveTemplates() {
        log.info("Get active template list");

        List<NotificationTemplate> templates = templateService.listActiveTemplates();
        return Result.success(templates);
    }

    /**
     * Get template details by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get template details", description = "Get notification template details by ID")
    public Result<NotificationTemplate> getTemplate(
            @Parameter(description = "Template ID", required = true)
            @PathVariable Long id) {
        log.info("Get template details: id={}", id);

        NotificationTemplate template = templateService.getTemplateById(id);
        return Result.success(template);
    }

    /**
     * Create a template
     */
    @PostMapping
    @Operation(summary = "Create template", description = "Create a new notification template")
    public Result<Boolean> createTemplate(
            @Parameter(description = "Template entity", required = true)
            @RequestBody NotificationTemplate template) {
        log.info("Create template: templateCode={}", template.getTemplateCode());

        Boolean result = templateService.createTemplate(template);
        return Result.success(result);
    }

    /**
     * Update a template
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update template", description = "Update notification template information")
    public Result<Boolean> updateTemplate(
            @Parameter(description = "Template ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "Template entity", required = true)
            @RequestBody NotificationTemplate template) {
        log.info("Update template: id={}", id);

        template.setId(id);
        Boolean result = templateService.updateTemplate(template);
        return Result.success(result);
    }

    /**
     * Delete a template
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete template", description = "Delete the specified notification template")
    public Result<Boolean> deleteTemplate(
            @Parameter(description = "Template ID", required = true)
            @PathVariable Long id) {
        log.info("Delete template: id={}", id);

        Boolean result = templateService.deleteTemplate(id);
        return Result.success(result);
    }

    /**
     * Send a test
     */
    @PostMapping("/{id}/test")
    @Operation(summary = "Send test", description = "Send a test notification using the template")
    public Result<Boolean> testTemplate(
            @Parameter(description = "Template ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "Test target", required = true)
            @RequestParam String target) {
        log.info("Send test template: id={}, target={}", id, target);

        Boolean result = templateService.testTemplate(id, target);
        return Result.success(result);
    }
}
