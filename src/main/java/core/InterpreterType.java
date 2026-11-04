package core;

import errors.InterpreterTypeUnknownError;

public enum InterpreterType {
    CHIP_8("CHIP-8"),
    SUPER_CHIP("Super CHIP");

    private final String displayName;

    InterpreterType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static InterpreterType fromString(String string) {
        switch (string.trim().toUpperCase()) {
            case "CHIP", "CHIP8", "CHIP-8" -> {
                return CHIP_8;
            }
            case "SUPER CHIP", "SUPERCHIP", "SCHIP" -> {
                return SUPER_CHIP;
            }
            default -> throw new InterpreterTypeUnknownError(string);
        }
    }
}
