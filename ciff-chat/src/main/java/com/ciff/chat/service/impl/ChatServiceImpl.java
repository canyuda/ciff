package com.ciff.chat.service.impl;

import com.ciff.agent.dto.AgentVO;
import com.ciff.agent.facade.AgentFacade;
import com.ciff.agent.service.AgentKnowledgeService;
import com.ciff.agent.service.AgentToolService;
import com.ciff.chat.dto.*;
import com.ciff.chat.entity.ChatMessagePO;
import com.ciff.chat.service.ChatMessageService;
import com.ciff.chat.service.ChatService;
import com.ciff.chat.service.ConversationService;
import com.ciff.common.constant.ErrorCode;
import com.ciff.common.enums.ProviderType;
import com.ciff.common.exception.BizException;
import com.ciff.common.http.LlmHttpClient;
import com.ciff.knowledge.entity.KnowledgeChunkPO;
import com.ciff.knowledge.facade.KnowledgeFacade;
import com.ciff.knowledge.service.DocumentService;
import com.ciff.mcp.dto.ToolVO;
import com.ciff.mcp.facade.ToolFacade;
import com.ciff.provider.dto.LlmCallConfig;
import com.ciff.provider.dto.ModelDefaultParam;
import com.ciff.provider.llm.OpenAiSseChunk;
import com.ciff.provider.facade.ProviderFacade;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final int TITLE_MAX_LENGTH = 50;
    private static final int RAG_TOP_N = 3;
    private static final int TOOL_TIMEOUT_SECONDS = 30;

    private final ConversationService conversationService;
    private final ChatMessageService messageService;
    private final AgentFacade agentFacade;
    private final AgentToolService agentToolService;
    private final AgentKnowledgeService agentKnowledgeService;
    private final ProviderFacade providerFacade;
    private final KnowledgeFacade knowledgeFacade;
    private final DocumentService documentService;
    private final ToolFacade toolFacade;
    private final LlmHttpClient llmHttpClient;
    private final ObjectMapper objectMapper;

    @Autowired
    @Qualifier("llmExecutor")
    private ThreadPoolTaskExecutor llmExecutor;

    private final WebClient toolWebClient = WebClient.builder().build();

    // ==================== Non-streaming chat ====================

    @Override
    public ChatResponse chat(ChatRequest request, Long userId) {
        AgentVO agent = requireAgent(request.getAgentId());
        boolean newConversation = request.getConversationId() == null;
        var conversation = newConversation
                ? conversationService.create(agent.getId(), truncateTitle(request.getMessage()), userId)
                : conversationService.getById(request.getConversationId(), userId);

        LlmCallConfig llmConfig = providerFacade.getLlmCallConfig(agent.getModelId());
        ChatMessagePO userMsg = messageService.saveUserMessage(conversation.getId(), request.getMessage());

        List<ChatMessagePO> history = messageService.listByConversationId(conversation.getId());
        List<LlmMessage> messages = buildMessages(agent, history);
        List<KnowledgeChunkPO> ragChunks = enhanceWithRag(agent, request.getMessage(), messages, request.getRagMode());
        List<String> referenceDocs = extractReferenceDocuments(ragChunks);

        List<Map<String, Object>> tools = buildToolsDefinition(agent);

        long start = System.currentTimeMillis();
        String responseBody = callLlm(llmConfig, messages, tools);
        ParsedResponse parsed = parseResponse(llmConfig, responseBody);

        String finalContent = parsed.content;
        TokenUsage finalTokenUsage = parsed.tokenUsage;

        // Single-round tool calling
        if (parsed.hasToolCall) {
            ToolCallResult toolResult = handleToolCall(agent, llmConfig, messages, tools, parsed);
            finalContent = toolResult.content;
            finalTokenUsage = toolResult.tokenUsage;
        }

        int latencyMs = (int) (System.currentTimeMillis() - start);
        List<Long> docIds = extractReferenceDocumentIds(ragChunks);
        ChatMessagePO assistantMsg = messageService.saveAssistantMessage(
                conversation.getId(), finalContent, finalTokenUsage, llmConfig.getModelName(), latencyMs, docIds);

        ChatResponse response = new ChatResponse();
        response.setConversationId(conversation.getId());
        response.setNewConversation(newConversation);
        response.setUserMessageId(userMsg.getId());
        response.setAssistantMessageId(assistantMsg.getId());
        response.setContent(finalContent);
        response.setTokenUsage(finalTokenUsage);
        response.setModelName(llmConfig.getModelName());
        response.setLatencyMs(latencyMs);
        response.setReferenceDocuments(referenceDocs);
        return response;
    }

    // ==================== SSE streaming chat ====================

    @Override
    public SseEmitter streamChat(ChatRequest request, Long userId) {
        SseEmitter emitter = new SseEmitter(180_000L);

        llmExecutor.execute(() -> {
            try {
                AgentVO agent = requireAgent(request.getAgentId());
                boolean newConv = request.getConversationId() == null;
                var conv = newConv
                        ? conversationService.create(agent.getId(), truncateTitle(request.getMessage()), userId)
                        : conversationService.getById(request.getConversationId(), userId);

                emitter.send(SseEmitter.event().name("meta").data(toJson(
                        SseMetaEvent.builder()
                                .conversationId(conv.getId())
                                .newConversation(newConv)
                                .build())));

                LlmCallConfig llmConfig = providerFacade.getLlmCallConfig(agent.getModelId());
                messageService.saveUserMessage(conv.getId(), request.getMessage());

                List<ChatMessagePO> history = messageService.listByConversationId(conv.getId());
                List<LlmMessage> messages = buildMessages(agent, history);
                List<KnowledgeChunkPO> ragChunks = enhanceWithRag(agent, request.getMessage(), messages, request.getRagMode());
                List<String> referenceDocs = extractReferenceDocuments(ragChunks);

                String url = getChatEndpoint(llmConfig);
                Map<String, String> headers = buildHeaders(llmConfig);
                String body = buildOpenAiRequestBody(llmConfig, messages, List.of(), true);

                long start = System.currentTimeMillis();
                StringBuilder fullContent = new StringBuilder();

                llmHttpClient.stream(llmConfig.getProviderName(), url, headers, body, data -> {
                    try {
                        String token = extractOpenAiToken(data);
                        if (token != null && !token.isEmpty()) {
                            fullContent.append(token);
                            emitter.send(SseEmitter.event().name("token").data(toJson(token)));
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

                String content = fullContent.toString();
                int latencyMs = (int) (System.currentTimeMillis() - start);
                TokenUsage tokenUsage = estimateTokenUsage(content);
                List<Long> docIds = extractReferenceDocumentIds(ragChunks);
                messageService.saveAssistantMessage(
                        conv.getId(), content, tokenUsage, llmConfig.getModelName(), latencyMs, docIds);

                emitter.send(SseEmitter.event().name("done").data(toJson(
                        SseDoneEvent.builder()
                                .tokenUsage(SseDoneEvent.SseTokenUsage.builder()
                                        .promptTokens(0)
                                        .completionTokens(tokenUsage.getCompletionTokens())
                                        .build())
                                .latencyMs(latencyMs)
                                .referenceDocuments(referenceDocs)
                                .build())));
                emitter.complete();

            } catch (Exception e) {
                log.warn("SSE stream error: {}", e.getMessage());
                try {
                    emitter.send(SseEmitter.event().name("error")
                            .data(toJson(SseErrorEvent.builder().message(e.getMessage()).build())));
                } catch (Exception ignored) {
                }
                emitter.completeWithError(e);
            }
        });

        emitter.onTimeout(emitter::complete);
        return emitter;
    }

    // ==================== Message building ====================

    private List<LlmMessage> buildMessages(AgentVO agent, List<ChatMessagePO> history) {
        List<LlmMessage> messages = new ArrayList<>();

        // System message
        if (agent.getSystemPrompt() != null && !agent.getSystemPrompt().isBlank()) {
            messages.add(new LlmMessage.Text("system", agent.getSystemPrompt()));
        }

        // History messages (limited by maxContextTurns)
        int maxTurns = getMaxContextTurns(agent);
        List<ChatMessagePO> limited = limitHistory(history, maxTurns);
        for (ChatMessagePO msg : limited) {
            messages.add(new LlmMessage.Text(msg.getRole(), msg.getContent()));
        }

        return messages;
    }

    private int getMaxContextTurns(AgentVO agent) {
        if (agent.getModelParams() != null && agent.getModelParams().getMaxContextTurns() != null) {
            return agent.getModelParams().getMaxContextTurns();
        }
        return 5;
    }

    private List<ChatMessagePO> limitHistory(List<ChatMessagePO> history, int maxTurns) {
        // Count turns: each turn = one user + one assistant message
        // The last entry is always the user message we just saved
        int userCount = 0;
        int cutIndex = 0;
        for (int i = history.size() - 1; i >= 0; i--) {
            if ("user".equals(history.get(i).getRole())) {
                userCount++;
                if (userCount > maxTurns) {
                    cutIndex = i + 1;
                    break;
                }
            }
        }
        return history.subList(cutIndex, history.size());
    }

    // ==================== RAG enhancement ====================

    private List<KnowledgeChunkPO> enhanceWithRag(AgentVO agent, String userMessage,
                                List<LlmMessage> messages, RagMode ragMode) {
        if (ragMode == RagMode.NO_RAG) {
            return List.of();
        }

        List<Long> knowledgeIds = agentKnowledgeService.listKnowledgeIds(agent.getId());
        if (knowledgeIds == null || knowledgeIds.isEmpty()) {
            return List.of();
        }

        try {
            List<KnowledgeChunkPO> chunks = knowledgeFacade.retrieve(
                    userMessage, knowledgeIds, RAG_TOP_N, ragMode == RagMode.RAG_WITH_RERANKER);
            if (chunks == null || chunks.isEmpty()) {
                return List.of();
            }

            String context = chunks.stream()
                    .map(KnowledgeChunkPO::getContent)
                    .collect(Collectors.joining("\n\n"));

            String ragPrompt = "以下是从知识库中检索到的相关内容，请参考这些内容回答用户问题：\n\n"
                    + context + "\n\n---\n请基于以上参考内容回答用户的问题。";

            // Insert RAG context as a system message before the user messages
            messages.add(1, new LlmMessage.Text("system", ragPrompt));

            return chunks;

        } catch (Exception e) {
            log.warn("RAG retrieval failed, falling back to direct chat: {}", e.getMessage());
            return List.of();
        }
    }

    private List<Long> extractReferenceDocumentIds(List<KnowledgeChunkPO> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }
        return chunks.stream()
                .map(KnowledgeChunkPO::getDocumentId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private List<String> extractReferenceDocuments(List<KnowledgeChunkPO> chunks) {
        List<Long> docIds = extractReferenceDocumentIds(chunks);
        if (docIds.isEmpty()) {
            return List.of();
        }
        Map<Long, String> nameMap = documentService.getDocumentNamesByIds(docIds);
        return docIds.stream()
                .map(nameMap::get)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    // ==================== Tool calling ====================

    private List<Map<String, Object>> buildToolsDefinition(AgentVO agent) {
        List<Long> toolIds = agentToolService.listToolIds(agent.getId());
        if (toolIds == null || toolIds.isEmpty()) {
            return List.of();
        }

        List<ToolVO> tools = toolFacade.listByIds(toolIds);
        if (tools == null || tools.isEmpty()) {
            return List.of();
        }

        return tools.stream()
                .filter(t -> t.getParamSchema() != null)
                .map(t -> {
                    Map<String, Object> function = new LinkedHashMap<>();
                    function.put("name", t.getName());
                    function.put("description", t.getDescription() != null ? t.getDescription() : "");
                    function.put("parameters", t.getParamSchema());
                    return Map.<String, Object>of("type", "function", "function", function);
                })
                .toList();
    }

    private ToolCallResult handleToolCall(AgentVO agent, LlmCallConfig llmConfig,
                                           List<LlmMessage> messages,
                                           List<Map<String, Object>> tools,
                                           ParsedResponse parsed) {
        try {
            // Find matching tool
            List<Long> toolIds = agentToolService.listToolIds(agent.getId());
            List<ToolVO> agentTools = toolFacade.listByIds(toolIds);
            Map<String, ToolVO> toolMap = agentTools.stream()
                    .collect(Collectors.toMap(ToolVO::getName, t -> t, (a, b) -> a));

            ToolVO tool = toolMap.get(parsed.toolCallName);
            if (tool == null) {
                log.warn("Tool not found: {}", parsed.toolCallName);
                return new ToolCallResult(
                        "Tool '" + parsed.toolCallName + "' not found", parsed.tokenUsage);
            }

            // Execute tool
            String toolResult = executeToolApi(tool, parsed.toolCallArguments);

            // Append tool call + result to messages, call LLM again
            messages.add(new LlmMessage.ToolCall("assistant", List.of(
                    ToolCallEntry.builder()
                            .id(parsed.toolCallId)
                            .type("function")
                            .function(ToolCallEntry.FunctionDef.builder()
                                    .name(parsed.toolCallName)
                                    .arguments(parsed.toolCallArguments)
                                    .build())
                            .build())));
            messages.add(new LlmMessage.ToolResult("tool", parsed.toolCallId, toolResult));

            String responseBody = callLlm(llmConfig, messages, tools);
            ParsedResponse finalParsed = parseResponse(llmConfig, responseBody);

            return new ToolCallResult(finalParsed.content, finalParsed.tokenUsage);

        } catch (Exception e) {
            log.warn("Tool execution failed, degrading to direct response: {}", e.getMessage());
            return new ToolCallResult(
                    "Tool call failed: " + e.getMessage(), parsed.tokenUsage);
        }
    }

    private String executeToolApi(ToolVO tool, String arguments) {
        log.debug("Tool API call - uri: {}, headers: {{}}, body: {}", tool.getEndpoint(), "Content-Type=application/json", arguments);
        try {
            return toolWebClient.post()
                    .uri(tool.getEndpoint())
                    .header("Content-Type", "application/json")
                    .bodyValue(arguments)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(TOOL_TIMEOUT_SECONDS))
                    .block();
        } catch (Exception e) {
            throw new RuntimeException("Tool API call failed: " + e.getMessage(), e);
        }
    }

    // ==================== LLM calling ====================

    private String callLlm(LlmCallConfig config, List<LlmMessage> messages,
                            List<Map<String, Object>> tools) {
        String url = getChatEndpoint(config);
        Map<String, String> headers = buildHeaders(config);
        String body = buildOpenAiRequestBody(config, messages, tools, false);
        return llmHttpClient.post(config.getProviderName(), url, headers, body);
    }

    private String getChatEndpoint(LlmCallConfig config) {
        String baseUrl = config.getApiBaseUrl().replaceAll("/+$", "");
        if (config.getProviderType() == ProviderType.CLAUDE) {
            return baseUrl + "/v1/messages";
        }
        return baseUrl + "/v1/chat/completions";
    }

    private Map<String, String> buildHeaders(LlmCallConfig config) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json");

        if (config.getProviderType() == ProviderType.CLAUDE) {
            headers.put("x-api-key", config.getApiKey());
            headers.put("anthropic-version", "2023-06-01");
        } else {
            headers.put("Authorization", "Bearer " + config.getApiKey());
        }
        return headers;
    }

    private String buildOpenAiRequestBody(LlmCallConfig config,
                                           List<LlmMessage> messages,
                                           List<Map<String, Object>> tools,
                                           boolean stream) {
        BigDecimal temperature = null;
        Integer maxTokens = config.getMaxTokens();
        if (config.getDefaultParams() != null) {
            ModelDefaultParam dp = config.getDefaultParams();
            if (dp.getTemperature() != null) temperature = dp.getTemperature();
            if (dp.getMaxTokens() != null) maxTokens = dp.getMaxTokens();
        }

        OpenAiChatRequest request = OpenAiChatRequest.builder()
                .model(config.getModelName())
                .temperature(temperature)
                .maxTokens(maxTokens)
                .stream(stream)
                .messages(messages)
                .tools(tools != null && !tools.isEmpty() ? tools : null)
                .build();

        return toJson(request);
    }

    // ==================== Response parsing ====================

    private ParsedResponse parseResponse(LlmCallConfig config, String responseBody) {
        try {
            if (config.getProviderType() == ProviderType.CLAUDE) {
                return parseClaudeResponse(responseBody);
            }
            return parseOpenAiResponse(responseBody);
        } catch (Exception e) {
            log.warn("Failed to parse LLM response: {}", e.getMessage());
            return new ParsedResponse(responseBody, new TokenUsage(0, 0),
                    false, null, null, null);
        }
    }

    private ParsedResponse parseOpenAiResponse(String responseBody) {
        try {
            OpenAiChatResponse resp = objectMapper.readValue(responseBody, OpenAiChatResponse.class);

            String content = "";
            TokenUsage tokenUsage = new TokenUsage(0, 0);
            boolean hasToolCall = false;
            String toolCallId = null;
            String toolCallName = null;
            String toolCallArguments = null;

            if (resp.getChoices() != null && !resp.getChoices().isEmpty()) {
                OpenAiChatResponse.Message message = resp.getChoices().get(0).getMessage();
                if (message != null) {
                    content = message.getContent() != null ? message.getContent() : "";

                    List<OpenAiChatResponse.ToolCall> toolCalls = message.getToolCalls();
                    if (toolCalls != null && !toolCalls.isEmpty()) {
                        hasToolCall = true;
                        OpenAiChatResponse.ToolCall tc = toolCalls.get(0);
                        toolCallId = tc.getId();
                        toolCallName = tc.getFunction().getName();
                        toolCallArguments = tc.getFunction().getArguments();
                    }
                }
            }

            if (resp.getUsage() != null) {
                tokenUsage = new TokenUsage(
                        resp.getUsage().getPromptTokens(),
                        resp.getUsage().getCompletionTokens());
            }

            return new ParsedResponse(content, tokenUsage, hasToolCall,
                    toolCallId, toolCallName, toolCallArguments);
        } catch (Exception e) {
            log.warn("Failed to parse OpenAI response: {}", e.getMessage());
            return new ParsedResponse(responseBody, new TokenUsage(0, 0),
                    false, null, null, null);
        }
    }

    private ParsedResponse parseClaudeResponse(String responseBody) {
        try {
            ClaudeChatResponse resp = objectMapper.readValue(responseBody, ClaudeChatResponse.class);

            String content = "";
            TokenUsage tokenUsage = new TokenUsage(0, 0);
            boolean hasToolCall = false;
            String toolCallId = null;
            String toolCallName = null;
            String toolCallArguments = null;

            if (resp.getContent() != null && !resp.getContent().isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (ClaudeChatResponse.ContentBlock block : resp.getContent()) {
                    String type = block.getType();
                    if ("text".equals(type)) {
                        sb.append(block.getText());
                    } else if ("tool_use".equals(type)) {
                        hasToolCall = true;
                        toolCallId = block.getId();
                        toolCallName = block.getName();
                        try {
                            toolCallArguments = objectMapper.writeValueAsString(block.getInput());
                        } catch (Exception e) {
                            toolCallArguments = "{}";
                        }
                    }
                }
                content = sb.toString();
            }

            if (resp.getUsage() != null) {
                tokenUsage = new TokenUsage(
                        resp.getUsage().getInputTokens(),
                        resp.getUsage().getOutputTokens());
            }

            return new ParsedResponse(content, tokenUsage, hasToolCall,
                    toolCallId, toolCallName, toolCallArguments);
        } catch (Exception e) {
            log.warn("Failed to parse Claude response: {}", e.getMessage());
            return new ParsedResponse(responseBody, new TokenUsage(0, 0),
                    false, null, null, null);
        }
    }

    // ==================== SSE helpers ====================

    private String extractOpenAiToken(String data) {
        try {
            OpenAiSseChunk chunk = objectMapper.readValue(data, OpenAiSseChunk.class);
            if (chunk.getChoices() != null && !chunk.getChoices().isEmpty()) {
                OpenAiSseChunk.Choice choice = chunk.getChoices().get(0);
                if (choice.getDelta() != null && choice.getDelta().getContent() != null) {
                    return choice.getDelta().getContent();
                }
            }
        } catch (Exception e) {
            log.debug("Failed to parse SSE token: {}", data);
        }
        return null;
    }

    private TokenUsage estimateTokenUsage(String content) {
        // Rough estimate: ~4 chars per token for English, ~2 chars per token for Chinese
        int estimatedTokens = Math.max(1, content.length() / 3);
        return new TokenUsage(0, estimatedTokens);
    }

    // ==================== Utility ====================

    private AgentVO requireAgent(Long agentId) {
        AgentVO agent = agentFacade.getById(agentId);
        if (agent == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Agent 不存在: " + agentId);
        }
        return agent;
    }

    private String truncateTitle(String message) {
        if (message == null || message.isBlank()) {
            return "新对话";
        }
        String title = message.replaceAll("\\s+", " ").trim();
        return title.length() > TITLE_MAX_LENGTH
                ? title.substring(0, TITLE_MAX_LENGTH) + "..." : title;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize object", e);
        }
    }

    // ==================== Inner records ====================

    private record ParsedResponse(
            String content,
            TokenUsage tokenUsage,
            boolean hasToolCall,
            String toolCallId,
            String toolCallName,
            String toolCallArguments
    ) {}

    private record ToolCallResult(String content, TokenUsage tokenUsage) {}
}
