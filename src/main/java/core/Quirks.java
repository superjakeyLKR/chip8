package core;

public enum Quirks {
    /// Whether VF should be set to 0 when doing logic operations
    ///
    /// CHIP-8 does this, SuperCHIP doesn't
    RESET_VF_ON_LOGIC,
    /// Whether I should be incremented when saving or loading
    /// from memory (instructions 0xFX55 and 0xFX65)
    INCREMENT_I_ON_SAVE_LOAD,
    /// Whether drawing sprites should be limited to 60 per second
    ///
    /// (It was supposed to stop flickering on 60hz screens)
    DISPLAY_WAIT,
    /// Whether sprites should wrap around the screen if
    /// they are drawn completely off one side of the screen
    CLIP_SPRITES,
    /// Whether the bitshift instructions should shift VY and store
    /// it in VX instead of shifting VX itself
    SHIFT_VY,
    /// Whether 0xBNNN should jump to NNN + VX instead of NNN + V0
    JUMP_WITH_VX
}
