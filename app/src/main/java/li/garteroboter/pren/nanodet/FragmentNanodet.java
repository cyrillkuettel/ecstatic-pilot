package li.garteroboter.pren.nanodet;

import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Bundle;

import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import li.garteroboter.pren.MainActivity;
import li.garteroboter.pren.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FragmentNanodet#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class FragmentNanodet extends Fragment implements SurfaceHolder.Callback {

    private static final Logger Log = LogManager.getLogger(FragmentNanodet.class);

    li.garteroboter.pren.nanodet.NanoDetNcnn nanodetncnn;
    int facing;

    private Spinner spinnerModel;
    private Spinner spinnerCPUGPU;
    private int current_model = 0;
    private int current_cpugpu = 0;

    private SurfaceView cameraView;
    private TextView textView;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            facing = getArguments().getInt("facing");
            nanodetncnn = getArguments().getParcelable("nanodetncnn");


        }


        reload();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_nanodet, container, false);
        textView = (TextView) view.findViewById(R.id.buttonPlantDetection);
        textView.setVisibility(View.INVISIBLE);


        return view;
    }

    private void reload()
    {
        boolean ret_init = nanodetncnn.loadModel(getContext().getAssets(), current_model, current_cpugpu);
        if (!ret_init)
        {
            Log.error("nanodetncnn loadModel failed");
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        // this should probably be in oncreateView right? And use the view parameter instead of getView()

        cameraView = (SurfaceView) getView().findViewById(R.id.cameraview2);
        cameraView.getHolder().setFormat(PixelFormat.RGBA_8888);
        cameraView.getHolder().addCallback(this);

        Button buttonSwitchCamera = (Button) getView().findViewById(R.id.buttonSwitchCamera2);
        buttonSwitchCamera.setOnClickListener(arg0 -> {

            int new_facing = 1 - facing;

            nanodetncnn.closeCamera();

            nanodetncnn.openCamera(new_facing);

            facing = new_facing;
        });

        spinnerModel = (Spinner) getView().findViewById(R.id.spinnerModel2);
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

        spinnerCPUGPU = (Spinner) getView().findViewById(R.id.spinnerCPUGPU2);
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



    }


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment FragmentNanodet.
     */

    public static FragmentNanodet newInstance(final li.garteroboter.pren.nanodet.NanoDetNcnn nanodetncnn, final int facing ) {
        FragmentNanodet fragment = new FragmentNanodet();
        Bundle args = new Bundle();
        args.putInt("facing", facing);
        args.putParcelable("nanodetncnn", nanodetncnn);
        fragment.setArguments(args);
        return fragment;
    }

    // this method is in fact used, IDE can't see it at compile time
    public static void durchstich() {

        // TODO: do nothing if last move was less than 40 ms ago
      //  Log.d("FragmentNanodet", "Durchstich VERDAMMT NOCH MAL ");

    }

    long lastTime = 0;
    // this method is in fact used, IDE can't see it at compile time
    public void nonStaticDurchstich(String objectLabel) {
        if ( System.currentTimeMillis() - lastTime > 1000) {
            // do nothing if last call was less than 1000 ms ago
            // Log.info(String.format("detected %s", objectLabel));

            setPlantDetectedText(objectLabel);
            lastTime = System.currentTimeMillis();
        }
    }

    public void setPlantDetectedText(final String objectLabel){
        Log.info("setPlantDetectedText");

        /*
        try {
            textView.setVisibility(View.VISIBLE);
            textView.setText(String.format("%s detected", objectLabel));
        } catch (Exception e) {
            Log.error("NullPointer for button id = buttonPlantDetection in the method you created: setPlantDetectedText ");
            e.printStackTrace();
        }

         */

    }




    public FragmentNanodet() {
        // Required empty public constructor
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        nanodetncnn.setOutputWindow(holder.getSurface());
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
    }



}