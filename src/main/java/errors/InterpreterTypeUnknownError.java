package errors;

public class InterpreterTypeUnknownError extends RuntimeException {
    public InterpreterTypeUnknownError(String message) {
        super("Error: Unknown interpreter type: " + message);
    }
}
