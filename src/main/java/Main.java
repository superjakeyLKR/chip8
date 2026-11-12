import gui.EmulatorConfig;
import gui.Launcher;
import javafx.application.Application;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

public class Main {
    void main(String[] args) {
        if (args.length == 0) {
            Application.launch(Launcher.class);

            // It will try to run the rest of it after closing the window
            return;
        }

        String interpreter = args[0];

        final List<String> help = List.of("help", "-help", "-h", "?");

        if (help.contains(interpreter)) {
            System.out.println("Usage: java -jar chip8.jar <INTERPRETER> <FILE> [CONFIG]");
            System.out.println("<INTERPRETER>: Which interpreter you'd like to selectInterpreter");
            System.out.println("CHIP: The base CHIP-8 interpreter");
            System.out.println("SCHIP: The Super CHIP-8 interpreter");
            System.out.println("<FILE>: The filepath to the file to run");
            System.out.println("It should probably be either a .ch8, .rom or .bin");
            System.out.println("[CONFIG]: The filepath to the config file");
            System.out.println("It is optional, and is much easier to edit in the GUI");
            return;
        }

        if (args.length < 2) {
            System.out.println("Error: Missing ROM path");
            return;
        }

        String romFilePath = args[1];

        EmulatorConfig emulatorConfig = null;

        if (args.length >= 3) {
            Path emulatorConfigPath = Path.of(args[2]);

            try {
                emulatorConfig = EmulatorConfig.loadFrom(emulatorConfigPath);
            } catch (IOException e) {
                // No more compact source class :(
                // (The class name can't be gotten from a
                // compact source file)
                Logger.getLogger(Main.class.getName())
                        .severe("Failed to load emulator configuration: "
                                + e.getMessage()
                        );
            }
        }

        Window window;

        if (emulatorConfig == null) {
            window = new Window(interpreter, romFilePath);
        } else {
            window = new Window(interpreter, romFilePath, emulatorConfig);
        }

        // Or you could just do:
        // Window window = new Window();
        // window.selectInterpreter(args[0]);
        // window.loadROM(args[1]);

        window.run();
    }
}
