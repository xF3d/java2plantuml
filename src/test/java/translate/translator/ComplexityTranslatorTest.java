package translate.translator;

import file.Config;
import file.FileLogger;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ComplexityTranslatorTest {

    @Test
    public void constructor() {
        FileLogger.init();
        Config.init();
        var translator = new ComplexityTranslator();

        Path projectDir = Paths.get(System.getProperty("user.dir"));
        translator.execute(projectDir.toFile());

    }
}