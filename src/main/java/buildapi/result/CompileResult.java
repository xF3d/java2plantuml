package buildapi.result;

public record CompileResult(
        boolean success,
        String errorMessage
) {}
