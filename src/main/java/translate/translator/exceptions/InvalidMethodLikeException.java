package translate.translator.exceptions;

public class InvalidMethodLikeException extends RuntimeException {
    public InvalidMethodLikeException(String clazz, String method) {
        super("Invalid method like: " + clazz + "#" + method);
    }
}
