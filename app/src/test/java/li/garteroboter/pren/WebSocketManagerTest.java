package li.garteroboter.pren;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


import static org.assertj.core.api.Assertions.assertThat;

import com.neovisionaries.ws.client.WebSocket;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import li.garteroboter.pren.MainActivity;

public class WebSocketManagerTest {

    private static final Logger Log = Logger.getLogger(WebSocketManagerTest.class);
   //  private static WebSocketManager manager;


    @BeforeAll
    public static void setup() {
        BasicConfigurator.configure();
        // manager = new WebSocketManager(Constants.WEBSOCKET_URI);
    }

    @Test
    public void testOpenSocketConnectionInThread() {
        // Note: You can Probably drop the executorService.
        // I'm going to try this now.

        WebSocketManager manager = new WebSocketManager(Constants.WEBSOCKET_URI);

        Callable<Boolean> callableObj = () -> {
            boolean managerResult =
                    manager.createAndOpenWebSocketConnection(Sockets.Text);
            return managerResult;
        };
        ExecutorService service = Executors.newSingleThreadExecutor();
        Future<Boolean> future = service.submit(callableObj);
        Boolean OpenSocketConnectionResult = false;
        try {
            OpenSocketConnectionResult = future.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        assertTrue(OpenSocketConnectionResult);
    }

    @Test
    public void testIsInternetAvailable() {
        WebSocketManager manager = new WebSocketManager(Constants.WEBSOCKET_URI);
        assertTrue(manager.isInternetAvailable());
        // Internet should at all times be available. If not we are fucked
    }

    @Test
    public void testisWebserverUp() {
        WebSocketManager manager = new WebSocketManager(Constants.WEBSOCKET_URI);
        assertThat(manager.isWebserverUp()).isTrue();
    }


    /**
     * {@link WebSocketManager#getInternetTime() get Time from Website}
     * {@link li.garteroboter.pren.MainActivity#getDeviceTimeStamp() get Time from Device }
     */
    @Test
    @Disabled
    public void testTime() {
        WebSocketManager manager = new WebSocketManager(Constants.WEBSOCKET_URI);

        Callable<Boolean> callableObj = () -> {
            boolean managerResult =
                    manager.createAndOpenWebSocketConnection(Sockets.Text);
            return managerResult;
        };
        ExecutorService service = Executors.newSingleThreadExecutor();
        Future<Boolean> future = service.submit(callableObj);
        Boolean OpenSocketConnectionResult = false;
        try {
            OpenSocketConnectionResult = future.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        if (OpenSocketConnectionResult == false) {
            Assertions.fail("createAndOpenWebSocketConnection == false");
        }

        String time = manager.getInternetTime();
        String localDeviceTime = MainActivity.getDeviceTimeStamp();

        // Assertions.assertThat(time).i
        // compare with local SystemTime for good measure (you never know what will hit you)
        // change to wintertime on webserver (how fucking inconvenient )
        assertThat(time).isEqualTo(localDeviceTime);
    }



     @Test
     public void testCurrentTime() {
         System.out.println(System.currentTimeMillis());
     }


    @Test
    public void testLoggingWithColor() {
        Log.info("Logging works");
        Log.error("error");
        Log.fatal("Houston, we have a problem");
        Log.debug("debug");
        // System.out.println(System.getProperty("java.class.path"));

    }






}