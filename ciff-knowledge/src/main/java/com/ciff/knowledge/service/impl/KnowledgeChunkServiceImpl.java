package com.ciff.knowledge.service.impl;

import com.ciff.knowledge.entity.KnowledgeChunkPO;
import com.ciff.knowledge.service.KnowledgeChunkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeChunkServiceImpl implements KnowledgeChunkService {

    @Qualifier("pgVectorJdbcTemplate")
    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<KnowledgeChunkPO> ROW_MAPPER = (rs, rowNum) -> {
        KnowledgeChunkPO chunk = new KnowledgeChunkPO();
        chunk.setId(rs.getLong("id"));
        chunk.setDocumentId(rs.getLong("document_id"));
        chunk.setKnowledgeId(rs.getLong("knowledge_id"));
        chunk.setContent(rs.getString("content"));
        chunk.setEmbedding(parseVector(rs.getString("embedding")));
        chunk.setChunkIndex(rs.getInt("chunk_index"));
        Timestamp ts = rs.getTimestamp("create_time");
        chunk.setCreateTime(ts != null ? ts.toLocalDateTime() : null);
        return chunk;
    };

    private static final RowMapper<KnowledgeChunkPO> SEARCH_ROW_MAPPER = (rs, rowNum) -> {
        KnowledgeChunkPO chunk = new KnowledgeChunkPO();
        chunk.setId(rs.getLong("id"));
        chunk.setDocumentId(rs.getLong("document_id"));
        chunk.setKnowledgeId(rs.getLong("knowledge_id"));
        chunk.setContent(rs.getString("content"));
        chunk.setChunkIndex(rs.getInt("chunk_index"));
        chunk.setSimilarity(rs.getDouble("similarity"));
        Timestamp ts = rs.getTimestamp("create_time");
        chunk.setCreateTime(ts != null ? ts.toLocalDateTime() : null);
        return chunk;
    };

    @Override
    public KnowledgeChunkPO getById(Long id) {
        List<KnowledgeChunkPO> list = jdbcTemplate.query(
                "SELECT id, document_id, knowledge_id, content, embedding, chunk_index, create_time " +
                        "FROM t_knowledge_chunk WHERE id = ?",
                ROW_MAPPER, id);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public List<KnowledgeChunkPO> listByDocumentId(Long documentId) {
        return jdbcTemplate.query(
                "SELECT id, document_id, knowledge_id, content, embedding, chunk_index, create_time " +
                        "FROM t_knowledge_chunk WHERE document_id = ? ORDER BY chunk_index",
                ROW_MAPPER, documentId);
    }

    @Override
    public List<KnowledgeChunkPO> listByKnowledgeId(Long knowledgeId) {
        return jdbcTemplate.query(
                "SELECT id, document_id, knowledge_id, content, embedding, chunk_index, create_time " +
                        "FROM t_knowledge_chunk WHERE knowledge_id = ? ORDER BY document_id, chunk_index",
                ROW_MAPPER, knowledgeId);
    }

    @Override
    public void batchInsert(List<KnowledgeChunkPO> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }
        String sql = "INSERT INTO t_knowledge_chunk (document_id, knowledge_id, content, embedding, chunk_index) " +
                "VALUES (?, ?, ?, ?::vector, ?)";
        jdbcTemplate.batchUpdate(sql, chunks, chunks.size(),
                (ps, chunk) -> {
                    ps.setLong(1, chunk.getDocumentId());
                    ps.setLong(2, chunk.getKnowledgeId());
                    ps.setString(3, chunk.getContent());
                    ps.setString(4, toVectorString(chunk.getEmbedding()));
                    ps.setInt(5, chunk.getChunkIndex());
                });
    }

    @Override
    public void deleteById(Long id) {
        jdbcTemplate.update("DELETE FROM t_knowledge_chunk WHERE id = ?", id);
    }

    @Override
    public void deleteByDocumentId(Long documentId) {
        jdbcTemplate.update("DELETE FROM t_knowledge_chunk WHERE document_id = ?", documentId);
    }

    @Override
    public void deleteByKnowledgeId(Long knowledgeId) {
        jdbcTemplate.update("DELETE FROM t_knowledge_chunk WHERE knowledge_id = ?", knowledgeId);
    }

    @Override
    public List<KnowledgeChunkPO> search(float[] embedding, Long knowledgeId, int limit) {
        String vectorStr = toVectorString(embedding);
        if (knowledgeId != null) {
            return jdbcTemplate.query(
                    "SELECT id, document_id, knowledge_id, content, chunk_index, create_time, " +
                            "1 - (embedding <=> ?::vector) AS similarity " +
                            "FROM t_knowledge_chunk WHERE knowledge_id = ? " +
                            "ORDER BY embedding <=> ?::vector LIMIT ?",
                    SEARCH_ROW_MAPPER, vectorStr, knowledgeId, vectorStr, limit);
        }
        return jdbcTemplate.query(
                "SELECT id, document_id, knowledge_id, content, chunk_index, create_time, " +
                        "1 - (embedding <=> ?::vector) AS similarity " +
                        "FROM t_knowledge_chunk " +
                        "ORDER BY embedding <=> ?::vector LIMIT ?",
                SEARCH_ROW_MAPPER, vectorStr, vectorStr, limit);
    }

    // ---- vector helpers ----

    static String toVectorString(float[] embedding) {
        if (embedding == null) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(embedding[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    static float[] parseVector(String vectorStr) {
        if (vectorStr == null || vectorStr.isEmpty()
                || "[]".equals(vectorStr) || "null".equalsIgnoreCase(vectorStr)) {
            return null;
        }
        String stripped = vectorStr.substring(1, vectorStr.length() - 1);
        String[] parts = stripped.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }
}
