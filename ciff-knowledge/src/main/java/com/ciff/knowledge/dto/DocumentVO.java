package com.ciff.knowledge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "知识库文档响应")
public class DocumentVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "知识库 ID")
    private Long knowledgeId;

    @Schema(description = "知识库名称")
    private String knowledgeName;

    @Schema(description = "原始文件名")
    private String fileName;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "分块数量")
    private Integer chunkCount;

    @Schema(description = "状态：uploading / processing / ready / failed")
    private String status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
