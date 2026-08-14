package ai.chat;

import dev.langchain4j.service.UserMessage;

public interface BaseChat {
    String chat(@UserMessage String userMessage);
}
