package com.ciff.knowledge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "知识库响应")
public class KnowledgeVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "知识库名称")
    private String name;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "固定分块长度")
    private Integer chunkSize;

    @Schema(description = "Embedding 模型名称")
    private String embeddingModel;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "文档数量")
    private Integer documentCount;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
