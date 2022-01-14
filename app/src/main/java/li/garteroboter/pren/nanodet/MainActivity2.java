package li.garteroboter.pren.nanodet;

import android.Manifest;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import li.garteroboter.pren.R;

public class MainActivity2 extends AppCompatActivity implements VibrationListener {

    public static final int REQUEST_CAMERA = 100;
    private static final Logger Log = LogManager.getLogger(FragmentNanodet.class);


    private li.garteroboter.pren.nanodet.NanoDetNcnn nanodetncnn =  new li.garteroboter.pren.nanodet.NanoDetNcnn();
    private int facing = 1;


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main2);

        // Prevent rotation of the screen. This is desirable, the cpp implementation handles rotation of camera output by itself

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (savedInstanceState == null) {
            Log.info("Starting Fragment transaction");

            li.garteroboter.pren.nanodet.FragmentNanodet fragmentNanodet =
                    li.garteroboter.pren.nanodet.FragmentNanodet.newInstance(nanodetncnn, facing);

            nanodetncnn.setObjectReferenceAsGlobal(fragmentNanodet);

            getSupportFragmentManager().beginTransaction().add(R.id.fragmentBluetoothChain, fragmentNanodet, "nanodet").commit();
        }
    }



    @Override
    public void onResume()
    {
        super.onResume();

        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED)
        {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, REQUEST_CAMERA);
        }

        nanodetncnn.openCamera(facing);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        nanodetncnn.closeCamera();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.info("on Destory fired!");
    }

    @Override
    public void startVibrating(final int millis) {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
// Vibrate for N milliseconds
        try {
            v.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE));
        } catch (Exception e) {
            Log.debug("Failed to vibrate");
            e.printStackTrace();
        }
    }
}
