package com.knowledge.base.foundation.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.knowledge.base.foundation.entity.NotificationTemplate;

import java.util.List;

/**
 * Notification template Service interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface NotificationTemplateService {

    /**
     * Paginated query of the template list
     *
     * @param current current page
     * @param size page size
     * @param notificationType notification type (optional)
     * @return paginated result
     */
    IPage<NotificationTemplate> pageTemplates(Long current, Long size, String notificationType);

    /**
     * Get all active templates
     *
     * @return template list
     */
    List<NotificationTemplate> listActiveTemplates();

    /**
     * Get a template by ID
     *
     * @param id template ID
     * @return template information
     */
    NotificationTemplate getTemplateById(Long id);

    /**
     * Create a template
     *
     * @param template template entity
     * @return whether it succeeded
     */
    Boolean createTemplate(NotificationTemplate template);

    /**
     * Update a template
     *
     * @param template template entity
     * @return whether it succeeded
     */
    Boolean updateTemplate(NotificationTemplate template);

    /**
     * Delete a template
     *
     * @param id template ID
     * @return whether it succeeded
     */
    Boolean deleteTemplate(Long id);

    /**
     * Send a test
     *
     * @param id template ID
     * @param target test target (email/phone number)
     * @return whether it succeeded
     */
    Boolean testTemplate(Long id, String target);
}
