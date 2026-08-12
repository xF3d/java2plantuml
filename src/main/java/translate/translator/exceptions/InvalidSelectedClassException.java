package translate.translator.exceptions;

public class InvalidSelectedClassException extends RuntimeException {
    public InvalidSelectedClassException(String clazz) {
        super("Invalid class: " + clazz);
    }
}
