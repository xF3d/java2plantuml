package ai.model;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.time.Duration;

public enum APIType {
    OPENAI,
    OLLAMA;

    public ChatModel build(String name, String url, String apiKey) {
        if (this == OLLAMA) return OllamaChatModel.builder()
                .modelName(name)
                .baseUrl(url)
                .timeout(Duration.ofMinutes(10))
                .build();
        return OpenAiChatModel.builder()
                .modelName(name)
                .baseUrl(url)
                .apiKey(apiKey)
                .timeout(Duration.ofMinutes(10))
                .build();
    }
}
