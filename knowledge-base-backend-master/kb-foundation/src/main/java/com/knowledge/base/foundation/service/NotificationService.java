package com.knowledge.base.foundation.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.knowledge.base.common.result.Result;
import com.knowledge.base.foundation.dto.NotificationDTO;
import com.knowledge.base.foundation.dto.NotificationQueryDTO;
import com.knowledge.base.foundation.entity.Notification;
import com.knowledge.base.foundation.vo.NotificationVO;

/**
 * Notification Service interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface NotificationService {

    /**
     * Send a notification (DTO-based)
     *
     * @param notificationDTO notification DTO
     * @return notification ID
     */
    Result<Long> sendNotification(NotificationDTO notificationDTO);

    /**
     * Paginated query of notifications
     *
     * @param current current page
     * @param size    page size
     * @param userId  user ID
     * @param isRead  whether read (optional)
     * @return paginated result
     */
    IPage<Notification> pageNotifications(Long current, Long size, Long userId, Integer isRead);

    /**
     * Query the notification list (query-DTO-based)
     *
     * @param queryDTO query conditions
     * @return paginated result
     */
    Result<IPage<NotificationVO>> getNotifications(NotificationQueryDTO queryDTO);

    /**
     * Get a notification by ID
     *
     * @param id notification ID
     * @return notification information
     */
    Notification getNotificationById(Long id);

    /**
     * Send a notification (entity-based, for internal calls)
     *
     * @param notification notification entity
     * @return whether it succeeded
     */
    Boolean sendNotification(Notification notification);

    /**
     * Mark a notification as read
     *
     * @param id notification ID
     * @return whether it succeeded
     */
    Result<Boolean> markAsRead(Long id);

    /**
     * Mark all notifications as read
     *
     * @param userId user ID
     * @return whether it succeeded
     */
    Result<Boolean> markAllAsRead(Long userId);

    /**
     * Delete a notification
     *
     * @param id notification ID
     * @return whether it succeeded
     */
    Result<Boolean> deleteNotification(Long id);

    /**
     * Get the unread notification count
     *
     * @param userId user ID
     * @return unread count
     */
    Result<Long> getUnreadCount(Long userId);
}
