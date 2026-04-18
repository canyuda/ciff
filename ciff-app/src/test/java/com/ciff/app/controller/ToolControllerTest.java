package com.ciff.app.controller;

import com.ciff.common.dto.PageResult;
import com.ciff.mcp.controller.ToolController;
import com.ciff.mcp.dto.ToolVO;
import com.ciff.mcp.service.ToolService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

@WebMvcTest(ToolController.class)
class ToolControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ToolService toolService;

    @Test
    void create_whenInputValid_shouldReturnOk() throws Exception {
        String body = """
                {
                    "name": "weather-api",
                    "description": "weather lookup",
                    "type": "api",
                    "endpoint": "https://api.weather.com/v1"
                }
                """;

        ToolVO vo = buildVO(1L, "weather-api", "api");
        given(toolService.create(any())).willReturn(vo);

        mockMvc.perform(post("/api/v1/tools")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("weather-api"))
                .andExpect(jsonPath("$.data.type").value("api"));
    }

    @Test
    void create_whenNameBlank_shouldReturn400() throws Exception {
        String body = """
                {
                    "name": "",
                    "type": "api",
                    "endpoint": "https://api.weather.com"
                }
                """;

        mockMvc.perform(post("/api/v1/tools")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_whenTypeBlank_shouldReturn400() throws Exception {
        String body = """
                {
                    "name": "weather-api",
                    "endpoint": "https://api.weather.com"
                }
                """;

        mockMvc.perform(post("/api/v1/tools")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_whenEndpointBlank_shouldReturn400() throws Exception {
        String body = """
                {
                    "name": "weather-api",
                    "type": "api"
                }
                """;

        mockMvc.perform(post("/api/v1/tools")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_whenInputValid_shouldReturnOk() throws Exception {
        String body = """
                {
                    "name": "weather-api-v2"
                }
                """;

        ToolVO vo = buildVO(1L, "weather-api-v2", "api");
        given(toolService.update(eq(1L), any())).willReturn(vo);

        mockMvc.perform(put("/api/v1/tools/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("weather-api-v2"));
    }

    @Test
    void getById_shouldReturnOk() throws Exception {
        ToolVO vo = buildVO(1L, "weather-api", "api");
        given(toolService.getById(1L)).willReturn(vo);

        mockMvc.perform(get("/api/v1/tools/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("weather-api"));
    }

    @Test
    void delete_shouldReturnOk() throws Exception {
        doNothing().when(toolService).delete(1L);

        mockMvc.perform(delete("/api/v1/tools/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void page_shouldReturnPageResult() throws Exception {
        ToolVO vo = buildVO(1L, "weather-api", "api");
        PageResult<ToolVO> pageResult = new PageResult<>(List.of(vo), 1L, 1, 20);
        given(toolService.page(any(), any(), eq("api"), any())).willReturn(pageResult);

        mockMvc.perform(get("/api/v1/tools")
                        .param("page", "1")
                        .param("pageSize", "20")
                        .param("type", "api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void page_withoutFilters_shouldReturnOk() throws Exception {
        PageResult<ToolVO> pageResult = new PageResult<>(List.of(), 0L, 1, 20);
        given(toolService.page(any(), any(), any(), any())).willReturn(pageResult);

        mockMvc.perform(get("/api/v1/tools"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isMap());
    }

    @Test
    void create_withParamSchema_shouldReturnOk() throws Exception {
        String body = """
                {
                    "name": "calc",
                    "type": "api",
                    "endpoint": "https://calc.example.com",
                    "paramSchema": {"type":"object","properties":{"expr":{"type":"string"}}}
                }
                """;

        ToolVO vo = buildVO(1L, "calc", "api");
        vo.setParamSchema(Map.of("type", "object"));
        given(toolService.create(any())).willReturn(vo);

        mockMvc.perform(post("/api/v1/tools")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("calc"));
    }

    private ToolVO buildVO(Long id, String name, String type) {
        ToolVO vo = new ToolVO();
        vo.setId(id);
        vo.setName(name);
        vo.setType(type);
        vo.setEndpoint("https://api.weather.com/v1");
        vo.setDescription("test tool");
        vo.setStatus("enabled");
        vo.setCreateTime(LocalDateTime.of(2026, 4, 18, 12, 0, 0));
        vo.setUpdateTime(LocalDateTime.of(2026, 4, 18, 12, 0, 0));
        return vo;
    }
}
