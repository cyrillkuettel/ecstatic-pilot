package li.garteroboter.pren;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.neovisionaries.ws.client.WebSocket;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class WebSocketManagerTest {

    private static final Logger Log = Logger.getLogger(WebSocketManagerTest.class);
    private static WebSocketManager manager;

    @BeforeAll
    public static void setup() {

        BasicConfigurator.configure();
        manager = new WebSocketManager(Constants.WEBSOCKET_URI);
    }

    @Test
    public void testOpenSocketConnectionInThread() {
        // Note: You can Probably drop the executorService.
        // I'm going to try this now.

        Callable<Boolean> callableObj = () -> {
            boolean OpenSocketConnectionResult =
                    manager.createAndOpenWebSocketConnection(Sockets.Text);
            return OpenSocketConnectionResult;
        };
        ExecutorService service = Executors.newSingleThreadExecutor();
        Future<Boolean> future = service.submit(callableObj);
        Boolean result = false;
        try {
            result = future.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        assertTrue(result);
    }

    @Test
    public void testGetInternetTime() {
        Log.info("Test");
        String time = manager.getInternetTime();
        System.out.println(time);

    }

    @Test
    public void testLogging() {
        Log.info("Logging works");
        Log.error("error");
        Log.fatal("this is fatal");
        Log.debug("adfa");
        // System.out.println(System.getProperty("java.class.path"));

    }






}