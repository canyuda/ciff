package com.ciff.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ciff.chat.entity.ConversationPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ConversationMapper extends BaseMapper<ConversationPO> {
}
