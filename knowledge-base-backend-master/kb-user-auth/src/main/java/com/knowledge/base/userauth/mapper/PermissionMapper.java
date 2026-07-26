package com.knowledge.base.userauth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.base.userauth.entity.Permission;
import org.apache.ibatis.annotations.Mapper;

/**
 * Permission Mapper interface
 *
 * <p>Designed following the Alibaba Java Development Guidelines; provides permission data access operations</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

}