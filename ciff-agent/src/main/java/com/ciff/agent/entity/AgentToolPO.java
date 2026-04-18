package com.ciff.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "t_agent_tool")
public class AgentToolPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long agentId;

    private Long toolId;

    private LocalDateTime createTime;
}
