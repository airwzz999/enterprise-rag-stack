package com.knowledge.base.document.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.base.document.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

/**
 * Comment Mapper interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

}
