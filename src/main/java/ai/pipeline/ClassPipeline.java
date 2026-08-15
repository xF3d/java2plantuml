package ai.pipeline;

import ai.*;
import ai.chat.BaseChat;
import ai.reponse.MessageValidator;
import ai.reponse.ValidatedResponse;
import dev.langchain4j.service.AiServices;
import translate.structure.complexity.ComplexityClass;
import translate.structure.pkg.PackageManager;
import translate.translator.SimpleTranslator;

import java.util.ArrayList;
import java.util.List;

public class ClassPipeline extends AIPipeline {
    private final ComplexityClass clazz;
    private final MethodBodyTool tool;

    public ClassPipeline(SimpleTranslator translator, ComplexityClass clazz) {
        this.clazz = clazz;
        this.tool = new MethodBodyTool(translator);
        String origin = PackageManager.resolveTypeDeclaration(clazz.getNode());

        super(origin, MessageValidator.CLASS);
    }

    @Override
    protected BaseChat initSuggestion() {
        var suggestionModel = AIManager.getInstance().getSuggestionModel();
        return AiServices.builder(BaseChat.class)
                .chatModel(suggestionModel)
                .systemMessage(Prompts.SYSTEM_REFACTOR_CLASS_GEN.formatted(suggestionModel.defaultRequestParameters().modelName()))
                .tools(tool)
                .build();
    }

    @Override
    protected List<BaseChat> initVerifiers() {
        ArrayList<BaseChat> output = new ArrayList<>();
        var verifiersModels = AIManager.getInstance().getVerifiers();
        for (var verifierModel : verifiersModels) {
            output.add(
                    AiServices.builder(BaseChat.class)
                            .chatModel(verifierModel)
                            .systemMessage(Prompts.SYSTEM_REFACTOR_CLASS_VER.formatted(verifierModel.defaultRequestParameters().modelName()))
                            .tools(tool)
                            .build()
            );
        }

        return output;
    }

    @Override
    public ValidatedResponse ask() {
        String mainMessage = Prompts.formattedClassContext(clazz, true);
        return message(mainMessage);
    }
}
