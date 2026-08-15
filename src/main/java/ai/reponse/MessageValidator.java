package ai.reponse;

@FunctionalInterface
public interface MessageValidator {
    /**
     * @return null on success, error message on fail
     */
    String validate(String origin, String message);

    MessageValidator CLASS = (origin, message) -> {
        if (message == null) return "Invalid message";

        DiffClassResponse diff = DiffClassResponse.fromResponse(origin, message);
        if (diff.isEmpty()) return "Response is empty or formatted incorrectly";

        if (!diff.isValid()) return "Invalid response";
        return null;
    };

    MessageValidator METHOD = (origin, message) -> {
        if (message == null) return "Invalid message";

        DiffMethodResponse diff = DiffMethodResponse.fromResponse(origin, message);
        if (diff.isEmpty()) return "Response is empty or formatted incorrectly";

        if (!diff.isValid()) return "Invalid method body";
        return null;
    };
}
