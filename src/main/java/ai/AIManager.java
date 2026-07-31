package ai;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import file.FileLogger;
import lombok.Getter;
import translate.structure.complexity.ComplexityClass;
import translate.structure.complexity.ComplexityMethod;
import file.Config;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class AIManager {
    private AIManager() {

        Config cfg = Config.getInstance();
        if (cfg.isAiDisabled()) return;

        this.suggestionModel = cfg.getMainModel().build();

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

    private ChatModel suggestionModel = null;
    private final ArrayList<ChatModel> verifiers = new ArrayList<>();

    public DiffPromptResponse fixMethod(ComplexityClass clazz, ComplexityMethod method) {
        if (suggestionModel == null) throw new IllegalCallerException();

        CompilationUnit cu = clazz.getNode().findCompilationUnit().orElseThrow();
        ArrayList<String> imports = new ArrayList<>(cu.getImports().size());
        for (ImportDeclaration singleImport : cu.getImports()) {
            String importString = singleImport.getNameAsString();
            if (singleImport.isStatic()) {
                importString = "static " + importString;
            }

            if (singleImport.isAsterisk()) {
                importString += ".*";
            }

            imports.add(importString);
        }

        String importString = String.join("\n", imports);

        var decl = clazz.getNode();
        ArrayList<String> methodSignatures = new ArrayList<>(decl.getMethods().size());
        for (var methodDecl : decl.getMethods()) {
            methodSignatures.add(methodDecl.getSignature().asString());
        }

        String methodSignaturesString = String.join("\n", methodSignatures);

        String methodMetrics;
        String classMetrics;
        String methodSmells;
        String classSmells;
        try {
            var mthResult = method.getComplexityResult().get();
            methodMetrics = mthResult.metricsAi();
            methodSmells = mthResult.codeSmellsAi();

            var clzResult = clazz.getComplexityResult().get();
            classMetrics = clzResult.metricsAi();
            classSmells = clzResult.codeSmellsAi();

        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }


        var range = method.getMethod().getRange().orElseThrow();

        String mainMessage = (Prompts.CLASS_CONTEXT + Prompts.METHOD_CONTEXT).formatted(
                importString,
                decl.getNameAsString(),
                methodSignaturesString,
                classMetrics,
                classSmells,
                methodMetrics,
                methodSmells,
                method.getMethod().toString(),
                range.begin,
                range.end
        );

        String response;
        try {
            response = suggestionModel.chat(
                    SystemMessage.systemMessage(Prompts.SYSTEM_PROMPT_GEN.formatted(Config.getInstance().getMainModel())),
                    UserMessage.userMessage(mainMessage)
            ).aiMessage().text();
        } catch (Exception e) {
            FileLogger.LOGGER.severe("Fatal error in generator: " + e.getMessage());
            return DiffPromptResponse.EMPTY;
        }

        System.out.println(response);
        String previousResponse = response;
        for (var verifier : verifiers) {
            String modelName = verifier.defaultRequestParameters().modelName();
            try {
                previousResponse = verifier.chat(
                        SystemMessage.systemMessage(Prompts.SYSTEM_PROMPT_VER.formatted(modelName)),
                        UserMessage.userMessage(mainMessage + "\n" + previousResponse)
                ).aiMessage().text();
            } catch (Exception e) {
                FileLogger.LOGGER.warning("Error in verifier " + e.getMessage());
                continue;
            }

            // TODO: further validation
            System.out.println(previousResponse);
        }

        return DiffPromptResponse.fromResponse(previousResponse);
    }
}
