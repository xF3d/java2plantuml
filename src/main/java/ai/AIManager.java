package ai;

import ai.pipeline.ClassPipeline;
import ai.pipeline.MethodPipeline;
import ai.reponse.DiffClassResponse;
import ai.reponse.DiffMethodResponse;
import dev.langchain4j.model.chat.ChatModel;
import file.Config;
import file.FileLogger;
import lombok.Getter;
import translate.structure.complexity.ComplexityClass;
import translate.structure.complexity.ComplexityMethod;
import translate.translator.ComplexityTranslator;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class AIManager {
    private AIManager() {
        Config cfg = Config.getInstance();

        var modelToBuild = cfg.getMainModel();
        if (modelToBuild == null) {
            return;
        }

        this.suggestionModel = modelToBuild.build();

        var verifiersModels = cfg.getVerifyModels();
        for (var verifiersModel : verifiersModels) {
            this.verifiers.add(
                    verifiersModel.build()
            );
        }

        // add at the end the suggestion
        if (cfg.isVerifyWithMain()) {
            this.verifiers.add(suggestionModel);
        }

        FileLogger.LOGGER.info("AI Init with main model:"
                + suggestionModel.defaultRequestParameters().modelName()
                + " and verifiers: "
                + verifiers.stream().map(it -> it.defaultRequestParameters().modelName()).collect(Collectors.joining(", "))
        );
    }

    @Getter
    private static AIManager instance = null;

    public static void init() {
        if (instance == null) instance = new AIManager();
    }

    public static void update() {
        if (instance != null) instance = new AIManager();
    }

    @Getter
    private ChatModel suggestionModel = null;

    @Getter
    private final ArrayList<ChatModel> verifiers = new ArrayList<>();

    public DiffMethodResponse fixMethod(ComplexityClass clazz, ComplexityMethod method) {
        if (suggestionModel == null) throw new IllegalCallerException();

        var pipeline = new MethodPipeline(clazz, method);
        var validated = pipeline.ask();
        if (!validated.isValid()) {
            FileLogger.LOGGER.warning("Fix method suggestion validation error, last error: " + validated.error());
            return DiffMethodResponse.EMPTY;
        }

        return (DiffMethodResponse) validated.parsed();
    }

    public DiffClassResponse fixClass(ComplexityTranslator translator, ComplexityClass clazz) {
        if (suggestionModel == null) throw new IllegalCallerException();

        var pipeline = new ClassPipeline(translator, clazz);
        var validated = pipeline.ask();
        if (!validated.isValid()) {
            FileLogger.LOGGER.warning("Fix class suggestion validation error, last error: " + validated.error());
            return DiffClassResponse.EMPTY;
        }

        return (DiffClassResponse) validated.parsed();
    }

    public static long countWords(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }

        return text.trim().split("\\s+").length;
    }

    public static long estimateTokens(String text) {
        long words = countWords(text);

        // Rough estimate for mixed natural language/code.
        return (long) Math.ceil(words * Config.getInstance().getWordTokenFactor());
    }
}