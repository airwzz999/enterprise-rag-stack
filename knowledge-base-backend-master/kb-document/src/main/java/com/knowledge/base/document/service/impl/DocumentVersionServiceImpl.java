package com.knowledge.base.document.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.base.common.exception.BusinessException;
import com.knowledge.base.common.utils.SnowflakeIdGenerator;
import com.knowledge.base.document.dto.DocumentVersionRestoreDTO;
import com.knowledge.base.document.entity.Document;
import com.knowledge.base.document.entity.DocumentVersion;
import com.knowledge.base.document.mapper.DocumentMapper;
import com.knowledge.base.document.mapper.DocumentVersionMapper;
import com.knowledge.base.document.service.DocumentContentService;
import com.knowledge.base.document.service.DocumentVersionService;
import com.knowledge.base.document.vo.DocumentVersionVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Document version Service implementation class
 *
 * <p>Designed following the Alibaba Java Coding Guidelines, implements document version related business logic</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Service
public class DocumentVersionServiceImpl implements DocumentVersionService {

    @Resource
    private DocumentVersionMapper documentVersionMapper;

    @Resource
    private DocumentMapper documentMapper;

    @Resource
    private DocumentContentService documentContentService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createVersion(Long documentId, String changeDescription, Long userId) {
        log.info("Create document version: documentId={}, userId={}", documentId, userId);

        if (documentId == null) {
            throw new BusinessException("Document ID must not be null");
        }

        // Get document information
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException("Document does not exist");
        }

        // Get the current maximum version number
        Long currentVersionCount = documentVersionMapper.selectCount(
                new LambdaQueryWrapper<DocumentVersion>()
                        .eq(DocumentVersion::getDocumentId, documentId)
        );
        Integer currentVersion = currentVersionCount.intValue();

        // Get the previous version to compute the change size
        Long changeSize = 0L;
        DocumentVersion lastVersion = documentVersionMapper.selectOne(
                new LambdaQueryWrapper<DocumentVersion>()
                        .eq(DocumentVersion::getDocumentId, documentId)
                        .orderByDesc(DocumentVersion::getVersion)
                        .last("LIMIT 1")
        );

        // Get the current document content from MongoDB
        String currentContent = null;
        if (document.getContentId() != null) {
            var documentContent = documentContentService.getContentById(document.getContentId());
            if (documentContent != null) {
                currentContent = documentContent.getContent();
            }
        }

        if (lastVersion != null && lastVersion.getContent() != null) {
            int oldSize = lastVersion.getContent().length();
            int newSize = currentContent != null ? currentContent.length() : 0;
            changeSize = (long) newSize - oldSize;
        }

        // TODO: get operator information
        String operatorName = "System User";

        // Create the version record
        DocumentVersion version = new DocumentVersion();
        version.setId(SnowflakeIdGenerator.getInstance().nextId());
        version.setDocumentId(documentId);
        version.setVersion(currentVersion + 1);
        version.setTitle(document.getTitle());
        version.setContent(currentContent); // content fetched from MongoDB
        version.setSummary(document.getSummary());
        version.setChangeDescription(changeDescription);
        version.setChangeSize(changeSize);
        version.setOperatorId(userId);
        version.setOperatorName(operatorName);
        version.setCreatedAt(LocalDateTime.now());

        int count = documentVersionMapper.insert(version);
        return count > 0;
    }

    @Override
    public IPage<DocumentVersionVO> getVersionList(Long documentId, Long current, Long size) {
        if (documentId == null) {
            throw new BusinessException("Document ID must not be null");
        }

        // Check whether the document exists
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException("Document does not exist");
        }

        // Paginated query of the version list
        Page<DocumentVersion> page = new Page<>(current, size);
        IPage<DocumentVersion> versionPage = documentVersionMapper.selectPage(
                page,
                new LambdaQueryWrapper<DocumentVersion>()
                        .eq(DocumentVersion::getDocumentId, documentId)
                        .orderByDesc(DocumentVersion::getVersion)
        );

        // Convert to VO
        return versionPage.convert(this::convertToVO);
    }

    @Override
    public DocumentVersionVO getVersionDetail(Long versionId) {
        if (versionId == null) {
            throw new BusinessException("Version ID must not be null");
        }

        DocumentVersion version = documentVersionMapper.selectById(versionId);
        if (version == null) {
            throw new BusinessException("Version does not exist");
        }

        return convertToVO(version);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean restoreVersion(Long documentId, DocumentVersionRestoreDTO restoreDTO, Long userId) {
        log.info("Restore document version: documentId={}, versionId={}, userId={}", documentId, restoreDTO.getVersionId(), userId);

        if (documentId == null) {
            throw new BusinessException("Document ID must not be null");
        }

        // Check whether the document exists
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException("Document does not exist");
        }

        // Check whether the version exists
        DocumentVersion version = documentVersionMapper.selectById(restoreDTO.getVersionId());
        if (version == null) {
            throw new BusinessException("Version does not exist");
        }

        // Check whether the version belongs to this document
        if (!version.getDocumentId().equals(documentId)) {
            throw new BusinessException("This version does not belong to this document");
        }

        // Create a backup of the current version (before restoring)
        createVersion(documentId, "Automatic backup before restore", userId);

        // Restore the document content
        document.setTitle(version.getTitle());
        document.setSummary(version.getSummary());
        documentMapper.updateById(document);

        // Update the content in MongoDB
        if (version.getContent() != null) {
            documentContentService.updateContent(documentId, version.getContent());
        }

        return true;
    }

    @Override
    public String compareVersions(Long versionId1, Long versionId2) {
        if (versionId1 == null || versionId2 == null) {
            throw new BusinessException("Version ID must not be null");
        }

        if (versionId1.equals(versionId2)) {
            throw new BusinessException("Cannot compare the same version");
        }

        DocumentVersion version1 = documentVersionMapper.selectById(versionId1);
        DocumentVersion version2 = documentVersionMapper.selectById(versionId2);

        if (version1 == null || version2 == null) {
            throw new BusinessException("Version does not exist");
        }

        // Build the diff comparison result
        StringBuilder diff = new StringBuilder();
        diff.append("=== Version comparison ===\n");
        diff.append(String.format("Version %d vs Version %d\n", version1.getVersion(), version2.getVersion()));
        diff.append("\n");

        // Title differences
        if (!version1.getTitle().equals(version2.getTitle())) {
            diff.append("[Title difference]\n");
            diff.append(String.format("- Version %d: %s\n", version1.getVersion(), version1.getTitle()));
            diff.append(String.format("+ Version %d: %s\n", version2.getVersion(), version2.getTitle()));
            diff.append("\n");
        }

        // Content differences (simple implementation; a diff library could be used in practice)
        String content1 = version1.getContent() != null ? version1.getContent() : "";
        String content2 = version2.getContent() != null ? version2.getContent() : "";

        if (!content1.equals(content2)) {
            diff.append("[Content difference]\n");
            diff.append(String.format("Version %d content length: %d characters\n", version1.getVersion(), content1.length()));
            diff.append(String.format("Version %d content length: %d characters\n", version2.getVersion(), content2.length()));
            diff.append(String.format("Difference size: %d characters\n", content2.length() - content1.length()));
        }

        return diff.toString();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteVersion(Long versionId, Long userId) {
        log.info("Delete document version: versionId={}, userId={}", versionId, userId);

        if (versionId == null) {
            throw new BusinessException("Version ID must not be null");
        }

        // Check whether the version exists
        DocumentVersion version = documentVersionMapper.selectById(versionId);
        if (version == null) {
            throw new BusinessException("Version does not exist");
        }

        // Check whether this is the latest version
        Long count = documentVersionMapper.selectCount(
                new LambdaQueryWrapper<DocumentVersion>()
                        .eq(DocumentVersion::getDocumentId, version.getDocumentId())
                        .gt(DocumentVersion::getVersion, version.getVersion())
        );

        if (count > 0) {
            throw new BusinessException("Cannot delete an intermediate version, only the latest version can be deleted");
        }

        // Delete the version
        int deleteCount = documentVersionMapper.deleteById(versionId);
        return deleteCount > 0;
    }

    /**
     * Converts to VO
     *
     * @param version version entity
     * @return version VO
     */
    private DocumentVersionVO convertToVO(DocumentVersion version) {
        return DocumentVersionVO.builder()
                .id(version.getId())
                .documentId(version.getDocumentId())
                .version(version.getVersion())
                .title(version.getTitle())
                .changeDescription(version.getChangeDescription())
                .changeSize(version.getChangeSize())
                .operatorId(version.getOperatorId())
                .operatorName(version.getOperatorName())
                .createdAt(version.getCreatedAt())
                .build();
    }
}
