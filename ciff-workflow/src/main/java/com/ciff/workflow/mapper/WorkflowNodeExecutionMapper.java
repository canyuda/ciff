package com.ciff.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ciff.workflow.entity.WorkflowNodeExecutionPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkflowNodeExecutionMapper extends BaseMapper<WorkflowNodeExecutionPO> {
}
