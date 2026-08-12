package translate.translator.exceptions;

public class InvalidSelectedConstructorException extends RuntimeException {
    public InvalidSelectedConstructorException(String clazz, String ctor) {
        super("Invalid constructor: " + clazz + "#" + ctor);
    }
}
