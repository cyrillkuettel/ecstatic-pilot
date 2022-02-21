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
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import android.os.Environment;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.PixelCopy;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import android.app.Activity;

import li.garteroboter.pren.R;
import simple.bluetooth.terminal.DevicesFragment;
import simple.bluetooth.terminal.VibrationListener;

public class MainActivityQRCodeNCNN extends FragmentActivity implements SurfaceHolder.Callback, VibrationListener
{
    private static final String TAG = "MainActivityQRCodeNCNN";
    private Context mContext=MainActivityQRCodeNCNN.this;

    public static final int REQUEST_CAMERA = 100;

    private li.garteroboter.pren.qrcodencnn.NanoDetNcnn nanodetncnn = new li.garteroboter.pren.qrcodencnn.NanoDetNcnn();
    private int facing = 1; // changed to 0
    private SurfaceView cameraView;

    private Bitmap photo;


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

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add(R.id.fragmentBluetoothChain, new DevicesFragment(), "devices").commit();

        }

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

         // Surface surface = holder.getSurface();
        cameraView.setOnClickListener(v -> {

            Canvas canvas = cameraView.getHolder().lockCanvas(); // maybe canvas clip rect?, and check if already locked
            Log.v(TAG, "onclick!");

            Surface surface = cameraView.getHolder().getSurface();

            if (canvas != null) {
               // photo = getBitmapFromView(cameraView); // this doe not work, returns black screen
                photo = pixelCopy(cameraView);
                if (photo == null) {
                    Log.d(TAG, "photo is null");
                } else {
                    Log.d(TAG, "photo is not null");

                    String[] PERMISSIONS = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                    if (!hasPermissions(mContext, PERMISSIONS)) {
                        ActivityCompat.requestPermissions((Activity) mContext, PERMISSIONS, REQUEST_READ_WRITE_EXTERNAL_STORAGE);
                    } else {
                        Log.e(TAG, "READ_EXTERNAL_STORAGE permission already granted");
                        savebitmap(photo);
                    }

                }

                //Log.d(TAG, String.valueOf(bytes_length));
                cameraView.getHolder().unlockCanvasAndPost(canvas);
            }
        });

    }

    public static Bitmap pixelCopy(View view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        int[] location = new int[2];
        view.getLocationInWindow(location);

        Rect rect = new Rect(location[0], location[1], location[0] + view.getWidth(), location[1] + view.getHeight());
        PixelCopy.OnPixelCopyFinishedListener listener = new PixelCopy.OnPixelCopyFinishedListener() {

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

            PixelCopy.request(activity.getWindow(), rect, bitmap, listener, new Handler());
        } catch (IllegalArgumentException e) {

            e.printStackTrace();
        }
    }


    public static Bitmap getBitmapFromView(SurfaceView view) {
        //Define a bitmap with the same size as the view
        Bitmap returnedBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(),Bitmap.Config.RGB_565);
        Log.d(TAG, String.format("view.getWidth() = %d", view.getWidth()));
        Log.d(TAG, String.format("view.getHeight() = %d", view.getHeight()));

        //Bind a canvas to it
        Canvas canvas = new Canvas();
        canvas.setBitmap(returnedBitmap);

        return returnedBitmap;
    }


    private static final int REQUEST_READ_WRITE_EXTERNAL_STORAGE = 112;

    public void savebitmap(Bitmap bmp)  {

        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 60, bytes);
            File f = new File(Environment.getExternalStorageDirectory()
                    + File.separator + "testimage.jpg");
            f.createNewFile();
            FileOutputStream fo = new FileOutputStream(f);
            fo.write(bytes.toByteArray());
            fo.close();
        } catch (IOException e) {
            Log.d(TAG, "failed to save bitmap");
            e.printStackTrace();
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_READ_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //do here
                savebitmap(photo);
            } else {
                Toast.makeText(mContext, "The app was not allowed to read your store.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
    }


    public void nonStaticDurchstich(String helloFromTheOtherSide) {
            startVibrating(100);
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


    public static boolean TOGGLE_VIBRATE = true;
    long lastTime = 0;
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
}
