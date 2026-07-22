package ai;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.expr.Name;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import lombok.Getter;
import translate.component.formatter.MemberFormatter;
import translate.structure.complexity.ComplexityClass;
import translate.structure.complexity.ComplexityMethod;
import translate.structure.pkg.PackageImport;
import translate.translator.Config;

import java.util.ArrayList;
import java.util.stream.Collectors;

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

    public void chat(ComplexityClass clazz, ComplexityMethod method) {
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

        String mainMessage = Config.FORM_REFACTOR_PROMPT.formatted(
                importString,
                clazz.getNode()
        );

    }
}
