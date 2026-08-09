package buildapi.runner;

import buildapi.TestParser;
import buildapi.result.CompileResult;
import buildapi.result.TestResult;
import file.Config;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.MavenInvocationException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class MavenRunner implements BuildRunner {
    @Override
    public boolean supports(Path projectDir) {
        return Files.exists(pom(projectDir));
    }

    @Override
    public CompileResult compile(Path projectDir) {
        DefaultInvoker invoker = invoker();

        DefaultInvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(pom(projectDir).toFile());
        request.setGoals(List.of("compile"));

        try {
            InvocationResult result = invoker.execute(request);
            return new CompileResult(
                    result.getExitCode() == 0,
                    result.getExecutionException() != null
                            ? result.getExecutionException().getMessage()
                            : null
            );
        } catch (MavenInvocationException e) {
            return new CompileResult(
                    false,
                    e.getMessage()
            );
        }
    }

    @Override
    public TestResult test(Path projectDir) {
        DefaultInvoker invoker = invoker();

        DefaultInvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(pom(projectDir).toFile());
        request.setGoals(List.of("test"));

        try {
            invoker.execute(request);
            Path reports = projectDir.resolve("target/surefire-reports");
            return TestParser.parseJUnitReports(reports); //TODO: failsafe too
        } catch (MavenInvocationException e) {
            return TestResult.fail(e.getMessage());
        }
    }

    private Path pom(Path projectDir) {
        return projectDir.resolve("pom.xml");
    }

    public static DefaultInvoker invoker() {
        DefaultInvoker invoker = new DefaultInvoker();
        invoker.setMavenHome(Config.getInstance().getMvnPath().toFile());
        return invoker;
    }
}
