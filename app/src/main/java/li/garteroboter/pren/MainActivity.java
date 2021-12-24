package li.garteroboter.pren;

import static li.garteroboter.pren.Utils.LogAndToast;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.hardware.camera2.CameraManager;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;

import simple.bluetooth.terminal.BlueActivity;


public class MainActivity extends AppCompatActivity {
    private static final Logger Log = LogManager.getLogger(MainActivity.class);

    private static final String ABSOLUTE_APK_PATH = "https://github.com/cyrillkuettel/ecstatic" +
            "-pilot/lob/master/app/build/outputs/apk/debug/app-debug.apk?raw=true";
    private WebSocketManager manager = null;
    private static final int CAMERA_REQUEST = 1888;
    private Handler toastHandler;
    final Context mainContext = MainActivity.this;
    private static final int MY_CAMERA_REQUEST_CODE = 2;
    public static final int PICK_IMAGE = 1; // so you can recognize when the user comes back from
    // the image gallery
    public TextureView mTextureView;
    private boolean START_SIGNAL_FIRED = false;
    private static final int PIXEL_CAMERA_WIDTH = 3036;  // default values when taking pictures
                                                        // with google camera.
    private static final int PIXEL_CAMERA_HEIGHT = 4048;

    SurfaceView surfaceView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        BasicConfigurator.configure();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        generateDropDownItems();

        Log.info(String.valueOf(android.os.Build.VERSION.SDK_INT));
        Log.info("CameraIDlist = " + getCameraIDList());





        Button btnStartStop = (Button) findViewById(R.id.btnStartStop);
        btnStartStop.setEnabled(false);
        btnStartStop.setOnClickListener(v -> {
            // if (!START_SIGNAL_FIRED) {
            sendStartSignalToWebServer();
            START_SIGNAL_FIRED = true;
            // }
        });


        Button updateApp = (Button) findViewById(R.id.updateApp);
        updateApp.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(ABSOLUTE_APK_PATH));
            startActivity(browserIntent);
        });

        Button btnSelectImageAndSend = (Button) findViewById(R.id.btnSelectImageAndSend);
        btnSelectImageAndSend.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
        });

        Button btnOpenByteSocketConnection = (Button) findViewById(R.id.btnOpenByteSocketConnection);
        btnOpenByteSocketConnection.setOnClickListener(v -> {
            if (manager != null) {
                manager.disconnectAll();
            } else {
                LogAndToast(mainContext, "Opening new Socket connection");
            }
            Spinner hostDropdown = findViewById(R.id.dropdown_menu);
            manager = new WebSocketManager(this, hostDropdown.getSelectedItem().toString());

            // TODO: change this so be more optimal
            new Thread(() -> manager.createAndOpenWebSocketConnection(SocketType.Bytes)).start();
        });

        Button btnInternetTime = (Button) findViewById(R.id.btnInternetTime);
        btnInternetTime.setEnabled(false);
        btnInternetTime.setOnClickListener(v -> {
            reOpenSocket();
            Toast.makeText(MainActivity.this, "Sent time Request!", Toast.LENGTH_LONG).show();
            String time = manager.getInternetTime();
            LogAndToast(mainContext, String.format("getInternetTime() == %s", time));
        });


        Button btnSendMessageToWebSocket  = (Button) findViewById(R.id.btnSendMessageToWebSocket);
        btnSendMessageToWebSocket.setEnabled(false);
        btnSendMessageToWebSocket.setOnClickListener(v -> {
            sendWebSocketMessage("Hello");
        });

        Button btnOpenConnection = (Button) findViewById(R.id.btnOpenConnection);
        btnOpenConnection.setOnClickListener(v -> {
            reOpenSocket();
            btnSendMessageToWebSocket.setEnabled(true);
            btnStartStop.setEnabled((true));
        });

        Button btnVideoProcessing = (Button) findViewById(R.id.btnVideoProcessing);
        btnVideoProcessing.setEnabled(false);
        btnVideoProcessing.setOnClickListener(v -> {

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            /*
                 to directly control the camera , it seems to be that case that we need to ask for
                 permission every time
                 It is unfortunate that this is necessary. But it seems to be necessary, if I
                 remove this it didn't work.
             */
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                        MY_CAMERA_REQUEST_CODE);
            } else {
                Log.info("Manifest.permission.CAMERA is okay");
                Intent intent = new Intent(this, VideoProcessingService.class);
                startService(intent);
            }
        });


        Button btnClose  = (Button) findViewById(R.id.btnClose);
        btnClose.setOnClickListener(v -> {
            manager.disconnectAll();
        });


    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                //Display an error
                Log.error("Image data from Intent is null");
                return;
            }
            byte[] bytes = selectImageWithIntent(data);
            Log.info(String.format("Sending %s bytes", bytes.length));

            try {
                manager.sendBytes(bytes);
            } catch (Exception e) {
                Log.info("Error when sending bytes");
                e.printStackTrace();
            }
        }
    }

    public byte[] selectImageWithIntent(Intent data) {
        byte[] bytes = new byte[0];
        try {
            InputStream inputStream = mainContext.getContentResolver().openInputStream(data.getData());
            bytes = IOUtils.toByteArray(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytes;
    }


    /**
     * Methods to check what camera ID's are available.
     * @return String of the Camera DI lists of device.
     */
    public String getCameraIDList() {
        CameraManager cameraManager =
                (CameraManager) mainContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            return Arrays.toString(cameraManager.getCameraIdList());
        } catch (CameraAccessException e) {
            Log.info(e.getMessage());
            return null;
        }
    }

    public void reopenSocketConnectionClickHandler(View view) {
        reOpenSocket();
        Button btnSendText = (Button) findViewById(R.id.btnSendMessageToWebSocket);
        Button btnStartStop = (Button) findViewById(R.id.btnStartStop);
        btnSendText.setEnabled(true);
        btnStartStop.setEnabled((true));
    }

    public void reOpenSocket() {

        if (manager != null) {
            manager.disconnectAll();
        } else {
            LogAndToast(mainContext, "Opening new Socket connection");
        }
        Spinner mySpinner = findViewById(R.id.dropdown_menu);
        manager = new WebSocketManager(this, mySpinner.getSelectedItem().toString());

        // TODO: change this so be more optimal
        new Thread(() -> manager.createAndOpenWebSocketConnection(SocketType.Text)).start();
    }


    /**
     * send a custom Message to the Website. Currently not active.
     */
    public void sendCustomMessageClickHandler(View view) {

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Send Status Update");
        // Set an EditText view to get user input
        final EditText input = new EditText(this);
        String defaultMessage = String.format("Hello from %s", android.os.Build.MODEL);
        input.setText(defaultMessage);

        alert.setView(input);
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                manager.sendText(String.valueOf(input.getText()));
                Toast.makeText(MainActivity.this, "Sent message!", Toast.LENGTH_LONG).show();
            }
        });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.cancel();
            }
        });
        alert.show();

    }


    public void sendWebSocketMessage(String message) {
        manager.sendText(message);
        Toast.makeText(MainActivity.this, "Sent message!", Toast.LENGTH_LONG).show();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainpagemenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.websocket:
                intent = new Intent(this, MainActivity.class);
                break;
            case R.id.bluetooth2:
                intent = new Intent(this, BlueActivity.class);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        startActivity(intent);
        return true;
    }


    public final void generateDropDownItems() {
        Spinner spinnerHostname = findViewById(R.id.dropdown_menu);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.uri, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerHostname.setAdapter(adapter);
    }



    /**
     * tells the webserver that the parkour has begun.
     */
    public void sendStartSignalToWebServer() {
        String value_now = getDeviceTimeStamp();
        LogAndToast(MainActivity.this, value_now);
        String message = String.format("command=startTime=%s", value_now);
        if (manager.sendText(message)) {
            LogAndToast(MainActivity.this, "sendStartSignalToWebServer - successful");
        } else {
            LogAndToast(MainActivity.this, "Error sendStartSignalToWebServer " + message);
        }
    }

    /**
     * This methods returns the current Time on the device. It may differ slightly from the
     * internet time, which is more precise.
     *
     * @return current System time
     */
    public static String getDeviceTimeStamp() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Zurich"));
        return simpleDateFormat.format(cal.getTime());
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!(manager == null)) {
            manager.disconnectAll();
        }
    }



    /***
     * Fires when the user has allowed or denied camera access during runtime.
     *
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == MY_CAMERA_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

}