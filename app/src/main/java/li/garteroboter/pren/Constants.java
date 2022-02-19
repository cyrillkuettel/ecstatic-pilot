package li.garteroboter.pren;


public interface Constants {

    // Message types sent from the BluetoothChatService Handler
    int MESSAGE_STATE_CHANGE = 1;
    int MESSAGE_READ = 2;
    int MESSAGE_WRITE = 3;
    int MESSAGE_DEVICE_NAME = 4;
    int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    String DEVICE_NAME = "device_name";
    String TOAST = "toast";

    String WEBSOCKET_URI =   "wss://pren.garteroboter.li:80/ws/";
    String LOCAL_WEBSOCKET_URI =   "ws://192.168.188.38:80/ws/";
    String GARTEROBOTERLI = "pren.garteroboter.li";
    String START_COMMAND_ESP32 = "start";
    String ESP32_NAME = "ESP32";
    String ESP32_MAC_ADDRESS = "4C:EB:D6:75:AB:4E";

    String STOP_COMMAND_ESP32 = "stop";


    String ANSI_YELLOW = "\033[33m";
    String ANSI_GREEN = "\033[32m";
    String ANSI_CYAN = "\033[36m";
    String ANSI_RESET = "\033[0m";
}
