package gui;

import java.util.Arrays;
import java.util.Properties;

public class ConfigTest {
    static void main(String[] args) {
        EmulatorConfig emulatorConfig = new EmulatorConfig();
        Properties properties = emulatorConfig.toProperties();

        EmulatorConfig emulatorConfig2 = EmulatorConfig.fromProperties(properties);

        System.out.println(Arrays.toString(emulatorConfig2.getBgColour()));
    }
}
