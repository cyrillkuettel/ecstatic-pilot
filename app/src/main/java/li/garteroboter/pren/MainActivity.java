package li.garteroboter.pren;

import static li.garteroboter.pren.Utils.LogAndToast;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

import simple.bluetooth.terminal.BlueActivity;


public class MainActivity extends AppCompatActivity {

    private static final Logger Log = LogManager.getLogger(MainActivity.class);

    private static final String ABSOLUTE_APK_PATH = "https://github.com/cyrillkuettel/ecstatic" +
            "-pilot/blob/master/app/build/outputs/apk/debug/app-debug.apk?raw=true";
    private WebSocketManager manager = null;
    private Handler toastHandler;
    final Context mainContext = MainActivity.this;
    private static final int MY_CAMERA_REQUEST_CODE = 2;
    public static final int PICK_IMAGE = 1; // so you can recognize when the user comes back from
    // the image gallery

    private boolean START_SIGNAL_FIRED = false;
    private static final int PIXEL_CAMERA_WIDTH = 3036;  // default values when taking pictures
    // with google camera.
    private static final int PIXEL_CAMERA_HEIGHT = 4048;


    @Override
    protected void onCreate(Bundle savedInstanceState) {


        BasicConfigurator.configure();
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        generateDropDownItems();

        Log.info(String.valueOf(android.os.Build.VERSION.SDK_INT));

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
            showUpdateMessageBox();

        });

        Button btnSelectImageAndSend = (Button) findViewById(R.id.btnSelectImageAndSend);
        btnSelectImageAndSend.setEnabled(false);
        btnSelectImageAndSend.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
        });

        Button btnOpenByteSocketConnection =
                (Button) findViewById(R.id.btnOpenByteSocketConnection);
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
            btnSelectImageAndSend.setEnabled(true);
        });

        Button btnInternetTime = (Button) findViewById(R.id.btnInternetTime);
        btnInternetTime.setEnabled(false);
        btnInternetTime.setOnClickListener(v -> {
            reOpenSocket();
            Toast.makeText(MainActivity.this, "Sent time Request!", Toast.LENGTH_LONG).show();
            String time = manager.getInternetTime();
            LogAndToast(mainContext, String.format("getInternetTime() == %s", time));
        });


        Button btnSendMessageToWebSocket = (Button) findViewById(R.id.btnSendMessageToWebSocket);
        btnSendMessageToWebSocket.setEnabled(false);
        btnSendMessageToWebSocket.setOnClickListener(v -> {
            manager.sendText("Hello from Android!");
            Toast.makeText(MainActivity.this, "Sent message!", Toast.LENGTH_LONG).show();
        });

        Button btnOpenConnection = (Button) findViewById(R.id.btnOpenConnection);
        btnOpenConnection.setOnClickListener(v -> {
            reOpenSocket();
            btnSendMessageToWebSocket.setEnabled(true);
            btnStartStop.setEnabled((true));
        });


        Button btnClose = (Button) findViewById(R.id.btnClose);
        btnClose.setOnClickListener(v -> {
            manager.disconnectAll();
        });

        Button btnSendImagesFromDisk = (Button) findViewById(R.id.btnSendImagesFromDisk);
        btnSendImagesFromDisk.setOnClickListener(v -> {
            testSendArrayOfPlants();

        });

        Button btnGetAbsoluteFilePath = (Button) findViewById(R.id.btnGetAbsoluteFilePath);
        btnGetAbsoluteFilePath.setOnClickListener(v -> {
            // nothing
        });

    }



    public void testSendArrayOfPlants() {

        // File file = new File(mainContext.getFilesDir(), "hello_world");
        File[] files = mainContext.getFilesDir().listFiles();
        List<String> file_names = Arrays.stream(files)
                .map(f -> f.getName().toString()).collect(Collectors.toList());

        StorageAccessAgent storageAccessAgent = new StorageAccessAgent(mainContext);
        List<String> plants = storageAccessAgent.fetchNames();
        storageAccessAgent.copyPlantsToInternalDirectory(plants.toArray(new String[0]));


/*
        String filename = "myfile";
        String string = "Hello world!";
        FileOutputStream outputStream;

        try {
            outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(string.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.info(String.format("Written %s", "a file"));

 */

    }

    public void vibrate() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
// Vibrate for 500 milliseconds
        v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
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
                Log.error("Error while sending bytes");
                e.printStackTrace();
            }
        }
    }

    public byte[] selectImageWithIntent(Intent data) {
        byte[] bytes = new byte[0];
        try {
            InputStream inputStream =
                    mainContext.getContentResolver().openInputStream(data.getData());
            bytes = IOUtils.toByteArray(inputStream);

        } catch (IOException e) {
            Log.error("Something went wrong when reading InputStream to bytes. Maybe context?");
            e.printStackTrace();
        }
        return bytes;
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


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainpagemenu, menu);
        return true;
    }

    /**
     * Hamburger Menu selection
     */
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
            case R.id.nanodet:
                intent = new Intent(this, li.garteroboter.pren.nanodet.MainActivity2.class);
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
     * Note that in the end this messsage has to be called as a result of the Bluetooth message
     */
    public void sendStartSignalToWebServer() {
        String value_now = getDeviceTimeStampAsMilliseconds();
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
        Log.info("startTime=" + message);
        return message;
    }


    public final void showUpdateMessageBox() {

        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Achtung: Update Vorgehensweise");
        final TextView messageForUpdateProcedure = new TextView(this);
        final String defaultMessage = String.format("Zuerst dieses App deinstallieren, bevor das " +
                "apk installiert wird! Wenn die Meldung \"App nicht installiert\" kommt, hilft " +
                "meistens ein Neustart des Geräts. ");
        messageForUpdateProcedure.setText(defaultMessage);
        alert.setView(messageForUpdateProcedure);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(ABSOLUTE_APK_PATH));
                startActivity(browserIntent);
            }
        });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.cancel();
            }
        });
        alert.show();

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