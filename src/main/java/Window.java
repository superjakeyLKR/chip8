import core.Display;
import core.Interpreter;
import core.Keypad;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import java.io.IOException;
import java.util.logging.Logger;

public class Window {
    private Interpreter interpreter; // The CHIP-8 interpreter

    private long window; // GLFW window handle

    private final double CPU_CLOCK_SPEED = 700; // Instructions per second
    private final double CYCLE_TIME = 1_000_000_000d / CPU_CLOCK_SPEED;
    private final double TIMER_INTERVAL = 1_000_000_000d / 60;

    private final Display display;
    private final DisplayRenderer displayRenderer;
    private final Keypad keypad; // The keypad for input

    public Window() {
        display = new Display();
        displayRenderer = new DisplayRenderer();
        keypad = new Keypad();
    }

    // Assumes CHIP-8 interpreter by default
    public Window(String filePath) {
        display = new Display();
        displayRenderer = new DisplayRenderer();
        keypad = new Keypad();

        use("CHIP");
        loadROM(filePath);
    }

    public Window(String interpreter, String filePath) {
        display = new Display();
        displayRenderer = new DisplayRenderer();
        keypad = new Keypad();

        use(interpreter);
        loadROM(filePath);
    }

    public String currentROMPath;

    // Load the ROM into the interpreter's memory
    public void loadROM(String filePath) {
        try {
            interpreter.loadROM(filePath);
            currentROMPath = filePath;
        } catch (IOException e) {
            Logger.getLogger(Window.class.getName())
                    .severe("Failed to load ROM: " + e.getMessage());
        }
    }

    // What interpreter should be used
    public void use(String choice) {
        switch (choice) {
            case "CHIP" -> interpreter = new Interpreter(display, keypad);
            // TODO: Change this when the SUPER Interpreter is actually made
            case "SCHIP" -> interpreter = new Interpreter(display, keypad);
            default -> throw new IllegalArgumentException("Unknown interpreter: " + choice);
        }
    }

    public void run() {
        if (interpreter == null) {
            throw new RuntimeException("Interpreter not specified (run `window.use(interpreter)` first)");
        }

        if (currentROMPath == null) {
            throw new RuntimeException("No ROM has been set (run `window.loadROM(pathToROM)` first)");
        }

        // Print errors to System.err which is like System.out but for errors.
        GLFWErrorCallback.createPrint(System.err).set();

        // Try to initialize GLFW. None of the GLFW stuff will work
        // if this isn't done first.
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // Create a windowed mode window and its OpenGL context
        window = GLFW.glfwCreateWindow(
                800,
                600,
                "CHIP-8 Emulator",
                0,
                0
        );

        // The window being 0 means that the window creation failed.
        if (window == 0) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        makeKeyboardCallbacks(); // Set up the keyboard callbacks for input

        // Make the OpenGL context current
        GLFW.glfwMakeContextCurrent(window);

        // Initialize OpenGL capabilities, to use OpenGL functions.
        GL.createCapabilities();

        // Initialize the display renderer,
        // which sets up the OpenGL projection matrix
        displayRenderer.init();

        // Enable v-sync
        // (A.k.a button that does nothing)
        GLFW.glfwSwapInterval(1);

        // Get the time
        double lastCycle = System.nanoTime();

        double nextTimerUpdate = System.nanoTime() + TIMER_INTERVAL;

        // Loop until the user closes the window
        while (!GLFW.glfwWindowShouldClose(window)) {
            // TESTING: Render a test pattern on the display
            // renderTest();

            // Poll for window events (keyboard input, mainly)
            GLFW.glfwPollEvents();

            // Get the time (again)
            double currentTime = System.nanoTime();

            // This should run ~CPU_CLOCK_SPEED instructions per second.
            // 700Hz is around the amount that an actual CHIP-8
            // running device would run at.
            while (currentTime - lastCycle >= CYCLE_TIME) {
                interpreter.cycle(); // Execute one cycle of the CHIP-8 interpreter
                lastCycle += CYCLE_TIME;
            }

            // The timers update at a fixed 60Hz,
            // independent of the CPU's clock cycles
            while (currentTime >= nextTimerUpdate) {
                interpreter.decrementTimers();

                nextTimerUpdate += TIMER_INTERVAL;
            }

            // Render the display to the window
            displayRenderer.render(display);

            // Swap the colour buffers
            // This is because OpenGL uses double buffering, which means that
            // there are two buffers, one that is being drawn to
            // and one that is being displayed
            // This is done to prevent flickering and tearing, which is when the image
            // is not drawn correctly and looks like it's tearing apart.
            GLFW.glfwSwapBuffers(window);
        }

        // Clean up after the window is closed

        // Free the window callbacks from memory and destroy the window
        GLFW.glfwDestroyWindow(window);

        // Terminate GLFW and free the error callback
        GLFW.glfwTerminate();
    }

    private void makeKeyboardCallbacks() {
        GLFW.glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            int chip8Key = keypad.mapKey(key); // Map the GLFW key to a CHIP-8 key

            if (chip8Key == -1) {
                return; // Ignore keys that are not mapped to CHIP-8 keys
            }

            if (action == GLFW.GLFW_PRESS) {
                keypad.pressKey(key);
            } else if (action == GLFW.GLFW_RELEASE) {
                keypad.releaseKey(key);
            }
        });
    }

    private void renderTest() {
        display.setPixel(10, 10, true);
        display.setPixel(11, 10, true);
        display.setPixel(12, 10, true);
        display.setPixel(10, 11, true);
        display.setPixel(10, 12, true);
    }
}
