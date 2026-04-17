package com.ciff.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProviderType {

    OPENAI("openai", "OpenAI"),
    CLAUDE("claude", "Claude"),
    GEMINI("gemini", "Gemini"),
    OLLAMA("ollama", "Ollama"),
    DEEPSEEK("deepseek", "DeepSeek"),
    QWEN("qwen", "通义千问"),
    ZHIPU("zhipu", "智谱"),
    KIMI("kimi", "Kimi"),
    WENXIN("wenxin", "文心一言"),
    DOUBAO("doubao", "豆包"),
    HUNYUAN("hunyuan", "混元"),
    YI("yi", "零一万物"),
    MINIMAX("minimax", "MiniMax"),
    SPARK("spark", "星火");
    @JsonValue
    @EnumValue
    private final String type;
    private final String displayName;
}
