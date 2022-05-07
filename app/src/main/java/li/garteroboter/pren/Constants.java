package li.garteroboter.pren;


import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class Constants {

    public static final String FROM_BINARY_START_COMMAND_ESP32 = "^A";
    public static final String START_COMMAND_ESP32 = "1";
    public static final String STOP_COMMAND_ESP32 = "0";

    public static final String WEBSOCKET_URI =   "wss://pren.garteroboter.li:80/ws/";
    public static final String LOCAL_WEBSOCKET_URI =   "ws://192.168.188.38:80/ws/";
    public static final String GARTEROBOTERLI_HOSTNAME = "pren.garteroboter.li";

    public static final String GARTEROBOTERLI = "garteroboterli";
    public static final String ESP32 = "esp32";


    public static final Set<String> ACCEPTED_ESP32_DEVICE_NAMES =  new HashSet<String>(Arrays.asList(
            GARTEROBOTERLI,
            ESP32));

    public static final String ESP32_BLUETOOTH_MAC_ADDRESS = "4C:EB:D6:75:AB:4E"; // Dave's ESP32
    public static final String TESTING_ESP32_BLUETOOTH_MAC_ADDRESS = "58:BF:25:81:D7:BE"; // ESP32 currently used for testing


    public static final Set<String> ESP_MAC_ADRESES = new HashSet<>(Arrays.asList(
            ESP32_BLUETOOTH_MAC_ADDRESS,
            TESTING_ESP32_BLUETOOTH_MAC_ADDRESS));
}
