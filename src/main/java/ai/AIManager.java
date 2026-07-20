package ai;

import dev.langchain4j.model.chat.ChatModel;
import lombok.Getter;

import java.util.ArrayList;

public class AIManager {
    private AIManager() {}

    @Getter
    private static AIManager instance;

    public static void init() {
        instance = new AIManager();
    }

    private final ChatModel suggestionModel = null;
    private final ArrayList<ChatModel> validators = new ArrayList<>();
}
