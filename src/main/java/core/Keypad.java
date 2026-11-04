package core;

import org.lwjgl.glfw.GLFW;

import java.util.Arrays;

public class Keypad {
    // The CHIP-8 has a 16-key hexadecimal keypad (0x0-0xF)
    // The keys are arranged in a 4x4 grid like this:
    // 1 2 3 C
    // 4 5 6 D
    // 7 8 9 E
    // A 0 B F
    // However, because no modern keyboard looks anywhere close to that,
    // we will map the keys to a more standard layout:
    // 1 2 3 4
    // Q W E R
    // A S D F
    // Z X C V
    // This means that the keys will be mapped as follows:
    public int mapKey(int key) {
        return switch (key) {
            case GLFW.GLFW_KEY_1 -> 0x1;
            case GLFW.GLFW_KEY_2 -> 0x2;
            case GLFW.GLFW_KEY_3 -> 0x3;
            case GLFW.GLFW_KEY_4 -> 0xC;
            case GLFW.GLFW_KEY_Q -> 0x4;
            case GLFW.GLFW_KEY_W -> 0x5;
            case GLFW.GLFW_KEY_E -> 0x6;
            case GLFW.GLFW_KEY_R -> 0xD;
            case GLFW.GLFW_KEY_A -> 0x7;
            case GLFW.GLFW_KEY_S -> 0x8;
            case GLFW.GLFW_KEY_D -> 0x9;
            case GLFW.GLFW_KEY_F -> 0xE;
            case GLFW.GLFW_KEY_Z -> 0xA;
            case GLFW.GLFW_KEY_X -> 0x0;
            case GLFW.GLFW_KEY_C -> 0xB;
            case GLFW.GLFW_KEY_V -> 0xF;
            default -> -1; // Return -1 for keys that are not mapped to CHIP-8 keys
        };
    }

    public Keypad() {
        clear(); // Initialize the keypad by clearing it
    }

    // The state of each key (pressed or not pressed)
    private boolean[] keys = new boolean[16];

    public void pressKey(int key) {
        int mappedKey = mapKey(key);
        keys[mappedKey] = true;
    }

    public void releaseKey(int key) {
        int mappedKey = mapKey(key);
        keys[mappedKey] = false;
    }

    public boolean isPressed(int key) {
        // Because registers can hold up to 0xFF,
        // and there isn't that many keys
        if (key < 0 || key >= keys.length) {
            return false;
        }

        return keys[key];
    }

    // Get the first key that is currently pressed
    public int getPressedKey() {
        for (int i = 0; i < keys.length; i++) {
            if (keys[i]) {
                return i;
            }
        }

        return -1;
    }

    public void clear() {
        // Set all keys to not pressed
        Arrays.fill(keys, false);
    }
}
