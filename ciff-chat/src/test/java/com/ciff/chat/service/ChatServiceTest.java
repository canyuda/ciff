package com.ciff.chat.service;

import com.ciff.agent.dto.AgentModelParam;
import com.ciff.agent.dto.AgentVO;
import com.ciff.agent.facade.AgentFacade;
import com.ciff.agent.service.AgentKnowledgeService;
import com.ciff.agent.service.AgentToolService;
import com.ciff.chat.dto.*;
import com.ciff.chat.entity.ChatMessagePO;
import com.ciff.chat.service.impl.ChatServiceImpl;
import com.ciff.common.enums.ProviderType;
import com.ciff.common.http.LlmHttpClient;
import com.ciff.knowledge.entity.KnowledgeChunkPO;
import com.ciff.knowledge.facade.KnowledgeFacade;
import com.ciff.mcp.facade.ToolFacade;
import com.ciff.provider.dto.LlmCallConfig;
import com.ciff.provider.dto.ModelDefaultParam;
import com.ciff.provider.facade.ProviderFacade;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @InjectMocks
    private ChatServiceImpl chatService;

    @Mock
    private ConversationService conversationService;

    @Mock
    private ChatMessageService messageService;

    @Mock
    private AgentFacade agentFacade;

    @Mock
    private AgentToolService agentToolService;

    @Mock
    private AgentKnowledgeService agentKnowledgeService;

    @Mock
    private ProviderFacade providerFacade;

    @Mock
    private KnowledgeFacade knowledgeFacade;

    @Mock
    private ToolFacade toolFacade;

    @Mock
    private LlmHttpClient llmHttpClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(chatService, "objectMapper", objectMapper);
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(10);
        executor.initialize();
        ReflectionTestUtils.setField(chatService, "llmExecutor", executor);
    }

    @Test
    void chat_basicDialog_shouldReturnResponse() {
        // Given
        ChatRequest request = new ChatRequest();
        request.setAgentId(1L);
        request.setMessage("Hello");

        AgentVO agent = buildAgent(1L, "test-agent");
        given(agentFacade.getById(1L)).willReturn(agent);

        ConversationVO conv = buildConversation(10L, "Hello");
        given(conversationService.create(eq(1L), any(), eq(100L))).willReturn(conv);

        LlmCallConfig llmConfig = buildLlmConfig();
        given(providerFacade.getLlmCallConfig(10L)).willReturn(llmConfig);

        ChatMessagePO userMsg = buildMessage(100L, "user", "Hello");
        given(messageService.saveUserMessage(eq(10L), eq("Hello"))).willReturn(userMsg);

        given(messageService.listByConversationId(10L)).willReturn(List.of(userMsg));
        given(agentKnowledgeService.listKnowledgeIds(1L)).willReturn(List.of());
        given(agentToolService.listToolIds(1L)).willReturn(List.of());

        String llmResponse = """
                {"choices":[{"message":{"role":"assistant","content":"Hi there!"}}],"usage":{"prompt_tokens":5,"completion_tokens":3}}
                """;
        given(llmHttpClient.post(eq("TestProvider"), anyString(), anyMap(), anyString()))
                .willReturn(llmResponse);

        ChatMessagePO assistantMsg = buildMessage(101L, "assistant", "Hi there!");
        given(messageService.saveAssistantMessage(eq(10L), eq("Hi there!"), any(), eq("gpt-4o"), anyInt()))
                .willReturn(assistantMsg);

        // When
        ChatResponse response = chatService.chat(request, 100L);

        // Then
        assertNotNull(response);
        assertEquals(10L, response.getConversationId());
        assertTrue(response.isNewConversation());
        assertEquals("Hi there!", response.getContent());
        assertEquals("gpt-4o", response.getModelName());
    }

    @Test
    void chat_withExistingConversation_shouldReuseConversation() {
        ChatRequest request = new ChatRequest();
        request.setAgentId(1L);
        request.setMessage("Follow up");
        request.setConversationId(10L);

        AgentVO agent = buildAgent(1L, "test-agent");
        given(agentFacade.getById(1L)).willReturn(agent);

        ConversationVO conv = buildConversation(10L, "Hello");
        given(conversationService.getById(10L, 100L)).willReturn(conv);

        LlmCallConfig llmConfig = buildLlmConfig();
        given(providerFacade.getLlmCallConfig(10L)).willReturn(llmConfig);

        ChatMessagePO userMsg = buildMessage(100L, "user", "Follow up");
        given(messageService.saveUserMessage(eq(10L), eq("Follow up"))).willReturn(userMsg);

        ChatMessagePO prevUserMsg = buildMessage(98L, "user", "Hello");
        ChatMessagePO prevAsstMsg = buildMessage(99L, "assistant", "Hi there!");
        given(messageService.listByConversationId(10L))
                .willReturn(List.of(prevUserMsg, prevAsstMsg, userMsg));
        given(agentKnowledgeService.listKnowledgeIds(1L)).willReturn(List.of());
        given(agentToolService.listToolIds(1L)).willReturn(List.of());

        String llmResponse = """
                {"choices":[{"message":{"role":"assistant","content":"Sure!"}}],"usage":{"prompt_tokens":10,"completion_tokens":2}}
                """;
        given(llmHttpClient.post(anyString(), anyString(), anyMap(), anyString()))
                .willReturn(llmResponse);

        ChatMessagePO assistantMsg = buildMessage(102L, "assistant", "Sure!");
        given(messageService.saveAssistantMessage(eq(10L), eq("Sure!"), any(), eq("gpt-4o"), anyInt()))
                .willReturn(assistantMsg);

        ChatResponse response = chatService.chat(request, 100L);

        assertFalse(response.isNewConversation());
        assertEquals("Sure!", response.getContent());
    }

    @Test
    void chat_agentNotFound_shouldThrow() {
        ChatRequest request = new ChatRequest();
        request.setAgentId(999L);
        request.setMessage("Hello");

        given(agentFacade.getById(999L)).willReturn(null);

        assertThrows(Exception.class, () -> chatService.chat(request, 100L));
    }

    @Test
    void chat_withRag_shouldInjectContext() {
        ChatRequest request = new ChatRequest();
        request.setAgentId(1L);
        request.setMessage("What is Ciff?");

        AgentVO agent = buildAgent(1L, "test-agent");
        given(agentFacade.getById(1L)).willReturn(agent);

        ConversationVO conv = buildConversation(10L, "What is Ciff?");
        given(conversationService.create(eq(1L), any(), eq(100L))).willReturn(conv);

        LlmCallConfig llmConfig = buildLlmConfig();
        given(providerFacade.getLlmCallConfig(10L)).willReturn(llmConfig);

        ChatMessagePO userMsg = buildMessage(100L, "user", "What is Ciff?");
        given(messageService.saveUserMessage(eq(10L), eq("What is Ciff?"))).willReturn(userMsg);
        given(messageService.listByConversationId(10L)).willReturn(List.of(userMsg));

        given(agentKnowledgeService.listKnowledgeIds(1L)).willReturn(List.of(50L));

        KnowledgeChunkPO chunk = new KnowledgeChunkPO();
        chunk.setContent("Ciff is an AI Agent platform");
        chunk.setSimilarity(0.85);
        given(knowledgeFacade.retrieve(eq("What is Ciff?"), eq(List.of(50L)), eq(3)))
                .willReturn(List.of(chunk));

        given(agentToolService.listToolIds(1L)).willReturn(List.of());

        String llmResponse = """
                {"choices":[{"message":{"role":"assistant","content":"Ciff is an AI Agent platform."}}],"usage":{"prompt_tokens":15,"completion_tokens":5}}
                """;
        given(llmHttpClient.post(anyString(), anyString(), anyMap(), anyString()))
                .willReturn(llmResponse);

        ChatMessagePO assistantMsg = buildMessage(101L, "assistant", "Ciff is an AI Agent platform.");
        given(messageService.saveAssistantMessage(anyLong(), anyString(), any(), anyString(), anyInt()))
                .willReturn(assistantMsg);

        ChatResponse response = chatService.chat(request, 100L);

        assertNotNull(response);
        assertEquals("Ciff is an AI Agent platform.", response.getContent());
    }

    @Test
    void chat_withEmptyPrompt_shouldWork() {
        ChatRequest request = new ChatRequest();
        request.setAgentId(1L);
        request.setMessage("Hello");

        AgentVO agent = buildAgent(1L, "test-agent");
        agent.setSystemPrompt(null);
        given(agentFacade.getById(1L)).willReturn(agent);

        ConversationVO conv = buildConversation(10L, "Hello");
        given(conversationService.create(eq(1L), any(), eq(100L))).willReturn(conv);

        LlmCallConfig llmConfig = buildLlmConfig();
        given(providerFacade.getLlmCallConfig(10L)).willReturn(llmConfig);

        ChatMessagePO userMsg = buildMessage(100L, "user", "Hello");
        given(messageService.saveUserMessage(eq(10L), eq("Hello"))).willReturn(userMsg);
        given(messageService.listByConversationId(10L)).willReturn(List.of(userMsg));
        given(agentKnowledgeService.listKnowledgeIds(1L)).willReturn(List.of());
        given(agentToolService.listToolIds(1L)).willReturn(List.of());

        String llmResponse = """
                {"choices":[{"message":{"role":"assistant","content":"Hello!"}}],"usage":{"prompt_tokens":3,"completion_tokens":2}}
                """;
        given(llmHttpClient.post(anyString(), anyString(), anyMap(), anyString()))
                .willReturn(llmResponse);

        ChatMessagePO assistantMsg = buildMessage(101L, "assistant", "Hello!");
        given(messageService.saveAssistantMessage(anyLong(), anyString(), any(), anyString(), anyInt()))
                .willReturn(assistantMsg);

        ChatResponse response = chatService.chat(request, 100L);
        assertNotNull(response);
    }

    // ==================== Helper methods ====================

    private AgentVO buildAgent(Long id, String name) {
        AgentVO vo = new AgentVO();
        vo.setId(id);
        vo.setName(name);
        vo.setType("chatbot");
        vo.setModelId(10L);
        vo.setSystemPrompt("You are helpful");
        AgentModelParam params = new AgentModelParam();
        params.setMaxContextTurns(5);
        vo.setModelParams(params);
        return vo;
    }

    private ConversationVO buildConversation(Long id, String title) {
        ConversationVO vo = new ConversationVO();
        vo.setId(id);
        vo.setAgentId(1L);
        vo.setTitle(title);
        vo.setStatus("active");
        return vo;
    }

    private ChatMessagePO buildMessage(Long id, String role, String content) {
        ChatMessagePO po = new ChatMessagePO();
        po.setId(id);
        po.setConversationId(10L);
        po.setRole(role);
        po.setContent(content);
        po.setCreateTime(LocalDateTime.now());
        return po;
    }

    private LlmCallConfig buildLlmConfig() {
        LlmCallConfig config = new LlmCallConfig();
        config.setProviderId(1L);
        config.setProviderName("TestProvider");
        config.setProviderType(ProviderType.OPENAI);
        config.setApiBaseUrl("https://api.openai.com");
        config.setApiKey("test-key");
        config.setModelName("gpt-4o");
        config.setMaxTokens(4096);
        ModelDefaultParam defaultParams = new ModelDefaultParam();
        defaultParams.setTemperature(new BigDecimal("0.7"));
        config.setDefaultParams(defaultParams);
        return config;
    }
}
