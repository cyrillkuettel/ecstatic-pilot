package com.example.pilot;

import android.util.Log;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketFactory;

import java.io.IOException;
import java.net.InetAddress;

public class EstablishSocketConnection {
    private WebSocket ws;
    private static final String TAG = MainActivity.class.getSimpleName();

    public EstablishSocketConnection() {
        ws = null;
    // TODO: check for internet connection first.
    }

    public WebSocket start() {

        // Create a WebSocket factory and set 5000 milliseconds as a timeout
        // value for socket connection.
        WebSocketFactory factory = new WebSocketFactory().setConnectionTimeout(100000);

        // Create a WebSocket. The timeout value set above is used.
        try {
            // "ws://147.88.62.66:80/ws/"

            ws = factory.createSocket("ws://192.168.188.38:80/ws/");
            ws.addListener(new WebSocketAdapter() {
                @Override
                public void onTextMessage(WebSocket websocket, String message) throws Exception {
                    Log.v(TAG, "onTextMessage: " + message);
                }
            });

            ws.connectAsynchronously();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception ex) {
            Log.v(TAG, "onTextMessage threw an Exception!");
        }

        Log.v(TAG, "ws connecting asynchronously");

        return ws;
    }

    public boolean isInternetAvailable() {
        try {
            InetAddress ipAddr = InetAddress.getByName("google.com");
            //You can replace it with your name
            return !ipAddr.equals("");

        } catch (Exception e) {
            return false;
        }
    }

}
