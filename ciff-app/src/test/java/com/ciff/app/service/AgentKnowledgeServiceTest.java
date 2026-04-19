package com.ciff.app.service;

import com.ciff.agent.entity.AgentKnowledgePO;
import com.ciff.agent.mapper.AgentKnowledgeMapper;
import com.ciff.agent.service.impl.AgentKnowledgeServiceImpl;
import com.ciff.common.exception.BizException;
import com.ciff.knowledge.dto.KnowledgeVO;
import com.ciff.knowledge.facade.KnowledgeFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentKnowledgeServiceTest {

    @Mock
    private AgentKnowledgeMapper agentKnowledgeMapper;

    @Mock
    private KnowledgeFacade knowledgeFacade;

    @InjectMocks
    private AgentKnowledgeServiceImpl agentKnowledgeService;

    @Test
    void bind_whenValid_shouldInsert() {
        when(knowledgeFacade.getById(1L)).thenReturn(kb(1L, "test-kb"));
        when(agentKnowledgeMapper.selectCount(any())).thenReturn(0L);
        doReturn(1).when(agentKnowledgeMapper).insert(any(AgentKnowledgePO.class));

        agentKnowledgeService.bind(1L, 1L);

        verify(agentKnowledgeMapper).insert(any(AgentKnowledgePO.class));
    }

    @Test
    void bind_whenKnowledgeNotFound_shouldThrowBizException() {
        when(knowledgeFacade.getById(999L)).thenReturn(null);

        assertThatThrownBy(() -> agentKnowledgeService.bind(1L, 999L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("知识库不存在");

        verify(agentKnowledgeMapper, never()).insert(any(AgentKnowledgePO.class));
    }

    @Test
    void bind_whenAlreadyBound_shouldThrowBizException() {
        when(knowledgeFacade.getById(1L)).thenReturn(kb(1L, "test-kb"));
        when(agentKnowledgeMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> agentKnowledgeService.bind(1L, 1L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("知识库已绑定");
    }

    @Test
    void unbind_shouldDelete() {
        when(agentKnowledgeMapper.delete(any())).thenReturn(1);

        agentKnowledgeService.unbind(1L, 1L);

        verify(agentKnowledgeMapper).delete(any());
    }

    @Test
    void replaceAll_shouldDeleteOldAndInsertNew() {
        when(knowledgeFacade.getById(1L)).thenReturn(kb(1L, "k1"));
        when(knowledgeFacade.getById(2L)).thenReturn(kb(2L, "k2"));
        when(agentKnowledgeMapper.delete(any())).thenReturn(2);
        doReturn(1).when(agentKnowledgeMapper).insert(any(AgentKnowledgePO.class));

        agentKnowledgeService.replaceAll(1L, List.of(1L, 2L));

        verify(agentKnowledgeMapper).delete(any());
        verify(agentKnowledgeMapper, times(2)).insert(any(AgentKnowledgePO.class));
    }

    @Test
    void replaceAll_withEmptyList_shouldOnlyDelete() {
        when(agentKnowledgeMapper.delete(any())).thenReturn(0);

        agentKnowledgeService.replaceAll(1L, List.of());

        verify(agentKnowledgeMapper).delete(any());
        verify(agentKnowledgeMapper, never()).insert(any(AgentKnowledgePO.class));
    }

    @Test
    void listKnowledges_shouldReturnKnowledgeVOs() {
        AgentKnowledgePO ak1 = new AgentKnowledgePO();
        ak1.setAgentId(1L);
        ak1.setKnowledgeId(10L);
        AgentKnowledgePO ak2 = new AgentKnowledgePO();
        ak2.setAgentId(1L);
        ak2.setKnowledgeId(20L);

        when(agentKnowledgeMapper.selectList(any())).thenReturn(List.of(ak1, ak2));
        when(knowledgeFacade.listByIds(List.of(10L, 20L)))
                .thenReturn(List.of(kb(10L, "k1"), kb(20L, "k2")));

        List<KnowledgeVO> knowledges = agentKnowledgeService.listKnowledges(1L);

        assertThat(knowledges).hasSize(2);
        assertThat(knowledges.get(0).getName()).isEqualTo("k1");
    }

    @Test
    void listKnowledgeIds_whenNoBindings_shouldReturnEmpty() {
        when(agentKnowledgeMapper.selectList(any())).thenReturn(List.of());

        List<Long> ids = agentKnowledgeService.listKnowledgeIds(1L);

        assertThat(ids).isEmpty();
    }

    private static KnowledgeVO kb(Long id, String name) {
        KnowledgeVO vo = new KnowledgeVO();
        vo.setId(id);
        vo.setName(name);
        return vo;
    }
}
