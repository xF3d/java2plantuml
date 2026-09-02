package ai.chat;

import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.service.Result;

public interface BaseChat {
    Result<String> chat(TextContent userMessage);
}
