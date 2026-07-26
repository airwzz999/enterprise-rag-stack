package com.knowledge.base.userauth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.base.userauth.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * User Mapper interface
 *
 * <p>Designed following the Alibaba Java Development Guidelines; provides user data access operations</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * Query a user by username
     *
     * @param username username
     * @return user information
     */
    User selectByUsername(@Param("username") String username);

    /**
     * Query a user by email
     *
     * @param email email
     * @return user information
     */
    User selectByEmail(@Param("email") String email);

    /**
     * Query a user by phone number
     *
     * @param phone phone number
     * @return user information
     */
    User selectByPhone(@Param("phone") String phone);

    /**
     * Count all users
     *
     * @return user count
     */
    int countUsers();

    /**
     * Count a user's documents (cross-database query against the kb_document view)
     *
     * @param authorId author ID
     * @return document count
     */
    Long countDocumentsByAuthorId(@Param("authorId") Long authorId);

    /**
     * Sum a user's total likes received (cross-database query against the kb_document view)
     *
     * @param authorId author ID
     * @return total likes
     */
    Long sumLikesByAuthorId(@Param("authorId") Long authorId);

    /**
     * Sum a user's total views received (cross-database query against the kb_document view)
     *
     * @param authorId author ID
     * @return total views
     */
    Long sumViewsByAuthorId(@Param("authorId") Long authorId);
}