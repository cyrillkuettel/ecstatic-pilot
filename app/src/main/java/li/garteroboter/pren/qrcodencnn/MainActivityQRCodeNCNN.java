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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.PixelCopy;
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
import simple.bluetooth.terminal.DevicesFragment;
import simple.bluetooth.terminal.VibrationListener;

public class MainActivityQRCodeNCNN extends FragmentActivity implements SurfaceHolder.Callback,
        VibrationListener {
    public static final int REQUEST_CAMERA = 100;
    private static final String TAG = "MainActivityQRCodeNCNN";
    private static final int REQUEST_READ_WRITE_EXTERNAL_STORAGE = 112;
    public static boolean TOGGLE_VIBRATE = true;
    private final Context mContext = MainActivityQRCodeNCNN.this;
    long lastTime = 0;
    private li.garteroboter.pren.qrcodencnn.NanoDetNcnn nanodetncnn =
            new li.garteroboter.pren.qrcodencnn.NanoDetNcnn();
    private int facing = 1;
    private SurfaceView cameraView;
    private PixelCopyCallback pixelCopyCallback;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        nanodetncnn.setObjectReferenceAsGlobal(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        cameraView = findViewById(R.id.cameraview);
        cameraView.getHolder().setFormat(PixelFormat.RGBA_8888);
        cameraView.getHolder().addCallback(this);

        // This class implements the callback after we have copied the bitmap.
        pixelCopyCallback = new PixelCopyCallback();

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

        String[] PERMISSIONS = {Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (!hasPermissions(mContext, PERMISSIONS)) {
            ActivityCompat.requestPermissions((Activity) mContext, PERMISSIONS,
                    REQUEST_READ_WRITE_EXTERNAL_STORAGE);
        } else {
            Log.e(TAG, "READ_EXTERNAL_STORAGE permission already granted");
            pixelCopyCallback.hasPermission = "YES";
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

            Canvas canvas = null; // maybe canvas clip rect?,
            int count = 0;
            while (canvas == null || count > 100) {
                try {
                    canvas = cameraView.getHolder().lockCanvas(); // maybe try hardware lock
                    Log.v(TAG, "onclick!");
                    if (canvas != null) {
                        Log.d(TAG, String.format("Got Lock on Canvas after %d tries", count));
                        copyBitmapAndAttachListener(cameraView, pixelCopyCallback);
                        cameraView.getHolder().unlockCanvasAndPost(canvas);
                    } else {
                        Log.v(TAG, "canvas == null");
                    }
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "it is fucking locked");
                    e.printStackTrace();
                }

                count++;
            }

        });
    }

    public void copyBitmapAndAttachListener(SurfaceView view, PostTake callback) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(),
                Bitmap.Config.ARGB_8888);

        PixelCopy.OnPixelCopyFinishedListener listener =
                new PixelCopy.OnPixelCopyFinishedListener() {
            @Override
            public void onPixelCopyFinished(int copyResult) {
                if (copyResult == PixelCopy.SUCCESS) {
                    callback.onSuccess(bitmap);
                } else {
                    callback.onFailure(copyResult);
                }
            }
        };
        try {
            Log.d(TAG, "Trying PixelCopy.request");
            PixelCopy.request(view, bitmap, listener, new Handler());
        } catch (Exception e) {
            Log.e(TAG, "failed: PixelCopy.request(view, bitmap, listener, new Handler());");
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_READ_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pixelCopyCallback.hasPermission = "YES";

            } else {
                pixelCopyCallback.hasPermission = "NO";
                Toast.makeText(mContext, "The app was not allowed to read your store.",
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

    public interface PostTake {

        void onSuccess(Bitmap bitmap);

        void onFailure(int error);
    }
}
