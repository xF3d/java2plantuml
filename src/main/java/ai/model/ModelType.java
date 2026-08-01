package ai.model;

import dev.langchain4j.model.chat.ChatModel;
import org.tomlj.TomlTable;

import java.util.Locale;

public record ModelType(APIType type, String modelName, String url, String apiKey) {
    public ModelType(APIType type, String modelName, String url) {
        this(type, modelName, url, null);
    }

    public static ModelType config(TomlTable table) {
        try {
            APIType verifyType = APIType.valueOf(table.getString("type").toUpperCase(Locale.ROOT));
            String verifyUrl = table.getString("url");
            String verifyName = table.getString("name");
            String verifyKey = table.getString("key");

            return new ModelType(verifyType, verifyName, verifyUrl, verifyKey);
        } catch (Exception ingored) {
            return null;
        }
    }

    public boolean isValid() {
        return type != null && modelName != null && url != null;
    }

    public ChatModel build() {
        return type.build(modelName, url, apiKey);
    }
}
