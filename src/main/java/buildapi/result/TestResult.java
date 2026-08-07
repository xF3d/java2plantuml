package buildapi.result;

public record TestResult(
        boolean success,
        int testsRun,
        int testsPassed,
        int testsFailed,
        int testsSkipped,
        String errorMessage
) {

    public static TestResult fail(String errorMessage) {
        return new TestResult(false, 0, 0, 0, 0, errorMessage);
    }
}