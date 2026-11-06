package core;

import java.util.Arrays;

public class Display {
    public static final int LOW_WIDTH = 64; // Width of low-res display in pixels
    public static final int LOW_HEIGHT = 32; // Height of the low-res display in pixels
    // The high-res display is twice the width and height
    public static final int HIGH_WIDTH = LOW_WIDTH * 2;
    public static final int HIGH_HEIGHT = LOW_HEIGHT * 2;

    // Yes, it's a 1D array, but we can treat it as a 2D array
    // by calculating the index using the formula: index = y * width + x
    // Each pixel can be either on (true) or off (false)
    // We assume that the array is the biggest size it can be,
    // because you can't make it any bigger at runtime
    private boolean[] pixels = new boolean[HIGH_WIDTH * HIGH_HEIGHT];

    private int width = LOW_WIDTH;
    private int height = LOW_HEIGHT;

    public Display() {
        clear(); // Initialize the display by clearing it
    }

    public boolean isLowRes() {
        return width == LOW_WIDTH;
    }

    public void clear() {
        // Set all pixels to false (off)
        Arrays.fill(pixels, false);
    }

    public int getIndex(int x, int y) {
        // Calculate the index in the 1D array
        return y * HIGH_WIDTH + x;
    }

    public boolean getPixel(int x, int y) {
        // Each pixel on low-res mode is instead a 2x2 pixel
        int scale = isLowRes() ? 2 : 1;

        int index = getIndex(x * scale, y * scale);
        return pixels[index];
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void setPixel(int x, int y, boolean value) {
        int scale = isLowRes() ? 2 : 1;

        // As the pixels can be two by two, each part of it
        // must be set
        for (int dy = 0; dy < scale; dy++) {
            for (int dx = 0; dx < scale; dx++) {
                pixels[getIndex(x * scale + dx, y * scale + dy)] = value;
            }
        }
    }

    public void setHighRes(boolean highRes) {
        if (highRes) {
            width = HIGH_WIDTH;
            height = HIGH_HEIGHT;
        } else {
            width = LOW_WIDTH;
            height = LOW_HEIGHT;
        }

        // Setting the resolution should clear the screen
        clear();
    }

    // Note: When scrolling, any pixels that go out of the screen
    // are VOIDED and do not wrap around.
    public void scrollLeft(int amount) {
        boolean[] newPixels = new boolean[pixels.length];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int newX = x - amount;

                if (newX >= 0) {
                    newPixels[getIndex(newX, y)] = getPixel(x, y);
                }
            }
        }

        pixels = newPixels;
    }

    public void scrollRight(int amount) {
        boolean[] newPixels = new boolean[pixels.length];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int newX = x + amount;

                if (newX < width) {
                    newPixels[getIndex(newX, y)] = getPixel(x, y);
                }
            }
        }

        pixels = newPixels;
    }

    public void scrollDown(int amount) {
        boolean[] newPixels = new boolean[pixels.length];

        for (int y = 0; y < height; y++) {
            int newY = y + amount;

            if (newY < height) {
                for (int x = 0; x < width; x++) {
                    newPixels[getIndex(x, newY)] = getPixel(x, y);
                }
            }
        }

        pixels = newPixels;
    }
}
