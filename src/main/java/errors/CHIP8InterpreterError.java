package errors;

public class CHIP8InterpreterError extends RuntimeException {
    public CHIP8InterpreterError(String message) {
        super("CHIP-8: " + message);
    }
}
