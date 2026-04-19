package com.ciff.knowledge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "召回检索结果")
public class SearchResultVO {

    @Schema(description = "分块内容")
    private String content;

    @Schema(description = "Embedding 模型名称")
    private String embedModel;

    @Schema(description = "所属知识库名称")
    private String knowledgeName;

    @Schema(description = "所属文档名称")
    private String documentName;

    @Schema(description = "分块序号（从1开始）")
    private Integer chunkIndex;

    @Schema(description = "向量相似度分数")
    private Double similarity;

    @Schema(description = "Rerank 相关性分数（未开启精排时为 null）")
    private Double relevanceScore;
}
