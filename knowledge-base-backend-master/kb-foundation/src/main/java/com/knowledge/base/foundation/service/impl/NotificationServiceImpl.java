package com.knowledge.base.foundation.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.knowledge.base.common.exception.BusinessException;
import com.knowledge.base.common.result.PageParam;
import com.knowledge.base.common.result.Result;
import com.knowledge.base.common.utils.SnowflakeIdGenerator;
import com.knowledge.base.foundation.dto.NotificationDTO;
import com.knowledge.base.foundation.dto.NotificationQueryDTO;
import com.knowledge.base.foundation.entity.Notification;
import com.knowledge.base.foundation.mapper.NotificationMapper;
import com.knowledge.base.foundation.service.NotificationService;
import com.knowledge.base.foundation.vo.NotificationVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {

    @Resource
    private NotificationMapper notificationMapper;

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> sendNotification(NotificationDTO notificationDTO) {
        log.info("Send notification: userId={}, type={}", notificationDTO.getUserId(), notificationDTO.getNotificationType());

        if (notificationDTO.getUserId() == null) {
            throw new BusinessException("Recipient user ID must not be empty");
        }
        if (!StringUtils.hasText(notificationDTO.getNotificationType())) {
            throw new BusinessException("Notification type must not be empty");
        }
        if (!StringUtils.hasText(notificationDTO.getTitle())) {
            throw new BusinessException("Notification title must not be empty");
        }
        if (!StringUtils.hasText(notificationDTO.getContent())) {
            throw new BusinessException("Notification content must not be empty");
        }

        Notification notification = new Notification();
        BeanUtil.copyProperties(notificationDTO, notification);
        notification.setId(SnowflakeIdGenerator.getInstance().nextId());

        if (notification.getIsRead() == null) {
            notification.setIsRead(0);
        }
        notification.setCreatedAt(LocalDateTime.now());

        int count = notificationMapper.insert(notification);
        if (count <= 0) {
            throw new BusinessException("Failed to send notification");
        }

        log.info("Notification sent successfully: notificationId={}", notification.getId());
        return Result.success(notification.getId());
    }

    /** {@inheritDoc} */
    @Override
    public IPage<Notification> pageNotifications(Long current, Long size, Long userId, Integer isRead) {
        log.info("Paginated query of notifications: current={}, size={}, userId={}, isRead={}", current, size, userId, isRead);

        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();

        if (userId != null) {
            wrapper.eq(Notification::getUserId, userId);
        }
        if (isRead != null) {
            wrapper.eq(Notification::getIsRead, isRead);
        }

        wrapper.orderByDesc(Notification::getCreatedAt);

        Page<Notification> page = new Page<>(current, size);
        return notificationMapper.selectPage(page, wrapper);
    }

    /** {@inheritDoc} */
    @Override
    public Result<IPage<NotificationVO>> getNotifications(NotificationQueryDTO queryDTO) {
        log.info("Query notification list: userId={}", queryDTO.getUserId());

        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();

        if (queryDTO.getUserId() == null) {
            throw new BusinessException("User ID must not be empty");
        }
        wrapper.eq(Notification::getUserId, queryDTO.getUserId());

        if (StringUtils.hasText(queryDTO.getNotificationType())) {
            wrapper.eq(Notification::getNotificationType, queryDTO.getNotificationType());
        }

        if (queryDTO.getIsRead() != null) {
            wrapper.eq(Notification::getIsRead, queryDTO.getIsRead());
        }

        if (StringUtils.hasText(queryDTO.getStartTime())) {
            wrapper.ge(Notification::getCreatedAt, queryDTO.getStartTime());
        }
        if (StringUtils.hasText(queryDTO.getEndTime())) {
            wrapper.le(Notification::getCreatedAt, queryDTO.getEndTime());
        }

        wrapper.orderByDesc(Notification::getCreatedAt);

        PageParam pageParam = queryDTO;
        Page<Notification> page = new Page<>(pageParam.getCurrent(), pageParam.getSize());
        IPage<Notification> notificationPage = notificationMapper.selectPage(page, wrapper);

        IPage<NotificationVO> voPage = notificationPage.convert(notification -> BeanUtil.copyProperties(notification, NotificationVO.class));

        return Result.success(voPage);
    }

    /** {@inheritDoc} */
    @Override
    public Notification getNotificationById(Long id, Long userId) {
        log.info("Query notification details: id={}, userId={}", id, userId);
        Notification notification = notificationMapper.selectById(id);
        if (notification == null) {
            throw new BusinessException("Notification does not exist");
        }
        if (!notification.getUserId().equals(userId)) {
            throw new BusinessException("No permission to access this notification");
        }
        return notification;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean sendNotification(Notification notification) {
        log.info("Send notification: userId={}, title={}", notification.getUserId(), notification.getTitle());

        if (notification.getUserId() == null) {
            throw new BusinessException("Recipient user ID must not be empty");
        }

        notification.setId(SnowflakeIdGenerator.getInstance().nextId());

        if (notification.getIsRead() == null) {
            notification.setIsRead(0);
        }
        notification.setCreatedAt(LocalDateTime.now());

        int count = notificationMapper.insert(notification);
        return count > 0;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> markAsRead(Long id, Long userId) {
        log.info("Mark notification as read: notificationId={}, userId={}", id, userId);

        if (id == null) {
            throw new BusinessException("Notification ID must not be empty");
        }

        Notification notification = notificationMapper.selectById(id);
        if (notification == null) {
            throw new BusinessException("Notification does not exist");
        }
        if (!notification.getUserId().equals(userId)) {
            throw new BusinessException("No permission to modify this notification");
        }

        LambdaUpdateWrapper<Notification> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Notification::getId, id)
                .set(Notification::getIsRead, 1)
                .set(Notification::getReadTime, LocalDateTime.now());

        int count = notificationMapper.update(null, updateWrapper);
        return Result.success(count > 0);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> markAllAsRead(Long userId) {
        log.info("Mark all notifications as read: userId={}", userId);

        if (userId == null) {
            throw new BusinessException("User ID must not be empty");
        }

        LambdaUpdateWrapper<Notification> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0)
                .set(Notification::getIsRead, 1)
                .set(Notification::getReadTime, LocalDateTime.now());

        int count = notificationMapper.update(null, updateWrapper);
        log.info("Marked {} notification(s) as read", count);
        return Result.success(true);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> deleteNotification(Long id, Long userId) {
        log.info("Delete notification: notificationId={}, userId={}", id, userId);

        if (id == null) {
            throw new BusinessException("Notification ID must not be empty");
        }

        Notification notification = notificationMapper.selectById(id);
        if (notification == null) {
            throw new BusinessException("Notification does not exist");
        }
        if (!notification.getUserId().equals(userId)) {
            throw new BusinessException("No permission to delete this notification");
        }

        int count = notificationMapper.deleteById(id);
        return Result.success(count > 0);
    }

    /** {@inheritDoc} */
    @Override
    public Result<Long> getUnreadCount(Long userId) {
        log.info("Get unread notification count: userId={}", userId);

        if (userId == null) {
            throw new BusinessException("User ID must not be empty");
        }

        Long count = notificationMapper.countUnreadByUserId(userId);
        return Result.success(count);
    }
}