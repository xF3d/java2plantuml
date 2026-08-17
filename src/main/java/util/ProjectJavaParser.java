package util;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import translate.ResolverUtils;

public class ProjectJavaParser {
    private static JavaParser getParser() {
        ParserConfiguration config = new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_25)
                .setSymbolResolver(ResolverUtils.getResolver());

        return new JavaParser(config);
    }

    public static ThreadLocal<JavaParser> makeParser() {
        return ThreadLocal.withInitial(ProjectJavaParser::getParser);
    }
}
