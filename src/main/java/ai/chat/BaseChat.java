package ai.chat;

import dev.langchain4j.service.Result;
import dev.langchain4j.service.UserMessage;

public interface BaseChat {
    Result<String> chat(@UserMessage String userMessage);
}
