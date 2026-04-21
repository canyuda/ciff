package com.ciff.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ciff.common.constant.ErrorCode;
import com.ciff.common.dto.PageResult;
import com.ciff.common.exception.BizException;
import com.ciff.common.util.PageHelper;
import com.ciff.knowledge.dto.DocumentVO;
import com.ciff.knowledge.entity.DocumentPO;
import com.ciff.knowledge.entity.KnowledgePO;
import com.ciff.knowledge.mapper.DocumentMapper;
import com.ciff.knowledge.mapper.KnowledgeMapper;
import com.ciff.knowledge.service.DocumentService;
import com.ciff.knowledge.service.FileStorage;
import com.ciff.knowledge.service.KnowledgeChunkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentMapper documentMapper;
    private final KnowledgeMapper knowledgeMapper;
    private final FileStorage fileStorage;
    private final DocumentProcessingService documentProcessingService;
    private final KnowledgeChunkService knowledgeChunkService;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    @Override
    public DocumentVO upload(Long knowledgeId, MultipartFile file, Long userId) {
        validateFile(file);
        requireKnowledgeExists(knowledgeId, userId);

        String originalFilename = file.getOriginalFilename();

        // Insert document record
        DocumentPO doc = new DocumentPO();
        doc.setKnowledgeId(knowledgeId);
        doc.setFileName(originalFilename);
        doc.setFileSize(file.getSize());
        doc.setChunkCount(0);
        doc.setStatus("uploading");
        documentMapper.insert(doc);

        // Save file to storage
        String storedName = doc.getId() + "_" + originalFilename;
        String category = "knowledge/" + knowledgeId;
        try {
            String filePath = fileStorage.store(category, storedName, file.getInputStream(), file.getSize());
            doc.setFilePath(filePath);
        } catch (IOException e) {
            log.error("Failed to read uploaded file input stream", e);
            documentMapper.deleteById(doc.getId());
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Failed to read uploaded file");
        } catch (Exception e) {
            log.error("Failed to store file", e);
            documentMapper.deleteById(doc.getId());
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Failed to save file");
        }

        doc.setStatus("uploaded");
        documentMapper.updateById(doc);

        // Trigger async processing
        documentProcessingService.process(doc.getId());

        KnowledgePO knowledge = knowledgeMapper.selectById(knowledgeId);
        return toVO(doc, knowledge != null ? knowledge.getName() : null);
    }

    @Override
    public List<DocumentVO> listByKnowledgeId(Long knowledgeId, Long userId) {
        requireKnowledgeExists(knowledgeId, userId);
        KnowledgePO knowledge = knowledgeMapper.selectById(knowledgeId);

        LambdaQueryWrapper<DocumentPO> wrapper = new LambdaQueryWrapper<DocumentPO>()
                .eq(DocumentPO::getKnowledgeId, knowledgeId)
                .orderByDesc(DocumentPO::getCreateTime);

        return documentMapper.selectList(wrapper).stream()
                .map(doc -> toVO(doc, knowledge != null ? knowledge.getName() : null))
                .toList();
    }

    @Override
    public void delete(Long documentId, Long userId) {
        DocumentPO doc = documentMapper.selectById(documentId);
        if (doc == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "文档不存在: " + documentId);
        }

        requireKnowledgeExists(doc.getKnowledgeId(), userId);

        if (doc.getFilePath() != null) {
            fileStorage.delete(doc.getFilePath());
        }

        knowledgeChunkService.deleteByDocumentId(documentId);
        documentMapper.deleteById(documentId);
    }

    @Override
    public DocumentVO updateFileName(Long documentId, String fileName, Long userId) {
        if (fileName == null || fileName.isBlank()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "文件名不能为空");
        }

        DocumentPO doc = documentMapper.selectById(documentId);
        if (doc == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "文档不存在: " + documentId);
        }

        requireKnowledgeExists(doc.getKnowledgeId(), userId);

        doc.setFileName(fileName);
        documentMapper.updateById(doc);

        KnowledgePO knowledge = knowledgeMapper.selectById(doc.getKnowledgeId());
        return toVO(doc, knowledge != null ? knowledge.getName() : null);
    }

    @Override
    public PageResult<DocumentVO> pageAll(Integer page, Integer pageSize, Long knowledgeId, String fileName, Long userId) {
        // Get knowledge IDs that the user has access to
        List<Long> allowedKnowledgeIds = knowledgeMapper.selectList(
                        new LambdaQueryWrapper<KnowledgePO>()
                                .eq(userId != null, KnowledgePO::getUserId, userId))
                .stream().map(KnowledgePO::getId).toList();

        if (allowedKnowledgeIds.isEmpty()) {
            return PageResult.of(Collections.emptyList(), 0, page != null ? page : 1, pageSize != null ? pageSize : 10);
        }

        List<Long> targetKnowledgeIds;
        if (knowledgeId != null) {
            if (!allowedKnowledgeIds.contains(knowledgeId)) {
                return PageResult.of(Collections.emptyList(), 0, page != null ? page : 1, pageSize != null ? pageSize : 10);
            }
            targetKnowledgeIds = List.of(knowledgeId);
        } else {
            targetKnowledgeIds = allowedKnowledgeIds;
        }

        Page<DocumentPO> pageParam = PageHelper.toPage(page, pageSize);
        LambdaQueryWrapper<DocumentPO> wrapper = new LambdaQueryWrapper<DocumentPO>()
                .in(DocumentPO::getKnowledgeId, targetKnowledgeIds);
        if (fileName != null && !fileName.isBlank()) {
            wrapper.like(DocumentPO::getFileName, fileName);
        }
        wrapper.orderByDesc(DocumentPO::getCreateTime);

        Page<DocumentPO> result = documentMapper.selectPage(pageParam, wrapper);

        if (result.getRecords().isEmpty()) {
            return PageResult.of(Collections.emptyList(), 0, (int) result.getCurrent(), (int) result.getSize());
        }

        // Batch query knowledge names
        Set<Long> docKnowledgeIds = result.getRecords().stream()
                .map(DocumentPO::getKnowledgeId)
                .collect(Collectors.toSet());
        Map<Long, String> knowledgeNameMap = knowledgeMapper.selectBatchIds(docKnowledgeIds).stream()
                .collect(Collectors.toMap(KnowledgePO::getId, KnowledgePO::getName, (a, b) -> a));

        List<DocumentVO> records = result.getRecords().stream()
                .map(doc -> toVO(doc, knowledgeNameMap.get(doc.getKnowledgeId())))
                .toList();
        return PageResult.of(records, result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    @Override
    public Map<Long, String> getDocumentNamesByIds(List<Long> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<DocumentPO> docs = documentMapper.selectBatchIds(documentIds);
        return docs.stream()
                .collect(Collectors.toMap(DocumentPO::getId, DocumentPO::getFileName, (a, b) -> a));
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".txt")) {
            throw new BizException(ErrorCode.BAD_REQUEST, "仅支持 .txt 文件");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BizException(ErrorCode.BAD_REQUEST,
                    "文件大小不能超过 5MB，当前: " + formatSize(file.getSize()));
        }
    }

    private KnowledgePO requireKnowledgeExists(Long knowledgeId, Long userId) {
        KnowledgePO po = knowledgeMapper.selectById(knowledgeId);
        if (po == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "知识库不存在: " + knowledgeId);
        }
        if (userId != null && !userId.equals(po.getUserId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "知识库不存在: " + knowledgeId);
        }
        return po;
    }

    private DocumentVO toVO(DocumentPO po) {
        return toVO(po, null);
    }

    private DocumentVO toVO(DocumentPO po, String knowledgeName) {
        DocumentVO vo = new DocumentVO();
        vo.setId(po.getId());
        vo.setKnowledgeId(po.getKnowledgeId());
        vo.setKnowledgeName(knowledgeName);
        vo.setFileName(po.getFileName());
        vo.setFileSize(po.getFileSize());
        vo.setChunkCount(po.getChunkCount());
        vo.setStatus(po.getStatus());
        vo.setCreateTime(po.getCreateTime());
        vo.setUpdateTime(po.getUpdateTime());
        return vo;
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + "B";
        if (bytes < 1024 * 1024) return String.format("%.1fKB", bytes / 1024.0);
        return String.format("%.1fMB", bytes / (1024.0 * 1024));
    }
}
