package com.ciff.app.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ciff.agent.dto.AgentCreateRequest;
import com.ciff.agent.dto.AgentUpdateRequest;
import com.ciff.agent.dto.AgentVO;
import com.ciff.agent.entity.AgentPO;
import com.ciff.agent.mapper.AgentMapper;
import com.ciff.agent.service.AgentToolService;
import com.ciff.agent.service.impl.AgentServiceImpl;
import com.ciff.common.dto.PageResult;
import com.ciff.common.exception.BizException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentServiceTest {

    @Mock
    private AgentMapper agentMapper;

    @Mock
    private AgentToolService agentToolService;

    @InjectMocks
    private AgentServiceImpl agentService;

    @Test
    void create_whenValid_shouldSaveAndReturnVo() {
        when(agentMapper.selectCount(any())).thenReturn(0L);
        doAnswer(invocation -> {
            AgentPO po = invocation.getArgument(0);
            po.setId(1L);
            return 1;
        }).when(agentMapper).insert(any(AgentPO.class));
        when(agentToolService.listTools(1L)).thenReturn(List.of());

        AgentCreateRequest request = new AgentCreateRequest();
        request.setName("test-agent");
        request.setType("chatbot");
        request.setModelId(10L);
        request.setSystemPrompt("You are a helpful assistant");

        AgentVO vo = agentService.create(request, 100L);

        assertThat(vo.getName()).isEqualTo("test-agent");
        assertThat(vo.getType()).isEqualTo("chatbot");
        assertThat(vo.getStatus()).isEqualTo("draft");
    }

    @Test
    void create_whenNameDuplicate_shouldThrowBizException() {
        when(agentMapper.selectCount(any())).thenReturn(1L);

        AgentCreateRequest request = new AgentCreateRequest();
        request.setName("duplicate");
        request.setType("chatbot");
        request.setModelId(10L);
        request.setSystemPrompt("prompt");

        assertThatThrownBy(() -> agentService.create(request, 100L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("Agent 名称已存在");

        verify(agentMapper, never()).insert(any(AgentPO.class));
    }

    @Test
    void create_whenInvalidType_shouldThrowBizException() {
        AgentCreateRequest request = new AgentCreateRequest();
        request.setName("test");
        request.setType("invalid");
        request.setModelId(10L);
        request.setSystemPrompt("prompt");

        assertThatThrownBy(() -> agentService.create(request, 100L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不支持的 Agent 类型");
    }

    @Test
    void create_withToolIds_shouldBindTools() {
        when(agentMapper.selectCount(any())).thenReturn(0L);
        doAnswer(invocation -> {
            AgentPO po = invocation.getArgument(0);
            po.setId(1L);
            return 1;
        }).when(agentMapper).insert(any(AgentPO.class));
        when(agentToolService.listTools(1L)).thenReturn(List.of());

        AgentCreateRequest request = new AgentCreateRequest();
        request.setName("agent-with-tools");
        request.setType("agent");
        request.setModelId(10L);
        request.setSystemPrompt("prompt");
        request.setToolIds(List.of(1L, 2L));

        agentService.create(request, 100L);

        verify(agentToolService).replaceAll(1L, List.of(1L, 2L));
    }

    @Test
    void update_whenValid_shouldUpdateAndReturnVo() {
        AgentPO existing = buildPO(1L, "old-name", "chatbot", 100L);
        when(agentMapper.selectById(1L)).thenReturn(existing);
        when(agentMapper.updateById(any(AgentPO.class))).thenReturn(1);
        when(agentToolService.listTools(1L)).thenReturn(List.of());

        AgentUpdateRequest request = new AgentUpdateRequest();
        request.setName("new-name");
        request.setSystemPrompt("new prompt");

        AgentVO vo = agentService.update(1L, request, 100L);

        assertThat(vo.getName()).isEqualTo("new-name");
    }

    @Test
    void update_whenNotFound_shouldThrowBizException() {
        when(agentMapper.selectById(999L)).thenReturn(null);

        AgentUpdateRequest request = new AgentUpdateRequest();
        request.setName("new");

        assertThatThrownBy(() -> agentService.update(999L, request, 100L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("Agent 不存在");
    }

    @Test
    void update_whenUserIdMismatch_shouldThrowBizException() {
        AgentPO existing = buildPO(1L, "test", "chatbot", 100L);
        when(agentMapper.selectById(1L)).thenReturn(existing);

        AgentUpdateRequest request = new AgentUpdateRequest();
        request.setName("new");

        assertThatThrownBy(() -> agentService.update(1L, request, 999L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("Agent 不存在");
    }

    @Test
    void update_withToolIds_shouldReplaceTools() {
        AgentPO existing = buildPO(1L, "test", "agent", 100L);
        when(agentMapper.selectById(1L)).thenReturn(existing);
        when(agentMapper.updateById(any(AgentPO.class))).thenReturn(1);
        when(agentToolService.listTools(1L)).thenReturn(List.of());

        AgentUpdateRequest request = new AgentUpdateRequest();
        request.setToolIds(List.of(3L, 4L));

        agentService.update(1L, request, 100L);

        verify(agentToolService).replaceAll(1L, List.of(3L, 4L));
    }

    @Test
    void getById_whenExists_shouldReturnVoWithTools() {
        AgentPO po = buildPO(1L, "test-agent", "chatbot", 100L);
        when(agentMapper.selectById(1L)).thenReturn(po);
        when(agentToolService.listTools(1L)).thenReturn(List.of());

        AgentVO vo = agentService.getById(1L, 100L);

        assertThat(vo.getName()).isEqualTo("test-agent");
        assertThat(vo.getTools()).isNotNull();
    }

    @Test
    void delete_whenExists_shouldDelete() {
        AgentPO po = buildPO(1L, "test", "chatbot", 100L);
        when(agentMapper.selectById(1L)).thenReturn(po);
        when(agentMapper.deleteById(1L)).thenReturn(1);

        agentService.delete(1L, 100L);

        verify(agentMapper).deleteById(1L);
    }

    @Test
    void page_whenHasRecords_shouldReturnPage() {
        AgentPO agent = buildPO(1L, "agent-1", "chatbot", 100L);
        Page<AgentPO> pageResult = new Page<>(1, 20);
        pageResult.setRecords(List.of(agent));
        pageResult.setTotal(1L);

        when(agentMapper.selectPage(any(), any())).thenReturn(pageResult);

        PageResult<AgentVO> result = agentService.page(1, 20, null, null, 100L);

        assertThat(result.getList()).hasSize(1);
        assertThat(result.getTotal()).isEqualTo(1);
    }

    @Test
    void page_withTypeFilter_shouldReturnFiltered() {
        Page<AgentPO> pageResult = new Page<>(1, 20);
        pageResult.setRecords(List.of());
        pageResult.setTotal(0L);

        when(agentMapper.selectPage(any(), any())).thenReturn(pageResult);

        PageResult<AgentVO> result = agentService.page(1, 20, "agent", null, 100L);

        assertThat(result.getList()).isEmpty();
    }

    private AgentPO buildPO(Long id, String name, String type, Long userId) {
        AgentPO po = new AgentPO();
        po.setId(id);
        po.setName(name);
        po.setType(type);
        po.setUserId(userId);
        po.setModelId(10L);
        po.setSystemPrompt("prompt");
        po.setStatus("active");
        return po;
    }
}
