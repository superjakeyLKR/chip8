package core.interpreter;

import core.Display;
import core.Keypad;
import core.Quirks;
import errors.CHIP8InvalidOpcodeError;

public class SuperCHIPInterpreter extends Chip8Interpreter {
    // Used for instructions 0xFX75 and 0xFX85, as a way to store
    // semi-persistent data between programs
    // (but not permanently)
    private final byte[] rplFlags = new byte[8];

    // The SuperCHIP has some bigger numbers to use as a font
    // (not letters though. I guess they couldn't be bothered with that)
    private static final byte[] LARGE_FONT_SET = {
            0x3C, 0x7E, (byte) 0xE7, (byte) 0xC3, (byte) 0xC3, (byte) 0xC3, (byte) 0xC3, (byte) 0xE7, 0x7E, 0x3C, // big 0
            0x18, 0x38, 0x58, 0x18, 0x18, 0x18, 0x18, 0x18, 0x18, 0x3C, // big 1
            0x3E, 0x7F, (byte) 0xC3, 0x06, 0x0C, 0x18, 0x30, 0x60, (byte) 0xFF, (byte) 0xFF, // big 2
            0x3C, 0x7E, (byte) 0xC3, 0x03, 0x0E, 0x0E, 0x03, (byte) 0xC3, 0x7E, 0x3C, // big 3
            0x06, 0x0E, 0x1E, 0x36, 0x66, (byte) 0xC6, (byte) 0xFF, (byte) 0xFF, 0x06, 0x06, // big 4
            (byte) 0xFF, (byte) 0xFF, (byte) 0xC0, (byte) 0xC0, (byte) 0xFC, (byte) 0xFE, 0x03, (byte) 0xC3, 0x7E, 0x3C, // big 5
            0x3E, 0x7C, (byte) 0xE0, (byte) 0xC0, (byte) 0xFC, (byte) 0xFE, (byte) 0xC3, (byte) 0xC3, 0x7E, 0x3C, // big 6
            (byte) 0xFF, (byte) 0xFF, 0x03, 0x06, 0x0C, 0x18, 0x30, 0x60, 0x60, 0x60, // big 7
            0x3C, 0x7E, (byte) 0xC3, (byte) 0xC3, 0x7E, 0x7E, (byte) 0xC3, (byte) 0xC3, 0x7E, 0x3C, // big 8
            0x3C, 0x7E, (byte) 0xC3, (byte) 0xC3, 0x7F, 0x3F, 0x03, 0x03, 0x3E, 0x7C  // big 9
    };

    // One after the end of the normal font set
    private static final int LARGE_FONT_SET_START_ADDRESS = 0xA0;

    public SuperCHIPInterpreter(Display display, Keypad keypad) {
        super(display, keypad);

        setQuirk(Quirks.RESET_VF_ON_LOGIC, false);

        loadLargeFontSet();
    }

    // This should end at 0x104 (0xA0 + 0x64)
    // or 260 in base 10
    private void loadLargeFontSet() {
        System.arraycopy(
                LARGE_FONT_SET,
                0,
                memory,
                LARGE_FONT_SET_START_ADDRESS,
                LARGE_FONT_SET.length
        );
    }

    @Override
    protected void executeZeroInstruction(int opcode) {
        switch (opcode & 0x00FF) {
            // 0x00FB: Scroll the display right by 4 pixels
            case 0xFB -> display.scrollRight(4);
            // 0x00FC: Scroll the display left by 4 pixels
            case 0xFC -> display.scrollLeft(4);
            // 0x00FD: Exit the interpreter
            case 0xFD -> running = false;
            // SuperCHIP allows for a high-res mode
            // which allows double the length and width
            // (a.k.a 4x the area)
            // although not every SuperCHIP program uses this
            // extra functionality.
            // 0x00FE: Turns high-res off
            case 0xFE -> display.setHighRes(false);
            // 0x00FF: What do you think
            case 0xFF -> display.setHighRes(true);
            default -> {
                // 0x00CN: Scrolls the screen down N pixels
                // Why this one allows for an argument and the others don't
                // despite there being more than enough room for them
                // to each be their own command is beyond me.
                // Guess they just like scrolling down more
                if ((opcode & 0x00F0) == 0x00C0) {
                    int n = opcode & 0x000F;
                    display.scrollDown(n);
                } else {
                    // Handles the base CHIP-8 instructions
                    // or invalid opcodes
                    super.executeZeroInstruction(opcode);
                }
            }
        }
    }

    @Override
    protected void executeDrawInstruction(int opcode) {
        // 0xDXY0: Draw a 16x16 sprite at coordinates VX, VY
        // You don't get to choose the height of this one
        // VF has the same interactions with this as with 0xDXYN
        // (see the Chip8Interpreter's executeDrawInstruction and drawSprite)
        if ((opcode & 0x000F) != 0) {
            super.executeDrawInstruction(opcode);
            // Don't want to be double drawing things.
            return;
        }

        int x = registers[(opcode & 0x0F00) >> 8]; // Get VX
        int y = registers[(opcode & 0x00F0) >> 4]; // Get VY

        registers[0xF] = drawSprite(x, y, 16, 16);
    }

    @Override
    protected void executeFInstruction(int opcode) {
        int x = (opcode & 0x0F00) >> 8;
        int nn = opcode & 0x00FF;

        switch (nn) {
            // 0xFX30: Make I point to VX's character in the large font
            // Each character is 10 bytes long
            case 0x30 -> {
                if (registers[x] > 10) {
                    throw new CHIP8InvalidOpcodeError(opcode);
                }

                I = LARGE_FONT_SET_START_ADDRESS + registers[x] * 10;
            }
            case 0x75 -> {
                if (x > 7) {
                    throw new CHIP8InvalidOpcodeError(opcode);
                }

                for (int i = 0; i < x; i++) {
                    rplFlags[i] = (byte) registers[i];
                }
            }
            case 0x85 -> {
                if (x > 7) {
                    throw new CHIP8InvalidOpcodeError(opcode);
                }

                for (int i = 0; i < x; i++) {
                    registers[i] = toInt(rplFlags[i]);
                }
            }
            default -> super.executeFInstruction(opcode);
        }
    }
}
