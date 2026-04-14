package com.ciff.app.controller;

import com.ciff.app.dto.DemoItemCreateRequest;
import com.ciff.app.dto.DemoItemUpdateRequest;
import com.ciff.app.dto.DemoItemVO;
import com.ciff.app.service.DemoItemService;
import com.ciff.common.dto.PageResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
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

@WebMvcTest(DemoItemController.class)
class DemoItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DemoItemService demoItemService;

    @Test
    void create_whenInputValid_shouldReturnOk() throws Exception {
        // Given
        DemoItemCreateRequest request = new DemoItemCreateRequest();
        request.setName("test");
        request.setStatus(0);

        DemoItemVO vo = buildVO(1L, "test", 0);
        given(demoItemService.create(any(DemoItemCreateRequest.class))).willReturn(vo);

        // When & Then
        mockMvc.perform(post("/api/v1/demo-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("test"))
                .andExpect(jsonPath("$.data.status").value(0));
    }

    @Test
    void create_whenNameBlank_shouldReturn400() throws Exception {
        // Given
        DemoItemCreateRequest request = new DemoItemCreateRequest();
        request.setName("");
        request.setStatus(0);

        // When & Then
        mockMvc.perform(post("/api/v1/demo-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_whenStatusNull_shouldReturn400() throws Exception {
        // Given
        DemoItemCreateRequest request = new DemoItemCreateRequest();
        request.setName("test");
        request.setStatus(null);

        // When & Then
        mockMvc.perform(post("/api/v1/demo-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_whenInputValid_shouldReturnOk() throws Exception {
        // Given
        DemoItemUpdateRequest request = new DemoItemUpdateRequest();
        request.setName("updated");

        DemoItemVO vo = buildVO(1L, "updated", 1);
        given(demoItemService.update(eq(1L), any(DemoItemUpdateRequest.class))).willReturn(vo);

        // When & Then
        mockMvc.perform(put("/api/v1/demo-items/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("updated"));
    }

    @Test
    void getById_shouldReturnOk() throws Exception {
        // Given
        DemoItemVO vo = buildVO(1L, "test", 0);
        given(demoItemService.getById(1L)).willReturn(vo);

        // When & Then
        mockMvc.perform(get("/api/v1/demo-items/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("test"));
    }

    @Test
    void delete_shouldReturnOk() throws Exception {
        // Given
        doNothing().when(demoItemService).delete(1L);

        // When & Then
        mockMvc.perform(delete("/api/v1/demo-items/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void page_shouldReturnPageResult() throws Exception {
        // Given
        DemoItemVO vo = buildVO(1L, "test", 0);
        PageResult<DemoItemVO> pageResult = new PageResult<>(List.of(vo), 1L, 1, 20);
        given(demoItemService.page(anyInt(), anyInt())).willReturn(pageResult);

        // When & Then
        mockMvc.perform(get("/api/v1/demo-items")
                        .param("page", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    private DemoItemVO buildVO(Long id, String name, Integer status) {
        DemoItemVO vo = new DemoItemVO();
        vo.setId(id);
        vo.setName(name);
        vo.setStatus(status);
        vo.setCreateTime(LocalDateTime.of(2026, 4, 14, 12, 0, 0));
        vo.setUpdateTime(LocalDateTime.of(2026, 4, 14, 12, 0, 0));
        return vo;
    }
}
