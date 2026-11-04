import gui.Launcher;
import javafx.application.Application;

void main(String[] args) {
    if (args.length == 0) {
        Application.launch(Launcher.class);

        // It will try to run the rest of it after closing the window
        return;
    }

    final List<String> help = List.of("help", "-help", "-h", "?");

    if (help.contains(args[0])) {
        System.out.println("Usage: java -jar chip8.jar <INTERPRETER> <FILE>");
        System.out.println("<INTERPRETER>: Which interpreter you'd like to selectInterpreter");
        System.out.println("CHIP: The base CHIP-8 interpreter");
        System.out.println("SCHIP: The Super CHIP-8 interpreter");
        System.out.println("<FILE>: The filepath to the file to run");
        System.out.println("It should probably be either a .ch8, .rom or .bin");
        return;
    }

    if (args.length < 2) {
        System.out.println("Error: Missing ROM path");
        return;
    }

    Window window = new Window(args[0], args[1]);

    // Or you could just do:
    // Window window = new Window();
    // window.selectInterpreter(args[0]);
    // window.loadROM(args[1]);

    window.run();
}
