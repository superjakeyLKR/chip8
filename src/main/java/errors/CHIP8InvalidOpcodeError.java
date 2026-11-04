package errors;

public class CHIP8InvalidOpcodeError extends CHIP8InterpreterError {
    public CHIP8InvalidOpcodeError(int opcode) {
        super("Invalid opcode: " + Integer.toHexString(opcode));
    }
}
