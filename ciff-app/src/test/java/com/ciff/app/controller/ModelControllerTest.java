package com.ciff.app.controller;

import com.ciff.common.dto.PageResult;
import com.ciff.provider.controller.ModelController;
import com.ciff.provider.dto.ModelVO;
import com.ciff.provider.service.ModelService;
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

@WebMvcTest(ModelController.class)
class ModelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ModelService modelService;

    @Test
    void create_whenInputValid_shouldReturnOk() throws Exception {
        String body = """
                {
                    "providerId": 1,
                    "name": "gpt-4o",
                    "displayName": "GPT-4o",
                    "maxTokens": 128000
                }
                """;

        ModelVO vo = buildVO(1L, 1L, "OpenAI", "gpt-4o", "GPT-4o");
        given(modelService.create(any())).willReturn(vo);

        mockMvc.perform(post("/api/v1/models")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("gpt-4o"))
                .andExpect(jsonPath("$.data.providerName").value("OpenAI"));
    }

    @Test
    void create_whenProviderIdNull_shouldReturn400() throws Exception {
        String body = """
                {
                    "name": "gpt-4o"
                }
                """;

        mockMvc.perform(post("/api/v1/models")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_whenNameBlank_shouldReturn400() throws Exception {
        String body = """
                {
                    "providerId": 1,
                    "name": ""
                }
                """;

        mockMvc.perform(post("/api/v1/models")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_whenInputValid_shouldReturnOk() throws Exception {
        String body = """
                {
                    "displayName": "GPT-4o Mini"
                }
                """;

        ModelVO vo = buildVO(1L, 1L, "OpenAI", "gpt-4o", "GPT-4o Mini");
        given(modelService.update(eq(1L), any())).willReturn(vo);

        mockMvc.perform(put("/api/v1/models/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("GPT-4o Mini"));
    }

    @Test
    void getById_shouldReturnOk() throws Exception {
        ModelVO vo = buildVO(1L, 1L, "OpenAI", "gpt-4o", "GPT-4o");
        given(modelService.getById(1L)).willReturn(vo);

        mockMvc.perform(get("/api/v1/models/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("gpt-4o"));
    }

    @Test
    void delete_shouldReturnOk() throws Exception {
        doNothing().when(modelService).delete(1L);

        mockMvc.perform(delete("/api/v1/models/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void page_withProviderId_shouldReturnPageResult() throws Exception {
        ModelVO vo = buildVO(1L, 1L, "OpenAI", "gpt-4o", "GPT-4o");
        PageResult<ModelVO> pageResult = new PageResult<>(List.of(vo), 1L, 1, 20);
        given(modelService.page(any(), any(), eq(1L), eq("active"))).willReturn(pageResult);

        mockMvc.perform(get("/api/v1/models")
                        .param("providerId", "1")
                        .param("status", "active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void listByProviderId_shouldReturnList() throws Exception {
        ModelVO vo = buildVO(1L, 1L, "OpenAI", "gpt-4o", "GPT-4o");
        given(modelService.listByProviderId(1L)).willReturn(List.of(vo));

        mockMvc.perform(get("/api/v1/models/providers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("gpt-4o"));
    }

    private ModelVO buildVO(Long id, Long providerId, String providerName,
                            String name, String displayName) {
        ModelVO vo = new ModelVO();
        vo.setId(id);
        vo.setProviderId(providerId);
        vo.setProviderName(providerName);
        vo.setName(name);
        vo.setDisplayName(displayName);
        vo.setMaxTokens(128000);
        vo.setStatus("active");
        vo.setCreateTime(LocalDateTime.of(2026, 4, 17, 12, 0, 0));
        vo.setUpdateTime(LocalDateTime.of(2026, 4, 17, 12, 0, 0));
        return vo;
    }
}
