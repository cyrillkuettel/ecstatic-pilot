package li.garteroboter.pren.nanodet;

import android.Manifest;
import androidx.fragment.app.FragmentActivity;
import android.content.Context;
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
import android.widget.Button;
import android.widget.Spinner;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import li.garteroboter.pren.R;
import li.garteroboter.pren.nanodet.image.ImageCopyRequest;
import li.garteroboter.pren.nanodet.image.ImageProcessor;
import simple.bluetooth.terminal.DevicesFragment;
import simple.bluetooth.terminal.VibrationListener;

import li.garteroboter.pren.R;
import simple.bluetooth.terminal.DevicesFragment;

public class MainActivityNanodetNCNN extends FragmentActivity implements SurfaceHolder.Callback,
        VibrationListener, PlaySoundListener {

    private static final String TAG = "MainActivityNanodetNCNN";
    private final Context mContext = MainActivityNanodetNCNN.this;

    public static boolean TOGGLE_VIBRATE = false;
    public static boolean TOGGLE_RINGTONE = true;

    final int waitingTime = 1000; // wait x milliseconds before vibrate / ringtone again (avoid spamming)

    long lastTime = 0;

    public static final int REQUEST_CAMERA = 100;
    private NanoDetNcnn nanodetncnn = new NanoDetNcnn();
    private int facing = 1;

    private Spinner spinnerModel;
    private Spinner spinnerCPUGPU;
    private int current_model = 0;
    private int current_cpugpu = 0;

    private SurfaceView cameraView;
    private Ringtone ringtone;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_nanodet_activity);

        nanodetncnn.setObjectReferenceAsGlobal(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        cameraView = (SurfaceView) findViewById(R.id.cameraview);
        cameraView.getHolder().setFormat(PixelFormat.RGBA_8888);
        cameraView.getHolder().addCallback(this);


        spinnerModel = (Spinner) findViewById(R.id.spinnerModel);
        spinnerModel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id)
            {
                if (position != current_model)
                {
                    current_model = position;
                    reload();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0)
            {
            }
        });

        spinnerCPUGPU = (Spinner) findViewById(R.id.spinnerCPUGPU);
        spinnerCPUGPU.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id)
            {
                if (position != current_cpugpu)
                {
                    current_cpugpu = position;
                    reload();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0)
            {
            }
        });
        // Initialize a little menu at the edge of the screen, to connect to a Bluetooth Device.
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add(R.id.fragmentBluetoothChain,
                    new DevicesFragment(), "devices").commit();
        }

        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        ringtone = RingtoneManager.getRingtone(getApplicationContext(), notification);

        reload();
    }

    private void reload()
    {
        boolean ret_init = nanodetncnn.loadModel(getAssets(), current_model, current_cpugpu);
        if (!ret_init)
        {
            Log.e("MainActivity", "nanodetncnn loadModel failed");
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        nanodetncnn.setOutputWindow(holder.getSurface());
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
    }

    public void nonStaticDurchstich(String helloFromTheOtherSide) {
        startRingtone();
       // startVibrating(100);
    }


    @Override
    public void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA);
        }
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
        }
    }


    @Override
    public void onPause()
    {
        super.onPause();
        nanodetncnn.closeCamera();
    }


}
