package com.ciff.knowledge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "更新知识库请求")
public class KnowledgeUpdateRequest {

    @Size(max = 128, message = "名称最长128个字符")
    @Schema(description = "知识库名称")
    private String name;

    @Size(max = 512, message = "描述最长512个字符")
    @Schema(description = "知识库描述")
    private String description;

    @Schema(description = "固定分块长度（128-2048）")
    private Integer chunkSize;

    @Schema(description = "Embedding 模型名称")
    private String embeddingModel;

    @Schema(description = "状态：active / inactive")
    private String status;
}
