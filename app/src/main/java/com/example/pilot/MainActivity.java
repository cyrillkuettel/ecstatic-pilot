package com.example.pilot;

import android.os.StrictMode;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.neovisionaries.ws.client.HostnameUnverifiedException;
import com.neovisionaries.ws.client.OpeningHandshakeException;
import com.neovisionaries.ws.client.StatusLine;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
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

        generateDropDownItems();

        // TODO: create infrastructure to have threads which handle network stuff outside of main

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy); // Override the default behaviour ( Network connection
        //on main thread. )

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

        EstablishSocketConnection esc = new EstablishSocketConnection("ws://192.168.188.38:80/ws/999");
        esc.openNewConnection("Logs");

    }

    public void handShakeClickHandler(View view) {


        Log.v(TAG, "Button pressed. Starting to establish connection to socket");
        // Create a WebSocket factory and set 5000 milliseconds as a timeout
        // value for socket connection.
        WebSocketFactory factory = new WebSocketFactory();
        factory.setSocketFactory(SocketFactory.getDefault());
        factory.setConnectionTimeout(5000);


        Spinner mySpinner = findViewById(R.id.dropdown_menu);
        String WEBSOCKET_URI = mySpinner.getSelectedItem().toString();

        try {
            String pren_DOMAIN = "ws://pren.garteroboter.li:80/ws"; // this worked
            String tryThisURI = "ws://192.168.188.38:80/ws"; // localhost @home
            String pren_VPM_DOMAIN = "ws://prenh21-ckuttel.enterpriselab.ch:80/ws";

            // The timeout value set above is used.
            ws = factory.createSocket(WEBSOCKET_URI);

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
                    super.onTextMessage(websocket, message);
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
                    super.onConnectError(websocket, exception);
                    Log.e(TAG, "onConnectError : " + exception.getMessage());
                }

                @Override
                public void onConnected(WebSocket websocket,
                                        Map<String, List<String>> headers) throws Exception {
                    super.onConnected(websocket, headers);
                    Log.v(TAG, "we are connected");
                }
            });

        } catch (IllegalArgumentException ex) {
            Log.e(TAG, "onTextMessage threw an IllegalArgumentException!" + ex.getMessage());
            Log.v(TAG, "Cause for this:" + ex.getCause());
        } catch (Exception ex) {
            Log.e(TAG, "onTextMessage threw an general Exception! printing Stacktrace");
            ex.printStackTrace();
        }

        try {
            // Connect to the server and perform an opening handshake.
            // This method blocks until the opening handshake is finished.
            Log.v(TAG, "starting connection now");

            ws.connect();

        } catch (OpeningHandshakeException e) {
            createDetailedExceptionLog(e);

        } catch (HostnameUnverifiedException e) {
            // The certificate of the peer does not match the expected hostname.
            Log.e(TAG, "HostnameUnverifiedException : " + e.getMessage());
        } catch (WebSocketException e) {
            // Failed to establish a WebSocket connection.
            Log.e(TAG, "WebSocketException : " + e.getMessage());
        }
    }

    public void createDetailedExceptionLog(OpeningHandshakeException e) {
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

    /**
     * This allows to easily select the hostname for Websocket.
     * If device is connected to HSLU-Network, the hostname can be accessed from within.
     */
    public void generateDropDownItems() {
        Spinner spinnerLanguages = findViewById(R.id.dropdown_menu);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.hostnames, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
        spinnerLanguages.setAdapter(adapter);
    }


    public boolean sendMessage(View view) {
        if (ws == null) {
            Log.v(TAG, "ws == null");
            return false;
        }

        if (ws.isOpen()) {
            ws.sendText("Message from Pilot!");
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

    public void onCloseSocketHandler(View view) {
        if (ws != null) {
            ws.disconnect();
            Log.v(TAG, "Disconnected Websocket.");
            ws = null;
        }
    }
}