package ai.reponse;

import lombok.*;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@EqualsAndHashCode
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class ValidatedResponse {
    private final String message;
    private final ApplicableDiff parsed;
    private final String error;

    public boolean isValid() {
        return error == null;
    }

    public boolean isError() {
        return error != null;
    }

    public static ValidatedResponse error(String error) {
        return new ValidatedResponse(null, null, error);
    }

    public static ValidatedResponse parsed(String message, ApplicableDiff parsed) {
        return new ValidatedResponse(message, parsed, null);
    }

    public static ValidatedResponse fromDiff(ApplicableDiff diff) {
        return new ValidatedResponse(null, diff, null);
    }
}
