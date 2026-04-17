package com.ciff.app.controller;

import com.ciff.common.dto.PageResult;
import com.ciff.provider.controller.ProviderController;
import com.ciff.provider.dto.ProviderVO;
import com.ciff.provider.service.ProviderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProviderController.class)
class ProviderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProviderService providerService;

    @Test
    void create_whenInputValid_shouldReturnOk() throws Exception {
        String body = """
                {
                    "name": "OpenAI",
                    "type": "OPENAI",
                    "authType": "BEARER",
                    "apiBaseUrl": "https://api.openai.com",
                    "apiKey": "sk-test-123"
                }
                """;

        ProviderVO vo = buildVO(1L, "OpenAI", "openai", "bearer");
        given(providerService.create(any())).willReturn(vo);

        mockMvc.perform(post("/api/v1/providers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("OpenAI"))
                .andExpect(jsonPath("$.data.type").value("openai"));
    }

    @Test
    void create_whenNameBlank_shouldReturn400() throws Exception {
        String body = """
                {
                    "name": "",
                    "type": "OPENAI",
                    "authType": "BEARER",
                    "apiBaseUrl": "https://api.openai.com"
                }
                """;

        mockMvc.perform(post("/api/v1/providers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_whenTypeNull_shouldReturn400() throws Exception {
        String body = """
                {
                    "name": "OpenAI",
                    "authType": "BEARER",
                    "apiBaseUrl": "https://api.openai.com"
                }
                """;

        mockMvc.perform(post("/api/v1/providers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_whenTypeIllegal_shouldReturn400() throws Exception {
        String body = """
                {
                    "name": "OpenAI",
                    "type": "INVALID_TYPE",
                    "authType": "BEARER",
                    "apiBaseUrl": "https://api.openai.com"
                }
                """;

        mockMvc.perform(post("/api/v1/providers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_whenAuthTypeNull_shouldReturn400() throws Exception {
        String body = """
                {
                    "name": "OpenAI",
                    "type": "OPENAI",
                    "apiBaseUrl": "https://api.openai.com"
                }
                """;

        mockMvc.perform(post("/api/v1/providers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_whenApiBaseUrlBlank_shouldReturn400() throws Exception {
        String body = """
                {
                    "name": "OpenAI",
                    "type": "OPENAI",
                    "authType": "BEARER",
                    "apiBaseUrl": ""
                }
                """;

        mockMvc.perform(post("/api/v1/providers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_whenInputValid_shouldReturnOk() throws Exception {
        String body = """
                {
                    "name": "OpenAI Updated"
                }
                """;

        ProviderVO vo = buildVO(1L, "OpenAI Updated", "openai", "bearer");
        given(providerService.update(eq(1L), any())).willReturn(vo);

        mockMvc.perform(put("/api/v1/providers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("OpenAI Updated"));
    }

    @Test
    void getById_shouldReturnOk() throws Exception {
        ProviderVO vo = buildVO(1L, "OpenAI", "openai", "bearer");
        given(providerService.getById(1L)).willReturn(vo);

        mockMvc.perform(get("/api/v1/providers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("OpenAI"));
    }

    @Test
    void delete_shouldReturnOk() throws Exception {
        doNothing().when(providerService).delete(1L);

        mockMvc.perform(delete("/api/v1/providers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void page_shouldReturnPageResult() throws Exception {
        ProviderVO vo = buildVO(1L, "OpenAI", "openai", "bearer");
        PageResult<ProviderVO> pageResult = new PageResult<>(List.of(vo), 1L, 1, 20);
        given(providerService.page(any(), any(), eq("active"))).willReturn(pageResult);

        mockMvc.perform(get("/api/v1/providers")
                        .param("page", "1")
                        .param("pageSize", "20")
                        .param("status", "active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void page_withoutStatus_shouldReturnOk() throws Exception {
        PageResult<ProviderVO> pageResult = new PageResult<>(List.of(), 0L, 1, 20);
        given(providerService.page(any(), any(), any())).willReturn(pageResult);

        mockMvc.perform(get("/api/v1/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isMap());
    }

    private ProviderVO buildVO(Long id, String name, String type, String authType) {
        ProviderVO vo = new ProviderVO();
        vo.setId(id);
        vo.setName(name);
        vo.setType(type);
        vo.setTypeDisplayName("OpenAI");
        vo.setAuthType(authType);
        vo.setApiBaseUrl("https://api.openai.com");
        vo.setApiKeyMasked("******");
        vo.setStatus("active");
        vo.setCreateTime(LocalDateTime.of(2026, 4, 17, 12, 0, 0));
        vo.setUpdateTime(LocalDateTime.of(2026, 4, 17, 12, 0, 0));
        return vo;
    }
}
