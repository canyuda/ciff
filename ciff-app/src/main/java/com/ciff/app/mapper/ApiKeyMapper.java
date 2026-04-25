package com.ciff.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ciff.app.entity.ApiKeyPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ApiKeyMapper extends BaseMapper<ApiKeyPO> {
}
