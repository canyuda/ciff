package com.ciff.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ciff.chat.convertor.ConversationConvertor;
import com.ciff.chat.dto.ConversationVO;
import com.ciff.chat.entity.ConversationPO;
import com.ciff.chat.mapper.ConversationMapper;
import com.ciff.chat.service.ConversationService;
import com.ciff.common.constant.ErrorCode;
import com.ciff.common.dto.PageResult;
import com.ciff.common.exception.BizException;
import com.ciff.common.util.PageHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final ConversationMapper conversationMapper;

    @Override
    public ConversationVO create(Long agentId, String title, Long userId) {
        ConversationPO po = new ConversationPO();
        po.setUserId(userId);
        po.setAgentId(agentId);
        po.setTitle(title);
        po.setStatus("active");
        conversationMapper.insert(po);
        return ConversationConvertor.toVO(po);
    }

    @Override
    public ConversationVO getById(Long id, Long userId) {
        ConversationPO po = requireExists(id);
        if (!po.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权访问该会话");
        }
        return ConversationConvertor.toVO(po);
    }

    @Override
    public PageResult<ConversationVO> page(Integer page, Integer pageSize, Long agentId, Long userId) {
        Page<ConversationPO> pageParam = PageHelper.toPage(page, pageSize);

        LambdaQueryWrapper<ConversationPO> wrapper = new LambdaQueryWrapper<ConversationPO>()
                .eq(ConversationPO::getUserId, userId)
                .eq(agentId != null, ConversationPO::getAgentId, agentId)
                .eq(ConversationPO::getStatus, "active")
                .orderByDesc(ConversationPO::getUpdateTime);

        Page<ConversationPO> result = conversationMapper.selectPage(pageParam, wrapper);
        var records = result.getRecords().stream().map(ConversationConvertor::toVO).toList();
        return PageResult.of(records, result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    @Override
    public void delete(Long id, Long userId) {
        ConversationPO po = requireExists(id);
        if (!po.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权删除该会话");
        }
        po.setStatus("archived");
        conversationMapper.updateById(po);
    }

    @Override
    public void updateTitle(Long id, String title) {
        ConversationPO po = new ConversationPO();
        po.setId(id);
        po.setTitle(title);
        conversationMapper.updateById(po);
    }

    private ConversationPO requireExists(Long id) {
        ConversationPO po = conversationMapper.selectById(id);
        if (po == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "会话不存在: " + id);
        }
        return po;
    }
}
