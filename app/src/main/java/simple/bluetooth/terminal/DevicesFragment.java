package simple.bluetooth.terminal;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.ListFragment;

import java.util.ArrayList;

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

    public DevicesFragment() {
        // required empty constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setHasOptionsMenu(true);

        if (getArguments() != null) {
            try {
                String autoConnect = getArguments().getString("autoConnect");
                if (autoConnect != null && autoConnect.equals("true")) {
                    Log.d(TAG, "autoConnectToESP32  == true");

                    autoConnectToESP32 = true;
                } else {
                    Log.e(TAG, "autoConnectToESP32  == false");
                    autoConnectToESP32 = false;
                }

            } catch (Exception e) {
                Log.e(TAG, "failed to get autoConnect key");
                e.printStackTrace();
            }
        }


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
                    TextView text1 = view.findViewById(R.id.text1);
                    TextView text2 = view.findViewById(R.id.text2);
                    text1.setText(device.getName());
                    text2.setText(device.getAddress());
                    return view;
                }
            };
        }

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
        Log.i(TAG, "onResume()");
        super.onResume();
        if (bluetoothAdapter == null) {
            setEmptyText("<bluetooth not supported>");

        } else if (!bluetoothAdapter.isEnabled()) {
            setEmptyText("<bluetooth is disabled>");

        }else if (!autoConnectToESP32) {
            setEmptyText("Bluetooh disabled in Pilot settings");
            return; // don't scan if we don't use bluetooth
        } else {
            setEmptyText("<no bluetooth devices found>");
        }
        refresh();
    }


    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    void refresh() {
        listItems.clear();
        if (bluetoothAdapter != null) {
            for (BluetoothDevice device : bluetoothAdapter.getBondedDevices())
                if (device.getType() != BluetoothDevice.DEVICE_TYPE_LE)
                    if (device.getName().contains("ESP32")) { // check here for only ESP32
                        listItems.add(device);
                    }

        }
        listItems.sort(DevicesFragment::compareTo);
        listAdapter.notifyDataSetChanged();

        if (autoConnectToESP32) {
            autoConnectToESP();
        }

    }

    private void autoConnectToESP() {
        Log.d(TAG, "autoConnectToESP ");
        if (listItems.size() == 1) {
            BluetoothDevice ESP32_Device = listItems.get(0);
            initializeTerminalFragment(ESP32_Device);;
        } else {
            // here I can check for the MAC Address.
            // This will be useful when there will be multiple ESP32 devices.
        }
    }

    @Override
    public void onListItemClick(@NonNull ListView l, @NonNull View v, int position, long id) {
        BluetoothDevice device = listItems.get(position - 1);
        initializeTerminalFragment(device);
    }

    public void initializeTerminalFragment(BluetoothDevice device) {
        Bundle args = new Bundle();
        args.putString("device", device.getAddress());
        Log.d(TAG, String.format("Clicked on List item with device.getAddress() %s",
                device.getAddress()));
        Fragment fragment = new TerminalFragment();
        fragment.setArguments(args);


        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.fragmentBluetoothChain, fragment,
                "terminal").addToBackStack(null).commit();
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
}
