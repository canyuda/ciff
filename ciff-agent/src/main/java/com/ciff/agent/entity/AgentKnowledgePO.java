package com.ciff.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "t_agent_knowledge")
public class AgentKnowledgePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long agentId;

    private Long knowledgeId;

    private LocalDateTime createTime;
}
