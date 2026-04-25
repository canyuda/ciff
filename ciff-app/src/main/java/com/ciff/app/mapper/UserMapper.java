package com.ciff.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ciff.app.entity.UserPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<UserPO> {
}
