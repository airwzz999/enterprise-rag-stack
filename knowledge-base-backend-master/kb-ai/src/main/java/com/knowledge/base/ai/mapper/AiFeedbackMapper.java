package com.knowledge.base.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.base.ai.entity.AiFeedback;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI feedback mapper
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Mapper
public interface AiFeedbackMapper extends BaseMapper<AiFeedback> {
}