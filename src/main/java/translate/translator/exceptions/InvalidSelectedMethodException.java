package translate.translator.exceptions;

public class InvalidSelectedMethodException extends RuntimeException {
    public InvalidSelectedMethodException(String clazz, String method) {
        super("Invalid method: " + clazz + "#" + method);
    }
}
