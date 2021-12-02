package li.garteroboter.pren;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
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
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.Toast;
import android.hardware.camera2.CameraManager;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.ByteArrayOutputStream;
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

    private WebSocketManager manager = null;
    private static final int CAMERA_REQUEST = 1888;
    private Handler toastHandler;
    final Context mainContext = MainActivity.this;
    private static final int MY_CAMERA_REQUEST_CODE = 2;
    public TextureView mTextureView;
    private boolean START_SIGNAL_FIRED = false;

    SurfaceView surfaceView;
    // LogFactory


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        BasicConfigurator.configure();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        generateDropDownItems();
        Log.info(String.valueOf(android.os.Build.VERSION.SDK_INT));
        Log.info("CameraIDlist = " + getCameraIDList());
        mTextureView = (TextureView) findViewById(R.id.textureView);

        Button myButton = (Button) findViewById(R.id.btnVideoProcessing);
        myButton.setEnabled(false);
        Button btnSendText = (Button) findViewById(R.id.btnSendMessageToWebSocket);

        btnSendText.setEnabled(false);
        myButton.setEnabled(false);

    }

    private void updateTextureViewSize(int viewWidth, int viewHeight) {
        mTextureView.setLayoutParams(new FrameLayout.LayoutParams(viewWidth, viewHeight));
    }


    /**
     * Methods to check what camera ID's are available.
     *
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
        btnSendText.setEnabled(true);
    }

    public void reOpenSocket() {


        if (manager != null) {
            manager.disconnectAll();
        } else {
            Utils.LogAndToast(mainContext, "Opening new Socket connection");
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

    public void sendCustomMessageNoAlert(View view) {
        sendWebSocketMessage("It works ¯\\_(ツ)_/¯");
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
            case R.id.simpleUI:
                intent = new Intent(this, simpleUIActivity.class);
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

    // Set this up in the UI thread.

    public final void getInternetTime(View view) throws ExecutionException, InterruptedException {
        Toast.makeText(MainActivity.this, "Sent time Request!", Toast.LENGTH_LONG).show();
        String time = manager.getInternetTime();

        // Utils.LogAndToast(mainContext, "Internet time received =  %s".format(time));


    }

    public void onCloseSocketClickHandler(View view) {
        manager.disconnectAll();
    }


    public void startStoppTimerClickHandler(View view) {
        // if (!START_SIGNAL_FIRED) {
        sendStartSignalToWebServer();
        START_SIGNAL_FIRED = true;
        // }
    }

    /**
     * tells the webserver that the parkour has begun.
     */
    public void sendStartSignalToWebServer() {
        String value_now = getDeviceTimeStamp();
        Utils.LogAndToast(MainActivity.this, value_now);
        String message = String.format("command=startTime=%s", value_now);
        if (manager.sendText(message)) {
            Utils.LogAndToast(MainActivity.this, "Sending message " + message);
        } else {
            Utils.LogAndToast(MainActivity.this, "Error Sending message " + message);
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


    /**
     * converts an InputStream of Bytes to an byte[] array
     */
    public byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }


    public void videoProcessingServiceClickHandler(View view) {
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