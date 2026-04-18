package com.ciff.knowledge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "创建知识库请求")
public class KnowledgeCreateRequest {

    @NotBlank(message = "名称不能为空")
    @Size(max = 128, message = "名称最长128个字符")
    @Schema(description = "知识库名称", example = "产品文档库")
    private String name;

    @Size(max = 512, message = "描述最长512个字符")
    @Schema(description = "知识库描述")
    private String description;

    @Schema(description = "固定分块长度（128-2048，默认500）", example = "500")
    private Integer chunkSize;

    @NotBlank(message = "Embedding 模型不能为空")
    @Schema(description = "Embedding 模型名称", example = "text-embedding-v3")
    private String embeddingModel;
}
