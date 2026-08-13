package util;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import translate.ResolverUtils;

public class ProjectJavaParser {
    public static final JavaParser PARSER = getParser();

    private static JavaParser getParser() {
        ParserConfiguration config = new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_25)
                .setSymbolResolver(ResolverUtils.getResolver());

        return new JavaParser(config);
    }
}
