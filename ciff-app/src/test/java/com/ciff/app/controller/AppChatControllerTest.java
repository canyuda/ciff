package com.ciff.app.controller;

import com.ciff.chat.dto.ChatRequest;
import com.ciff.chat.dto.ChatResponse;
import com.ciff.chat.dto.TokenUsage;
import com.ciff.chat.service.ChatService;
import com.ciff.common.context.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AppChatController.class)
class AppChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatService chatService;

    @BeforeEach
    void setUp() {
        UserContext.setUserId(1L);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void chat_shouldReturnChatResponse() throws Exception {
        ChatResponse response = new ChatResponse();
        response.setConversationId(10L);
        response.setNewConversation(true);
        response.setContent("Hello!");
        response.setModelName("gpt-4o");
        response.setLatencyMs(500);
        response.setTokenUsage(new TokenUsage(10, 5));

        given(chatService.chat(any(ChatRequest.class), eq(1L))).willReturn(response);

        String body = """
                {"agentId": 1, "message": "Hello"}
                """;

        mockMvc.perform(post("/api/v1/app/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.conversationId").value(10))
                .andExpect(jsonPath("$.data.content").value("Hello!"))
                .andExpect(jsonPath("$.data.newConversation").value(true));
    }

    @Test
    void chat_missingMessage_shouldReturn400() throws Exception {
        String body = """
                {"agentId": 1}
                """;

        mockMvc.perform(post("/api/v1/app/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chat_missingAgentId_shouldReturn400() throws Exception {
        String body = """
                {"message": "Hello"}
                """;

        mockMvc.perform(post("/api/v1/app/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chat_withConversationId_shouldPassThrough() throws Exception {
        ChatResponse response = new ChatResponse();
        response.setConversationId(20L);
        response.setNewConversation(false);
        response.setContent("Follow up");

        given(chatService.chat(any(ChatRequest.class), eq(1L))).willReturn(response);

        String body = """
                {"agentId": 1, "message": "Follow up", "conversationId": 20}
                """;

        mockMvc.perform(post("/api/v1/app/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.conversationId").value(20))
                .andExpect(jsonPath("$.data.newConversation").value(false));
    }
}
