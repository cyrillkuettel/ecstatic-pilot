// Tencent is pleased to support the open source community by making ncnn available.
//
// Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
//
// Licensed under the BSD 3-Clause License (the "License"); you may not use this file except
// in compliance with the License. You may obtain a copy of the License at
//
// https://opensource.org/licenses/BSD-3-Clause
//
// Unless required by applicable law or agreed to in writing, software distributed
// under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
// CONDITIONS OF ANY KIND, either express or implied. See the License for the
// specific language governing permissions and limitations under the License.

package li.garteroboter.pren.qrcodencnn;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import li.garteroboter.pren.R;
import li.garteroboter.pren.qrcodencnn.image.ImageCopyRequest;
import li.garteroboter.pren.qrcodencnn.image.ImageProcessor;
import simple.bluetooth.terminal.DevicesFragment;
import simple.bluetooth.terminal.VibrationListener;

public class MainActivityQRCodeNCNN extends FragmentActivity implements SurfaceHolder.Callback,
        VibrationListener {

    private static final String TAG = "MainActivityQRCodeNCNN";

    public static boolean TOGGLE_VIBRATE = true;
    private final Context mContext = MainActivityQRCodeNCNN.this;
    long lastTime = 0;
    private li.garteroboter.pren.qrcodencnn.NanoDetNcnn nanodetncnn =
            new li.garteroboter.pren.qrcodencnn.NanoDetNcnn();
    private int facing = 1;
    private SurfaceView cameraView;
    private ImageProcessor imageProcessor;

    public static final int REQUEST_READ_WRITE_EXTERNAL_STORAGE = 112;
    public static final int REQUEST_CAMERA = 100;
    public static final String[] EXTERNAL_STORAGE_PERMISSIONS =
            {Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        nanodetncnn.setObjectReferenceAsGlobal(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        cameraView = findViewById(R.id.cameraview);
        cameraView.getHolder().setFormat(PixelFormat.RGBA_8888);
        cameraView.getHolder().addCallback(this);

        imageProcessor = new ImageCopyRequest(cameraView);

        Button buttonSwitchCamera = findViewById(R.id.buttonSwitchCamera);
        buttonSwitchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                int new_facing = 1 - facing;
                nanodetncnn.closeCamera();
                nanodetncnn.openCamera(new_facing);
                facing = new_facing;
            }
        });

        // Initialize a little menu at the edge of the screen, to connect to a Bluetooth Device.
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add(R.id.fragmentBluetoothChain,
                    new DevicesFragment(), "devices").commit();
        }


        if (!hasPermissions(mContext, EXTERNAL_STORAGE_PERMISSIONS)) {
            ActivityCompat.requestPermissions((Activity) mContext, EXTERNAL_STORAGE_PERMISSIONS,
                    REQUEST_READ_WRITE_EXTERNAL_STORAGE);
        } else {
            Log.e(TAG, "READ_EXTERNAL_STORAGE permission already granted");
            imageProcessor.setHasPermissionToSave(true);
        }

        reload();
    }

    private void reload() {
        boolean ret_init = nanodetncnn.loadModel(getAssets());
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

        cameraView.setOnClickListener(v -> {
            imageProcessor.start();
        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_READ_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                imageProcessor.setHasPermissionToSave(true);
            } else {
                imageProcessor.setHasPermissionToSave(false);
                Toast.makeText(mContext, "The app was not allowed to read storage.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    public void nonStaticDurchstich(String helloFromTheOtherSide) {
        startVibrating(100);
    }

    @Override
    public void onPause() {
        super.onPause();
        nanodetncnn.closeCamera();
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

        final int waitingTimeUntilNextVibrate = 1000;
        // safety mechanism to not vibrate too often.
        if (TOGGLE_VIBRATE && System.currentTimeMillis() - lastTime > waitingTimeUntilNextVibrate) {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
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

    private static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }
}
