package com.example.pilot;

import android.os.StrictMode;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.neovisionaries.ws.client.HostnameUnverifiedException;
import com.neovisionaries.ws.client.OpeningHandshakeException;
import com.neovisionaries.ws.client.StatusLine;
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

        // Override the default behaviour ( Network connection on main thread. )
        // Probably I will eventually add en Executor for this thread.
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

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





    }


    public void handShakeClickHandler(View view) {

        Log.v(TAG, "Button pressed. Starting to establlish connection to socket");
        // Create a WebSocket factory and set 5000 milliseconds as a timeout
        // value for socket connection.
        WebSocketFactory factory = new WebSocketFactory();
        factory.setSocketFactory(SocketFactory.getDefault());
        factory.setConnectionTimeout(10000);

        // Create a WebSocket. The timeout value set above is used.
        // "ws://147.88.62.66:80/ws/"

        try {
            String tryThisURI = "ws://192.168.188.38:80/ws";
            String URI = "ws://192.168.188.38:80/test";


            ws = factory.createSocket(tryThisURI);
            // what to add?

            ws.addProtocol("chat");
            ws.addExtension("foo");
            ws.addHeader("X-My-Custom-Header", "23");

        } catch (IOException e) {
            e.printStackTrace();
        }


        try {
            ws.addListener(new WebSocketAdapter() {
                @Override
                public void onTextMessage(WebSocket websocket,
                                          String message) throws Exception {
                     Log.v(TAG, "onTextMessage: " + message);
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

            try {
                // Connect to the server and perform an opening handshake.
                // This method blocks until the opening handshake is finished.
                Log.v(TAG, "starting connection now");

                ws.connect();

                Log.v(TAG, "connected ..?");
            } catch (OpeningHandshakeException e) {
                // A violation against the WebSocket protocol was detected
                // during the opening handshake.
                // Status line.
                Log.e(TAG, "OpeningHandshakeException " + e.getMessage());

                StatusLine sl = e.getStatusLine();
                System.out.println("=== Status Line ===");
                System.out.format("HTTP Version  = %s\n", sl.getHttpVersion());
                System.out.format("Status Code   = %d\n", sl.getStatusCode());
                System.out.format("Reason Phrase = %s\n", sl.getReasonPhrase());

                // HTTP headers.
                Map<String, List<String>> headers = e.getHeaders();
                System.out.println("=== HTTP Headers ===");
                for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                    // Header name.
                    String name = entry.getKey();

                    // Values of the header.
                    List<String> values = entry.getValue();

                    if (values == null || values.size() == 0) {
                        // Print the name only.
                        System.out.println(name);
                        continue;
                    }

                    for (String value : values) {
                        // Print the name and the value.

                        String msg = String.format("%s: %s\n", name, value);
                        Log.e(TAG, msg);
                    }
                }


            } catch (HostnameUnverifiedException e) {
                // The certificate of the peer does not match the expected hostname.
                Log.e(TAG, "HostnameUnverifiedException : " + e.getMessage());
            } catch (WebSocketException e) {
                // Failed to establish a WebSocket connection.
                Log.e(TAG, "WebSocketException : " + e.getMessage());
            }

            // ws.connectAsynchronously();


        } catch (IllegalArgumentException ex) {
            Log.e(TAG, "onTextMessage threw an IllegalArgumentException!" + ex.getMessage());
            Log.v(TAG, "Cause for this:" + ex.getCause());
        } catch (Exception ex) {
            Log.e(TAG, "onTextMessage threw an general Exception! printing Stacktrace");
            ex.printStackTrace();

        }
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

    public boolean sendMessage(View view) {
        if (ws == null) {
            Log.v(TAG, "ws == null");
            return false;
        }

        if (ws.isOpen()) {
            ws.sendText("Message from Android!");
            return true;
        }

        Log.v(TAG, "Tried to call method 'sendText', but Websocket is not open!");
        return false;
    }

    public void startClickHandler(View view) {
        if (sendMessage(view)) {
            Log.v(TAG, "Sent the Message using the websocket");
        }
    }
}