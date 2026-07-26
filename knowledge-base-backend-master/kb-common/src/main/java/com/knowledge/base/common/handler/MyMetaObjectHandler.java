package com.knowledge.base.common.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.knowledge.base.common.utils.UserContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis Plus field auto-fill handler
 *
 * <p>Designed following the Alibaba Java Development Guidelines, automatically fills fields such as creation time and update time</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    /**
     * Auto-fill on insert
     *
     * @param metaObject the meta object
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        log.debug("Starting insert fill...");
        LocalDateTime now = LocalDateTime.now();
        Long userId = UserContextUtil.getUserId();

        // Fill creation time
        if (metaObject.hasSetter("createdAt")) {
            this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        }

        // Fill update time
        if (metaObject.hasSetter("updatedAt")) {
            this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
        }

        // Fill creator ID
        if (userId != null) {
            if (metaObject.hasSetter("createBy")) {
                this.strictInsertFill(metaObject, "createBy", Long.class, userId);
            }

            // Fill updater ID
            if (metaObject.hasSetter("updatedBy")) {
                this.strictInsertFill(metaObject, "updatedBy", Long.class, userId);
            }
        }
    }

    /**
     * Auto-fill on update
     *
     * @param metaObject the meta object
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        log.debug("Starting update fill...");
        Long userId = UserContextUtil.getUserId();

        // Fill update time
        if (metaObject.hasSetter("updatedAt")) {
            this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
        }

        // Fill updater ID
        if (userId != null) {
            if (metaObject.hasSetter("updateBy")) {
                this.strictUpdateFill(metaObject, "updateBy", Long.class, userId);
            }
            if (metaObject.hasSetter("updatedBy")) {
                this.strictUpdateFill(metaObject, "updatedBy", Long.class, userId);
            }
        }
    }
}
