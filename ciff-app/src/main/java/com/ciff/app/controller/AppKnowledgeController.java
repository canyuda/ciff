package com.ciff.app.controller;

import com.ciff.common.context.UserContext;
import com.ciff.common.dto.PageResult;
import com.ciff.common.dto.Result;
import com.ciff.knowledge.dto.DocumentVO;
import com.ciff.knowledge.dto.KnowledgeCreateRequest;
import com.ciff.knowledge.dto.KnowledgeUpdateRequest;
import com.ciff.knowledge.dto.KnowledgeVO;
import com.ciff.knowledge.service.DocumentService;
import com.ciff.knowledge.service.KnowledgeService;
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
}
