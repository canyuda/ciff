package com.ciff.app;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
        scanBasePackages = "com.ciff",
        exclude = {
                org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration.class,
                org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionAutoConfiguration.class,
                org.springframework.ai.model.openai.autoconfigure.OpenAiModerationAutoConfiguration.class,
                org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration.class,
                org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration.class,
                org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration.class,
        }
)
@MapperScan("com.ciff.**.mapper")
public class CiffApplication {

    public static void main(String[] args) {
        SpringApplication.run(CiffApplication.class, args);
    }
}