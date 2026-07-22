package ai;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import lombok.Getter;
import translate.structure.complexity.ComplexityClass;
import translate.structure.complexity.ComplexityMethod;
import translate.translator.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

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
    private final ArrayList<ChatModel> verifiers = new ArrayList<>();

    public DiffPromptResponse chat(ComplexityClass clazz, ComplexityMethod method) {
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

        List<String> methodMetrics;
        List<String> classMetrics;
        try {
            methodMetrics = method.getComplexityResult().get().toAiList();
            classMetrics = clazz.getComplexityResult().get().toAiList();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        String methodMetricsString = String.join("\n", methodMetrics);

        String classMetricsString = String.join("\n", classMetrics);

        var range = method.getMethod().getRange().orElseThrow();

        String mainMessage = Config.FORM_REFACTOR_PROMPT.formatted(
                importString,
                decl.getNameAsString(),
                methodSignaturesString,
                classMetricsString,
                methodMetricsString,
                "None", // TODO: add code smells support
                method.getMethod().toString(),
                range.begin,
                range.end
        );

        String response = suggestionModel.chat(
                SystemMessage.systemMessage(Config.SYSTEM_PROMPT_GEN.formatted(Config.getInstance().getMainModel())),
                UserMessage.userMessage(mainMessage)
        ).aiMessage().text();

        System.out.println(response);
        String previousResponse = response;
        for (var verifier : verifiers) {
            previousResponse = verifier.chat(
                    SystemMessage.systemMessage(Config.SYSTEM_PROMPT_VER.formatted(verifier.defaultRequestParameters().modelName())),
                    UserMessage.userMessage(mainMessage + "\n" + previousResponse)
            ).aiMessage().text();

            System.out.println(previousResponse);
        }

        return DiffPromptResponse.fromResponse(previousResponse);
    }
}
