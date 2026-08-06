package ai;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import file.FileLogger;
import lombok.Getter;
import translate.structure.complexity.ComplexityClass;
import translate.structure.complexity.ComplexityMethod;
import file.Config;
import translate.structure.pkg.PackageManager;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
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



        String origin = PackageManager.resolveTypeDeclaration(clazz.getNode()) + "#" + method.getMethod().getSignature();
        ValidatedResponse response = validatedChat(suggestionModel, origin,
                SystemMessage.systemMessage(Prompts.SYSTEM_PROMPT_GEN.formatted(Config.getInstance().getMainModel())),
                UserMessage.userMessage(mainMessage)
        );

        if (!response.isValid()) {
            FileLogger.LOGGER.severe("Fatal error in generator: " + response.error());
            return DiffPromptResponse.EMPTY;
        }

        System.out.println(response);
        ValidatedResponse previousResponse = response;
        for (var verifier : verifiers) {
            String modelName = verifier.defaultRequestParameters().modelName();
            previousResponse = validatedChat(verifier, origin,
                    SystemMessage.systemMessage(Prompts.SYSTEM_PROMPT_VER.formatted(modelName)),
                    UserMessage.userMessage(mainMessage + "\n Previous Response: " + previousResponse.message())
            );

            if (!previousResponse.isValid()) {
                FileLogger.LOGGER.warning("Error in verifier " + previousResponse.error());
                continue;
            }

            System.out.println(previousResponse);
        }

        return DiffPromptResponse.fromResponse(origin, previousResponse.message());
    }

    /**
     * @return null on success, error message on fail
     */
    private String handleValidation(String origin, String message) {
        if (message == null) return "Invalid message";

        DiffPromptResponse diff = DiffPromptResponse.fromResponse(origin, message);
        if (diff.isEmpty()) return "Invalid response";

        if (!diff.isValid()) return "Invalid method body";
        return null;
    }

    private ValidatedResponse validatedChat(ChatModel model, String origin, ChatMessage... messages) {
        ValidatedResponse lastResponse = null;
        for (int i = 0; i < Config.getInstance().getValidationRetries(); i++) {
            String stringResponse;
            try {
                stringResponse = model.chat(messages).aiMessage().text();
            } catch (Exception e) {
                lastResponse = new ValidatedResponse(null, e.getMessage());
                continue;
            }

            String error = handleValidation(origin, stringResponse);
            if (error == null) {
                return new ValidatedResponse(stringResponse, null);
            } else {
                lastResponse = new ValidatedResponse(null, error);
            }
        }

        return lastResponse;
    }
}