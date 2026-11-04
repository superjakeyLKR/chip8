package errors;

/// You've done something that the interpreter doesn't like.
public class CHIP8InterpreterError extends RuntimeException {
    public CHIP8InterpreterError(String message) {
        super("CHIP-8: " + message);
    }
}
