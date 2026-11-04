package errors;

/// The ROM has an instruction that the CHIP8 can't recognise.
public class CHIP8InvalidOpcodeError extends CHIP8InterpreterError {
    public CHIP8InvalidOpcodeError(int opcode) {
        super("Invalid opcode: " + Integer.toHexString(opcode));
    }
}
