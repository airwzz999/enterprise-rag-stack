package com.knowledge.base.document.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.base.document.entity.Like;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Like Mapper interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Mapper
public interface LikeMapper extends BaseMapper<Like> {

    /**
     * Deletes a like record by target ID, user ID, and target type
     *
     * @param targetId   target ID
     * @param userId     user ID
     * @param targetType target type
     * @return number of rows deleted
     */
    int deleteByTargetAndUser(@Param("targetId") Long targetId,
                               @Param("userId") Long userId,
                               @Param("targetType") Integer targetType);
}
