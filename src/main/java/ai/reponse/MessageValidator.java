package ai.reponse;

@FunctionalInterface
public interface MessageValidator {
    /**
     * @return null on success, error message on fail
     */
    ValidatedResponse validate(String origin, String message);

    MessageValidator CLASS = (origin, message) -> {
        if (message == null) return ValidatedResponse.error("Invalid message");

        DiffClassResponse diff = DiffClassResponse.fromResponse(origin, message);
        if (diff.isEmpty()) return ValidatedResponse.error("Response is empty or formatted incorrectly");

        if (!diff.isValid()) return ValidatedResponse.error("Invalid response");
        return ValidatedResponse.parsed(message, diff);
    };

    MessageValidator METHOD = (origin, message) -> {
        if (message == null) return ValidatedResponse.error("Invalid message");

        DiffMethodResponse diff = DiffMethodResponse.fromResponse(origin, message);
        if (diff.isEmpty()) return ValidatedResponse.error("Response is empty or formatted incorrectly");

        if (!diff.isValid()) return ValidatedResponse.error("Invalid method body");
        return ValidatedResponse.parsed(message, diff);
    };
}
