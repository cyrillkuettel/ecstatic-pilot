package com.example.pilot;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.StrictMode;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;

import com.neovisionaries.ws.client.HostnameUnverifiedException;
import com.neovisionaries.ws.client.OpeningHandshakeException;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.net.SocketFactory;

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


        Log.v(TAG, "onCreate fired!!");
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


    public void sendMessageClickHandler(View view) {
        manager.sendText();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        manager.disconnectAll();
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