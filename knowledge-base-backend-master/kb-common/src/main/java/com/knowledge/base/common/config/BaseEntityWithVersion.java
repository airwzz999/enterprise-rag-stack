package com.knowledge.base.common.config;

import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Base entity class with optimistic locking
 *
 * <p>Extends BaseEntity, adding an optimistic-lock version field</p>
 * <p>Used for entity classes that need optimistic lock control</p>
 *
 * <p>Usage notes:</p>
 * <ul>
 *   <li>A version field must be added to the database table</li>
 *   <li>MyBatis Plus automatically handles the optimistic-lock logic</li>
 *   <li>On update, the version is checked and incremented automatically</li>
 * </ul>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class BaseEntityWithVersion extends BaseEntity {

    /**
     * Optimistic lock version number
     *
     * <p>MyBatis Plus automatically handles optimistic locking:</p>
     * <ul>
     *   <li>On query: reads the version value</li>
     *   <li>On update: checks whether the version matches; if so, increments and updates it, otherwise returns failure</li>
     * </ul>
     *
     * <p>Database table design requirements:</p>
     * <ul>
     *   <li>Field name: version</li>
     *   <li>Type: INT</li>
     *   <li>Default value: 0</li>
     *   <li>Not null</li>
     * </ul>
     */
    @Version
    private Integer version;
}
