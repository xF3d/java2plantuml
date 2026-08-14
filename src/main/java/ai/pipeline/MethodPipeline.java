package ai.pipeline;

import ai.AIManager;
import ai.reponse.MessageValidator;
import ai.Prompts;
import ai.ValidatedResponse;
import ai.chat.BaseChat;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import translate.structure.complexity.ComplexityClass;
import translate.structure.complexity.ComplexityMethod;
import translate.structure.pkg.PackageManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MethodPipeline extends AIPipeline {
    private final ComplexityClass clazz;
    private final ComplexityMethod method;

    public MethodPipeline(ComplexityClass clazz, ComplexityMethod method) {
        this.clazz = clazz;
        this.method = method;
        String origin = PackageManager.resolveTypeDeclaration(clazz.getNode()) + "#" + method.getMethod().raw().getSignature();

        super(origin, MessageValidator.METHOD);
    }

    @Override
    protected BaseChat initSuggestion() {
        ChatModel suggestionModel = AIManager.getInstance().getSuggestionModel();
        return AiServices.builder(BaseChat.class)
                .chatModel(suggestionModel)
                .systemMessage(Prompts.SYSTEM_REFACTOR_METHOD_GEN.formatted(suggestionModel.defaultRequestParameters().modelName()))
                .build();
    }

    @Override
    protected List<BaseChat> initVerifiers() {
        ArrayList<BaseChat> output = new ArrayList<>();
        ArrayList<ChatModel> verifiersModels = AIManager.getInstance().getVerifiers();
        for (ChatModel verifierModel : verifiersModels) {
            output.add(
                    AiServices.builder(BaseChat.class)
                            .chatModel(verifierModel)
                            .systemMessage(Prompts.SYSTEM_REFACTOR_METHOD_VER.formatted(verifierModel.defaultRequestParameters().modelName()))
                            .build()
            );
        }

        return output;
    }

    @Override
    public ValidatedResponse ask() {
        String methodMetrics;
        String methodSmells;
        try {
            var mthResult = method.getComplexityResult().get();
            methodMetrics = mthResult.metricsAi();
            methodSmells = mthResult.codeSmellsAi();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        String mainMessage = Prompts.formattedClassContext(clazz, false) + Prompts.METHOD_CONTEXT.formatted(
                methodMetrics,
                methodSmells,
                method.getMethod().toString()
        );

        return message(mainMessage);
    }
}
