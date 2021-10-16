package com.example.pilot;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;

import java.io.IOException;

import javax.net.SocketFactory;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_ENABLE_BT = 1;

    /*
    The REQUEST_ENABLE_BT constant passed to startActivityForResult()
     is a locally-defined integer
     that must be greater than or equal to 0.
     The system passes this constant back to you in your onActivityResult() implementation
      as the requestCode parameter.
     */


    WebSocket ws = null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
           Log.v(TAG, "Device does not support bluetooth");
        } else {
            Log.v(TAG, "Device supports bluetooth");
        }

        if (!bluetoothAdapter.isEnabled()) { // if bluetooth is not enabled, ask to enable
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }




        // Create a WebSocket factory and set 5000 milliseconds as a timeout
        // value for socket connection.
        WebSocketFactory factory = new WebSocketFactory();
        factory.setSocketFactory(SocketFactory.getDefault());
        factory.setConnectionTimeout(10000);

        // Create a WebSocket. The timeout value set above is used.
        try {
            // "ws://147.88.62.66:80/ws/"

            ws = factory.createSocket("ws://192.168.188.38:80/ws/");
            ws.addListener(new WebSocketAdapter() {
                @Override
                public void onTextMessage(WebSocket websocket,
                                          String message) throws Exception {
                    Log.e(TAG, "onTextMessage: " + message);
                }

                @Override
                public void onError(WebSocket websocket,
                                    WebSocketException cause) throws Exception {
                    Log.e(TAG, "Error : " + cause.getMessage());
                    super.onError(websocket, cause);
                }

                @Override
                public void onConnectError(WebSocket websocket,
                                           WebSocketException exception) throws Exception {
                    Log.e(TAG, "onConnectError : " + exception.getMessage());
                }

            });

            ws.connectAsynchronously();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception ex) {
            Log.v(TAG, "onTextMessage threw an Exception!");
        }

        Log.v(TAG, "ws connecting asynchronously");



    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (ws != null) {
            ws.disconnect();
            Log.v(TAG, "Disconnected Websocket.");
            ws = null;
        }
    }

    public boolean sendMessage(View v) {
        if (ws == null) {
            Log.v(TAG, "ws == null");
        }

        if (ws.isOpen()) {
            ws.sendText("Message from Android!");
            return true;
        }
            Log.v(TAG, "Tried to call method 'sendText', but Websocket is not open!");
            return false;
    }

    public void startClickHandler(View target) {
        if (sendMessage(target)) {
            Log.v(TAG, "Sent the Message using the websocket");
        };

    }
}