package li.garteroboter.pren;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;


import android.content.Context;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketState;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.Calendar;
import java.util.Date;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class WebSocketManagerTest {

    private static final Logger Log = Logger.getLogger(WebSocketManagerTest.class);
    // Context context = ApplicationProvider.getApplicationContext();
    // Context context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getContext();
    // Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    MainActivity main = new MainActivity();

    Context context = main.mainContext;

    @BeforeAll
    public static void setup() {
        BasicConfigurator.configure();
    }

    @Test
    public void testOpenSocketConnectionInThread() {
        WebSocketManager manager = createWebSocket();
        WebSocket socket = manager.getSocketFromMap(SocketType.Text);
          // Check the Websocket statte as well for good measure
           // Possible values CREATED,CONNECTING,OPEN,CLOSING,CLOSED
        assertThat(socket.getState()).isEqualTo(WebSocketState.OPEN);
    }

    /**
     * Utility Method to create WebSocket. Currently only used for testing.
     */
    public WebSocketManager  createWebSocket() {
        WebSocketManager manager = new WebSocketManager(context, Constants.WEBSOCKET_URI);

        Callable<Boolean> callableObj = () -> {
            boolean managerResult =
                    manager.createAndOpenWebSocketConnection(SocketType.Text);
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
        // assertTrue(OpenSocketConnectionResult);
        // WebSocket socket = manager.getSocketFromMap(SocketType.Text);
        // Check the Websocket state as well for good measure
        // Possible values CREATED,CONNECTING,OPEN,CLOSING,CLOSED
        // assertThat(socket.getState()).isEqualTo(WebSocketState.OPEN);

        return manager;
    }

    /**
     * {@link WebSocketManager#getInternetTime() get Time from Website}
     * {@link li.garteroboter.pren.MainActivity#getDeviceTimeStampAsMilliseconds() get Time from Device }
     */
    @Test
    public void localTimeAndInternetTime_ShouldBeWithinOneSecond() {
        WebSocketManager manager = createWebSocket();
        WebSocket socket = manager.getSocketFromMap(SocketType.Text);
        assertThat(socket.getState()).isEqualTo(WebSocketState.OPEN);


        String internetTime = manager.getInternetTime();
        internetTime = internetTime.replace(",", "");
        String localDeviceTime = MainActivity.getDeviceTimeStampAsMilliseconds();
        String[] timeOnlyMinSecMillisec = new String[]{internetTime, localDeviceTime};

        Date[] date = new Date[2];

        for (int i = 0, timesLength = timeOnlyMinSecMillisec.length; i < timesLength; i++) {
            String ele = timeOnlyMinSecMillisec[i];
            String timeStartingWithMinutes = ele.substring(ele.length() - 9); // take the last 9 chars
            timeOnlyMinSecMillisec[i] = timeStartingWithMinutes;
            Date myDate = parseDate(timeOnlyMinSecMillisec[i]);
            date[i] = myDate;
        }

        // Time difference from Local Time and the time fetched from Server should never be higher
        // than one second.
        assertEquals(DateUtils.round(date[0],Calendar.SECOND),
                DateUtils.round(date[1],Calendar.SECOND));
    }

    public static Date parseDate(String date) {
        try {
            return new SimpleDateFormat("mm:ss.SSS").parse(date);
        } catch (ParseException e) {
            Log.error("Could not parse date! ");
            return null;
        }
    }


    @Test
    public void sendTextData_ListenerOnMessageShouldFire() {
        WebSocketManager manager = createWebSocket();
        WebSocket socket = manager.getSocketFromMap(SocketType.Text);
        assertThat(socket.getState()).isEqualTo(WebSocketState.OPEN);
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
        assertThat(manager.isWebserverUp()).isTrue();
    }




     @Test
     public void testCurrentMillisecondsTime() {
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