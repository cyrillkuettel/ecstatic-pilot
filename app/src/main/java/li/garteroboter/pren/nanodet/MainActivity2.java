package li.garteroboter.pren.nanodet;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import li.garteroboter.pren.R;

public class MainActivity2 extends AppCompatActivity {

    public static final int REQUEST_CAMERA = 100;


    private li.garteroboter.pren.nanodet.NanoDetNcnn nanodetncnn =  new li.garteroboter.pren.nanodet.NanoDetNcnn();
    private int facing = 1;


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main2);


        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (savedInstanceState == null) {
            Log.v("MainActivity2", "Starting Fragment transaction");

            li.garteroboter.pren.nanodet.FragmentNanodet fragmentNanodet =
                    li.garteroboter.pren.nanodet.FragmentNanodet.newInstance(nanodetncnn, facing);

            nanodetncnn.setObjectReferenceAsGlobal(fragmentNanodet);

            getSupportFragmentManager().beginTransaction().add(R.id.fragment, fragmentNanodet, "nanodet").commit();
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

}
