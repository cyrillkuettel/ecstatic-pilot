package li.garteroboter.pren;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

public class WebSocketManagerTest {
    private static WebSocketManager manager;

    @BeforeAll
    public static void createActivity(){
        manager = new WebSocketManager(Constants.WEBSOCKET_URI);
    }

    @Test
    public void nothing() {

       assertEquals(3,3);
    }

    /*
    @Test
    public void testOpenSocketConnectionInThread() {

        // to actually read the value which createAndOpenWebSocketConnection returns,
        // we have to run this function in a context of Callable

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

     */


}