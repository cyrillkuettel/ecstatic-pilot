package li.garteroboter.pren;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class BluetoothActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_ENABLE_BT = 1;
    private static final String MAC_ADDRESS_ESP32 = "4C:EB:D6:75:AB:4E";
    private static final String BLUETOOTH_DEVICE_NAME_ESP32 = "ESP32test";

    // generated online
    private static final String UUID = "0fb7bb6b-3227-4776-bf9a-3356b52b0316";
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice selectedDevice = null;

    private final Map<String, BluetoothDevice> devices = new HashMap<>();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Utils.LogAndToast(BluetoothActivity.this, TAG, "Could not find bluetooth adapter. ");
            return;
        }
        if (!bluetoothAdapter.isEnabled()) { // if bluetooth is not enabled, ask to enable
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                devices.put(deviceName, device);
            }
        }

        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);

        String[] bluetoothDevicesArray = new String[devices.size()]; // this can be written with more
                                                                // elegance using java8 streams !! Let's do it
        int count = 0;
        for ( String key : devices.keySet() ) {
            bluetoothDevicesArray[count] = key;
            count++;
        }
       // this block above was very ugly. I could write this much more compact.

        // i could write my own ArrayAdapter. This is simply an object to store information about the item.
        // I'm not sure if it's worth it tho

        ArrayAdapter<String> myAdapter = new ArrayAdapter<String>( this, android.R.layout.simple_list_item_1, bluetoothDevicesArray );
        ListView bluetoothDevicesListView = findViewById(R.id.myListView);
        bluetoothDevicesListView.setAdapter(myAdapter);

        bluetoothDevicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position,
                                    long id) {

                for (int i = 0; i < bluetoothDevicesListView.getChildCount(); i++) {
                    if(position == i ){
                        bluetoothDevicesListView.getChildAt(i).setBackgroundColor(
                                Color.rgb(128, 128, 255));
                    }else{
                        bluetoothDevicesListView.getChildAt(i).setBackgroundColor(Color.TRANSPARENT);
                    }
                }
                String value = (String) parent.getItemAtPosition(position);

                selectedDevice = devices.get(value);
                Toast.makeText(BluetoothActivity.this,"You selected : " + value, Toast.LENGTH_SHORT).show();
                Log.v(TAG, value);
            }
        });

    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                devices.put(device.getName(), device);
                Utils.LogAndToast(BluetoothActivity.this, TAG,"Discovery has found a device.");
            }
        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainpagemenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            /*
            case R.id.bluetooth:
                intent = new Intent(this, BluetoothActivity.class);
                break;
             */
            case R.id.websocket:
                intent = new Intent(this, MainActivity.class);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        startActivity(intent);
        return true;
    }


    public void sendBluetoothMessageClickHandler(View view) {

        // create a thread to connect

        if (selectedDevice == null) {
            Log.e(TAG, "selected Device is Null");
            return;
        }
        ConnectThread connectThread = new ConnectThread(selectedDevice); // get the current selected bluetooth device
        connectThread.start();
    }


    @SuppressLint("HandlerLeak")
    Handler mHandler=new Handler();
    public void manageMyConnectedSocket(BluetoothSocket socket) {


        MyBluetoothService service = new MyBluetoothService(socket, mHandler);

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // unregister the ACTION_FOUND receiver.
        unregisterReceiver(receiver);
    }


    private class ConnectThread extends Thread {

        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private final String TAG = ConnectThread.class.getSimpleName();
        // generated online

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.

                tmp = device.createRfcommSocketToServiceRecord(java.util.UUID.fromString(UUID));
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.

            // MyBluetoothService ConnectedThread needs the socket
            manageMyConnectedSocket(mmSocket);


        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

}

