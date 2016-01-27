package twcore.core.lag;

@SuppressWarnings("serial")
public class InvalidStateException extends RuntimeException {
    public InvalidStateException() {
        super();
    }

    public InvalidStateException(String message) {
        super(message);
    }
}
