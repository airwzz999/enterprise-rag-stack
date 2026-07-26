package com.knowledge.base.foundation.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.knowledge.base.common.result.Result;
import com.knowledge.base.common.utils.UserContextUtil;
import com.knowledge.base.foundation.entity.Notification;
import com.knowledge.base.foundation.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/notifications")
@Tag(name = "Notification Management", description = "System notification management endpoints")
public class NotificationController {

    @Resource
    private NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Paginated query of notifications", description = "Paginated query of the notification list")
    public Result<IPage<Notification>> pageNotifications(
        @Parameter(description = "Current page") @RequestParam(defaultValue = "1") Long current,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "10") Long size,
        @Parameter(description = "Whether read") @RequestParam(required = false) Integer isRead) {
        Long userId = UserContextUtil.getUserId();
        log.info("Paginated notification query request: current={}, size={}, userId={}, isRead={}", current, size, userId, isRead);

        IPage<Notification> page = notificationService.pageNotifications(current, size, userId, isRead);
        return Result.success(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Query notification details", description = "Query notification details by notification ID")
    public Result<Notification> getNotificationById(
        @Parameter(description = "Notification ID", required = true)
        @PathVariable Long id) {
        log.info("Query notification details request: id={}", id);

        Notification notification = notificationService.getNotificationById(id);
        return Result.success(notification);
    }

    @PostMapping
    @Operation(summary = "Send notification", description = "Create a new notification")
    public Result<Boolean> sendNotification(@Valid @RequestBody Notification notification) {
        log.info("Send notification request: userId={}, title={}", notification.getUserId(), notification.getTitle());

        Boolean success = notificationService.sendNotification(notification);
        return Result.success("Notification sent successfully", success);
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark as read", description = "Mark a notification as read")
    public Result<Boolean> markAsRead(
        @Parameter(description = "Notification ID", required = true)
        @PathVariable Long id) {
        log.info("Mark notification as read request: id={}", id);

        return notificationService.markAsRead(id);
    }

    @PutMapping("/read-all")
    @Operation(summary = "Mark all as read", description = "Mark all of the user's unread notifications as read")
    public Result<Boolean> markAllAsRead() {
        Long userId = UserContextUtil.getUserId();
        log.info("Mark all as read request: userId={}", userId);

        return notificationService.markAllAsRead(userId);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete notification", description = "Delete a notification by notification ID")
    public Result<Boolean> deleteNotification(
        @Parameter(description = "Notification ID", required = true)
        @PathVariable Long id) {
        log.info("Delete notification request: id={}", id);

        return notificationService.deleteNotification(id);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread count", description = "Get the user's unread notification count")
    public Result<Long> getUnreadCount() {
        Long userId = UserContextUtil.getUserId();
        log.info("Get unread count request: userId={}", userId);

        return notificationService.getUnreadCount(userId);
    }
}