package com.example.pilot;

import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.neovisionaries.ws.client.OpeningHandshakeException;
import com.neovisionaries.ws.client.StatusLine;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketExtension;
import com.neovisionaries.ws.client.WebSocketFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class EstablishSocketConnection extends AppCompatActivity {


    /**
     * The timeout value in milliseconds for socket connection.
     */

    private static final int TIMEOUT = 5000;
    private final String URI;
    private ExecutorService executorService;
    private Map<String, WebSocket> sockets;
    private static final int numberOfThreads = 3;

    private static final String TAG = MainActivity.class.getSimpleName();

    public EstablishSocketConnection(String URI) {
        this.URI = URI;
        this.sockets = new HashMap<>();

        /* It does make sense to re-use the SingleThreadExecutor for different connections." */
        executorService = Executors.newFixedThreadPool(numberOfThreads);
    }


    public boolean openNewConnection(String typeOfSocket) {
        if (!isInternetAvailable()) {
            Utils.LogAndToast(EstablishSocketConnection.this, TAG, "Internet is not available. Are you online? ");
            return false;
        }


        Future<WebSocket> future = null;
        WebSocket ws = null;

        try {
            ws = createWebSocket();
        } catch (WebSocketException e) {
            // Failed to establish a WebSocket connection.
            Log.e(TAG, "WebSocketException : " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (ws == null) {
            return false;
        }

        try {
            future = ws.connect(executorService);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            future.get();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof WebSocketException) {
                Log.e(TAG, String.valueOf(e.getMessage()));
            }
        } catch (InterruptedException ex) {
            Log.d(TAG, String.valueOf(ex.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        sockets.put(typeOfSocket, ws);
        return true;
    }

    /**
     * Connect to the server.
     */
    private WebSocket createWebSocket() throws IOException, WebSocketException {
        return new WebSocketFactory()
                .setConnectionTimeout(TIMEOUT)
                .createSocket(URI)
                .addListener(new WebSocketAdapter() {
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
                    public void onConnected(WebSocket websocket,
                                            Map<String, List<String>> headers) throws Exception {
                        super.onConnected(websocket, headers);
                        Log.v(TAG, "we are connected");

                    }
                })
                .addExtension(WebSocketExtension.PERMESSAGE_DEFLATE);
    }

    private static BufferedReader getInput() throws IOException {
        return new BufferedReader(new InputStreamReader(System.in));
    }

    public void disconnectAll() {
        for (WebSocket w : sockets.values()) {
            w.disconnect();
            Log.v(TAG, "Disconnected Websocket.");
        }
    }


    /**
     * This method actually checks if device is connected to internet
     * (There is a possibility it's connected to a network but not to internet).
     *
     * @return False if internet is not available, true otherwise
     */
    public boolean isInternetAvailable() {
        try {
            InetAddress address = InetAddress.getByName("www.google.com");
            return !address.equals("");
        } catch (UnknownHostException e) {
            String msg = "Internet does not seem to be available";
            Utils.LogAndToast(EstablishSocketConnection.this, TAG, "Could not find bluetooth adapter. ");

        }
        return false;
    }


    private void createDetailedExceptionLog(OpeningHandshakeException e) {
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


}
