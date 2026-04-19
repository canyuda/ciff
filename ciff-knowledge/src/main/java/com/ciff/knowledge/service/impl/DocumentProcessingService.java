package com.ciff.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ciff.knowledge.config.ChunkProperties;
import com.ciff.knowledge.entity.DocumentPO;
import com.ciff.knowledge.entity.KnowledgeChunkPO;
import com.ciff.knowledge.entity.KnowledgePO;
import com.ciff.knowledge.mapper.DocumentMapper;
import com.ciff.knowledge.mapper.KnowledgeMapper;
import com.ciff.knowledge.service.EmbeddingService;
import com.ciff.knowledge.service.FileStorage;
import com.ciff.knowledge.service.KnowledgeChunkService;
import com.ciff.knowledge.service.TxtChunker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentProcessingService {

    private final DocumentMapper documentMapper;
    private final KnowledgeMapper knowledgeMapper;
    private final EmbeddingService embeddingService;
    private final KnowledgeChunkService knowledgeChunkService;
    private final FileStorage fileStorage;
    private final ChunkProperties chunkProperties;

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
            KnowledgePO knowledge = knowledgeMapper.selectById(doc.getKnowledgeId());
            int chunkSize = knowledge != null ? knowledge.getChunkSize() : chunkProperties.getSize();
            chunkAndEmbed(doc, chunkSize, chunkProperties.getOverlap());
            log.info("Document {} processing complete", documentId);
        } catch (Exception e) {
            log.error("Document processing failed: {}", documentId, e);
            doc.setStatus("failed");
            documentMapper.updateById(doc);
        }
    }

    /**
     * Rebuild vectors for documents under a knowledge base.
     *
     * @param knowledgeId knowledge base ID (required)
     * @param documentId  specific document ID (optional, rebuild all if null)
     * @param chunkSize   chunk size (optional, use knowledge base default if null)
     * @param overlap     overlap chars (optional, use TxtChunker default if null)
     */
    public void rebuildVectors(Long knowledgeId, Long documentId, Integer chunkSize, Integer overlap) {
        KnowledgePO knowledge = knowledgeMapper.selectById(knowledgeId);
        if (knowledge == null) {
            throw new IllegalArgumentException("Knowledge not found: " + knowledgeId);
        }

        int effectiveChunkSize = chunkSize != null ? chunkSize : (knowledge.getChunkSize() != null ? knowledge.getChunkSize() : chunkProperties.getSize());
        int effectiveOverlap = overlap != null ? overlap : chunkProperties.getOverlap();

        List<DocumentPO> docs;
        if (documentId != null) {
            DocumentPO doc = documentMapper.selectById(documentId);
            if (doc == null || !doc.getKnowledgeId().equals(knowledgeId)) {
                throw new IllegalArgumentException("Document not found or not in this knowledge base: " + documentId);
            }
            docs = List.of(doc);
        } else {
            docs = documentMapper.selectList(
                    new LambdaQueryWrapper<DocumentPO>()
                            .eq(DocumentPO::getKnowledgeId, knowledgeId)
                            .eq(DocumentPO::getDeleted, 0));
        }

        if (docs.isEmpty()) {
            log.info("No documents to rebuild for knowledge {}", knowledgeId);
            return;
        }

        log.info("Rebuilding vectors for knowledge {}, {} documents, chunkSize={}, overlap={}",
                knowledgeId, docs.size(), effectiveChunkSize, effectiveOverlap);

        int successCount = 0;
        for (DocumentPO doc : docs) {
            try {
                rebuildSingleDocument(doc, effectiveChunkSize, effectiveOverlap);
                successCount++;
            } catch (Exception e) {
                log.error("Rebuild failed for document {}", doc.getId(), e);
            }
        }

        log.info("Rebuild complete for knowledge {}: {}/{} documents succeeded",
                knowledgeId, successCount, docs.size());
    }

    private void rebuildSingleDocument(DocumentPO doc, int chunkSize, int overlap) {
        // 1. Delete old chunks
        knowledgeChunkService.deleteByDocumentId(doc.getId());
        log.info("Deleted old chunks for document {}", doc.getId());

        // 2. Chunk + embed + store
        chunkAndEmbed(doc, chunkSize, overlap);
    }

    private void chunkAndEmbed(DocumentPO doc, int chunkSize, int overlap) {
        String content = readFileContent(doc.getFilePath());
        if (content == null || content.isBlank()) {
            doc.setStatus("failed");
            documentMapper.updateById(doc);
            log.warn("Document {} content is empty", doc.getId());
            return;
        }

        List<String> chunks = TxtChunker.chunk(content, chunkSize, overlap);

        if (chunks.isEmpty()) {
            doc.setStatus("failed");
            documentMapper.updateById(doc);
            log.warn("Document {} chunking produced no chunks", doc.getId());
            return;
        }

        // Embed
        log.info("Document {} generating embeddings for {} chunks", doc.getId(), chunks.size());
        List<float[]> embeddings = embeddingService.embed(chunks);

        // Store
        List<KnowledgeChunkPO> chunkPOs = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            KnowledgeChunkPO chunkPO = new KnowledgeChunkPO();
            chunkPO.setDocumentId(doc.getId());
            chunkPO.setKnowledgeId(doc.getKnowledgeId());
            chunkPO.setContent(chunks.get(i));
            chunkPO.setEmbedding(embeddings.get(i));
            chunkPO.setChunkIndex(i);
            chunkPOs.add(chunkPO);
        }
        knowledgeChunkService.batchInsert(chunkPOs);

        doc.setChunkCount(chunks.size());
        doc.setStatus("ready");
        documentMapper.updateById(doc);
        log.info("Document {} stored {} chunks", doc.getId(), chunks.size());
    }

    private String readFileContent(String filePath) {
        if (filePath == null) {
            return null;
        }
        try (InputStream is = fileStorage.load(filePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            log.error("Failed to read file: {}", filePath, e);
            return null;
        }
    }
}
