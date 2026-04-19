package com.ciff.knowledge.service;

import com.ciff.common.dto.PageResult;
import com.ciff.knowledge.dto.DocumentVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentService {

    DocumentVO upload(Long knowledgeId, MultipartFile file, Long userId);

    List<DocumentVO> listByKnowledgeId(Long knowledgeId, Long userId);

    void delete(Long documentId, Long userId);

    DocumentVO updateFileName(Long documentId, String fileName, Long userId);

    PageResult<DocumentVO> pageAll(Integer page, Integer pageSize, Long knowledgeId, String fileName, Long userId);
}
