package ai.reponse;

@FunctionalInterface
public interface MessageValidator {
    /**
     * @return null on success, error message on fail
     */
    String validate(String origin, String message);

    MessageValidator CLASS = (origin, message) -> {
        if (message == null) return "Invalid message";

        // TODO change
        DiffMethodResponse diff = DiffMethodResponse.fromResponse(origin, message);
        if (diff.isEmpty()) return "Invalid response";

        if (!diff.isValid()) return "Invalid method body";
        return null;
    };

    MessageValidator METHOD = (origin, message) -> {
        if (message == null) return "Invalid message";

        DiffMethodResponse diff = DiffMethodResponse.fromResponse(origin, message);
        if (diff.isEmpty()) return "Invalid response";

        if (!diff.isValid()) return "Invalid method body";
        return null;
    };
}
