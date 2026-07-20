package translate.translator;

import file.FileLogger;
import lombok.Getter;
import org.tomlj.Toml;
import org.tomlj.TomlParseResult;
import translate.ClassDiagramConfig;
import visitors.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Getter
public class Config {
    private static final String CONFIG_NAME = "config.toml";

    @Getter
    private static Config instance = null;

    public static void init() {
        instance = new Config();
        instance.loadFromFile();
    }

    private final ClassDiagramConfig.Builder configBuilder = new ClassDiagramConfig.Builder();
    private ClassDiagramConfig config;

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
        """;

        try {
            Files.writeString(getConfigPath(), toml);
        } catch (IOException e) {
            FileLogger.LOGGER.severe("Error in regenerating default config");
            e.printStackTrace();
        }

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


    }

    public Path getConfigPath() {
        return Path.of(CONFIG_NAME);
    }
}
