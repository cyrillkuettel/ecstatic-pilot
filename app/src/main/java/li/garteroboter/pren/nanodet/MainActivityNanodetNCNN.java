package li.garteroboter.pren.nanodet;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;

import android.Manifest;

import androidx.fragment.app.FragmentActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Spinner;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import li.garteroboter.pren.R;
import li.garteroboter.pren.qrcode.QrCodeActivity;
import li.garteroboter.pren.settings.container.CustomSettingsBundle;
import li.garteroboter.pren.settings.container.SettingsBundle;
import simple.bluetooth.terminal.DevicesFragment;
import simple.bluetooth.terminal.VibrationListener;

public class MainActivityNanodetNCNN extends FragmentActivity implements SurfaceHolder.Callback,
        VibrationListener, PlaySoundListener {

    public static final int REQUEST_CAMERA = 100;
    private static final String TAG = "MainActivityNanodetNCNN";
    public static boolean TOGGLE_VIBRATE = false;
    public static boolean TOGGLE_RINGTONE = true;

    final int waitingTime = 1000; // wait x milliseconds before vibrate / ringtone again (avoid
    // spamming)
    private final Context mContext = MainActivityNanodetNCNN.this;
    long lastTime = 0;
    private NanoDetNcnn nanodetncnn = new NanoDetNcnn();
    private int facing = 1;


    private Spinner spinnerModel;
    private Spinner spinnerCPUGPU;
    private int current_model = 0;
    private int current_cpugpu = 0;

    private SurfaceView cameraView;
    private Ringtone ringtone;

    private SettingsBundle settingsBundle;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_nanodet_activity);
        // create a reference to the object of this class in the C++ layer
        nanodetncnn.setObjectReferenceAsGlobal(this);

        settingsBundle = readCurrentPreferenceState();
        boolean useBluetooth = settingsBundle.isUsingBluetooth();

        nanodetncnn.injectBluetoothSettings(settingsBundle.isUsingBluetooth());
        nanodetncnn.injectFPSPreferences(settingsBundle.isShowFPS());
        nanodetncnn.injectProbThresholdSettings(settingsBundle.getProb_threshold());

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        cameraView = findViewById(R.id.cameraview);
        cameraView.getHolder().setFormat(PixelFormat.RGBA_8888);
        cameraView.getHolder().addCallback(this);


        spinnerModel = findViewById(R.id.spinnerModel);
        spinnerModel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
                if (position != current_model) {
                    current_model = position;
                    reload();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        spinnerCPUGPU = (Spinner) findViewById(R.id.spinnerCPUGPU);
        spinnerCPUGPU.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
                if (position != current_cpugpu) {
                    current_cpugpu = position;
                    reload();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
        // Initialize a little menu at the edge of the screen, to connect to a Bluetooth Device.
        // interesting: based on whether we want to automatically connect to ESP,
        // I could pass in a Parameter into devices Fragment to do this.
        if (savedInstanceState == null) {
            Bundle args = new Bundle();
            if (useBluetooth) {
                args.putString("autoConnect", "true");
            } else {
                args.putString("autoConnect", "false");
            }
            DevicesFragment devicesFragment = new DevicesFragment();
            devicesFragment.setArguments(args);

            getSupportFragmentManager().beginTransaction().add(R.id.fragmentBluetoothChain,
                    devicesFragment, "devices").commit();
        }

        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        ringtone = RingtoneManager.getRingtone(getApplicationContext(), notification);


        reload();
    }

    private void reload() {
        boolean ret_init = nanodetncnn.loadModel(getAssets(), current_model, current_cpugpu);
        if (!ret_init) {
            Log.e("MainActivity", "nanodetncnn loadModel failed");
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        nanodetncnn.setOutputWindow(holder.getSurface());
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    long count = 0;
    public void nonStaticDurchstich(String helloFromTheOtherSide) {
        if (count++ >= 1) { // number of confirmations. The lower, the faster
            Log.d(TAG, String.valueOf(count));
            count = 0;
            startRingtone();
        }


        /*

        startVibrating(100);
        // plant detection, so we switch to the QR Activity     */
        Intent myIntent = new Intent(this, QrCodeActivity.class);
        myIntent.addFlags(FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(myIntent);


    }


    @Override
    public void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA);
        }
        // could also here
        nanodetncnn.openCamera(facing);
    }

    @Override
    public void startVibrating(final int millis) {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        // safety mechanism to not vibrate too often.
        if (TOGGLE_VIBRATE && System.currentTimeMillis() - lastTime > waitingTime) {

// Vibrate for N milliseconds
            try {
                v.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE));
            } catch (Exception e) {
                Log.d(TAG, "Failed to initialize SystemService Vibration");
                e.printStackTrace();
            }
            lastTime = System.currentTimeMillis();
        }
    }

    @Override
    public void startRingtone() {
        if (TOGGLE_RINGTONE && System.currentTimeMillis() - lastTime > waitingTime) {
            try {
                ringtone.play();
            } catch (Exception e) {
                e.printStackTrace();
            }
            lastTime = System.currentTimeMillis();
            Log.d(TAG, String.valueOf(count));
        }
    }

    private SettingsBundle readCurrentPreferenceState() {
        // Read the preferences
        Context applicationContext = getApplicationContext();
        SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(applicationContext);

        boolean useBluetooth = preferences.getBoolean("key_bluetooth", false);
        boolean drawFps = preferences.getBoolean("key_fps", false);
        String _value = preferences.getString("key_prob_threshold", "0.40");
        float probThreshold = Float.parseFloat(_value);

        return new CustomSettingsBundle(useBluetooth, drawFps, probThreshold);

    /*
                Like this you could loop through the preferences

        Map<String, ?> map = preferences.getAll();

        if (map.size() == 0) { // default settings
            Log.i(TAG, "Using the default Settings");
            return new DefaultSettingsBundle();
        }

        for(Map.Entry<String,?> entry : map.entrySet()){
            Log.d("map values",entry.getKey() + ": " +
                    entry.getValue().toString());
        }

      */

    }


    @Override
    public void onPause() {
        super.onPause();
        nanodetncnn.closeCamera();
    }


}
