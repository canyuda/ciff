package com.ciff.app.controller;

import com.ciff.agent.controller.AgentController;
import com.ciff.agent.dto.AgentVO;
import com.ciff.agent.service.AgentService;
import com.ciff.agent.service.AgentToolService;
import com.ciff.common.context.UserContext;
import com.ciff.common.dto.PageResult;
import com.ciff.mcp.controller.ToolController;
import com.ciff.mcp.service.ToolService;
import com.ciff.provider.dto.ModelVO;
import com.ciff.provider.facade.ProviderFacade;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AppAgentController.class, AgentController.class, ToolController.class})
class AppAgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AgentService agentService;

    @MockBean
    private AgentToolService agentToolService;

    @MockBean
    private ProviderFacade providerFacade;

    @MockBean
    private ToolService toolService;

    @BeforeEach
    void setUp() {
        UserContext.setUserId(100L);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void create_whenModelValid_shouldReturnOk() throws Exception {
        ModelVO model = buildModelVO(10L, "gpt-4o");
        given(providerFacade.getModelById(10L)).willReturn(model);

        AgentVO agent = buildAgentVO(1L, "test-agent", "chatbot");
        given(agentService.create(any(), eq(100L))).willReturn(agent);

        String body = """
                {
                    "name": "test-agent",
                    "type": "chatbot",
                    "modelId": 10,
                    "systemPrompt": "You are helpful"
                }
                """;

        mockMvc.perform(post("/api/v1/app/agents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("test-agent"));
    }

    @Test
    void create_whenModelIdInvalid_shouldReturn400() throws Exception {
        given(providerFacade.getModelById(999L)).willReturn(null);

        String body = """
                {
                    "name": "test-agent",
                    "type": "chatbot",
                    "modelId": 999,
                    "systemPrompt": "You are helpful"
                }
                """;

        mockMvc.perform(post("/api/v1/app/agents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("模型不存在: 999"));
    }

    @Test
    void update_whenModelIdValid_shouldReturnOk() throws Exception {
        ModelVO model = buildModelVO(20L, "claude-3");
        given(providerFacade.getModelById(20L)).willReturn(model);

        AgentVO agent = buildAgentVO(1L, "updated", "chatbot");
        agent.setModelId(20L);
        given(agentService.update(eq(1L), any(), eq(100L))).willReturn(agent);

        String body = """
                {
                    "modelId": 20
                }
                """;

        mockMvc.perform(put("/api/v1/app/agents/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.modelId").value(20));
    }

    @Test
    void getById_shouldEnrichModelName() throws Exception {
        AgentVO agent = buildAgentVO(1L, "test-agent", "chatbot");
        agent.setModelId(10L);
        given(agentService.getById(1L, 100L)).willReturn(agent);
        given(providerFacade.getModelById(10L)).willReturn(buildModelVO(10L, "gpt-4o"));

        mockMvc.perform(get("/api/v1/app/agents/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.modelName").value("gpt-4o"));
    }

    @Test
    void delete_shouldReturnOk() throws Exception {
        doNothing().when(agentService).delete(1L, 100L);

        mockMvc.perform(delete("/api/v1/app/agents/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void page_shouldReturnPageResult() throws Exception {
        AgentVO agent = buildAgentVO(1L, "agent-1", "chatbot");
        agent.setModelId(10L);
        PageResult<AgentVO> pageResult = new PageResult<>(List.of(agent), 1L, 1, 20);
        given(agentService.page(any(), any(), any(), any(), eq(100L))).willReturn(pageResult);
        given(providerFacade.getModelById(10L)).willReturn(buildModelVO(10L, "gpt-4o"));

        mockMvc.perform(get("/api/v1/app/agents")
                        .param("page", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void bindTool_shouldReturnOk() throws Exception {
        doNothing().when(agentToolService).bind(1L, 10L);

        mockMvc.perform(post("/api/v1/app/agents/1/tools/10"))
                .andExpect(status().isOk());
    }

    @Test
    void unbindTool_shouldReturnOk() throws Exception {
        doNothing().when(agentToolService).unbind(1L, 10L);

        mockMvc.perform(delete("/api/v1/app/agents/1/tools/10"))
                .andExpect(status().isOk());
    }

    @Test
    void replaceTools_shouldReturnOk() throws Exception {
        doNothing().when(agentToolService).replaceAll(eq(1L), any());

        String body = "[1, 2, 3]";
        mockMvc.perform(put("/api/v1/app/agents/1/tools")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    private AgentVO buildAgentVO(Long id, String name, String type) {
        AgentVO vo = new AgentVO();
        vo.setId(id);
        vo.setName(name);
        vo.setType(type);
        vo.setStatus("draft");
        vo.setSystemPrompt("prompt");
        vo.setTools(List.of());
        vo.setCreateTime(LocalDateTime.of(2026, 4, 18, 12, 0, 0));
        vo.setUpdateTime(LocalDateTime.of(2026, 4, 18, 12, 0, 0));
        return vo;
    }

    private ModelVO buildModelVO(Long id, String name) {
        ModelVO vo = new ModelVO();
        vo.setId(id);
        vo.setName(name);
        vo.setStatus("active");
        return vo;
    }
}
