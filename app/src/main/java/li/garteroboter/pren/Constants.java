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

    String WEBSOCKET_URI =   "ws://pren.garteroboter.li:80/ws/";
    String LOCAL_WEBSOCKET_URI =   "ws://192.168.188.38:80/ws/";
    String GARTEROBOTERLI = "pren.garteroboter.li";
    String START_COMMAND_ESP32 = "start";


    String STOP_COMMAND_ESP32 = "stop";
}
