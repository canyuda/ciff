package com.ciff.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "会话响应")
public class ConversationVO {

    @Schema(description = "会话 ID")
    private Long id;

    @Schema(description = "Agent ID")
    private Long agentId;

    @Schema(description = "Agent 名称")
    private String agentName;

    @Schema(description = "会话标题")
    private String title;

    @Schema(description = "状态：active / archived")
    private String status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
