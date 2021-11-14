package li.garteroboter.pren;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;



import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import simple.bluetooth.terminal.BlueActivity;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private WebSocketManager manager = null;
    private static final int CAMERA_REQUEST = 1888;
    private Handler toastHandler;


    private boolean START_SIGNAL_FIRED = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        generateDropDownItems();
        Log.v(TAG, String.valueOf(android.os.Build.VERSION.SDK_INT));
        Log.v(TAG, "onCreate fired!");




    }

    public void reopenSocketConnection(View view) {
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
            Log.v(TAG, "got to setting the image");
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            ImageView myview = findViewById(R.id.imageView);
            myview.setImageBitmap(photo);
        }
    }



    public void startStoppTimer(View view) {
        // if (!START_SIGNAL_FIRED) {

            sendCurrentTime();
            START_SIGNAL_FIRED = true;
        // }
    }


    public void sendCurrentTime() {
        String value_now = getTimeStampNow();
        Utils.LogAndToast(MainActivity.this, TAG, value_now);
        String message = String.format("command=startTime=%s", value_now);
        manager.sendText(message);
        Utils.LogAndToast(MainActivity.this, TAG, "Sending message " + message);
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


}