package li.garteroboter.pren.qrcode;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.util.Size;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import li.garteroboter.pren.R;
import li.garteroboter.pren.nanodet.PlaySoundListener;
import li.garteroboter.pren.nanodet.VibrationListener;

public class QrCodeActivity extends AppCompatActivity implements PlaySoundListener,
        VibrationListener {
    private static final String TAG = "QrCodeActivity";
    private static final int PERMISSION_REQUEST_CAMERA = 0;
    /**
     * after X seconds, we want to stop looking for QRCode.
     * False positives are rare, but they to happen.
     * If it the Activity lifetime exceed X seconds, then exit.
     */
    private final long MaximumActivityLifetime = 5000;
    private final int waitingTimeVibrate = 1000; // wait x milliseconds before ring again
    private final int waitingTimeRingtone = 500; // wait x milliseconds before vibrate again
    /**
     * Control Ringtone and vibration onQRCodefound
     */
    public boolean TOGGLE_RINGTONE = false;
    public boolean TOGGLE_VIBRATE = true;
    /**
     * tracks how long this Activity is already running
     */
    private long lastTimeNoQRCodeWasFound = -1;
    /**
     * The actual decoded QR Code String
     */
    private String qrCode;
    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private Ringtone ringtone;
    private long lastTimeVibrated = 0;
    private long lastTimeRingtonePlayed = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrcode);

        previewView = findViewById(R.id.activity_main_previewView);

        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        ringtone = RingtoneManager.getRingtone(getApplicationContext(), notification);
        if (lastTimeNoQRCodeWasFound == -1) {
            lastTimeNoQRCodeWasFound = System.currentTimeMillis();
        }
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        requestCamera();


    }

    private void startCamera() {
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Error starting camera " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void requestCamera() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA)) {
                ActivityCompat.requestPermissions(QrCodeActivity.this,
                        new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                        PERMISSION_REQUEST_CAMERA);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void bindCameraPreview(@NonNull ProcessCameraProvider cameraProvider) {
        previewView.setPreferredImplementationMode(PreviewView.ImplementationMode.SURFACE_VIEW);

        Preview preview = new Preview.Builder()
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        preview.setSurfaceProvider(previewView.createSurfaceProvider());

        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this),
                new QRCodeImageAnalyzer(new QRCodeFoundListener() {
                    @Override
                    public void onQRCodeFound(String _qrCode) {
                        Log.i(TAG, "onQRCodeFound");
                        qrCode = _qrCode;
                        // TODO: save QR String to database
                        // TODO: Send start command in TerminalFragment
                        startVibrating(100);
                        Log.i(TAG, "QRCodeListener fired. Resume to plant object detection mode. ");
                        exitActivity();
                    }

                    @Override
                    public void qrCodeNotFound() {
                        if (System.currentTimeMillis() - lastTimeNoQRCodeWasFound > MaximumActivityLifetime) {
                            final long MaximumActivityLifetime =
                                    TimeUnit.MILLISECONDS.toSeconds(QrCodeActivity.this.MaximumActivityLifetime);
                            Log.i(TAG, String.format("QRCodeListener has not fired for %d " +
                                    "seconds. Assuming false positive. The driving break is finished!",
                                    MaximumActivityLifetime));
                            // TODO: Send start command in TerminalFragment
                            exitActivity();
                        }
                    }
                }));

        Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector,
                imageAnalysis, preview);

    }

    @Override
    public void startRingtone() {
        if (TOGGLE_RINGTONE && System.currentTimeMillis() - lastTimeRingtonePlayed > waitingTimeRingtone) {

            try {
                ringtone.play();
            } catch (Exception e) {
                Log.e(TAG, "Caught Exception while starting Ringtone");
                e.printStackTrace();
            }
            lastTimeRingtonePlayed = System.currentTimeMillis();
        }
    }

    @Override
    public void startVibrating(final int millis) {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        // safety mechanism to not vibrate too often.
        if (TOGGLE_VIBRATE && System.currentTimeMillis() - lastTimeVibrated > waitingTimeVibrate) {

// Vibrate for N milliseconds
            try {
                v.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE));
            } catch (Exception e) {
                Log.e(TAG, "Caught Exception while starting Vibration");
                e.printStackTrace();
            }
            lastTimeVibrated = System.currentTimeMillis();
        }
    }

    public final void exitActivity() {
        Log.d(TAG, "Exiting QrCodeActivity");
        finish();
    }
}

