package li.garteroboter.pren;


import android.content.Context;
import android.opengl.Visibility;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import li.garteroboter.pren.log.LogcatData;
import li.garteroboter.pren.log.LogcatDataReader;
import li.garteroboter.pren.shell.RootShell;
import li.garteroboter.pren.socket.SocketType;
import li.garteroboter.pren.socket.WebSocketManager;
import li.garteroboter.pren.socket.WebSocketManagerInstance;


public class LoggingFragment extends Fragment {
    private static final String TAG = "LoggingFragment";
    private WebSocketManager manager;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public static LoggingFragment newInstance() {
        LoggingFragment fragment = new LoggingFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            WebSocketManagerInstance instance = (WebSocketManagerInstance) context;
            manager = instance.getManager();
        } catch (ClassCastException castException) {
            /** The activity does not implement the listener. */
            Log.e(TAG, "Failed to implement VibrationListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_websocket_send_logs, container, false);
        generateDropDownItems(view);

        Button btnClose =  view.findViewById(R.id.btnClose);

        Button btnSendMessageToWebSocket = view.findViewById(R.id.btnSendMessageToWebSocket);
        btnSendMessageToWebSocket.setEnabled(false);
        btnSendMessageToWebSocket.setOnClickListener(v -> {
            manager.sendText("Hello from Android!");
        });

        Button btnStartStopTimer = view.findViewById(R.id.btnStartStop);
        btnStartStopTimer.setEnabled(false);
        btnStartStopTimer.setOnClickListener(v -> {
            sendStartSignalToWebServer();

        });
        Button btnOpenConnection = view.findViewById(R.id.btnOpenConnection);
        btnOpenConnection.setOnClickListener(v -> {
            Spinner hostnameDropdown = view.findViewById(R.id.dropdown_menu);
            reOpenSocket(hostnameDropdown.getSelectedItem().toString());
            btnSendMessageToWebSocket.setEnabled(true);
            btnStartStopTimer.setEnabled((true));
            btnClose.setEnabled(true);
        });


        btnClose.setEnabled(false);
        btnClose.setOnClickListener(v -> {
            manager.disconnectAll();
        });

        Button btnDumpLogcat = view.findViewById(R.id.btnDumpLogcat);
        btnDumpLogcat.setOnClickListener(v -> {
            Log.i(TAG, "Attempt logcat read");
            LogcatData logcatreader = new LogcatDataReader();
            try {
                // Todo: print this to fragment
                System.out.println(logcatreader.read());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });


        Button btnRootShell = view.findViewById(R.id.btnRootShell);
        btnRootShell.setVisibility(View.INVISIBLE);
        btnRootShell.setOnClickListener(v -> {
            // This is an attempt to turn the the flashlight on using the kernel interface.
            // Somehow did not work. I have no idea why.
            try {
                Log.i(TAG, "creating Rootshell Class");
                String availableDevices = RootShell.sudoForResult("ls -lah /sys/class/leds/");
                Log.v(TAG,availableDevices );
                // String torch_light0 = RootShell.sudoForResult("cd /sys/class/");

                // String nowitworks = RootShell.sudoForResult("cd /sys/class/leds/torch-light0/power && pwd && ls -l");
                // Log.v(TAG, nowitworks);
                String shouldTurnOn = RootShell.sudoForResult("echo 255 > /sys/class/leds:torch-light1/brightness");


                Log.v(TAG,shouldTurnOn);

                // String torchON = "echo 128 > /sys/class/leds/torch-light0/brightness";
            } catch (Exception e) {
                e.printStackTrace();
            }

        });


        /*
        Button btnInternetTime =  view.findViewById(R.id.btnInternetTime);
        btnInternetTime.setEnabled(false);
        btnInternetTime.setOnClickListener(v -> {
            Spinner hostnameDropdown = view.findViewById(R.id.dropdown_menu);
            reOpenSocket(hostnameDropdown.getSelectedItem().toString());
            String time = manager.getInternetTime();

            Log.d(TAG, String.format("getInternetTime() == %s", time));
        });

         */

        return view;
    }



    public void reOpenSocket(final String hostname) {
        if (manager != null) {
            manager.disconnectAll();
        } else {
            Log.i(TAG, "Opening new Socket connection");
        }
        manager = new WebSocketManager(getContext(), hostname);
        // TODO: change this so be more optimal
        new Thread(() -> manager.createAndOpenWebSocketConnection(SocketType.Text)).start();
    }

    public final void generateDropDownItems(View view) {
        Spinner spinnerHostname = view.findViewById(R.id.dropdown_menu);

        ArrayAdapter<CharSequence> adapter = null;
        try {
            adapter = ArrayAdapter.createFromResource(
                    getContext(), R.array.uri, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerHostname.setAdapter(adapter);
        } catch (Exception e) {
            Log.e(TAG, "Failed creating spinner items");
            e.printStackTrace();
        }
    }

    /**
     * tells the webserver that the parkour has begun.
     * Note that in the end this messsage has to be called as a result of the Bluetooth message
     */
    public void sendStartSignalToWebServer() {
        String value_now = getDeviceTimeStampAsMilliseconds();
        Log.v(TAG, value_now);
        String message = String.format("command=startTime=%s", value_now);
        if (manager.sendText(message)) {
            Log.i(TAG, "sendStartSignalToWebServer - successful");
        } else {
            Log.e(TAG, "Error sendStartSignalToWebServer " + message);
        }
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

    public LoggingFragment() {
        // Required empty public constructor
    }
}

