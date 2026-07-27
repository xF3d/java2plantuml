package translate.translator;

import ai.AIManager;
import ai.model.APIType;
import ai.model.ModelType;
import file.FileLogger;
import lombok.Getter;
import org.tomlj.Toml;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;
import translate.ClassDiagramConfig;
import visitors.*;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;

@Getter
public class Config {
    private static final String CONFIG_NAME = "config.toml";

    public static final String SYSTEM_PROMPT_GEN = """
            You are an AI agent named %s-GEN that given java source code with some problematic metrics and/or code smells,
            has the job to fix said issues.
            
            Your answer should follow this format, similar to git differences with a NOTE section:
            -----BEGIN_LINEDIFF-----
            LINE_NUMBER +/- CODE_PART
            ...
            -----END_LINEDIFF-----
            AI_MODEL : NOTE_1
            AI_MODEL : NOTE_2
            AI_MODEL_2 : NOTE_3
            
            Example:
            -----BEGIN_LINEDIFF-----
            1 - int a = calc.add(4, 5);
            2 + int b = calc.div(6, 2);
            3 + int c = b + d; // assuming d exists in the context supplied by the user
            -----END_LINEDIFF-----
            chatgpt-GEN : myCalc's class might be a overabstraction, consider removing them
            grok : anotherClass's behaviour could be replaced with a Strategy design pattern
            claude : fancyClass should have a more descriptive name
            
            Rules:
            - You must not change the logic of the code, the past behaviour must be preserved, the structure however can be changed
            - You can create new functions only if you implement them fully yourself
            - NOTEs are optional
            
            There are actions that you cannot do directly, however you can suggest them in a NOTE section
            in a short format (max 50 words) and using your name in the AI_MODEL before the message, mainly:
            - Creating new functions in other classes
            - Adding an external dependency
            - Suggesting changes to different classes
            - Adding new classes
            """;

    public static final String SYSTEM_PROMPT_VER = """
            You are an AI agent named %s that given java code diffs you need to verify and improve the solution when possible.
            
            Your answer should follow this format, similar to git differences with a NOTE section:
            -----BEGIN_LINEDIFF-----
            LINE_NUMBER +/- CODE_PART
            ...
            -----END_LINEDIFF-----
            AI_MODEL : NOTE_1
            AI_MODEL : NOTE_2
            AI_MODEL_2 : NOTE_3
            
            Example:
            -----BEGIN_LINEDIFF-----
            1 - int a = myCalc.add(4, 5);
            2 + int b = myCalc.div(6, 2);
            3 + int c = b + d; // assuming d exists in the context supplied by the user
            -----END_LINEDIFF-----
            chatgpt-GEN : myCalc's class might be a overabstraction, consider removing them
            grok : anotherClass's behaviour could be replaced with a Strategy design pattern
            claude : fancyClass should have a more descriptive name
            
            Rules:
            - You must not change the logic of the code, the past behaviour must be preserved, the structure however can be changed
            - You can create new functions only if you implement them fully yourself
            - You cannot create NOTEs that refer to some other NOTE or to the LINEDIFF
            - NOTEs are optional
            - If you don't want to change anything, just resend the message AS-IS
            
            There are actions that you cannot do directly, however you can suggest them in a NOTE section
            in a short format (max 50 words) and using your name in the AI_MODEL before the message, mainly:
            - Creating new functions in other classes
            - Adding an external dependency
            - Suggesting changes to different classes
            - Adding new classes
            
            You can change the LINEDIFF as you see fit if necessary, but not the NOTEs, you can only add to the NOTEs, not delete them
            """;

    public static final String FORM_REFACTOR_PROMPT = """
            IMPORTS
            %s
            
            SELECTED_CLASS
            %s
            
            METHODS_IN_CLASS
            %s
            
            CLASS_METRICS
            %s
            
            METHOD_METRICS
            %s
            
            CLASS_CODE_SMELLS
            %s
            
            METHOD_CODE_SMELLS
            %s
            
            SELECTED_METHOD
            %s
            
            METHOD_LINE_START: %s
            METHOD_LINE_END: %s
            """;



    @Getter
    private static Config instance = null;

    public static void init() {
        instance = new Config();
        instance.loadFromFile();
        AIManager.update();
    }

    private final ClassDiagramConfig.Builder configBuilder = new ClassDiagramConfig.Builder();
    private ClassDiagramConfig config;

    private ModelType mainModel;
    private final ArrayList<ModelType> verifyModels = new ArrayList<>();

    private boolean verifyWithMain;

    public void updateTranslator(Translator translator) {
        this.config = configBuilder
                        .withVisitor(new ClassVisitor(translator))
                        .withVisitor(new InterfaceVisitor(translator))
                        .withVisitor(new EnumVisitor(translator))
                        .withVisitor(new RecordVisitor(translator))
                        .withVisitor(new AnnotationVisitor(translator))
                        .build();
    }

    public void regenDefaults() {
        String toml = """
        [translator]
        show_methods = true
        show_attributes = true
        show_colored_access_specifiers = true
        
        [ai]
        verify_with_main = true
        
        """;

        try {
            Files.writeString(getConfigPath(), toml);
        } catch (IOException e) {
            FileLogger.LOGGER.severe("Error in regenerating default config");
            e.printStackTrace();
        }

        mainModel = null;
        verifyModels.clear();
        verifyWithMain = true;

        configBuilder
                .setShowMethods(true)
                .setShowAttributes(true)
                .setShowColoredAccessSpecifiers(true);
    }

    private void loadFromFile() {
        String toml;
        try {
            toml = Files.readString(getConfigPath());
        } catch (IOException e) {
            regenDefaults();
            return;
        }

        TomlParseResult result = Toml.parse(toml);

        if (result.hasErrors()) {
            regenDefaults();
            return;
        }

        boolean showMethods = result.getBoolean("translator.show_methods", () -> true);
        boolean showAttributes = result.getBoolean("translator.show_attributes", () -> true);
        boolean showColoredAccessSpecifiers = result.getBoolean("translator.show_colored_access_specifiers", () -> true);

        configBuilder
                .setShowMethods(showMethods)
                .setShowAttributes(showAttributes)
                .setShowColoredAccessSpecifiers(showColoredAccessSpecifiers);


        var rawMainType = result.getString("generator.type");
        if (rawMainType != null) {
            APIType mainType = APIType.valueOf(rawMainType.toUpperCase(Locale.ROOT));
            String mainUrl = result.getString("generator.url");
            String modelName = result.getString("generator.name");
            String mainKey = result.getString("generator.key");
            this.mainModel = new ModelType(mainType, modelName, mainUrl, mainKey);
        }

        var array = result.getArrayOrEmpty("verify");
        for (int i = 0; i < array.size(); i++) {
            TomlTable table = array.getTable(i);

            APIType verifyType = APIType.valueOf(table.getString("type").toUpperCase(Locale.ROOT));
            String verifyUrl = table.getString("url");
            String verifyName = table.getString("name");
            String verifyKey = table.getString("key");

            ModelType verify = new ModelType(verifyType, verifyName, verifyUrl, verifyKey);

            if (verify.isValid()) {
                verifyModels.add(verify);
            } else {
                FileLogger.LOGGER.severe("Verifier is invalid: " + verify);
            }
        }

        verifyWithMain = result.getBoolean("ai.verify_with_main", () -> true);
    }

    public boolean isAiDisabled() {
        return ModelType.isValid(mainModel);
    }

    public Path getConfigPath() {
        return Path.of(CONFIG_NAME);
    }
}
