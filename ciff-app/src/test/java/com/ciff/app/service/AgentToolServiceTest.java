package com.ciff.app.service;

import com.ciff.agent.entity.AgentToolPO;
import com.ciff.agent.mapper.AgentToolMapper;
import com.ciff.agent.service.impl.AgentToolServiceImpl;
import com.ciff.common.exception.BizException;
import com.ciff.mcp.dto.ToolVO;
import com.ciff.mcp.facade.ToolFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentToolServiceTest {

    @Mock
    private AgentToolMapper agentToolMapper;

    @Mock
    private ToolFacade toolFacade;

    @InjectMocks
    private AgentToolServiceImpl agentToolService;

    @Test
    void bind_whenValid_shouldInsert() {
        ToolVO tool = new ToolVO();
        tool.setId(1L);
        tool.setName("weather-api");
        when(toolFacade.getById(1L)).thenReturn(tool);
        when(agentToolMapper.selectCount(any())).thenReturn(0L);
        doReturn(1).when(agentToolMapper).insert(any(AgentToolPO.class));

        agentToolService.bind(1L, 1L);

        verify(agentToolMapper).insert(any(AgentToolPO.class));
    }

    @Test
    void bind_whenToolNotFound_shouldThrowBizException() {
        when(toolFacade.getById(999L)).thenReturn(null);

        assertThatThrownBy(() -> agentToolService.bind(1L, 999L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("工具不存在");

        verify(agentToolMapper, never()).insert(any(AgentToolPO.class));
    }

    @Test
    void bind_whenAlreadyBound_shouldThrowBizException() {
        ToolVO tool = new ToolVO();
        tool.setId(1L);
        when(toolFacade.getById(1L)).thenReturn(tool);
        when(agentToolMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> agentToolService.bind(1L, 1L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("工具已绑定");
    }

    @Test
    void unbind_shouldDelete() {
        when(agentToolMapper.delete(any())).thenReturn(1);

        agentToolService.unbind(1L, 1L);

        verify(agentToolMapper).delete(any());
    }

    @Test
    void replaceAll_shouldDeleteOldAndInsertNew() {
        ToolVO tool1 = new ToolVO();
        tool1.setId(1L);
        ToolVO tool2 = new ToolVO();
        tool2.setId(2L);
        when(toolFacade.getById(1L)).thenReturn(tool1);
        when(toolFacade.getById(2L)).thenReturn(tool2);
        when(agentToolMapper.delete(any())).thenReturn(2);
        doReturn(1).when(agentToolMapper).insert(any(AgentToolPO.class));

        agentToolService.replaceAll(1L, List.of(1L, 2L));

        verify(agentToolMapper).delete(any());
        // 2 tools => 2 inserts
        verify(agentToolMapper, org.mockito.Mockito.times(2)).insert(any(AgentToolPO.class));
    }

    @Test
    void replaceAll_withEmptyList_shouldOnlyDelete() {
        when(agentToolMapper.delete(any())).thenReturn(0);

        agentToolService.replaceAll(1L, List.of());

        verify(agentToolMapper).delete(any());
        verify(agentToolMapper, never()).insert(any(AgentToolPO.class));
    }

    @Test
    void listTools_shouldReturnToolVOs() {
        AgentToolPO at1 = new AgentToolPO();
        at1.setAgentId(1L);
        at1.setToolId(10L);
        AgentToolPO at2 = new AgentToolPO();
        at2.setAgentId(1L);
        at2.setToolId(20L);

        when(agentToolMapper.selectList(any())).thenReturn(List.of(at1, at2));

        ToolVO tool1 = new ToolVO();
        tool1.setId(10L);
        tool1.setName("tool-1");
        ToolVO tool2 = new ToolVO();
        tool2.setId(20L);
        tool2.setName("tool-2");
        when(toolFacade.listByIds(List.of(10L, 20L))).thenReturn(List.of(tool1, tool2));

        List<ToolVO> tools = agentToolService.listTools(1L);

        assertThat(tools).hasSize(2);
        assertThat(tools.get(0).getName()).isEqualTo("tool-1");
    }

    @Test
    void listToolIds_whenNoBindings_shouldReturnEmpty() {
        when(agentToolMapper.selectList(any())).thenReturn(List.of());

        List<Long> ids = agentToolService.listToolIds(1L);

        assertThat(ids).isEmpty();
    }
}
