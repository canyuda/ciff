package com.ciff.app.controller;

import com.ciff.common.context.UserContext;
import com.ciff.common.dto.PageResult;
import com.ciff.common.dto.Result;
import com.ciff.knowledge.dto.DocumentVO;
import com.ciff.knowledge.dto.KnowledgeCreateRequest;
import com.ciff.knowledge.dto.KnowledgeUpdateRequest;
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
import com.ciff.knowledge.service.TxtChunker;
import com.ciff.knowledge.service.impl.DocumentProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/app/knowledge")
@RequiredArgsConstructor
@Tag(name = "知识库聚合接口", description = "知识库 CRUD + 文档上传管理")
public class AppKnowledgeController {

    private final KnowledgeService knowledgeService;
    private final DocumentService documentService;
    private final DocumentProcessingService documentProcessingService;
    private final EmbeddingService embeddingService;
    private final KnowledgeChunkService knowledgeChunkService;
    private final RerankService rerankService;
    private final SearchFilterService searchFilterService;
    private final KnowledgeMapper knowledgeMapper;
    private final DocumentMapper documentMapper;

    // ==================== Knowledge CRUD ====================

    @PostMapping
    @Operation(summary = "创建知识库")
    public Result<KnowledgeVO> create(@Valid @RequestBody KnowledgeCreateRequest request) {
        return Result.ok(knowledgeService.create(request, UserContext.getUserId()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新知识库")
    public Result<KnowledgeVO> update(
            @Parameter(description = "知识库 ID") @PathVariable Long id,
            @Valid @RequestBody KnowledgeUpdateRequest request) {
        return Result.ok(knowledgeService.update(id, request, UserContext.getUserId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询知识库详情")
    public Result<KnowledgeVO> getById(
            @Parameter(description = "知识库 ID") @PathVariable Long id) {
        return Result.ok(knowledgeService.getById(id, UserContext.getUserId()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除知识库")
    public Result<Void> delete(
            @Parameter(description = "知识库 ID") @PathVariable Long id) {
        knowledgeService.delete(id, UserContext.getUserId());
        return Result.ok();
    }

    @GetMapping
    @Operation(summary = "分页查询知识库列表")
    public Result<PageResult<KnowledgeVO>> page(
            @Parameter(description = "页码") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页条数") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "状态筛选") @RequestParam(required = false) String status) {
        return Result.ok(knowledgeService.page(page, pageSize, status, UserContext.getUserId()));
    }

    // ==================== Document Management ====================

    @PostMapping("/{knowledgeId}/documents")
    @Operation(summary = "上传文档到知识库")
    public Result<DocumentVO> uploadDocument(
            @Parameter(description = "知识库 ID") @PathVariable Long knowledgeId,
            @Parameter(description = "上传的 TXT 文件") @RequestParam("file") MultipartFile file) {
        return Result.ok(documentService.upload(knowledgeId, file, UserContext.getUserId()));
    }

    @GetMapping("/{knowledgeId}/documents")
    @Operation(summary = "查询知识库下的文档列表")
    public Result<List<DocumentVO>> listDocuments(
            @Parameter(description = "知识库 ID") @PathVariable Long knowledgeId) {
        return Result.ok(documentService.listByKnowledgeId(knowledgeId, UserContext.getUserId()));
    }

    @GetMapping("/documents")
    @Operation(summary = "分页查询所有文档（跨知识库）")
    public Result<PageResult<DocumentVO>> pageAllDocuments(
            @Parameter(description = "页码") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页条数") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "知识库 ID") @RequestParam(required = false) Long knowledgeId,
            @Parameter(description = "文档名（模糊匹配）") @RequestParam(required = false) String fileName) {
        return Result.ok(documentService.pageAll(page, pageSize, knowledgeId, fileName, UserContext.getUserId()));
    }

    @DeleteMapping("/documents/{documentId}")
    @Operation(summary = "删除文档")
    public Result<Void> deleteDocument(
            @Parameter(description = "文档 ID") @PathVariable Long documentId) {
        documentService.delete(documentId, UserContext.getUserId());
        return Result.ok();
    }

    @PutMapping("/documents/{documentId}")
    @Operation(summary = "更新文档文件名")
    public Result<DocumentVO> updateDocument(
            @Parameter(description = "文档 ID") @PathVariable Long documentId,
            @RequestParam String fileName) {
        return Result.ok(documentService.updateFileName(documentId, fileName, UserContext.getUserId()));
    }

    // ==================== Test ====================

    @PostMapping("/documents/{documentId}/process")
    @Operation(summary = "手动触发文档处理（测试用）")
    public Result<Void> processDocument(
            @Parameter(description = "文档 ID") @PathVariable Long documentId) {
        documentProcessingService.process(documentId);
        return Result.ok();
    }

    @PostMapping("/{knowledgeId}/rebuild")
    @Operation(summary = "重建向量索引")
    public Result<Void> rebuildVectors(
            @Parameter(description = "知识库 ID") @PathVariable Long knowledgeId,
            @Parameter(description = "文档 ID（不传则重建全部文档）") @RequestParam(required = false) Long documentId,
            @Parameter(description = "分片大小（不传使用知识库默认值）") @RequestParam(required = false) Integer chunkSize,
            @Parameter(description = "重叠字符数（不传使用默认值）") @RequestParam(required = false) Integer overlap) {
        documentProcessingService.rebuildVectors(knowledgeId, documentId, chunkSize, overlap);
        return Result.ok();
    }

    @PostMapping("/test/chunk")
    @Operation(summary = "文本分片测试（测试用）")
    public Result<List<String>> testChunk(
            @Parameter(description = "TXT 文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "分片大小") @RequestParam(defaultValue = "700") int chunkSize,
            @Parameter(description = "重叠字符数") @RequestParam(defaultValue = "70") int overlap) throws Exception {
        String content = new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
        return Result.ok(TxtChunker.chunk(content, chunkSize, overlap));
    }

    @GetMapping("/search")
    @Operation(summary = "向量召回 + Rerank 精排（测试用）")
    public Result<List<SearchResultVO>> search(
            @Parameter(description = "查询文本") @RequestParam String query,
            @Parameter(description = "知识库 ID 列表（至少1个，逗号分隔）") @RequestParam List<Long> knowledgeIds,
            @Parameter(description = "是否开启精排") @RequestParam(defaultValue = "true") boolean enableRerank,
            @Parameter(description = "置信度过滤上限") @RequestParam(defaultValue = "0.3") double confidence,
            @Parameter(description = "返回条数") @RequestParam(defaultValue = "5") int limit) {
        if (query == null || query.isBlank()) {
            return Result.ok(List.of());
        }
        if (knowledgeIds == null || knowledgeIds.isEmpty()) {
            return Result.ok(List.of());
        }

        // 1. Vector search: fetch 3x candidates for reranking
        int candidateCount = Math.max(limit * 3, 20);
        float[] embedding = embeddingService.embed(List.of(query)).get(0);
        List<KnowledgeChunkPO> candidates = knowledgeChunkService.search(embedding, knowledgeIds, candidateCount);

        if (candidates.isEmpty()) {
            return Result.ok(List.of());
        }

        // Batch query metadata
        Set<Long> docIds = candidates.stream().map(KnowledgeChunkPO::getDocumentId).collect(Collectors.toSet());
        Set<Long> kIds = candidates.stream().map(KnowledgeChunkPO::getKnowledgeId).collect(Collectors.toSet());
        Map<Long, KnowledgePO> knowledgeMap = knowledgeMapper.selectBatchIds(kIds).stream()
                .collect(Collectors.toMap(KnowledgePO::getId, k -> k));
        Map<Long, DocumentPO> documentMap = documentMapper.selectBatchIds(docIds).stream()
                .collect(Collectors.toMap(DocumentPO::getId, d -> d));

        List<SearchResultVO> results;
        if (enableRerank) {
            // 2. Rerank
            List<String> texts = candidates.stream().map(KnowledgeChunkPO::getContent).toList();
            List<RerankService.RerankEntry> reranked = rerankService.rerank(query, texts, limit);

            // 3. Confidence filter (strict, no fallback)
            List<RerankService.RerankEntry> filtered = searchFilterService.filterStrict(reranked, confidence);

            // 4. Map results
            results = filtered.stream()
                    .map(r -> toSearchResultVO(candidates.get(r.index()), knowledgeMap, documentMap, r.relevanceScore()))
                    .toList();
        } else {
            // No rerank, sort by similarity desc
            results = candidates.stream()
                    .sorted((a, b) -> Double.compare(
                            b.getSimilarity() != null ? b.getSimilarity() : 0.0,
                            a.getSimilarity() != null ? a.getSimilarity() : 0.0))
                    .limit(limit)
                    .map(c -> toSearchResultVO(c, knowledgeMap, documentMap, null))
                    .toList();
        }

        return Result.ok(results);
    }

    private SearchResultVO toSearchResultVO(KnowledgeChunkPO chunk,
                                             Map<Long, KnowledgePO> knowledgeMap,
                                             Map<Long, DocumentPO> documentMap,
                                             Double relevanceScore) {
        SearchResultVO vo = new SearchResultVO();
        vo.setContent(chunk.getContent());
        vo.setChunkIndex(chunk.getChunkIndex() != null ? chunk.getChunkIndex() + 1 : null);
        vo.setSimilarity(chunk.getSimilarity());
        vo.setRelevanceScore(relevanceScore);

        KnowledgePO knowledge = knowledgeMap.get(chunk.getKnowledgeId());
        if (knowledge != null) {
            vo.setKnowledgeName(knowledge.getName());
            vo.setEmbedModel(knowledge.getEmbeddingModel());
        }

        DocumentPO document = documentMap.get(chunk.getDocumentId());
        if (document != null) {
            vo.setDocumentName(document.getFileName());
        }

        return vo;
    }
}
