package buildapi.runner;

import buildapi.result.CompileResult;
import buildapi.result.TestResult;

import java.nio.file.Path;

public interface BuildRunner {
    boolean supports(Path projectDir);

    CompileResult compile(Path projectDir);

    TestResult test(Path projectDir);


    BuildRunner[] RUNNERS = {
            new GradleRunner(),
            new MavenRunner()
    };

    static BuildRunner of(Path projectDir) {
        for (var runner : RUNNERS) {
            if (runner.supports(projectDir)) return runner;
        }

        return null;
    }
}
