package com.ciff.app.controller;

import com.ciff.common.context.UserContext;
import com.ciff.common.dto.PageResult;
import com.ciff.common.dto.Result;
import com.ciff.knowledge.dto.DocumentVO;
import com.ciff.knowledge.dto.KnowledgeCreateRequest;
import com.ciff.knowledge.dto.KnowledgeUpdateRequest;
import com.ciff.knowledge.dto.KnowledgeVO;
import com.ciff.knowledge.entity.KnowledgeChunkPO;
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

import java.util.List;

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

    @DeleteMapping("/documents/{documentId}")
    @Operation(summary = "删除文档")
    public Result<Void> deleteDocument(
            @Parameter(description = "文档 ID") @PathVariable Long documentId) {
        documentService.delete(documentId, UserContext.getUserId());
        return Result.ok();
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
    public Result<List<KnowledgeChunkPO>> search(
            @Parameter(description = "查询文本") @RequestParam String query,
            @Parameter(description = "知识库 ID（可选）") @RequestParam(required = false) Long knowledgeId,
            @Parameter(description = "返回条数") @RequestParam(defaultValue = "5") int limit) {
        // 1. Vector search: fetch 3x candidates for reranking
        int candidateCount = Math.max(limit * 3, 20);
        float[] embedding = embeddingService.embed(List.of(query)).get(0);
        List<KnowledgeChunkPO> candidates = knowledgeChunkService.search(embedding, knowledgeId, candidateCount);

        if (candidates.isEmpty()) {
            return Result.ok(List.of());
        }

        // 2. Rerank
        List<String> texts = candidates.stream().map(KnowledgeChunkPO::getContent).toList();
        List<RerankService.RerankEntry> reranked = rerankService.rerank(query, texts, limit);

        // 3. Confidence filter
        List<RerankService.RerankEntry> filtered = searchFilterService.filter(reranked);

        // 4. Map results back to chunks
        List<KnowledgeChunkPO> results = filtered.stream()
                .map(r -> {
                    KnowledgeChunkPO chunk = candidates.get(r.index());
                    chunk.setSimilarity(r.relevanceScore());
                    return chunk;
                })
                .toList();

        return Result.ok(results);
    }
}
