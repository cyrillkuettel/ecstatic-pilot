package li.garteroboter.pren.qrcode;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
//import androidx.fragment.app.
import androidx.lifecycle.LifecycleOwner;

import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.util.concurrent.ExecutionException;

import li.garteroboter.pren.Constants;
import li.garteroboter.pren.MainActivity;
import li.garteroboter.pren.R;
import li.garteroboter.pren.nanodet.VibrationListener;
import simple.bluetooth.terminal.TerminalFragment;
import simple.bluetooth.terminal.screen.ScreenSlidePageFragment;


public class CameraPreviewFragment extends Fragment {

    private static final Logger Log = LogManager.getLogger(simple.bluetooth.terminal.BlueActivity.class);
    private static final int PERMISSION_REQUEST_CAMERA = 0;
    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    // final Vibrator vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);


    private Button qrCodeFoundButton;
    private String qrCode;
    private VibrationListener vibrationListener;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) { // @Nullable  means that the return value of onCreateView method can be null
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);


        cameraProviderFuture = ProcessCameraProvider.getInstance(getActivity());
        requestCamera();
    }

    public static CameraPreviewFragment newInstance() {
        CameraPreviewFragment fragment = new CameraPreviewFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }




    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_camerax, container, false);
    }







    private void requestCamera() {
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.CAMERA)) {
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
            } else { /* kind of dubious */
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA},
                        PERMISSION_REQUEST_CAMERA);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Log.warn("\"Camera Permission Denied\"");
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getActivity(), "Camera Permission Denied", Toast.LENGTH_SHORT).show();
                    }
                });

            }
        }
    }

    private void startCamera() {
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.error("Error starting camera " + e.getMessage());
               // Toast.makeText(this, "Error starting camera " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(getActivity()));
    }

    private void bindCameraPreview(@NonNull ProcessCameraProvider cameraProvider) {
        previewView.setPreferredImplementationMode(PreviewView.ImplementationMode.SURFACE_VIEW);

        Preview preview = new Preview.Builder()
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        preview.setSurfaceProvider(previewView.createSurfaceProvider());
        // 720 480
        // before was 1280 720

        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder()
                        // enable the following line if RGBA output is needed.
                        //.setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .setTargetResolution(new Size(720, 480))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();



        /**
         I use the setAnaylzer() method on the ImageAnalysis object
         provide an object of the custom image analyzer class {@link QRCodeImageAnalyzer}
         Then implement the onQRCodeFound(…) and qrCodeNotFound() methods from the {@link QRCodeFoundListener}
         */
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(getActivity()), new QRCodeImageAnalyzer(new QRCodeFoundListener() {

            long lastTime = 0;
            @Override
            public void onQRCodeFound(String _qrCode) {
                if ( System.currentTimeMillis() - lastTime > 400) {
                    // do nothing if last call was less than 1000 ms ago
                    vibrationListener.startVibrating(100);
                    lastTime = System.currentTimeMillis();
                    qrCode = _qrCode;
                    qrCodeFoundButton.setVisibility(View.VISIBLE);

                }

            }
            @Override
            public void qrCodeNotFound() {
                qrCodeFoundButton.setVisibility(View.INVISIBLE);
            }
        }));
        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, imageAnalysis, preview);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        previewView = (PreviewView)  getView().findViewById(R.id.activity_main_previewView);
        qrCodeFoundButton = (Button) getView().findViewById(R.id.activity_main_qrCodeFoundButton);
        qrCodeFoundButton.setText("Send Stop Command");
        qrCodeFoundButton.setVisibility(View.INVISIBLE);
        // TODO : remove this useless buttton, add a Listener to Terminal Fragment
        qrCodeFoundButton.setOnClickListener(v -> {
            // Toast.makeText(getApplicationContext(), qrCode, Toast.LENGTH_SHORT).show();
            Log.info(MainActivity.class.getSimpleName() + " QR Code Found: " + qrCode);
            
            try {
                /*  This is the quick and dirty solution for now.
                    Use Interface which TerminalFragment Implements.

                     this is the better, and safer, approach from a design perspective
                     https://stackoverflow.com/questions/12659747/call-an-activity-method-from-a-fragment
                 */


                TerminalFragment tf = (TerminalFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.fragmentBluetoothChain);
                tf.send(Constants.STOP_COMMAND_ESP32);
            } catch (Exception e) {
                Log.info("Accessing TerminalFragment Object failed. ");
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            vibrationListener = (VibrationListener) context;
        } catch (ClassCastException castException) {
            /** The activity does not implement the listener. */
            Log.error("Failed to implement VibrationListener");
        }
    }



    public CameraPreviewFragment() {
        // Required empty public constructor
    }

}