package core;

import java.util.Arrays;

public class Display {
    public static final int WIDTH = 64; // Width of the display in pixels
    public static final int HEIGHT = 32; // Height of the display in pixels

    // Yes, it's a 1D array, but we can treat it as a 2D array
    // by calculating the index using the formula: index = y * WIDTH + x
    // Each pixel can be either on (true) or off (false)
    private boolean[] pixels = new boolean[WIDTH * HEIGHT];

    public Display() {
        clear(); // Initialize the display by clearing it
    }

    public void clear() {
        // Set all pixels to false (off)
        Arrays.fill(pixels, false);
    }

    public int getIndex(int x, int y) {
        // Calculate the index in the 1D array
        return y * WIDTH + x;
    }

    public boolean getPixel(int x, int y) {
        int index = getIndex(x, y);
        return pixels[index];
    }

    public void setPixel(int x, int y, boolean value) {
        // Calculate the index in the 1D array
        int index = getIndex(x, y);
        pixels[index] = value;
    }
}
