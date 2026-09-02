package ai.pipeline;

import ai.*;
import ai.chat.BaseChat;
import ai.reponse.MessageValidator;
import ai.reponse.ValidatedResponse;
import dev.langchain4j.service.AiServices;
import file.Config;
import translate.structure.complexity.ComplexityClass;
import translate.structure.pkg.PackageManager;
import translate.translator.SimpleTranslator;

import java.util.ArrayList;
import java.util.List;

public class ClassPipeline extends AIPipeline {
    private final ComplexityClass clazz;
    private final CodeAccessTool tool;

    public ClassPipeline(SimpleTranslator translator, ComplexityClass clazz) {
        this.clazz = clazz;
        this.tool = new CodeAccessTool(translator);
        String origin = PackageManager.resolveTypeDeclaration(clazz.getNode());

        super(origin, MessageValidator.CLASS);
    }

    @Override
    protected BaseChat initSuggestion() {
        var suggestionModel = AIManager.getInstance().getSuggestionModel();
        var builder = AiServices.builder(BaseChat.class)
                .chatModel(suggestionModel)
                .systemMessage(Prompts.SYSTEM_REFACTOR_CLASS_GEN);

        if (Config.getInstance().isToolCalling()) {
            builder.tools(tool);
        }

        return builder
                //.tools(tool)
                .build();
    }

    @Override
    protected List<BaseChat> initVerifiers() {
        ArrayList<BaseChat> output = new ArrayList<>();
        var verifiersModels = AIManager.getInstance().getVerifiers();
        for (var verifierModel : verifiersModels) {
            var builder = AiServices.builder(BaseChat.class)
                    .chatModel(verifierModel)
                    .systemMessage(Prompts.SYSTEM_REFACTOR_CLASS_VER);

            if (Config.getInstance().isToolCalling()) {
                builder.tools(tool);
            }

            output.add(builder.build());
        }

        return output;
    }

    @Override
    public ValidatedResponse ask() {
        String mainMessage = Prompts.formattedClassContext(clazz);
        return message(mainMessage);
    }
}
