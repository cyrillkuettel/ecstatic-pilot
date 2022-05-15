package simple.bluetooth.terminal;

import static li.garteroboter.pren.Constants.ACCEPTED_ESP32_DEVICE_NAMES;
import static li.garteroboter.pren.Constants.ESP32_BLUETOOTH_MAC_ADDRESS;
import static li.garteroboter.pren.Constants.ESP_MAC_ADRESES;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.ListFragment;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

import li.garteroboter.pren.R;

public class DevicesFragment extends ListFragment {

    private static final String TAG = "DevicesFragment";

    private BluetoothAdapter bluetoothAdapter;
    private final ArrayList<BluetoothDevice> listItems = new ArrayList<>();
    private ArrayAdapter<BluetoothDevice> listAdapter;

    private boolean autoConnectToESP32;

    public static DevicesFragment newInstance() {
        DevicesFragment fragment = new DevicesFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        autoConnectToESP32 = parseAutoConnectArgs();


        if (getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            listAdapter = new ArrayAdapter<BluetoothDevice>(getActivity(), 0, listItems) {
                @SuppressLint("MissingPermission")
                @NonNull
                @Override
                public View getView(int position, View view, @NonNull ViewGroup parent) {
                    BluetoothDevice device = listItems.get(position);
                    if (view == null)
                        view = getActivity().getLayoutInflater().inflate(R.layout.device_list_item,
                                parent, false);
                    TextView textViewDeviceName = view.findViewById(R.id.textViewDeviceName);
                    TextView textViewDeviceAddress = view.findViewById(R.id.textViewDeviceAddress);
                    textViewDeviceName.setText(device.getName());
                    textViewDeviceAddress.setText(device.getAddress());
                    return view;
                }
            };
        }

    }

    // In some situations, we might not want to automatically connect to ESP. For example,
    // if there is no device at hand and we just want to test.
    private boolean parseAutoConnectArgs() {
        boolean autoConnectToESP32 = false;
        if (getArguments() != null) {
            try {
                String autoConnect = getArguments().getString("autoConnect");
                if (autoConnect != null && autoConnect.equals("true")) {
                    Log.d(TAG, "autoConnectToESP32  == true");
                    autoConnectToESP32 = true;
                } else {
                    Log.e(TAG, "autoConnectToESP32  == false");
                }
            } catch (Exception e) {
                Log.e(TAG, "Attempted to get Arguments in Fragment startup. " +
                        "The autoConnectToESP32 Property was not found.");
                e.printStackTrace();
            }
        }
        return autoConnectToESP32;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setListAdapter(null);
        View header = getActivity().getLayoutInflater().inflate(R.layout.device_list_header, null
                , false);
        getListView().addHeaderView(header, null, false);
        setEmptyText("initializing...");
        ((TextView) getListView().getEmptyView()).setTextSize(18);
        setListAdapter(listAdapter);
    }


    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume()");

        if (bluetoothAdapter == null) {
            setEmptyText("<bluetooth not supported>");
            return;
        } else if (!bluetoothAdapter.isEnabled()) {
            setEmptyText("<Bluetooth is disabled on device>");
            return;
        } else if (!autoConnectToESP32) {
            setEmptyText("Toggle Bluetooth in App-Settings. ");
            return; // don't scan if we don't use bluetooth
        }
        refreshBluetoothDevices();
    }


    @SuppressLint("MissingPermission")
    void refreshBluetoothDevices() {
        listItems.clear();
        if (bluetoothAdapter != null) {
            final Set<BluetoothDevice> scannedDevices = bluetoothAdapter.getBondedDevices();
            if (scannedDevices != null) {
                for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
                    if (device.getType() != BluetoothDevice.DEVICE_TYPE_LE) {
                        if (isRelevantBluetoothDevice(device)) {
                            listItems.add(device);
                        }
                    }
                }
                if (listItems.isEmpty()) {
                    setEmptyText("<no bluetooth devices matching the provided MAC-Addresses>");
                }
                if (scannedDevices.isEmpty()) {
                    setEmptyText("<Couldn't find a single Bluetooth Device>");
                }
                Log.d(TAG, String.format("listItems.size == %s", listItems.size()));
                listItems.sort(DevicesFragment::compareTo);
                listAdapter.notifyDataSetChanged();

                manageAutoConnection();
            }
        } else {
            Log.e(TAG, "bluetooth Adapter == null");
        }
    }

    private void manageAutoConnection() {
        if (autoConnectToESP32) {
            Log.d(TAG, "autoConnectToESP ");
            if (listItems.size() == 1) {
                BluetoothDevice ESP32_Device = listItems.get(0);
                initializeTerminalFragment(ESP32_Device);
            } else {
                // there may be multiple devices with the name "ESP32"
                // here I can check for the MAC Address.
                // This can be useful when there are multiple ESP32 devices in the area.
                // Probably not, but you never know.
                for (BluetoothDevice device : listItems) {
                    if (Objects.equals(device.getAddress(), ESP32_BLUETOOTH_MAC_ADDRESS)) {
                        initializeTerminalFragment(device);
                    }
                }
            }
        }
    }

    private boolean isRelevantBluetoothDevice(BluetoothDevice device) {
        final String bluetoothDeviceAddress = device.getAddress().toUpperCase();
        Log.e(TAG, bluetoothDeviceAddress);
        if (!ESP_MAC_ADRESES.contains(bluetoothDeviceAddress)) {
            Log.d(TAG, String.format("Not a relevant bluetooth devie %s " +
                    "is not present in the ESP_MAC_ADRESES Constant:", bluetoothDeviceAddress));
            Log.d(TAG, ESP_MAC_ADRESES.toString());
            return false;
        }
        return true;
    }

    private boolean isRelevantBluetoothDeviceByName(BluetoothDevice device) {
        @SuppressLint("MissingPermission") final String bluetoothDeviceName =
                device.getName().toLowerCase();
        return ACCEPTED_ESP32_DEVICE_NAMES.contains(bluetoothDeviceName);
    }

    @Override
    public void onListItemClick(@NonNull ListView l, @NonNull View v, int position, long id) {
        BluetoothDevice device = listItems.get(position - 1);
        initializeTerminalFragment(device);
    }

    public void initializeTerminalFragment(BluetoothDevice device) {
        Bundle args = new Bundle();
        args.putString("device", device.getAddress());

        Fragment terminalFragment = new TerminalFragment();
        terminalFragment.setArguments(args);

        if (getActivity() == null) {
            Log.e(TAG, "FATAL: getActivity() == null");
            return;
        }
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.fragmentBluetoothChain, terminalFragment,
                "terminal").addToBackStack("terminal").commit();
    }

    /**
     * sort by name, then address. sort named devices first
     */
    static int compareTo(BluetoothDevice a, BluetoothDevice b) {
        @SuppressLint("MissingPermission") boolean aValid =
                a.getName() != null && !a.getName().isEmpty();
        @SuppressLint("MissingPermission") boolean bValid =
                b.getName() != null && !b.getName().isEmpty();
        if (aValid && bValid) {
            @SuppressLint("MissingPermission") int ret = a.getName().compareTo(b.getName());
            if (ret != 0) return ret;
            return a.getAddress().compareTo(b.getAddress());
        }
        if (aValid) return -1;
        if (bValid) return +1;
        return a.getAddress().compareTo(b.getAddress());
    }

    public DevicesFragment() {
        // required empty constructor
    }
}
