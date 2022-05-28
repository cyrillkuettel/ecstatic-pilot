package li.garteroboter.pren.socket;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.neovisionaries.ws.client.HostnameUnverifiedException;
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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import li.garteroboter.pren.Constants;


/**
 * This Class realises the Websocket connections, and sending and receiving Data from the Server
 * pren.garteroboter.li
 * There are two distinct websocket connections in this application:
 * image data and text data.
 * I have both textual (e.g. Logs and Commands) and binary data (Images) and don't want to bother
 * with adding my own protocol layer to distinguish. That's why we have two connections.
 */

public class WebSocketManager {
    private static final String TAG = "WebSocketManager";

    private static final String INTERNET_NOT_AVAILABLE = "Internet is not available. Are you " +
            "online? ";
    final Context context;
    /**
     * The timeout value in milliseconds for socket connection.
     */
    private static final int TIMEOUT = 5000;
    private static final int NUMBER_OF_THREADS = 2;

    private final ExecutorService executorService;

    /**
     * Stores the current active socket connections. There are at most two. (Textual and Binary
     * data)
     */
    private final Map<SocketType, WebSocket> sockets;
    private final String URI;
    private String receivedInternetTime = "Not initialized";

    public WebSocketManager(Context context, String URI) {
        this.context = context;
        this.URI = URI;
        this.sockets = new HashMap<>();
        Log.i(TAG, "Creating new fixed Threadpool!");
        executorService = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
    }


    public boolean createAndOpenWebSocketConnection(SocketType socketType) {
        Log.i(TAG, "createAndOpenWebSocketConnection");


        final String completeURI = this.URI + socketType.id;

        WebSocket ws = null;

        try {
            Log.i(TAG, String.format("Connecting to %s", completeURI));
            ws = createWebSocket(completeURI);
        } catch (HostnameUnverifiedException hostnameUnverifiedException) {
            Log.i(TAG, "HostnameUnverifiedException");
            final String hostname = hostnameUnverifiedException.getHostname();
            final String ssl = hostnameUnverifiedException.getSSLSocket().toString();

            Log.e(TAG, String.format("getHostname = %s", hostname));
            Log.e(TAG, String.format("getSSLSocket = %s", ssl));
            hostnameUnverifiedException.printStackTrace();
        } catch (WebSocketException e) {

            // Failed to establish a WebSocket connection.
            Log.e(TAG, "WebSocketException : " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (ws == null) {
            return false;
        }

        Future<WebSocket> future = null;
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
            ex.printStackTrace();
        } catch (NullPointerException e) {
            Log.d(TAG, "Nullpointer in future.get() in WebSocketManager");
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        sockets.put(socketType, ws);
        return true;

    }



    public static Runnable createToast(String message, Context context) {
        return new Runnable() {
            public void run() {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        };
    }


    /**
     * Connect to the server.
     */
    private WebSocket createWebSocket(final String completeURI) throws IOException,
            WebSocketException {
        WebSocketFactory factory = new WebSocketFactory();
        factory.setServerName("pren.garteroboter.li");

        return factory
                .setConnectionTimeout(TIMEOUT)
                .createSocket(completeURI)
                .addListener(new WebSocketAdapter() {
                    @Override
                    public void onTextMessage(WebSocket websocket,
                                              String message) throws Exception {
                        super.onTextMessage(websocket, message);

                        if (message.contains("time=")) {

                            receivedInternetTime = message.replace("time=", "");
                        }

                        Log.i(TAG, "WebSocket onTextMessage: " + message);
                    }

                    @Override
                    public void onBinaryMessage(WebSocket websocket, byte[] binary) {
                        Log.i(TAG, "WebSocket incoming binary data");
                    }

                    @Override
                    public void onError(WebSocket websocket,
                                        WebSocketException cause) throws Exception {
                        super.onError(websocket, cause);
                        Log.e(TAG, "Error : " + cause.getMessage());
                    }

                    @Override
                    public void onConnected(WebSocket websocket,
                                            Map<String, List<String>> headers) throws Exception {
                        super.onConnected(websocket, headers);
                        Log.i(TAG, "connected!");
                        new Handler(Looper.getMainLooper()).post(createToast("Websocket " +
                                "Connected", context));

                    }
                })
                // used in the handshake to indicate whether a connection should use compression
                .addExtension(WebSocketExtension.PERMESSAGE_DEFLATE);
    }


    public String getInternetTime() {
        String command = "command=requestTime";
        sendText(command);

        // wait for the completion
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Log.e(TAG, "sleep interrupted!");
        }

        return receivedInternetTime;

    }


    /**
     * Sends Text data to the webserver
     * Under the assumption that there exists an open connection
     * Run createAndOpenWebSocketConnection before this !
     */
    public boolean sendText(String message) {
        WebSocket ws = sockets.get(SocketType.Text);

        if (message.equals("") || ws == null) {
            if (ws == null) {
                Log.e(TAG, "Websocket == Null in method sendText");
            }
            return false;
        }
        if (ws.isOpen()) {
            ws.sendText(message);
            return true;
        } else {
            Log.e(TAG, "Websocket not open");
        }

        Log.e(TAG, "Tried to call method 'sendText', but Websocket is not open!");
        return false;
    }

    public void startTimer() {
        String value_now = getDeviceTimeStampAsMilliseconds();
        String message = String.format("command=startTime=%s", value_now);
        sendText(message);
    }


    /**
     * Sends a image to the webserver
     * Under the assumption that there exists an open connection
     * Run createAndOpenWebSocketConnection before this !
     */
    public boolean sendBytes(byte[] bytes) {
        Log.i(TAG, "sending Bytes");
        WebSocket ws = sockets.get(SocketType.Binary);

        if (ws == null) {
            Log.i(TAG, "Websocket == Null in method sendBytes");
            return false;
        }

        if (ws.isOpen()) {
            ws.sendBinary(bytes);
            Log.i(TAG, String.format("sent the bytes to %s} !", URI));
            return true;
        }
        Log.i(TAG, "Tried to call method 'sendBytes', but Websocket is not open!");
        return false;

    }


    public void disconnectAll() {
        if (!sockets.isEmpty()) {
            for (WebSocket w : sockets.values()) {
                w.disconnect();

                Log.i(TAG, "Disconnected Websocket and Shutdown Executor.");
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
            Log.i(TAG, msg);
            // Utils.LogAndToast(WebSocketManager.this, ,msg);
        }
        return false; // no internet
    }

    /**
     * @return False if internet is not available, true otherwise
     */
    public boolean isWebserverUp() {
        try {
            InetAddress address = InetAddress.getByName(Constants.GARTEROBOTERLI_HOSTNAME);
            return !address.equals("");
        } catch (UnknownHostException e) {
            String msg = String.format("The website %s is not reachable with InetAddress",
                    Constants.GARTEROBOTERLI_HOSTNAME);
            Log.i(TAG, msg);
        }
        return false;
    }

    /**
     * This methods returns the current Time on the device. It may differ slightly from the
     * internet time, which is more precise.
     * I modify the String manually ( which is bad) to include 'T' and 'Z'
     *
     * @return current System time
     */
    public static String getDeviceTimeStampAsMilliseconds() {
        // ISO 8601
        // 2018-04-04T16:00:00.000Z
        // expects https://day.js.org/docs/en/parse/string
        final Calendar cal = Calendar.getInstance();
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Zurich"));

        final long timeInMillis = cal.getTimeInMillis();
        final String message = Long.toString(timeInMillis);
        final int numDigits = String.valueOf(timeInMillis).length();
        assert numDigits == 13; // (13 digits, since the Unix Epoch Jan 1 1970 12AM UTC).
        Log.i(TAG, "startTime=" + message);
        return message;
    }


    public WebSocket getSocketFromMap(SocketType socketType) {
        return sockets.get(socketType);
    }


    /*
     * Pretty Prints the OpeningHandshakeException e (Debugging purposes)
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
