package com.ciff.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ciff.common.constant.ErrorCode;
import com.ciff.common.exception.BizException;
import com.ciff.knowledge.dto.DocumentVO;
import com.ciff.knowledge.entity.DocumentPO;
import com.ciff.knowledge.entity.KnowledgePO;
import com.ciff.knowledge.mapper.DocumentMapper;
import com.ciff.knowledge.mapper.KnowledgeMapper;
import com.ciff.knowledge.service.DocumentService;
import com.ciff.knowledge.service.FileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentMapper documentMapper;
    private final KnowledgeMapper knowledgeMapper;
    private final FileStorage fileStorage;
    private final DocumentProcessingService documentProcessingService;

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

        return toVO(doc);
    }

    @Override
    public List<DocumentVO> listByKnowledgeId(Long knowledgeId, Long userId) {
        requireKnowledgeExists(knowledgeId, userId);

        LambdaQueryWrapper<DocumentPO> wrapper = new LambdaQueryWrapper<DocumentPO>()
                .eq(DocumentPO::getKnowledgeId, knowledgeId)
                .orderByDesc(DocumentPO::getCreateTime);

        return documentMapper.selectList(wrapper).stream()
                .map(this::toVO)
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

        documentMapper.deleteById(documentId);
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
        DocumentVO vo = new DocumentVO();
        vo.setId(po.getId());
        vo.setKnowledgeId(po.getKnowledgeId());
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
