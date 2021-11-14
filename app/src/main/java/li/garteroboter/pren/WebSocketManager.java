package li.garteroboter.pren;

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

public final class WebSocketManager extends AppCompatActivity {


    /**
     * The timeout value in milliseconds for socket connection.
     */
    private static final int TIMEOUT = 5000;
    private static final int NUMBER_OF_THREADS = 2;
    private final ExecutorService executorService;

    /**
     * The WebSocket connections will be established in this class
     *
     * Currently, I use one Websocket connection, although it's possible to create multiple.
     * this connection handels both text and binary data. Text can be further broken down into Logs
     * and commands. Commands differ from Logs. They change the 'state' of the website.
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


    public final String GenerateRandomNumber(int charLength) {
        Random r = new Random(System.currentTimeMillis());
        int low = 1; // inclusive
        int high = 1000000000; //exclusive
        int result = r.nextInt(high-low) + low;
        return String.valueOf(result);
    }


    public boolean openNewConnection(Sockets socket) {
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
        typeOfSocketConnection = GenerateRandomNumber(11);
        Log.v(TAG, "random_id = " + typeOfSocketConnection);
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
                .addExtension(WebSocketExtension.PERMESSAGE_DEFLATE);
    }

    public void getInternetTime() {
        String command = "command=requestTime";
        sendText(command);
    }


    public boolean sendText(String message) {
        WebSocket ws = sockets.get(Sockets.Text);
        if (message == "") {
            return false;
        }
        if (ws.isOpen()) {
            ws.sendText(message);
            return true;
        }
        Log.v(TAG, "Tried to call method 'sendText', but Websocket is not open!");

        // Toast.makeText(WebSocketManager.this,"First Open a Connection!" ,Toast.LENGTH_LONG).show();
        return false;
    }

    public void disconnectAll() {
        for (WebSocket w : sockets.values()) {
            w.disconnect();
            Log.v(TAG, "Disconnected Websocket and Shutdown Executor.");
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
