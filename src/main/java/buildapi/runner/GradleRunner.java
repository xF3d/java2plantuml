package buildapi.runner;

import buildapi.TestParser;
import buildapi.result.CompileResult;
import buildapi.result.TestResult;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;

import java.nio.file.Files;
import java.nio.file.Path;

public class GradleRunner implements BuildRunner {
    @Override
    public boolean supports(Path projectDir) {
        return Files.exists(projectDir.resolve("build.gradle")) ||
                Files.exists(projectDir.resolve("build.gradle.kts"));
    }

    @Override
    public CompileResult compile(Path projectDir) {
        GradleConnector connector = GradleConnector.newConnector()
                .forProjectDirectory(projectDir.toFile());

        try (ProjectConnection connection = connector.connect()) {

            connection.newBuild()
                    .forTasks("classes")
                    .run();

            return new CompileResult(true, null);

        } catch (Exception e) {

            return new CompileResult(false, e.getMessage());
        }
    }

    @Override
    public TestResult test(Path projectDir) {
        GradleConnector connector = GradleConnector.newConnector()
                .forProjectDirectory(projectDir.toFile());

        try (ProjectConnection connection = connector.connect()) {

            connection.newBuild()
                    .forTasks("test")
                    .run();

            return TestParser.parseJUnitReports(projectDir.resolve("build/test-results/test"));
        } catch (Exception e) {
            return TestResult.fail(e.getMessage());
        }
    }

}
