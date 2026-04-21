package com.ciff.knowledge.service;

import com.ciff.knowledge.entity.KnowledgeChunkPO;
import com.ciff.knowledge.service.impl.KnowledgeChunkServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VectorStoreTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private KnowledgeChunkServiceImpl knowledgeChunkService;

    @Test
    void search_withKnowledgeId_shouldReturnOrderedResults() {
        float[] embedding = new float[]{0.1f, 0.2f, 0.3f};
        KnowledgeChunkPO chunk = buildChunk(1L, 1L, 1L, "content", 0.85);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(), any(), any(), any()))
                .thenReturn(List.of(chunk));

        List<KnowledgeChunkPO> results = knowledgeChunkService.search(embedding, 1L, 5);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getContent()).isEqualTo("content");
        assertThat(results.get(0).getSimilarity()).isEqualTo(0.85);
    }

    @Test
    void search_withoutKnowledgeId_shouldReturnResults() {
        float[] embedding = new float[]{0.1f, 0.2f, 0.3f};
        KnowledgeChunkPO chunk = buildChunk(1L, 1L, 1L, "content", 0.90);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(), any(), any()))
                .thenReturn(List.of(chunk));

        List<KnowledgeChunkPO> results = knowledgeChunkService.search(embedding, (Long) null, 5);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getSimilarity()).isEqualTo(0.90);
    }

    @Test
    void search_withMultipleKnowledgeIds_shouldFilter() {
        float[] embedding = new float[]{0.1f, 0.2f, 0.3f};
        KnowledgeChunkPO chunk = buildChunk(1L, 1L, 1L, "content", 0.80);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(), any(), any()))
                .thenReturn(List.of(chunk));

        List<KnowledgeChunkPO> results = knowledgeChunkService.search(embedding, List.of(1L, 2L), 5);

        assertThat(results).hasSize(1);
    }

    @Test
    void search_withEmptyKnowledgeIds_shouldReturnEmpty() {
        float[] embedding = new float[]{0.1f, 0.2f, 0.3f};

        List<KnowledgeChunkPO> results = knowledgeChunkService.search(embedding, List.of(), 5);

        assertThat(results).isEmpty();
    }

    @Test
    void search_withNullKnowledgeIds_shouldReturnEmpty() {
        float[] embedding = new float[]{0.1f, 0.2f, 0.3f};

        List<KnowledgeChunkPO> results = knowledgeChunkService.search(embedding, (List<Long>) null, 5);

        assertThat(results).isEmpty();
    }

    @Test
    void batchInsert_shouldExecuteBatchUpdate() {
        KnowledgeChunkPO chunk1 = buildChunk(null, 1L, 1L, "chunk1", null);
        KnowledgeChunkPO chunk2 = buildChunk(null, 1L, 1L, "chunk2", null);

        when(jdbcTemplate.batchUpdate(anyString(), any(List.class), any(int.class), any()))
                .thenReturn(new int[][]{{1, 1}});

        knowledgeChunkService.batchInsert(List.of(chunk1, chunk2));

        verify(jdbcTemplate).batchUpdate(anyString(), eq(List.of(chunk1, chunk2)), eq(2), any());
    }

    @Test
    void batchInsert_withEmptyList_shouldDoNothing() {
        knowledgeChunkService.batchInsert(List.of());
        verify(jdbcTemplate, org.mockito.Mockito.never()).batchUpdate(anyString(), any(List.class), any(int.class), any());
    }

    @Test
    void batchInsert_withNull_shouldDoNothing() {
        knowledgeChunkService.batchInsert(null);
        verify(jdbcTemplate, org.mockito.Mockito.never()).batchUpdate(anyString(), any(List.class), any(int.class), any());
    }

    @Test
    void getById_shouldReturnChunk() {
        KnowledgeChunkPO chunk = buildChunk(1L, 1L, 1L, "content", null);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(1L)))
                .thenReturn(List.of(chunk));

        KnowledgeChunkPO result = knowledgeChunkService.getById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getById_whenNotFound_shouldReturnNull() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(999L)))
                .thenReturn(List.of());

        KnowledgeChunkPO result = knowledgeChunkService.getById(999L);

        assertThat(result).isNull();
    }

    @Test
    void listByDocumentId_shouldReturnChunks() {
        KnowledgeChunkPO chunk1 = buildChunk(1L, 1L, 1L, "chunk1", null);
        KnowledgeChunkPO chunk2 = buildChunk(2L, 1L, 1L, "chunk2", null);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(1L)))
                .thenReturn(List.of(chunk1, chunk2));

        List<KnowledgeChunkPO> results = knowledgeChunkService.listByDocumentId(1L);

        assertThat(results).hasSize(2);
    }

    @Test
    void listByKnowledgeId_shouldReturnChunks() {
        KnowledgeChunkPO chunk = buildChunk(1L, 1L, 1L, "chunk", null);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(1L)))
                .thenReturn(List.of(chunk));

        List<KnowledgeChunkPO> results = knowledgeChunkService.listByKnowledgeId(1L);

        assertThat(results).hasSize(1);
    }

    @Test
    void deleteById_shouldExecuteDelete() {
        when(jdbcTemplate.update(anyString(), eq(1L))).thenReturn(1);

        knowledgeChunkService.deleteById(1L);

        verify(jdbcTemplate).update(anyString(), eq(1L));
    }

    @Test
    void deleteByDocumentId_shouldExecuteDelete() {
        when(jdbcTemplate.update(anyString(), eq(1L))).thenReturn(1);

        knowledgeChunkService.deleteByDocumentId(1L);

        verify(jdbcTemplate).update(anyString(), eq(1L));
    }

    @Test
    void deleteByKnowledgeId_shouldExecuteDelete() {
        when(jdbcTemplate.update(anyString(), eq(1L))).thenReturn(1);

        knowledgeChunkService.deleteByKnowledgeId(1L);

        verify(jdbcTemplate).update(anyString(), eq(1L));
    }

    // --- Helper ---

    private KnowledgeChunkPO buildChunk(Long id, Long documentId, Long knowledgeId, String content, Double similarity) {
        KnowledgeChunkPO chunk = new KnowledgeChunkPO();
        chunk.setId(id);
        chunk.setDocumentId(documentId);
        chunk.setKnowledgeId(knowledgeId);
        chunk.setContent(content);
        chunk.setChunkIndex(0);
        chunk.setSimilarity(similarity);
        chunk.setCreateTime(LocalDateTime.now());
        return chunk;
    }
}
