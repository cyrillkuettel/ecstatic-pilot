package li.garteroboter.pren;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private WebSocketManager manager = null;
    private static final int CAMERA_REQUEST = 1888;


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
        alert.setTitle("WebSocket client");
        // Set an EditText view to get user input
        final EditText input = new EditText(this);
        String defaultMessage = String.format("Hello from %s", android.os.Build.MODEL);
        input.setText(defaultMessage);

        alert.setView(input);
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                manager.sendText(String.valueOf(input.getText()));
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
            case R.id.bluetooth:
                intent = new Intent(this, BluetoothActivity.class);
                break;
            case R.id.websocket:
                intent = new Intent(this, MainActivity.class);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        startActivity(intent);
        return true;
    }


    public final void generateDropDownItems() {
        Spinner spinnerLanguages = findViewById(R.id.dropdown_menu);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.uri, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
        spinnerLanguages.setAdapter(adapter);
    }


    public void onCloseSocketHandler(View view) {
        manager.disconnectAll();
    }

    public void onCameraOpenClickHandler(View view) {
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        startActivity(intent);

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




}