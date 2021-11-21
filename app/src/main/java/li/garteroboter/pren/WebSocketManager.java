package li.garteroboter.pren;

import android.media.Image;
import android.os.Message;
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

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


/**
 * This Class realises the Websocket connections, and sending and rceiving Data from the Server
 * pren.garteroboter.li
 * There are two distinct websocket connections to the same server from the same client.
 * It is technically possible to to both text and binary in the same connection. But I assume this
 * would add more complexity to the websocket endpoint. It would have to check each incoming
 * message every time, which could decrease performance.
 *
 * I  have both textual (e.g. Logs and Commands) and binary data (Images) and don't want to bother
 * with adding your own protocol layer to distinguish since WebSockets already do this.

 */

public class WebSocketManager extends AppCompatActivity {


    /**
     * The timeout value in milliseconds for socket connection.
     */
    private static final int TIMEOUT = 5000;
    private static final int NUMBER_OF_THREADS = 2;

    private static boolean allowMultiplePilots = true;
    private final ExecutorService executorService;

    /**
     * Stores the current active socket connections. There are at most two. (Textual and Binary data)
     */
    private final Map<Sockets, WebSocket> sockets;
    private final String URI;
    private static final String TAG = MainActivity.class.getSimpleName();

    public WebSocketManager(String URI) {
        this.sockets = new HashMap<>();
        this.URI = URI;
        /* It does make sense to re-use the SingleThreadExecutor for different connections." */
        executorService = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
    }



    public boolean createAndOpenWebSocketConection(Sockets socket) {
        if (!isInternetAvailable()) {
            Log.e(TAG, "Internet is not available. Are you online? ");
            return false;
        }

        String typeOfSocketConnection = null;
        if (socket.equals(Sockets.Text)) {
            typeOfSocketConnection = "999";  // I define these special client ID's on the server of course
        }
        if (socket.equals(Sockets.Binary)) {
            typeOfSocketConnection = "888";
        }

        // temporary for testing to allow multiple websocket clients ( I will change this)
        /*
        typeOfSocketConnection = GenerateRandomNumber(11);
        Log.v(TAG, "random_id = " + typeOfSocketConnection);

         */


        String completeURI = this.URI + typeOfSocketConnection;
        Future<WebSocket> future = null;
        WebSocket ws = null;

        try {
            ws = createWebSocket(completeURI);
        } catch (WebSocketException e) {
            // Failed to establish a WebSocket connection.
            Log.e(TAG, "WebSocketException : " + e.getMessage());
        } catch (Exception e) {
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


        sockets.put(socket, ws);

        // this shit crashed the whole app
        // Utils.LogAndToast(WebSocketManager.this, TAG, "Opened New Socket!");
        return true;

        // is it recommended so use wait() to finish for executor?
    }

    /**
     * Connect to the server.
     */
    private WebSocket createWebSocket(String completeURI) throws IOException, WebSocketException {
        return new WebSocketFactory()
                .setConnectionTimeout(TIMEOUT)
                .createSocket(completeURI)
                .addListener(new WebSocketAdapter() {
                    @Override
                    public void onTextMessage(WebSocket websocket,
                                              String message) throws Exception {
                        super.onTextMessage(websocket, message);
                        // if message is time, use it here

                        Log.v(TAG, "onTextMessage: " + message);
                    }

                    @Override
                    public void onBinaryMessage(WebSocket websocket, byte[] binary) {
                        Log.v(TAG, "received binary message");
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
                        Log.v(TAG, "connected!");
                       // Utils.LogAndToast(WebSocketManager.this, TAG, "Connected");

                    }
                })
                // used in the handshake to indicate whether a connection should use compression
                .addExtension(WebSocketExtension.PERMESSAGE_DEFLATE);
    }



    public void getInternetTime() {
        String command = "command=requestTime";
        sendText(command);
    }


    /**
     * Sends Text data to the webserver
     * Under the assumption that there exists an open connection
     * (functions createWebSocket and openNewConnection have been called)
     */

    public boolean sendText(String message) {
        WebSocket ws = sockets.get(Sockets.Text);

        if (message == "" || ws == null) {
            if (ws == null) {
                Log.v(TAG, "Websocket == Null in method sendText");
            }
            return false;
        }
        if (ws.isOpen()) {
            ws.sendText(message);
            return true;
        }
        Log.v(TAG, "Tried to call method 'sendText', but Websocket is not open!");
        return false;
    }


    /**
     * Sends a image to the webserver
     * Under the assumption that there exists an open connection
     * (functions createWebSocket and openNewConnection have been called)
     */
    public boolean sendBytes(byte[] bytes) {
        Log.v(TAG, "sending Bytes");
        WebSocket ws = sockets.get(Sockets.Binary);

        if (ws == null) { Log.v(TAG, "Websocket == Null in method sendBytes");return false; }

        if (ws.isOpen()) {
            ws.sendBinary(bytes);
            Log.v(TAG, "I sent the bytes to %s} !".format(URI));
            return true;
        }
        Log.v(TAG, "Tried to call method 'sendBytes', but Websocket is not open!");
        return false;

    }


    public void disconnectAll() {
        if(!sockets.isEmpty()) {
            for (WebSocket w : sockets.values()) {
                w.disconnect();
                Log.v(TAG, "Disconnected Websocket and Shutdown Executor.");
            }
        }
        executorService.shutdown();
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
            Log.v(TAG, msg);
            // Utils.LogAndToast(WebSocketManager.this, TAG,msg);
        }
        return false;
    }

    public final String GenerateRandomNumber(int charLength) {
        Random r = new Random(System.currentTimeMillis());
        int low = 1; // inclusive
        int high = 1000000000; //exclusive
        int result = r.nextInt(high-low) + low;
        return String.valueOf(result);
    }

    /*
     * Pretty Print the OpeningHandshakeException e (Debugging purposes)
     */
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
