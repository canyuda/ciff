package com.ciff.provider.llm;

/**
 * 流式聊天回调。
 * 支持文本 token 和工具调用两种事件。
 */
public interface StreamCallback {

    /** 收到增量文本 token。 */
    void onToken(String content);

    /** 收到完整的工具调用（流结束后累积完成）。 */
    default void onToolCall(String id, String name, String arguments) {
        // default no-op for backward compatibility
    }
}
