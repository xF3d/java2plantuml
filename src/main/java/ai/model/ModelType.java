package ai.model;

import dev.langchain4j.model.chat.ChatModel;

public record ModelType(APIType type, String modelName, String url, String apiKey) {
    public ModelType(APIType type, String modelName, String url) {
        this(type, modelName, url, null);
    }

    public boolean isValid() {
        return type != null && modelName != null && url != null;
    }

    public ChatModel build() {
        return type.build(modelName, url, apiKey);
    }

    public static boolean isValid(ModelType type) {
        if (type == null) return false;

        return type.isValid();
    }
}
