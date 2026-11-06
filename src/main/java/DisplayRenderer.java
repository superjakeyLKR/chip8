import core.Display;
import org.lwjgl.opengl.GL11;

public class DisplayRenderer {
    public static final int PIXEL_SIZE = 10; // Size of each pixel in the display
    public static final float[] PIXEL_COLOUR = {0f, 1f, 0f}; // RGB colour for the pixels (green)

    public void init() {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();

        // CHIP-8 wants (0, 0) to be the top-left corner of the screen,
        // but OpenGL wants it to be the bottom-left corner.
        // So we flip the y-axis by setting the top to 0
        // and the bottom to core.Display.LOW_HEIGHT * PIXEL_SIZE.
        GL11.glOrtho(
                0,
                Display.HIGH_WIDTH * PIXEL_SIZE,
                Display.HIGH_HEIGHT * PIXEL_SIZE,
                0,
                -1,
                1
        );

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
    }


    public void render(Display display) {
        // Clear the screen to black
        GL11.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        // Clear the framebuffer
        // This is because whatever was drawn on the previous frame
        // is still there, so we need to clear it before drawing the next frame.
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        GL11.glColor3fv(PIXEL_COLOUR); // Set the colour for the pixels to be drawn

        // Quads are rectangles made up of two triangles,
        // which is what OpenGL uses to draw shapes.
        // Triangle shaped pixels aren't exactly ideal.
        GL11.glBegin(GL11.GL_QUADS);

        for (int y = 0; y < display.getHeight(); y++) {
            for (int x = 0; x < display.getWidth(); x++) {
                if (display.getPixel(x, y)) {
                    // The pixels are bigger in low-res, so that the
                    // windows take up the same size
                    int pixelSize = PIXEL_SIZE * Display.HIGH_WIDTH / display.getWidth();

                    // Draw a pixel at the specified position
                    // These are each of the four corners of the rectangle that makes up the pixel
                    GL11.glVertex2f(x * pixelSize, y * pixelSize); // Bottom-left corner
                    GL11.glVertex2f((x + 1) * pixelSize, y * pixelSize); // Bottom-right corner
                    GL11.glVertex2f((x + 1) * pixelSize, (y + 1) * pixelSize); // Top-right corner
                    GL11.glVertex2f(x * pixelSize, (y + 1) * pixelSize); // Top-left corner
                }
            }
        }

        // Tell OpenGL that we're done drawing the pixels
        GL11.glEnd();
    }
}
