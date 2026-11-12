package gui;

import core.Quirks;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.CheckBox;
import javafx.scene.paint.Color;

public class ConfigPanel extends VBox {
    private final EmulatorConfig emulatorConfig;

    public ConfigPanel(EmulatorConfig emulatorConfig) {
        this.emulatorConfig = emulatorConfig;

        build();
    }

    private void build() {
        Label configTitle = new Label("Configs");

        configTitle.setStyle("-fx-font-size: 24px;");

        HBox header = new HBox(12, configTitle);
        header.setAlignment(Pos.CENTER);

        Label quirksLabel = new Label("Quirks:");

        Label quirksWarning = new Label("""
                If you don't know what these do, don't change them.
                Some programs may require the specific quirks of
                certain versions of CHIP-8 to run.""");

        VBox quirkCheckboxes = new VBox(10);

        for (Quirks quirk : Quirks.values()) {
            String label = quirk.name().replace("_", " ");
            boolean state = emulatorConfig.isQuirkEnabled(quirk);

            CheckBox checkbox = new CheckBox(label);

            checkbox.setSelected(state);

            checkbox.selectedProperty().addListener(
                    (_, _, newValue) ->
                            emulatorConfig.setQuirk(quirk, newValue)
            );

            quirkCheckboxes.getChildren().add(checkbox);
        }

        quirkCheckboxes.setAlignment(Pos.CENTER_LEFT);
        quirkCheckboxes.setPadding(new Insets(8, 0, 0, 15));

        VBox quirksSection = new VBox(8, quirksLabel, quirksWarning, quirkCheckboxes);
        quirksSection.setAlignment(Pos.CENTER);

        Label bgLabel = new Label("Background Colour:");
        Label displayLabel = new Label("Display Colour:");

        ColorPicker bgPicker = new ColorPicker(
                emulatorConfig.getBgColourAsColor()
        );

        ColorPicker displayPicker = new ColorPicker(
                emulatorConfig.getDisplayColourAsColor()
        );

        bgPicker.setOnAction(event -> {
            Color selected = bgPicker.getValue();
            emulatorConfig.setBgColour(selected);
        });

        displayPicker.setOnAction(event -> {
            Color selected = displayPicker.getValue();
            emulatorConfig.setDisplayColour(selected);
        });

        bgPicker.setPrefSize(120, 40);
        displayPicker.setPrefSize(120, 40);

        VBox colourSection = new VBox(
                8, bgLabel, bgPicker, displayLabel, displayPicker
        );
        colourSection.setAlignment(Pos.CENTER);

        VBox layout = new VBox(25, header,
                quirksSection,
                colourSection);

        getChildren().add(layout);
    }
}
