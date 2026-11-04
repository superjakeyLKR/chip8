package gui;

import core.InterpreterType;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.geometry.Insets;

import java.io.File;
import java.util.logging.Logger;

public class Launcher extends Application {
    private File rom;
    private InterpreterType interpreterType;

    @Override
    public void start(Stage stage) {
        Label title = new Label("CHIP-8 Interpreter");
        Label selectedROM = new Label("No ROM selected");

        Button browseFilesButton = new Button("Browse files...");
        Button startButton = new Button("Start");

        ComboBox<String> interpreterChoice = new ComboBox<>();
        interpreterChoice.getItems().addAll("CHIP-8", "Super CHIP");
        interpreterChoice.setValue("CHIP-8");

        // This allows the user to open their file explorer (or equivalent)
        // to pick the ROM
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select ROM...");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(
                        "CHIP-8 ROMs",
                        "*.ch8", "*.rom", "*.bin"
                )
        );

        browseFilesButton.setOnAction(event -> {
            File file = fileChooser.showOpenDialog(stage);

            if (file != null) {
                rom = file;
                selectedROM.setText("Current ROM: " + file.getName());
                startButton.setDisable(false);
            }
        });

        startButton.setDisable(true);

        // This is because GLFW needs its own thread to this
        // otherwise the Window wouldn't be able to be paused
        // without pausing this, which would make it impossible to
        // unpause it. Not ideal, and neither is this, but whatever.
        startButton.setOnAction(event -> {
            try {
                String executable = System.getProperty("os.name")
                        .toLowerCase().contains("win") ? "java.exe" : "java";

                String java = System.getProperty("java.home")
                        + "/bin/" + executable;

                String classPath = System.getProperty("java.class.path");

                // This is basically like calling it from the command line.
                ProcessBuilder process = new ProcessBuilder(
                        java,
                        "-cp", classPath,
                        "Main",
                        interpreterChoice.getValue(),
                        rom.getAbsolutePath()
                );

                process.inheritIO();
                process.start();

            } catch (Exception e) {
                Logger.getLogger(Launcher.class.getName())
                        .severe("Failed to start interpreter: " + e.getMessage());
            }
        });

        VBox layout = new VBox(
                15,
                title,
                new Label("Interpreter"),
                interpreterChoice,
                new Label("ROM"),
                selectedROM,
                browseFilesButton,
                startButton
        );

        final Insets insets = new Insets(25);

        layout.setPadding(insets);

        Scene scene = new Scene(
                layout,
                400,
                350
        );

        stage.setTitle("It Does CHIP-8!");
        stage.setScene(scene);
        stage.show();
    }
}
