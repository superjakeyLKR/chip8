package core.interpreter;

import core.Display;
import core.Keypad;
import core.Quirks;
import errors.CHIP8InterpreterError;
import errors.CHIP8InvalidOpcodeError;

import java.awt.Toolkit;
import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

public class Chip8Interpreter {
    // --- Initialization ---

    protected final Display display;
    protected final Keypad keypad;
    protected final Random random;

    // This is more so for the SuperCHIP interpreter,
    // as the base interpreter doesn't have the concept of
    // an `exit` command.
    protected boolean running = true;

    // What quirks are currently enabled
    // Basically, not every CHIP-8 is made equal
    // and a bunch of different systems do things differently
    protected Map<Quirks, Boolean> quirks = new EnumMap<>(Quirks.class);

    // See Quirks.DISPLAY_WAIT
    public boolean displayDrawPending = false;

    public Chip8Interpreter(Display display, Keypad keypad) {
        this.display = display;
        this.keypad = keypad;

        this.random = new Random();

        loadFontSet();

        enableAllQuirks();

        // This took way to long to make work
        // setQuirk(Quirks.CLIP_SPRITES, false);
    }

    // If you want to run it without the Window class
    public Chip8Interpreter() {
        this.display = new Display();
        this.keypad = new Keypad();

        this.random = new Random();

        loadFontSet();

        enableAllQuirks();
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isQuirkEnabled(Quirks quirk) {
        return quirks.getOrDefault(quirk, false);
    }

    public void setQuirk(Quirks quirk, boolean enabled) {
        quirks.put(quirk, enabled);
    }

    public void enableAllQuirks() {
        for (Quirks quirk : Quirks.values()) {
            quirks.put(quirk, true);
        }
    }

    // --- Memory ---

    public static final int MEMORY_SIZE = 4096; // 4KB of memory

    protected byte[] memory = new byte[MEMORY_SIZE];

    protected int[] registers = new int[16]; // 16 general purpose registers (V0 to VF)

    // Because bytes are signed in Java, we need to selectInterpreter explicit casting
    // to ensure they are treated as unsigned values for values
    // greater than 127 (0x7F). This is important for the CHIP-8 interpreter,
    // as it uses 8-bit unsigned values.
    private static final byte[] FONT_SET = {
            (byte) 0xF0, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xF0, // 0
            0x20, 0x60, 0x20, 0x20, 0x70, // 1
            (byte) 0xF0, 0x10, (byte) 0xF0, (byte) 0x80, (byte) 0xF0, // 2
            (byte) 0xF0, 0x10, (byte) 0xF0, 0x10, (byte) 0xF0, // 3
            (byte) 0x90, (byte) 0x90, (byte) 0xF0, 0x10, 0x10, // 4
            (byte) 0xF0, (byte) 0x80, (byte) 0xF0, 0x10, (byte) 0xF0, // 5
            (byte) 0xF0, (byte) 0x80, (byte) 0xF0, (byte) 0x90, (byte) 0xF0, // 6
            (byte) 0xF0, 0x10, 0x20, 0x40, 0x40, // 7
            (byte) 0xF0, (byte) 0x90, (byte) 0xF0, (byte) 0x90, (byte) 0xF0, // 8
            (byte) 0xF0, (byte) 0x90, (byte) 0xF0, 0x10, (byte) 0xF0, // 9
            (byte) 0xF0, (byte) 0x90, (byte) 0xF0, (byte) 0x90, (byte) 0x90, // A
            (byte) 0xE0, (byte) 0x90, (byte) 0xE0, (byte) 0x90, (byte) 0xE0, // B
            (byte) 0xF0, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0xF0, // C
            (byte) 0xE0, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xE0, // D
            (byte) 0xF0, (byte) 0x80, (byte) 0xF0, (byte) 0x80, (byte) 0xF0, // E
            (byte) 0xF0, (byte) 0x80, (byte) 0xF0, (byte) 0x80, (byte) 0x80 // F
    };

    // Imagine 255 as a byte. it would be
    // 11111111
    // However, ints are 32 bits, so when you convert a byte to an int, it will be
    // 11111111 11111111 11111111 11111111
    // Because ints are Two's Complement, the first 1 is actually a negative sign,
    // so the int value of a byte with all bits set is -1. To get the correct value, we
    // need to mask the int with 0xFF, which is 00000000 00000000 00000000 11111111
    // This removes everything but the last 8 bits, which is what we want.
    protected int toInt(byte b) {
        return b & 0xFF;
    }

    private static final int FONT_SET_START_ADDRESS = 0x50; // Starting address for the font set in memory
    // Load the font set into memory starting at address 0x50 (80 in decimal)
    // It should end at 0x9F (159 in decimal) since the font set is 80 bytes long.
    private void loadFontSet() {
        System.arraycopy(FONT_SET, 0, memory, FONT_SET_START_ADDRESS, FONT_SET.length);
    }

    // --- Stack ---

    private static final int STACK_SIZE = 16; // Maximum stack depth
    // The stack is used to store return addresses when calling subroutines.
    // I could have used a short[] array for the stack,
    // ints are just more convenient to work with.
    // The memory usage is negligible anyway.
    private int[] stack = new int[STACK_SIZE];
    private int stackPointer = 0; // Points to the top of the stack

    private void push(int address) {
        if (stackPointer >= STACK_SIZE) {
            throw new CHIP8InterpreterError("Stack overflow");
        }

        // Addresses are 12-bit values, so we need to ensure that
        // the address is within the valid range of 0x000 to 0xFFF (0 to 4095 in decimal).
        if (address < 0 || address > MEMORY_SIZE - 1) {
            throw new CHIP8InterpreterError("Address out of bounds");
        }

        // We mask it with 0xFFF to pretend that it's actually a 12-bit value
        // and not just a regular 32-bit int. This is important because the CHIP-8
        // uses 12-bit addresses, and we want to ensure that we don't accidentally
        // store an address that is larger than 12 bits.
        stack[stackPointer++] = address & 0xFFF;
    }

    protected int pop() {
        if (stackPointer == 0) {
            throw new RuntimeException("CHIP-8: Stack underflow");
        }
        return stack[--stackPointer];
    }

    // --- Timers ---
    // The delay timer is used for timing events,
    // such as animations or delays between actions.
    // The sound timer is literally just used to play a beep when it's non-zero.
    // They are both decremented at a rate of 60Hz (60 times per second)
    // and are 8 bits.
    private int delayTimer = 0;
    private int soundTimer = 0;

    private void setDelayTimer(int value) {
        if (value < 0 || value > 255) {
            throw new IllegalArgumentException("Delay timer value must be between 0 and 255");
        }
        delayTimer = value & 0xFF;
    }

    private void setSoundTimer(int value) {
        if (value < 0 || value > 255) {
            throw new IllegalArgumentException("Sound timer value must be between 0 and 255");
        }
        soundTimer = value & 0xFF;
    }

    public void decrementTimers() {
        if (delayTimer > 0) {
            delayTimer--;
        }
        if (soundTimer > 0) {
            soundTimer--;
            beep();
        }
    }

    public void beep() {
        // This existing is hilarious to me
        // Like there is a whole function for just making a beep sound.
        Toolkit.getDefaultToolkit().beep();
    }

    // --- FDE Cycle stuff ---

    private final int START_OF_MEMORY = 0x200; // Start of program memory (512 in decimal)

    // Where the next instruction to be executed is located in memory
    protected int programCounter = START_OF_MEMORY;

    // I is a special register that holds memory addresses.
    // It's used for operations that involve memory,
    // such as loading and storing data.
    protected int I = 0;

    public void cycle() {
        if (isQuirkEnabled(Quirks.DISPLAY_WAIT) && displayDrawPending) {
            return; // Don't do CPU cycles when waiting to draw something
        }

        // Fetch the next instruction from memory
        int opcode = fetch();
        // Decode and execute the instruction
        decodeAndExecute(opcode);
    }

    private int fetch() {
        // Each instruction is 2 bytes long, so we need to fetch 2 bytes from memory
        // It gets the first byte, shifts it left by 8 bits (to make room for the second byte),
        // and then ORs it with the second byte to get the full 16-bit instruction
        int opcode = toInt(memory[programCounter]) << 8
                | toInt(memory[programCounter + 1]);

        programCounter += 2;

        return opcode;
    }

    private void decodeAndExecute(int opcode) {
        // This method will decode the opcode and execute the corresponding instruction.

        // Get the first nibble (4 bits) of the opcode
        // This is done by ANDing the opcode with 0xF000 (which is 1111000000000000 in binary)
        // and then shifting it right by 12 bits to get the first nibble.
        // This is because the first nibble of the opcode determines the instruction type.
        // E.g: 0x0 is system instructions, 0x1 is jump instructions, etc.
        int instruction = (opcode & 0xF000) >> 12;
        switch (instruction) {
            case 0x0 -> executeZeroInstruction(opcode);
            // 0x1NNN: Jump to address NNN
            // Get the last 12 bits of the opcode
            // and set the program counter to that address.
            case 0x1 -> programCounter = opcode & 0x0FFF;
            case 0x2 -> {
                // 0x2NNN: Call subroutine at NNN
                // It's different from 0x1NNN because it needs to save the current program counter
                // so that it can return to it later. This is done by pushing the current program
                // Push the current program counter onto the stack
                push(programCounter);
                // Set the program counter to the address NNN
                programCounter = opcode & 0x0FFF;
            }
            case 0x3 -> {
                // 0x3XNN: Skip the next instruction if VX equals NN
                // Get the second nibble (X) and the last byte (NN)
                int x = (opcode & 0x0F00) >> 8;
                int nn = opcode & 0x00FF;
                if (registers[x] == nn) {
                    programCounter += 2; // Skip the next instruction
                }
            }
            case 0x4 -> {
                // 0x4XNN: Skip the next instruction if VX does not equal NN
                // The opposite of 0x3XNN.
                int x = (opcode & 0x0F00) >> 8;
                int nn = opcode & 0x00FF;
                if (registers[x] != nn) {
                    programCounter += 2; // Skip the next instruction
                }
            }
            case 0x5 -> {
                // 0x5XY0: Skip the next instruction if VX equals VY

                // The last nibble MUST be 0
                if ((opcode & 0x000F) != 0) {
                    throw new CHIP8InvalidOpcodeError(opcode);
                }

                int x = (opcode & 0x0F00) >> 8;
                int y = (opcode & 0x00F0) >> 4;
                if (registers[x] == registers[y]) {
                    programCounter += 2; // Skip the next instruction
                }
            }
            case 0x6 -> {
                // 0x6XNN: Set VX to NN
                // VX is the Xth register (V0 to VF), and NN is the 8-bit value to set it to.
                // Get the second nibble (X) and the last byte (NN)
                int x = (opcode & 0x0F00) >> 8;
                int nn = opcode & 0x00FF;
                registers[x] = nn;
            }
            case 0x7 -> {
                // 0x7XNN: Add NN to VX (without carry)
                int x = (opcode & 0x0F00) >> 8;
                int nn = opcode & 0x00FF;
                registers[x] = (registers[x] + nn) & 0xFF; // Ensure it stays within 8 bits
            }
            case 0x8 -> {
                // 0x8XYN: Perform various operations on VX and VY based on the last nibble (N)
                int x = (opcode & 0x0F00) >> 8;
                int y = (opcode & 0x00F0) >> 4;
                int n = opcode & 0x000F;

                switch (n) {
                    // None of these operations should edit the value in VY
                    case 0x0 -> registers[x] = registers[y]; // Set VX to the value of VY
                    // A quirk of CHIP-8 is that these logic operations
                    // should reset VF to 0, but it doesn't do that on the
                    // SuperCHIP, hence the weirdness.
                    // See Quirks.
                    case 0x1, 0x2, 0x3 -> eightXYLogic(x, y, n);

                    // According to 4-flags.ch8, there used to be a pretty big oversight
                    // with these instructions. If VX or VY were themselves VF,
                    // then it was exceedingly easy to override their values,
                    // which isn't how it's supposed to do it.

                    case 0x4 -> {
                        // Add VY to VX. VF is set to 1 if there's a carry, and to 0 if there isn't.
                        int vx = registers[x];
                        int vy = registers[y];

                        int sum = vx + vy;

                        registers[x] = sum & 0xFF; // Keep only the last 8 bits
                        registers[0xF] = sum > 255 ? 1 : 0; // Check for carry
                    }
                    case 0x5 -> {
                        // Subtract VX from VY. VF is set to 0 if there's a borrow, and to 1 if there isn't.
                        int vx = registers[x];
                        int vy = registers[y];

                        int difference = vx - vy;

                        registers[x] = difference & 0xFF; // Keep only the last 8 bits
                        registers[0xF] = vx >= vy ? 1 : 0; // Check for borrow
                    }
                    case 0x6 -> {
                        // No more vaguepost alarm, we've got a quirk for this now!
                        // Store the least significant bit of VX in VF and then shift VX to the right by 1.
                        int vx = registers[x];

                        if (isQuirkEnabled(Quirks.SHIFT_VY)) {
                            // Instead of shifting VX, we shift VY and set VX to it
                            // It makes y actually useful in this instruction
                            registers[x] = registers[y] >> 1;
                        } else {
                            registers[x] >>= 1; // Shift right by 1
                        }

                        registers[0xF] = vx & 0x1; // Store LSB in VF
                    }
                    case 0x7 -> {
                        // Evil version of 0x8XY5
                        // Set VX to VY minus VX. VF is set to 0 if there's a borrow, and to 1 if there isn't.
                        int vx = registers[x];
                        int vy = registers[y];

                        int difference = vy - vx;

                        registers[x] = difference & 0xFF; // Keep only the last 8 bits
                        registers[0xF] = vy >= vx ? 1 : 0; // Check for borrow
                    }
                    case 0xE -> {
                        // Yeah, see 0x8XY6
                        // Store the most significant bit of VX in VF and then shift VX to the left by 1.
                        int vx = registers[x];

                        if (isQuirkEnabled(Quirks.SHIFT_VY)) {
                            // You get the point
                            registers[x] = (registers[y] << 1) & 0xFF;
                        } else {
                            registers[x] = (registers[x] << 1) & 0xFF; // Shift left by 1
                        }
                        registers[0xF] = (vx & 0x80) >> 7;
                    }
                    default -> throw new CHIP8InvalidOpcodeError(opcode);
                }
            }
            case 0x9 -> {
                // 0x9XY0: Skip the next instruction if VX does not equal VY
                // This is the opposite of 0x5XY0.
                if ((opcode & 0x000F) != 0) {
                    throw new CHIP8InvalidOpcodeError(opcode);
                }
                int x = (opcode & 0x0F00) >> 8;
                int y = (opcode & 0x00F0) >> 4;
                if (registers[x] != registers[y]) {
                    programCounter += 2; // Skip the next instruction
                }
            }
            // 0xANNN: Set I to the address NNN
            // Get the last 12 bits of the opcode and set I to that address.
            case 0xA -> I = opcode & 0x0FFF;
            // Yep, this has a quirk too now
            // 0xBNNN (OR 0xBXNN): Jump to NNN plus V0
            // (If it were 0xBXNN, it would jump to NN plus VX)
            case 0xB -> {
                if (isQuirkEnabled(Quirks.JUMP_WITH_VX)) {
                    int x = opcode & 0x0F00 >> 8;
                    int nn = opcode & 0x0FFF;

                    programCounter = nn + registers[x];
                } else {
                    programCounter = (opcode & 0x0FFF) + registers[0x0];
                }
            }

            case 0xC -> {
                // 0xCXNN: Generate a random number and logical AND it with NN
                // and put the result in VX
                int x = (opcode & 0x0F00) >> 8;
                int nn = opcode & 0x00FF;
                // 0-255 is every value an 8-bit number could be
                registers[x] = random.nextInt(256) & nn;
            }
            case 0xD -> executeDrawInstruction(opcode);
            case 0xE -> {
                // 0xEXNN: Skips based on the state of the key corresponding to the value
                // in VX
                int x = (opcode & 0x0F00) >> 8;
                int nn = opcode & 0x00FF;

                boolean keyPressed = keypad.isPressed(registers[x]);

                switch (nn) {
                    // 0xEX9E: Skips if the key IS pressed
                    case 0x9E -> {
                        if (keyPressed) {
                            programCounter += 2;
                        }
                    }
                    // 0xEXA1: Skips if the key ISN'T pressed
                    case 0xA1 -> {
                        if (!keyPressed) {
                            programCounter += 2;
                        }
                    }
                    default -> throw new CHIP8InvalidOpcodeError(opcode);
                }
            }
            case 0xF -> executeFInstruction(opcode);
            default -> throw new CHIP8InvalidOpcodeError(opcode);
        }
    }

    protected void executeZeroInstruction(int opcode) {
        // 0x00E0: Clear the display
        if (opcode == 0x00E0) {
            // Clear the display
            display.clear();
        } else if (opcode == 0x00EE) {
            // 0x00EE: Return from a subroutine
            // Pop the address from the stack and set the program counter to that address.
            programCounter = pop();
        } else {
            throw new CHIP8InvalidOpcodeError(opcode);
        }
    }

    protected void executeDrawInstruction(int opcode) {
        // 0xDXYN: Draw a sprite at coordinate (VX, VY)
        // with width 8 pixels and height N pixels.
        // Note that X and Y refer to the registers VX and VY, which hold the x and y coordinates.
        // The sprite is located in memory at the address stored in I.
        // VF is set to 1 if any pixels are flipped from set to unset when the sprite is drawn,
        // and to 0 if that doesn't happen.

        int x = registers[(opcode & 0x0F00) >> 8]; // Get VX
        int y = registers[(opcode & 0x00F0) >> 4]; // Get VY
        int height = opcode & 0x000F; // Get N

        displayDrawPending = true;

        registers[0xF] = drawSprite(x, y, 8, height);
    }

    protected void executeFInstruction(int opcode) {
        // 0xFXNN: A bunch of stuff. Timers, key inputs, memory
        int x = (opcode & 0x0F00) >> 8;
        int nn = opcode & 0x00FF;

        switch (nn) {
            // Set VX to the delay timer
            case 0x07 -> registers[x] = delayTimer & 0xFF;
            // Set the delay timer to VX
            case 0x15 -> delayTimer = registers[x];
            // Set the sound timer to VX
            case 0x18 -> soundTimer = registers[x];
            // Add VX to I, and set VF to 1 if it would "overflow" above 0x0FFF
            // It's not actually overflowing, but it shouldn't be any higher than that.
            case 0x1E -> {
                I += registers[x];
                registers[0xF] = 0;

                if (I > 0x0FFF) {
                    I = 0x0FFF;
                    registers[0xF] = 1;
                }
            }
            // Halts until any key is pressed
            // Puts its hex value in VX when it is pressed
            case 0x0A -> {
                int pressedKey = keypad.getPressedKey();

                if (pressedKey == -1) {
                    programCounter -= 2;
                } else {
                    registers[x] = x;
                }
            }
            // Make I point to VX's character in the font
            // Each character in the font is 5 bytes long
            case 0x29 -> I = FONT_SET_START_ADDRESS + registers[x] * 5;
            // BINARY CODED DECIMAL OR SOMETHING
            // Convert a number into each of its digits, then store them
            // one by one at I, I+1 and I+2
            case 0x33 -> {
                int num = registers[x];
                memory[I] = (byte) (num / 100);
                memory[I+1] = (byte) ((num / 10) % 10);
                memory[I+2] = (byte) (num % 10);
            }
            // Store the values in registers V0 to VX inclusive
            // in contiguous spaces in memory starting from I
            // If the quirk is on, then the value of I gets incremented
            // Otherwise, it stays the same
            case 0x55 -> {
                for (int i = 0; i <= x; i++) {
                    memory[I+i] = (byte) registers[i];
                }
                if (isQuirkEnabled(Quirks.INCREMENT_I_ON_SAVE_LOAD)) {
                   I += x + 1;
                }
            }
            // The opposite of 0xFX55
            case 0x65 -> {
                for (int i = 0; i <= x; i++) {
                    registers[i] = toInt(memory[I+i]); // Unsign it.
                }
                if (isQuirkEnabled(Quirks.INCREMENT_I_ON_SAVE_LOAD)) {
                    I += x + 1;
                }
            }
            default -> throw new CHIP8InvalidOpcodeError(opcode);
        }
    }

    protected void eightXYLogic(int x, int y, int n) {
        switch (n) {
            case 0x1 -> registers[x] |= registers[y]; // Set VX to VX OR VY
            case 0x2 -> registers[x] &= registers[y]; // Set VX to VX AND VY
            case 0x3 -> registers[x] ^= registers[y]; // Set VX to VX XOR VY
        }
        if (isQuirkEnabled(Quirks.RESET_VF_ON_LOGIC)) {
            registers[0xF] = 0;
        }
    }

    protected int drawSprite(int x, int y, int width, int height) {
        int pixelFlipped = 0; // VF register

        // Yep, the COSMAC VIP did it like this,
        // so we're doing it like this
        // Why would you even want to draw stuff outside
        // the screen, just to have it draw in the screen?
        // The inner machinations of CHIP-8 programmers are
        // an enigma at best.
        x %= display.getWidth();
        y %= display.getHeight();

        // width 8 sprites need one byte per row
        // (one bit for each pixel)
        // width 16 need two, and so on
        int bytesPerRow = width / 8;

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                // Find where the byte actually is in memory
                int byteIndex = I + row * bytesPerRow + col / 8;

                // Get the sprite byte from memory at address byteIndex
                int spriteByte = toInt(memory[byteIndex]);

                // Check if the current bit in the sprite byte is set
                // We do `% 8` to wrap the bit positions within each byte
                if ((spriteByte & (0x80 >> (col % 8))) != 0) {
                    // Calculate the pixel coordinates on the display
                    int pixelX = (x + col);
                    int pixelY = (y + row);

                    // If the CLIP_SPRITES quirk is on, sprites shouldn't
                    // wrap around the screen and pixel should just be discarded
                    // if not drawn to a valid position
                    if (isQuirkEnabled(Quirks.CLIP_SPRITES)) {
                        if (pixelX < 0 || pixelX >= display.getWidth() ||
                        pixelY < 0 || pixelY >= display.getHeight()) {
                            continue;
                        }
                    } else {
                        // Wrap them around the screen otherwise
                        pixelX %= display.getWidth();
                        pixelY %= display.getHeight();
                    }

                    // Get the current pixel state
                    boolean currentPixelState = display.getPixel(pixelX, pixelY);

                    // XOR the current pixel state with the new pixel state
                    boolean newPixelState = !currentPixelState;

                    // Set the new pixel state on the display
                    display.setPixel(pixelX, pixelY, newPixelState);

                    // If a pixel was turned off, set VF to 1
                    if (!newPixelState) {
                        pixelFlipped = 1;
                    }
                }
            }
        }

        return pixelFlipped;
    }

    // --- Loading ROMs ---

    public void loadROM(String filePath) throws IOException {
        // Load the ROM into memory starting at address 0x200
        byte[] romData = java.nio.file.Files.readAllBytes(Path.of(filePath));

        if (romData.length > MEMORY_SIZE - START_OF_MEMORY) {
            throw new IllegalArgumentException("ROM is too large to fit in memory");
        }

        System.arraycopy(
                romData,
                0,
                memory,
                START_OF_MEMORY,
                romData.length
        );
    }
}
