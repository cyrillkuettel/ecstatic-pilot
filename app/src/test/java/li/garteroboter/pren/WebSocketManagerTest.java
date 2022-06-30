package li.garteroboter.pren;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import android.content.Context;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketState;

import org.junit.Before;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import li.garteroboter.pren.network.SocketType;
import li.garteroboter.pren.network.WebSocketManager;

public class WebSocketManagerTest {
    private static final String TAG = "WebSocketManagerTest";

    MainActivity main = new MainActivity();
    Context context;


    @Before
    public void setupContext() {
        // https://github.com/android/android-test/issues/409
        // this is a hack to get hte context
        this.context =  InstrumentationRegistry.getInstrumentation().getTargetContext();

    }

    @Test
    public void testOpenSocketConnectionInThread() {
        WebSocketManager manager = createWebSocket();
        WebSocket socket = manager.getSocketFromMap(SocketType.Text);
        // Possible values: CREATED, CONNECTING, OPEN, CLOSING, CLOSED
        assertEquals(socket.getState(), WebSocketState.OPEN);

    }

    /**
     * Utility Method to create WebSocket. Currently only used for testing.
     */
    public WebSocketManager  createWebSocket() {
        WebSocketManager manager = new WebSocketManager(context, Constants.WEBSOCKET_URI);

        Callable<Boolean> callableObj = () -> manager.createAndOpenWebSocketConnection(SocketType.Text);
        ExecutorService service = Executors.newSingleThreadExecutor();
        Future<Boolean> future = service.submit(callableObj);
        Boolean OpenSocketConnectionResult = false;
        try {
            OpenSocketConnectionResult = future.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        // assertTrue(OpenSocketConnectionResult);
        // WebSocket socket = manager.getSocketFromMap(SocketType.Text);
        // Check the Websocket state as well for good measure
        // Possible values CREATED,CONNECTING,OPEN,CLOSING,CLOSED
        // assertThat(socket.getState()).isEqualTo(WebSocketState.OPEN);

        return manager;
    }



    @Test
    public void sendTextData_ListenerOnMessageShouldFire() {
        WebSocketManager manager = createWebSocket();
        WebSocket socket = manager.getSocketFromMap(SocketType.Text);
        assertEquals(socket.getState(), WebSocketState.OPEN);

        manager.sendText("Test");


    }

    @Test
    public void testIsInternetAvailable() {
        WebSocketManager manager = new WebSocketManager(context,Constants.WEBSOCKET_URI);
        assertTrue(manager.isInternetAvailable());
        // Internet should at all times be available. If not we are fucked
    }

    @Test
    public void testisWebserverUp() {
        WebSocketManager manager = new WebSocketManager(context,Constants.WEBSOCKET_URI);
        assertTrue(manager.isWebserverUp());
    }




     @Test
     public void testCurrentMillisecondsTime() {
         System.out.println(System.currentTimeMillis());
     }


    @Test
    public void testLoggingWithColor() {
        Log.i(TAG, "Logging works");
        Log.e(TAG, "error");
        Log.d(TAG, "debug");
        // System.out.println(System.getProperty("java.class.path"));

    }

}