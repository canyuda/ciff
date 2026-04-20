package com.ciff.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ciff.chat.entity.ChatMessagePO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessagePO> {
}
