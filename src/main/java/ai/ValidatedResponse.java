package ai;

public record ValidatedResponse(String message, String error) {
    public boolean isValid() {
        return error == null;
    }
}
