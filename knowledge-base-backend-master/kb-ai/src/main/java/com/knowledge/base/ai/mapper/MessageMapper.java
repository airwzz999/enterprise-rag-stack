package com.knowledge.base.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.base.ai.entity.Message;
import org.apache.ibatis.annotations.Mapper;

/**
 * Message mapper
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}