package ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import lombok.Getter;
import translate.translator.Config;

import java.util.ArrayList;

public class AIManager {
    private AIManager() {
        if (Config.getInstance().isAiDisabled()) return;

        String url = Config.getInstance().getOllamaUrl();
        String main = Config.getInstance().getMainModel();
        OllamaChatModel.OllamaChatModelBuilder builder = OllamaChatModel.builder().baseUrl(url);

        this.suggestionModel = builder.modelName(main)
                .build();

        var verifiersModels = Config.getInstance().getVerifyModels();
        for (String verifiersModel : verifiersModels) {
            this.verifiers.add(
                    builder.modelName(verifiersModel).build()
            );
        }

        // add at the end the suggestion
        if (Config.getInstance().isVerifyWithMain()) {
            this.verifiers.add(
                    builder.modelName(main).build()
            );
        }
    }

    @Getter
    private static AIManager instance = null;

    public static void init() {
        if (instance == null) instance = new AIManager();
    }

    public static void update() {
        if (instance != null) instance = new AIManager();
    }

    private ChatModel suggestionModel = null;
    private ArrayList<ChatModel> verifiers = new ArrayList<>();

    public void chat() {
        if (suggestionModel == null) throw new IllegalCallerException();


    }
}
