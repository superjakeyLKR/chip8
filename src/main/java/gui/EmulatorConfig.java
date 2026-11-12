package gui;

import core.Quirks;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Properties;

// Yes, I know it's not technically an emulator,
// but I'd argue that the interpreter is somewhat separate
// to this, so I'm going to call it an Emulator
public class EmulatorConfig {
    private final EnumMap<Quirks, Boolean> quirks = new EnumMap<>(Quirks.class);

    // Not exactly an inspired palette
    private float[] bgColour = { 0, 0, 0 };
    private float[] displayColour = { 0, 1, 0 };

    public float[] fromColor(Color colour) {
        return new float[] {
                (float) colour.getRed(),
                (float) colour.getGreen(),
                (float) colour.getBlue()
        };
    }

    public float[] fromString(String colour) {
        return fromColor(Color.web(colour));
    }

    public Color toColor(float[] colour) {
        return new Color(
                colour[0],
                colour[1],
                colour[2],
                1
        );
    }

    public Color toColor(float[] colour, float opacity) {
        return new Color(
                colour[0],
                colour[1],
                colour[2],
                opacity
        );
    }

    public String toHex(float[] colour) {
        return String.format(
                "#%02X%02X%02X",
                Math.round(colour[0] * 255),
                Math.round(colour[1] * 255),
                Math.round(colour[2] * 255)
        );
    }

    // Cloning it makes it so that editing the returned array will actually
    // edit the internal array, instead of just editing a copy of it.
    public float[] getBgColour() {
        return bgColour.clone();
    }

    public Color getBgColourAsColor() {
        return toColor(bgColour);
    }

    public float[] getDisplayColour() {
        return displayColour.clone();
    }

    public Color getDisplayColourAsColor() {
        return toColor(displayColour);
    }

    public void setBgColour(float[] bgColour) {
        this.bgColour = bgColour;
    }

    public void setBgColour(Color bgColour) {
        this.bgColour = fromColor(bgColour);
    }

    public void setBgColour(String bgColour) {
        this.bgColour = fromString(bgColour);
    }

    public void setDisplayColour(float[] displayColour) {
        this.displayColour = displayColour;
    }

    public void setDisplayColour(Color displayColour) {
        this.displayColour = fromColor(displayColour);
    }

    public void setDisplayColour(String displayColour) {
        this.displayColour = fromString(displayColour);
    }

    public boolean isQuirkEnabled(Quirks quirk) {
        return quirks.getOrDefault(quirk, false);
    }

    public void setQuirk(Quirks quirk, boolean enabled) {
        quirks.put(quirk, enabled);
    }

    public EnumMap<Quirks, Boolean> getQuirks() {
        return quirks;
    }

    public Properties toProperties() {
        Properties properties = new Properties();
        properties.setProperty("bgColour", toHex(bgColour));
        properties.setProperty("displayColour", toHex(displayColour));

        for (Quirks q : Quirks.values()) {
            properties.setProperty("quirks." + q.name(),
                    String.valueOf(isQuirkEnabled(q))
            );
        }

        return properties;
    }

    public static EmulatorConfig fromProperties(Properties properties) {
        EmulatorConfig emulatorConfig = new EmulatorConfig();

        emulatorConfig.setBgColour(
                properties.getProperty("bgColour", "#000000")
        );
        emulatorConfig.setDisplayColour(
                properties.getProperty("displayColour", "#00FF00")
        );

        for (Quirks q : Quirks.values()) {
            String value = properties.getProperty(
                    "quirks." + q.name()
            );

            if (value == null) continue;

            emulatorConfig.setQuirk(q, Boolean.parseBoolean(value));
        }

        return emulatorConfig;
    }

    public void saveTo(Path path) throws IOException {
        try (Writer w = Files.newBufferedWriter(path)) {
            toProperties().store(w, "CHIP-8 Emulator Config");
        }
    }

    public static EmulatorConfig loadFrom(Path path) throws IOException {
        Properties properties = new Properties();

        try (Reader r = Files.newBufferedReader(path)) {
            properties.load(r);
        }

        return fromProperties(properties);
    }
}
