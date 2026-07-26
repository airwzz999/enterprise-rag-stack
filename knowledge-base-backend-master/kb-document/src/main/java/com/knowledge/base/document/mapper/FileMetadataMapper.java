package com.knowledge.base.document.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.base.document.entity.FileMetadata;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * File metadata Mapper interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Mapper
public interface FileMetadataMapper extends BaseMapper<FileMetadata> {

    /**
     * Queries a user's file list
     *
     * @param userId user ID
     * @return file list
     */
    @Select("SELECT * FROM kb_file_metadata WHERE uploader_id = #{userId} AND deleted = 0 ORDER BY created_at DESC")
    List<FileMetadata> findByUploaderId(@Param("userId") Long userId);

    /**
     * Queries a file list filtered by file category
     *
     * @param userId       user ID
     * @param fileCategory file category
     * @return file list
     */
    @Select("SELECT * FROM kb_file_metadata WHERE uploader_id = #{userId} AND file_category = #{fileCategory} AND deleted = 0 ORDER BY created_at DESC")
    List<FileMetadata> findByUploaderIdAndCategory(@Param("userId") Long userId, @Param("fileCategory") String fileCategory);

    /**
     * Counts a user's files
     *
     * @param userId user ID
     * @return file count
     */
    @Select("SELECT COUNT(*) FROM kb_file_metadata WHERE uploader_id = #{userId} AND deleted = 0")
    Integer countByUploaderId(@Param("userId") Long userId);

    /**
     * Sums the total file size for a user
     *
     * @param userId user ID
     * @return total file size
     */
    @Select("SELECT COALESCE(SUM(file_size), 0) FROM kb_file_metadata WHERE uploader_id = #{userId} AND deleted = 0")
    Long sumFileSizeByUploaderId(@Param("userId") Long userId);
}
