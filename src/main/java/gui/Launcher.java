package gui;

import atlantafx.base.theme.PrimerDark;
import core.InterpreterType;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.geometry.Insets;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

public class Launcher extends Application {
    private File rom;
    private InterpreterType interpreterType;
    private EmulatorConfig emulatorConfig;

    @Override
    public void start(Stage stage) {
        Label title = new Label("CHIP-8 Interpreter");

        title.setStyle("-fx-font-size: 24px;");

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

                Path configFile = Files.createTempFile("idc-8", "cfg");
                emulatorConfig.saveTo(configFile);

                // This is basically like calling it from the command line.
                ProcessBuilder processBuilder = new ProcessBuilder(
                        java,
                        "-cp", classPath,
                        "Main",
                        // Yep, they all do this in a different way
                        // getValue() is for ComboBoxes
                        // getAbsolutePath() is for Files
                        // toAbsolutePath().toString() is for Paths
                        interpreterChoice.getValue(),
                        rom.getAbsolutePath(),
                        configFile.toAbsolutePath().toString()
                );

                processBuilder.inheritIO();
                Process process = processBuilder.start();

                // Delete the config file after closing the interpreter
                // because once it's finished, there's no real reason
                // to keep it around.
                process.onExit().thenRun(() -> {
                    try {
                        Files.deleteIfExists(configFile);
                    } catch (IOException e) {
                        Logger.getLogger(Launcher.class.getName())
                                .severe("Failed to delete configuration file: "
                                        + e.getMessage()
                                );
                    }
                });

            } catch (Exception e) {
                Logger.getLogger(Launcher.class.getName())
                        .severe("Failed to start interpreter: " + e.getMessage());
            }
        });

        // Config stuff
        emulatorConfig = new EmulatorConfig();

        Button configButton = new Button("Config");

        configButton.setOnAction(event -> {
            Stage configStage = new Stage();

            ConfigPanel configPanel = new ConfigPanel(emulatorConfig);

            ScrollPane scrollPane = new ScrollPane(configPanel);
            scrollPane.setFitToWidth(true);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollPane.setStyle("-fx-background: -color-bg-default; -fx-background-color: -color-bg-default;");

            Scene configScene = new Scene(scrollPane, 350, 450);

            configStage.setTitle("It Does CHIP-8! - Emulator Settings");
            configStage.initOwner(stage);
            // You know those popup windows that stop you from
            // doing anything in the main window before you do what they want?
            // This does that.
            configStage.initModality(Modality.WINDOW_MODAL);
            configStage.setScene(configScene);
            configStage.show();
        });

        // New layout stuff

        // How big the stuff should be length-wise
        interpreterChoice.setPrefWidth(120);
        browseFilesButton.setPrefWidth(120);
        startButton.setPrefWidth(100);
        configButton.setPrefWidth(100);

        BorderPane root = new BorderPane();

        Label interpreterTitle = new Label("Interpreter");
        Label romTitle = new Label("ROM");

        VBox interpreterSection = new VBox(
                8,
                interpreterTitle,
                interpreterChoice
        );

        VBox romSection = new VBox(
                8,
                romTitle,
                selectedROM,
                browseFilesButton
        );

        interpreterSection.setAlignment(Pos.CENTER);
        romSection.setAlignment(Pos.CENTER);

        HBox header = new HBox(12, title);
        VBox content = new VBox(20, interpreterSection, romSection);
        HBox footer = new HBox(20, configButton, startButton);

        header.setAlignment(Pos.CENTER);
        content.setAlignment(Pos.CENTER);
        footer.setAlignment(Pos.CENTER);

        root.setTop(header);
        root.setCenter(content);
        root.setBottom(footer);

        // RIP old layout
//        VBox layout = new VBox(
//                15,
//                title,
//                new Label("Interpreter"),
//                interpreterChoice,
//                new Label("ROM"),
//                selectedROM,
//                browseFilesButton,
//                configButton,
//                startButton
//        );

        BorderPane.setMargin(header, new Insets(25, 25, 10, 25));
        BorderPane.setMargin(content, new Insets(10, 25, 10, 25));
        BorderPane.setMargin(footer, new Insets(10, 25, 25, 25));

        Scene scene = new Scene(
                root,
                450,
                400
        );

        // Add the CSS to make the window not look horrible
        Application.setUserAgentStylesheet(
                new PrimerDark().getUserAgentStylesheet()
        );

        stage.setTitle("It Does CHIP-8!");
        stage.setScene(scene);
        stage.show();
    }
}
