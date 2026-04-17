package com.ciff.provider.llm;

import java.util.function.Consumer;

/**
 * LLM 聊天客户端统一接口。
 * 屏蔽各厂商 API 差异，上层只需关心 request/response。
 */
public interface LlmChatClient {

    /**
     * 同步聊天，阻塞等待完整响应。
     */
    LlmChatResponse chat(LlmChatRequest request);

    /**
     * 流式聊天，每个 SSE chunk 解析后回调。
     * callback 接收的是每个 chunk 解析出的增量 content。
     */
    void streamChat(LlmChatRequest request, Consumer<String> callback);

    /**
     * 连通性探测，不消耗 token。
     * 优先使用各厂商的模型列表接口（如 GET /v1/models），
     * 确认 API Key 有效、网络可达、服务可用。
     */
    void probe();
}
