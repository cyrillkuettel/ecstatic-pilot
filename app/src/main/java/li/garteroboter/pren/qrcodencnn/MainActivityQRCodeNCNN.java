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

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import li.garteroboter.pren.R;
import simple.bluetooth.terminal.VibrationListener;

public class MainActivityQRCodeNCNN extends Activity implements SurfaceHolder.Callback, VibrationListener
{
    private static final String TAG = "MainActivityQRCodeNCNN";

    public static final int REQUEST_CAMERA = 100;

    private li.garteroboter.pren.qrcodencnn.NanoDetNcnn nanodetncnn = new li.garteroboter.pren.qrcodencnn.NanoDetNcnn();
    private int facing = 1; // changed to 0
    private SurfaceView cameraView;


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        nanodetncnn.setObjectReferenceAsGlobal(this);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        cameraView = (SurfaceView) findViewById(R.id.cameraview);
        cameraView.getHolder().setFormat(PixelFormat.RGBA_8888);
        cameraView.getHolder().addCallback(this);


        Button buttonSwitchCamera = (Button) findViewById(R.id.buttonSwitchCamera);
        buttonSwitchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                int new_facing = 1 - facing;
                nanodetncnn.closeCamera();
                nanodetncnn.openCamera(new_facing);
                facing = new_facing;
            }
        });

        reload();
    }

    private void reload()
    {
        boolean ret_init = nanodetncnn.loadModel(getAssets());
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

    public static final boolean TOGGLE_VIBRATE = true;
    long lastTime = 0;
    public void nonStaticDurchstich(String helloFromTheOtherSide) {
        if (TOGGLE_VIBRATE && System.currentTimeMillis() - lastTime > 1000) { // safety mechanism to not vibrate too often.
            startVibrating(100);

            lastTime = System.currentTimeMillis();
        }
    }


    @Override
    public void onPause()
    {
        super.onPause();
        nanodetncnn.closeCamera();
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
    public void startVibrating(final int millis) {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
// Vibrate for N milliseconds
        try {
            v.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE));
        } catch (Exception e) {
            Log.d(TAG, "Failed to vibrate");
            e.printStackTrace();
        }
    }
}
