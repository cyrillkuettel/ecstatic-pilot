package com.example.pilot;

import android.os.StrictMode;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.neovisionaries.ws.client.HostnameUnverifiedException;
import com.neovisionaries.ws.client.OpeningHandshakeException;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.net.SocketFactory;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_ENABLE_BT = 1;
    private WebSocketManager manager;
    /*
    The REQUEST_ENABLE_BT constant passed to startActivityForResult()
     is a locally-defined integer
     that must be greater than or equal to 0.
     The system passes this constant back to you in your onActivityResult() implementation
      as the requestCode parameter.

     */


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        generateDropDownItems();

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Utils.LogAndToast(MainActivity.this, TAG, "Could not find bluetooth adapter. ");
            return;
        }
        if (!bluetoothAdapter.isEnabled()) { // if bluetooth is not enabled, ask to enable
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        Log.v(TAG, String.valueOf(android.os.Build.VERSION.SDK_INT));

        Spinner mySpinner = findViewById(R.id.dropdown_menu);
        String URI = mySpinner.getSelectedItem().toString();
        manager = new WebSocketManager(URI);

        new Thread(() -> manager.openNewConnection(Sockets.Text)).start();

    }

    public void sendMessageClickHandler(View view) {
        manager.sendText();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        manager.disconnectAll();
    }

    /**
     * This allows to easily select the hostname for Websocket.
     * If device is connected to HSLU-Network, the hostname can be accessed from within.
     */
    public final void generateDropDownItems() {
        Spinner spinnerLanguages = findViewById(R.id.dropdown_menu);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.uri, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
        spinnerLanguages.setAdapter(adapter);
    }

    public void onCloseSocketHandler(View view) {
        manager.disconnectAll();
    }
}