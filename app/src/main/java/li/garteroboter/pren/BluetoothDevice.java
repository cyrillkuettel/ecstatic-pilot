package li.garteroboter.pren;

/**
 * Simple Dataclass to Store all Bluetooth devices
 */

public class BluetoothDevice {

     String deviceName;
     String deviceHardwareAddress;

    public BluetoothDevice(String deviceName, String deviceHardwareAddress) {
        this.deviceName = deviceName;
        this.deviceHardwareAddress = deviceHardwareAddress;
    }

    @Override
    public String toString() {
        return "BluetoothDevice{" +
                "deviceName='" + deviceName + '\'' +
                ", deviceHardwareAddress='" + deviceHardwareAddress + '\'' +
                '}';
    }
}
