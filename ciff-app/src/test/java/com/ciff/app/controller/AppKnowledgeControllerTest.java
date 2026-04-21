package com.ciff.app.controller;

import com.ciff.common.context.UserContext;
import com.ciff.common.dto.PageResult;
import com.ciff.knowledge.dto.DocumentVO;
import com.ciff.knowledge.dto.KnowledgeVO;
import com.ciff.knowledge.dto.SearchResultVO;
import com.ciff.knowledge.entity.DocumentPO;
import com.ciff.knowledge.entity.KnowledgeChunkPO;
import com.ciff.knowledge.entity.KnowledgePO;
import com.ciff.knowledge.mapper.DocumentMapper;
import com.ciff.knowledge.mapper.KnowledgeMapper;
import com.ciff.knowledge.service.DocumentService;
import com.ciff.knowledge.service.EmbeddingService;
import com.ciff.knowledge.service.KnowledgeChunkService;
import com.ciff.knowledge.service.KnowledgeService;
import com.ciff.knowledge.service.RerankService;
import com.ciff.knowledge.service.SearchFilterService;
import com.ciff.knowledge.service.impl.DocumentProcessingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AppKnowledgeController.class)
class AppKnowledgeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KnowledgeService knowledgeService;

    @MockBean
    private DocumentService documentService;

    @MockBean
    private DocumentProcessingService documentProcessingService;

    @MockBean
    private EmbeddingService embeddingService;

    @MockBean
    private KnowledgeChunkService knowledgeChunkService;

    @MockBean
    private RerankService rerankService;

    @MockBean
    private SearchFilterService searchFilterService;

    @MockBean
    private KnowledgeMapper knowledgeMapper;

    @MockBean
    private DocumentMapper documentMapper;

    @BeforeEach
    void setUp() {
        UserContext.setUserId(1L);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void create_whenValid_shouldReturnOk() throws Exception {
        String body = """
                {
                    "name": "test-kb",
                    "embeddingModel": "text-embedding-v3",
                    "chunkSize": 700
                }
                """;

        KnowledgeVO vo = buildKnowledgeVO(1L, "test-kb", "text-embedding-v3");
        given(knowledgeService.create(any(), eq(1L))).willReturn(vo);

        mockMvc.perform(post("/api/v1/app/knowledge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("test-kb"))
                .andExpect(jsonPath("$.data.embeddingModel").value("text-embedding-v3"));
    }

    @Test
    void create_whenNameBlank_shouldReturn400() throws Exception {
        String body = """
                {
                    "name": "",
                    "embeddingModel": "text-embedding-v3"
                }
                """;

        mockMvc.perform(post("/api/v1/app/knowledge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_whenEmbeddingModelBlank_shouldReturn400() throws Exception {
        String body = """
                {
                    "name": "test-kb"
                }
                """;

        mockMvc.perform(post("/api/v1/app/knowledge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_whenValid_shouldReturnOk() throws Exception {
        String body = """
                {
                    "name": "updated-kb",
                    "chunkSize": 512
                }
                """;

        KnowledgeVO vo = buildKnowledgeVO(1L, "updated-kb", "text-embedding-v3");
        vo.setChunkSize(512);
        given(knowledgeService.update(eq(1L), any(), eq(1L))).willReturn(vo);

        mockMvc.perform(put("/api/v1/app/knowledge/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("updated-kb"))
                .andExpect(jsonPath("$.data.chunkSize").value(512));
    }

    @Test
    void getById_shouldReturnKnowledge() throws Exception {
        KnowledgeVO vo = buildKnowledgeVO(1L, "test-kb", "text-embedding-v3");
        given(knowledgeService.getById(1L, 1L)).willReturn(vo);

        mockMvc.perform(get("/api/v1/app/knowledge/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("test-kb"));
    }

    @Test
    void delete_shouldReturnOk() throws Exception {
        doNothing().when(knowledgeService).delete(1L, 1L);

        mockMvc.perform(delete("/api/v1/app/knowledge/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void page_shouldReturnPageResult() throws Exception {
        KnowledgeVO vo = buildKnowledgeVO(1L, "kb1", "text-embedding-v3");
        PageResult<KnowledgeVO> pageResult = new PageResult<>(List.of(vo), 1L, 1, 20);
        given(knowledgeService.page(any(), any(), any(), eq(1L))).willReturn(pageResult);

        mockMvc.perform(get("/api/v1/app/knowledge")
                        .param("page", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void page_withStatusFilter_shouldReturnOk() throws Exception {
        KnowledgeVO vo = buildKnowledgeVO(1L, "kb1", "text-embedding-v3");
        vo.setStatus("active");
        PageResult<KnowledgeVO> pageResult = new PageResult<>(List.of(vo), 1L, 1, 20);
        given(knowledgeService.page(any(), any(), eq("active"), eq(1L))).willReturn(pageResult);

        mockMvc.perform(get("/api/v1/app/knowledge")
                        .param("page", "1")
                        .param("pageSize", "20")
                        .param("status", "active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list[0].status").value("active"));
    }

    @Test
    void uploadDocument_shouldReturnDocumentVO() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "test content".getBytes());

        DocumentVO vo = buildDocumentVO(1L, "test.txt");
        given(documentService.upload(eq(1L), any(), eq(1L))).willReturn(vo);

        mockMvc.perform(multipart("/api/v1/app/knowledge/1/documents")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileName").value("test.txt"));
    }

    @Test
    void listDocuments_shouldReturnList() throws Exception {
        DocumentVO vo = buildDocumentVO(1L, "test.txt");
        given(documentService.listByKnowledgeId(1L, 1L)).willReturn(List.of(vo));

        mockMvc.perform(get("/api/v1/app/knowledge/1/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].fileName").value("test.txt"));
    }

    @Test
    void deleteDocument_shouldReturnOk() throws Exception {
        doNothing().when(documentService).delete(1L, 1L);

        mockMvc.perform(delete("/api/v1/app/knowledge/documents/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void search_withValidQuery_shouldReturnResults() throws Exception {
        float[] embedding = new float[1024];
        given(embeddingService.embed(List.of("test query"))).willReturn(List.of(embedding));

        KnowledgeChunkPO chunk = new KnowledgeChunkPO();
        chunk.setId(1L);
        chunk.setDocumentId(1L);
        chunk.setKnowledgeId(1L);
        chunk.setContent("test content");
        chunk.setSimilarity(0.85);
        given(knowledgeChunkService.search(embedding, List.of(1L), 20))
                .willReturn(List.of(chunk));

        KnowledgePO knowledge = new KnowledgePO();
        knowledge.setId(1L);
        knowledge.setName("test-kb");
        knowledge.setEmbeddingModel("text-embedding-v3");
        given(knowledgeMapper.selectBatchIds(any())).willReturn(List.of(knowledge));

        DocumentPO document = new DocumentPO();
        document.setId(1L);
        document.setFileName("test.txt");
        given(documentMapper.selectBatchIds(any())).willReturn(List.of(document));

        mockMvc.perform(get("/api/v1/app/knowledge/search")
                        .param("query", "test query")
                        .param("knowledgeIds", "1")
                        .param("enableRerank", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].content").value("test content"));
    }

    @Test
    void search_withEmptyQuery_shouldReturnEmpty() throws Exception {
        mockMvc.perform(get("/api/v1/app/knowledge/search")
                        .param("query", "")
                        .param("knowledgeIds", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void rebuildVectors_shouldReturnOk() throws Exception {
        doNothing().when(documentProcessingService).rebuildVectors(1L, null, null, null);

        mockMvc.perform(post("/api/v1/app/knowledge/1/rebuild"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // --- Helpers ---

    private KnowledgeVO buildKnowledgeVO(Long id, String name, String embeddingModel) {
        KnowledgeVO vo = new KnowledgeVO();
        vo.setId(id);
        vo.setName(name);
        vo.setDescription("test description");
        vo.setChunkSize(700);
        vo.setEmbeddingModel(embeddingModel);
        vo.setStatus("active");
        vo.setDocumentCount(0);
        return vo;
    }

    private DocumentVO buildDocumentVO(Long id, String fileName) {
        DocumentVO vo = new DocumentVO();
        vo.setId(id);
        vo.setKnowledgeId(1L);
        vo.setFileName(fileName);
        vo.setFileSize(1024L);
        vo.setChunkCount(5);
        vo.setStatus("ready");
        return vo;
    }
}
