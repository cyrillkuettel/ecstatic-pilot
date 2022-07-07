package li.garteroboter.pren.ui;


import android.content.Context;
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
import java.util.List;

import li.garteroboter.pren.R;
import li.garteroboter.pren.log.LogcatData;
import li.garteroboter.pren.log.LogcatDataReader;
import li.garteroboter.pren.shell.RootShell;
import li.garteroboter.pren.network.SocketType;
import li.garteroboter.pren.network.WebSocketManager;
import li.garteroboter.pren.network.WebSocketManagerInstance;


public class LoggingFragment extends Fragment {
    private static final String TAG = "LoggingFragment";
    private WebSocketManager manager;
    private LogcatData logcatreader;

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

        setupOnClickListeners(view);


        return view;
    }

    private void setupOnClickListeners(View view) {
        Button btnClose =  view.findViewById(R.id.btnCloseConnection);

        Button btnSendMessageToWebSocket = view.findViewById(R.id.btnSendMessageToWebSocket);
        btnSendMessageToWebSocket.setEnabled(false);
        btnSendMessageToWebSocket.setOnClickListener(v -> {
            manager.sendText("Hello from Android!");
        });

        Button btnSTart = view.findViewById(R.id.btnStart);
        btnSTart.setEnabled(false);
        btnSTart.setOnClickListener(v -> {
            btnSTart.setEnabled((false));
            manager.startTimer();
        });

        Button btnStop = view.findViewById(R.id.btnStop);
        btnStop.setOnClickListener(v -> {
            btnSTart.setEnabled((false));
            manager.stopTimer();
        });

        Button btnOpenConnection = view.findViewById(R.id.btnOpenConnection);
        btnOpenConnection.setOnClickListener(v -> {
            Spinner hostnameDropdown = view.findViewById(R.id.dropdown_menu);
            reOpenSocket(hostnameDropdown.getSelectedItem().toString());
            btnSendMessageToWebSocket.setEnabled(true);
            btnSTart.setEnabled((true));
            btnClose.setEnabled(true);
        });


        btnClose.setEnabled(false);
        btnClose.setOnClickListener(v -> manager.disconnectAll());


        logcatreader = new LogcatDataReader();
        logcatreader.flush();

        Button btnDumpLogcat = view.findViewById(R.id.btnDumpLogcat);
        btnDumpLogcat.setOnClickListener(v -> {
            Log.i(TAG, "Attempt logcat read");

            try {

                Log.i(TAG, "     -------- Dumping Logcat output --------       ");
                List<String> logs = logcatreader.read(300);
                logs.forEach(System.out::println);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });


        Button btnRootShell = view.findViewById(R.id.btnRootShell);
        btnRootShell.setVisibility(View.INVISIBLE);
        btnRootShell.setOnClickListener(v -> {
            // This is an attempt to turn the the flashlight on using the kernel interface.
            // Somehow it did not work. I have no idea why.
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
    }


    public void reOpenSocket(final String hostname) {
        if (manager != null) {
            manager.disconnectAll();
        } else {
            Log.i(TAG, "Opening new Socket connection");
        }
        manager = new WebSocketManager(getContext(), hostname, SocketType.Text);
        // TODO: change this so be more optimal
        new Thread(() -> manager.createAndOpenWebSocketConnection()).start();
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


    public LoggingFragment() {
        // Required empty public constructor
    }
}

