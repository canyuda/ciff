package com.ciff.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ciff.workflow.entity.WorkflowPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkflowMapper extends BaseMapper<WorkflowPO> {
}
