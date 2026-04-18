package com.ciff.knowledge.service.impl;

import com.ciff.knowledge.entity.DocumentPO;
import com.ciff.knowledge.entity.KnowledgePO;
import com.ciff.knowledge.mapper.DocumentMapper;
import com.ciff.knowledge.mapper.KnowledgeMapper;
import com.ciff.knowledge.service.TxtChunker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentProcessingService {

    private final DocumentMapper documentMapper;
    private final KnowledgeMapper knowledgeMapper;

    @Async("docProcessExecutor")
    public void process(Long documentId) {
        DocumentPO doc = documentMapper.selectById(documentId);
        if (doc == null) {
            log.warn("Document not found, skip processing: {}", documentId);
            return;
        }

        if (!"uploaded".equals(doc.getStatus()) && !"processing".equals(doc.getStatus())) {
            log.info("Document {} status is {}, skip processing", documentId, doc.getStatus());
            return;
        }

        doc.setStatus("processing");
        documentMapper.updateById(doc);

        try {
            // 1. Read file content
            String content = readFileContent(doc.getFilePath());
            if (content == null || content.isBlank()) {
                doc.setStatus("failed");
                documentMapper.updateById(doc);
                log.warn("Document {} content is empty", documentId);
                return;
            }

            // 2. Chunk
            KnowledgePO knowledge = knowledgeMapper.selectById(doc.getKnowledgeId());
            int chunkSize = knowledge != null ? knowledge.getChunkSize() : 500;
            List<String> chunks = TxtChunker.chunk(content, chunkSize);

            if (chunks.isEmpty()) {
                doc.setStatus("failed");
                documentMapper.updateById(doc);
                log.warn("Document {} chunking produced no chunks", documentId);
                return;
            }

            doc.setChunkCount(chunks.size());
            documentMapper.updateById(doc);

            // TODO: 3. Generate embeddings via text-embedding-v3
            log.info("Document {} generated {} chunks, embedding generation pending", documentId, chunks.size());

            // TODO: 4. Store vectors in PGVector

            doc.setStatus("ready");
            documentMapper.updateById(doc);
            log.info("Document {} processing complete, {} chunks", documentId, chunks.size());

        } catch (Exception e) {
            log.error("Document processing failed: {}", documentId, e);
            doc.setStatus("failed");
            documentMapper.updateById(doc);
        }
    }

    private String readFileContent(String filePath) {
        if (filePath == null) {
            return null;
        }
        try {
            return Files.readString(Path.of(filePath), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to read file: {}", filePath, e);
            return null;
        }
    }
}
