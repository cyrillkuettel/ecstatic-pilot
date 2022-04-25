package li.garteroboter.pren.nanodet;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static li.garteroboter.pren.Constants.STOP_COMMAND_ESP32;

import android.Manifest;
import android.annotation.SuppressLint;
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

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import li.garteroboter.pren.R;
import li.garteroboter.pren.databinding.ActivityNanodetncnnBinding;
import li.garteroboter.pren.preferences.bundle.CustomSettingsBundle;
import li.garteroboter.pren.preferences.bundle.SettingsBundle;
import li.garteroboter.pren.qrcode.QrcodeActivity;
import simple.bluetooth.terminal.DevicesFragment;
import simple.bluetooth.terminal.TerminalFragment;
import simple.bluetooth.terminal.VibrationListener;

public class NanodetncnnActivity extends FragmentActivity implements SurfaceHolder.Callback,
        VibrationListener, PlaySoundListener {

    public static final int REQUEST_CAMERA = 100;
    private static final String TAG = "MainActivityNanodetNCNN";
    public static boolean TOGGLE_VIBRATE = false;
    public static boolean TOGGLE_RINGTONE = true;

    final int waitingTime = 1000; // wait x milliseconds before vibrate / ringtone again (avoid
    // spamming)
    long lastTime = 0;

    long lastTimePlantCallback = 0;
    final int waitingTimePlantCallback = 5000; // 5 seconds till a pant is again detected

    private final AtomicInteger atomicCounter = new AtomicInteger(0);
    private static final int REQUEST_PERMISSIONS_CODE_BLUETOOTH_CONNECT = 11;

    private final NanoDetNcnn nanodetncnn = new NanoDetNcnn();

    private boolean useBluetooth;
    private int current_model = 0;
    private int current_cpugpu = 0;

    private SurfaceView cameraView;
    private Ringtone ringtone;
    private TerminalFragment terminalFragment;

    boolean transitionToQRActivityEnabled = true;
    private ActivityNanodetncnnBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNanodetncnnBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // creates a reference to the currently active instance
        // of MainActivityNanodetNCNN in the C++ layer
        nanodetncnn.setObjectReferenceAsGlobal(this);

        SettingsBundle settingsBundle = generatePreferenceBundle();
        useBluetooth = settingsBundle.isUsingBluetooth();
        nanodetncnn.injectBluetoothSettings(useBluetooth);
        nanodetncnn.injectFPSPreferences(settingsBundle.isShowFPS());
        nanodetncnn.injectProbThresholdSettings(settingsBundle.getProb_threshold());

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        cameraView = binding.cameraview;
        cameraView.getHolder().setFormat(PixelFormat.RGBA_8888);
        cameraView.getHolder().addCallback(this);


        Spinner spinnerModel = binding.spinnerModel;
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

        Spinner spinnerCPUGPU = binding.spinnerCPUGPU;
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

        if (savedInstanceState == null) {
            Bundle args = new Bundle();
            if (useBluetooth) {
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG," != PackageManager.PERMISSION_GRANTED" );
                    requestPermissions(new String[]
                                    {Manifest.permission.BLUETOOTH_CONNECT},
                                    REQUEST_PERMISSIONS_CODE_BLUETOOTH_CONNECT);

                } else {
                    args.putString("autoConnect", "true");
                }
            } else {
                args.putString("autoConnect", "false");
            }
            DevicesFragment devicesFragment = DevicesFragment.newInstance();
            devicesFragment.setArguments(args);

            getSupportFragmentManager().beginTransaction().add(R.id.fragmentBluetoothChain,
                    devicesFragment, "devices").commit();
        } else {
            Log.d(TAG, "savedInstanceState != NULL");
        }

        initializePreferences();


        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        ringtone = RingtoneManager.getRingtone(getApplicationContext(), notification);

        // To be absolutely certain we have a plant detected, and not a false positive, I
        // implemented a little extra check.
        // It will watch for a fast burst of plantVaseDetectedCallback.
        // To detect that burst of callbacks in a short period of time, I use an atomicCounter.
        // This atomicCounter tracks the number of plantVaseDetectedCallback in a given interval.
        // After the interval has passed, simply reset the atomicCounter back to zero.
        Runnable resetAtomicCounterEveryNSeconds = () -> {
            Log.v(TAG, "resetting counter");
            atomicCounter.incrementAndGet();
        };
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(resetAtomicCounterEveryNSeconds,
                0,
                3,
                TimeUnit.SECONDS);


        reload();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS_CODE_BLUETOOTH_CONNECT) {
            Bundle args = new Bundle();
            args.putString("autoConnect", "true");
            DevicesFragment devicesFragment = DevicesFragment.newInstance();
            devicesFragment.setArguments(args);

            getSupportFragmentManager().beginTransaction().add(R.id.fragmentBluetoothChain,
                    devicesFragment, "devices").commit();
        }
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

    /** This method is called by the native layer.
     * It is in a sense the most important part of this application.
     * Note that this is effectively called by a different thread.
     * It _is_ a different thread. that also means you cannot change the UI from this method directly*/
    @SuppressWarnings("unused")
    public void plantVaseDetectedCallback(String helloFromTheOtherSide) {
        int _count = atomicCounter.incrementAndGet();
        if (_count != 0) {
            // Log.v(TAG,String.format("current number of confirmations = %d", _count));
        }
        if ( _count >= 5) { // count = number of confirmations. The lower, the faster
            atomicCounter.set(0); // reset the counter back

            if (lastTimeWasNSecondsAGo() ){
                lastTimePlantCallback = System.currentTimeMillis();

                Log.d(TAG, String.format("Accept potted plant detection with %d confirmations", _count));

                synchronized (this) {
                    if (bluetoothCheck(terminalFragment)) {
                        terminalFragment.send(STOP_COMMAND_ESP32);
                    }
                }
                startRingtone();

                startQRActivityIfEnabled();
            }
        }
    }

    private boolean lastTimeWasNSecondsAGo() {
        return  System.currentTimeMillis() - lastTimePlantCallback > waitingTimePlantCallback;
    }

    private boolean bluetoothCheck(TerminalFragment terminalFragment) {
        if (useBluetooth && terminalFragment != null)  {
            return true;
        }
        if (terminalFragment== null) {
            Log.d(TAG, "bluetoothCheck failed, terminalFragment == null ");
        }
        return false;
    }

    public void startQRActivityIfEnabled() {
        if (transitionToQRActivityEnabled) {
            Intent myIntent = new Intent(this, QrcodeActivity.class);
            myIntent.addFlags(FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(myIntent);
        }
    }

    /** Called from terminal Fragment */
    public void receiveTerminalFragmentReference(TerminalFragment terminalFragment) {
        if (bluetoothCheck(terminalFragment)) {
            this.terminalFragment = terminalFragment;
        } else {
         Log.e(TAG, "receiveTerminalFragmentReference failed to get reference to terminalFragment");
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA);
        }

        nanodetncnn.openCamera(1); // open front always
    }

    @SuppressLint("MissingPermission")
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
        if (TOGGLE_RINGTONE ) {
            try {
                ringtone.play();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // creates an object, which is a container for some preferences. This bundle object is then
    // passed to the native layer)
    private SettingsBundle generatePreferenceBundle() {

        SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        boolean useBluetooth = preferences.getBoolean("key_bluetooth", false);
        boolean drawFps = preferences.getBoolean("key_fps", false);
        String _value = preferences.getString("key_prob_threshold", "0.40");
        float probThreshold = Float.parseFloat(_value);
        Log.d(TAG, String.format("prob_threshold == %s" , probThreshold));
        int plantCount = preferences.getInt("number_picker_preference", 4);
        Log.d(TAG, String.format("number_picker_preference == %s" , plantCount));

        return new CustomSettingsBundle(useBluetooth, drawFps, probThreshold);

    }

    private void initializePreferences() {
        SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        // load preferences ot local variable
        transitionToQRActivityEnabled = preferences.getBoolean("key_start_transition", false);

    }


    @Override
    public void onPause() {
        super.onPause();
        nanodetncnn.closeCamera();
    }
}
