package com.ciff.chat.controller;

import com.ciff.chat.dto.ChatMessageVO;
import com.ciff.chat.dto.ConversationVO;
import com.ciff.chat.dto.TokenUsage;
import com.ciff.chat.service.ChatMessageService;
import com.ciff.chat.service.ConversationService;
import com.ciff.common.context.UserContext;
import com.ciff.common.dto.PageResult;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConversationService conversationService;

    @MockBean
    private ChatMessageService chatMessageService;

    @BeforeEach
    void setUp() {
        UserContext.setUserId(1L);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void page_shouldReturnConversations() throws Exception {
        ConversationVO vo = buildConversationVO(1L, "Test Chat");
        PageResult<ConversationVO> pageResult = new PageResult<>(List.of(vo), 1L, 1, 20);
        given(conversationService.page(any(), any(), any(), eq(1L))).willReturn(pageResult);

        mockMvc.perform(get("/api/v1/conversations")
                        .param("page", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void getById_shouldReturnConversation() throws Exception {
        ConversationVO vo = buildConversationVO(1L, "Test Chat");
        given(conversationService.getById(1L, 1L)).willReturn(vo);

        mockMvc.perform(get("/api/v1/conversations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Test Chat"));
    }

    @Test
    void delete_shouldCascadeMessages() throws Exception {
        doNothing().when(chatMessageService).deleteByConversationId(1L);
        doNothing().when(conversationService).delete(1L, 1L);

        mockMvc.perform(delete("/api/v1/conversations/1"))
                .andExpect(status().isOk());
    }

    @Test
    void pageMessages_shouldReturnMessages() throws Exception {
        ChatMessageVO msg = buildChatMessageVO(1L, "user", "Hello");
        PageResult<ChatMessageVO> pageResult = new PageResult<>(List.of(msg), 1L, 1, 20);
        given(chatMessageService.page(eq(1L), any(), any())).willReturn(pageResult);

        mockMvc.perform(get("/api/v1/conversations/1/messages")
                        .param("page", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    private ConversationVO buildConversationVO(Long id, String title) {
        ConversationVO vo = new ConversationVO();
        vo.setId(id);
        vo.setAgentId(10L);
        vo.setTitle(title);
        vo.setStatus("active");
        vo.setCreateTime(LocalDateTime.of(2026, 4, 20, 12, 0));
        vo.setUpdateTime(LocalDateTime.of(2026, 4, 20, 12, 0));
        return vo;
    }

    private ChatMessageVO buildChatMessageVO(Long id, String role, String content) {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(id);
        vo.setConversationId(1L);
        vo.setRole(role);
        vo.setContent(content);
        vo.setModelName("gpt-4o");
        vo.setCreateTime(LocalDateTime.of(2026, 4, 20, 12, 0));
        return vo;
    }
}
