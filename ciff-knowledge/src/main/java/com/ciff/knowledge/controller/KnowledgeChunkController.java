package com.ciff.knowledge.controller;

import com.ciff.common.dto.Result;
import com.ciff.knowledge.entity.KnowledgeChunkPO;
import com.ciff.knowledge.service.KnowledgeChunkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/knowledge-chunks")
@RequiredArgsConstructor
@Tag(name = "知识库分块", description = "t_knowledge_chunk CRUD（PGVector）")
public class KnowledgeChunkController {

    private final KnowledgeChunkService knowledgeChunkService;

    // todo [未被前端使用:待清理]
    @GetMapping("/{id}")
    @Operation(summary = "查询单个分块")
    public Result<KnowledgeChunkPO> getById(
            @Parameter(description = "分块 ID") @PathVariable Long id) {
        KnowledgeChunkPO chunk = knowledgeChunkService.getById(id);
        return chunk == null ? Result.fail(404, "Chunk not found") : Result.ok(chunk);
    }

    @GetMapping(params = "documentId")
    @Operation(summary = "按文档ID查询分块列表")
    public Result<List<KnowledgeChunkPO>> listByDocumentId(
            @Parameter(description = "文档 ID") @RequestParam Long documentId) {
        return Result.ok(knowledgeChunkService.listByDocumentId(documentId));
    }

    // todo [未被前端使用:待清理]
    @GetMapping(params = "knowledgeId")
    @Operation(summary = "按知识库ID查询分块列表")
    public Result<List<KnowledgeChunkPO>> listByKnowledgeId(
            @Parameter(description = "知识库 ID") @RequestParam Long knowledgeId) {
        return Result.ok(knowledgeChunkService.listByKnowledgeId(knowledgeId));
    }

    // todo [未被前端使用:待清理]
    @DeleteMapping("/{id}")
    @Operation(summary = "删除单个分块")
    public Result<Void> deleteById(
            @Parameter(description = "分块 ID") @PathVariable Long id) {
        knowledgeChunkService.deleteById(id);
        return Result.ok();
    }

    // todo [未被前端使用:待清理]
    @DeleteMapping(params = "documentId")
    @Operation(summary = "按文档ID删除所有分块")
    public Result<Void> deleteByDocumentId(
            @Parameter(description = "文档 ID") @RequestParam Long documentId) {
        knowledgeChunkService.deleteByDocumentId(documentId);
        return Result.ok();
    }

    // todo [未被前端使用:待清理]
    @DeleteMapping(params = "knowledgeId")
    @Operation(summary = "按知识库ID删除所有分块")
    public Result<Void> deleteByKnowledgeId(
            @Parameter(description = "知识库 ID") @RequestParam Long knowledgeId) {
        knowledgeChunkService.deleteByKnowledgeId(knowledgeId);
        return Result.ok();
    }
}
