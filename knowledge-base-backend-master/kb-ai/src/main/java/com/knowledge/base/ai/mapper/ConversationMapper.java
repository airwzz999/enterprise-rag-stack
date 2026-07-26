package com.knowledge.base.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.base.ai.entity.Conversation;
import org.apache.ibatis.annotations.Mapper;

/**
 * Conversation mapper
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {
}