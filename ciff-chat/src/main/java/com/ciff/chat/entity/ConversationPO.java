package com.ciff.chat.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ciff.common.entity.SoftDeletableEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_conversation")
public class ConversationPO extends SoftDeletableEntity {

    private Long userId;

    private Long agentId;

    private String title;

    /** active / archived */
    private String status;
}
