package li.garteroboter.pren;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.hardware.camera2.CameraManager;





import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;

import simple.bluetooth.terminal.BlueActivity;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private WebSocketManager manager = null;
    private static final int CAMERA_REQUEST = 1888;
    private Handler toastHandler;
    private final Context mainContext = MainActivity.this;

    private boolean START_SIGNAL_FIRED = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        generateDropDownItems();
        Log.v(TAG, String.valueOf(android.os.Build.VERSION.SDK_INT));
        Log.v(TAG, "onCreate fired!");

        Log.v(TAG, "CameraIDlist = " + getCamera());
        selectImage();

    }

    public String getCamera() {
        CameraManager manager = (CameraManager) mainContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            return Arrays.toString(manager.getCameraIdList());
        } catch (CameraAccessException e) {
            Log.v(TAG, e.getMessage());
            return null;
        }
    }

    public void reopenSocketConnectionClickHandler(View view) {
        reOpenSocket();

    }

    public void reOpenSocket() {
        if (!(manager == null)) {
            manager.disconnectAll();
        }
        Spinner mySpinner = findViewById(R.id.dropdown_menu);
        String URI = mySpinner.getSelectedItem().toString();
        manager = new WebSocketManager(URI);

        new Thread(() -> manager.openNewConnection(Sockets.Text)).start();
    }


    /**
     * send a custom Message to the Website
     *
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

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
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

    // Set this up in the UI thread.

    public void getInternetTime(View view) throws ExecutionException, InterruptedException {
        Toast.makeText(MainActivity.this, "Sent time Request!", Toast.LENGTH_LONG).show();

        manager.getInternetTime();

 /*
        ExecutorService pool = Executors.newFixedThreadPool(10);
        Set<Future<String>> set = new HashSet<>();


        Callable<String> callable = new InternetTime();
        Future<String> future = pool.submit(callable);
        set.add(future);


         String result = future.get();
         Utils.LogAndToast(MainActivity.this, TAG, result);
*/

    }



    public void onCloseSocketHandler(View view) {
        manager.disconnectAll();
    }

    public void onCameraOpenClickHandler(View view) {



    }





    public void startStoppTimerClickHandler(View view) {
        // if (!START_SIGNAL_FIRED) {
            sendStartSignalToWebServer();
            START_SIGNAL_FIRED = true;
        // }
    }

    /**
     * tells the webserver that the parkour has begun.
     *
     */
    public void sendStartSignalToWebServer() {
        String value_now = getTimeStampNow();
        Utils.LogAndToast(MainActivity.this, TAG, value_now);
        String message = String.format("command=startTime=%s", value_now);
        if (manager.sendText(message)) {
            Utils.LogAndToast(MainActivity.this, TAG, "Sending message " + message);
        } else {
            Utils.LogAndToast(MainActivity.this, TAG, "Error Sending message " + message);
        }
    }


    public String getTimeStampNow() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Zurich"));

        String value_now = simpleDateFormat.format(cal.getTime());
        // value_now = value_now.replaceAll("\\s","");  // strip whitespace
        return value_now;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!(manager == null)) {
            manager.disconnectAll();
        }
    }
    static final int REQUEST_IMAGE_GET = 1;


    /**
     * from the android documentation
     */
    public void selectImage() {

        File file = getFilesDir();
        String path = file.getAbsolutePath();
        Log.v(TAG, path);

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_IMAGE_GET);
        }
    }

    /**
     * also from the android documentation
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode == REQUEST_IMAGE_GET && resultCode == RESULT_OK) {
           // Bitmap thumbnail = data.getParcelable("data");
            Uri fullImageUri = data.getData();
            InputStream iStream = null;
            try {
                iStream = getContentResolver().openInputStream(fullImageUri);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            if (iStream != null ) {
                try {
                    byte[] inputData = getBytes(iStream);

                    Log.v(TAG, Arrays.toString(inputData));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Log.e(TAG, "iStream is null");
            }



        }
    }

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



    public void changeButtonColorOnpress() {
        Button mButton = findViewById(R.id.btnImageRead);
        mButton.setBackgroundColor(Color.RED); // not working ,why

    }

    public void readLocalImageClickHandler(View view) {
        selectImage();
    }
}