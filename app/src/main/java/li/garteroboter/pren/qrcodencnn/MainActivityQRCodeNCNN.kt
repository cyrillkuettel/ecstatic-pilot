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

package li.garteroboter.pren.qrcodencnn

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import li.garteroboter.pren.R
import li.garteroboter.pren.qrcodencnn.image.ImageProcessor
import simple.bluetooth.terminal.VibrationListener



class MainActivityQRCodeNCNN : FragmentActivity(), SurfaceHolder.Callback,
    VibrationListener {
    private val mContext: Context = this@MainActivityQRCodeNCNN
    var lastTime: Long = 0
    private val nanodetncnn = NanoDetNcnn()
    private var facing = 1
    private var cameraView: SurfaceView? = null
    private var imageProcessor: ImageProcessor? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        nanodetncnn.setObjectReferenceAsGlobal(this)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        cameraView = findViewById(R.id.cameraview)
        cameraView!!.holder.setFormat(PixelFormat.RGBA_8888)
        cameraView!!.holder.addCallback(this)
        val buttonSwitchCamera = findViewById<Button>(R.id.buttonSwitchCamera)
        buttonSwitchCamera.setOnClickListener {
            val new_facing = 1 - facing
            nanodetncnn.closeCamera()
            nanodetncnn.openCamera(new_facing)
            facing = new_facing
        }

        if (!hasPermissions(mContext, *EXTERNAL_STORAGE_PERMISSIONS)) {
            ActivityCompat.requestPermissions(
                (mContext as Activity), EXTERNAL_STORAGE_PERMISSIONS,
                REQUEST_READ_WRITE_EXTERNAL_STORAGE
            )
        } else {
            Log.e(TAG, "READ_EXTERNAL_STORAGE permission already granted")
        }
        reload()
    }

    private fun reload() {
        val ret_init = nanodetncnn.loadModel(assets)
        if (!ret_init) {
            Log.e("MainActivity", "nanodetncnn loadModel failed")
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        nanodetncnn.setOutputWindow(holder.surface)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        cameraView!!.setOnClickListener { v: View? -> imageProcessor!!.start() }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_READ_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                imageProcessor!!.setHasPermissionToSave(true)
            } else {
                imageProcessor!!.setHasPermissionToSave(false)
                Toast.makeText(
                    mContext, "The app was not allowed to read storage.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        if (requestCode == REQUEST_PERMISSIONS_CODE_BLUETOOTH_CONNECT) {

        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {}
    fun nonStaticDurchstich(helloFromTheOtherSide: String?) {
        startVibrating(100)
    }

    public override fun onPause() {
        super.onPause()
        nanodetncnn.closeCamera()
    }

    public override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_DENIED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA
            )
        }
        nanodetncnn.openCamera(facing)
    }

    override fun startVibrating(millis: Int) {
        val waitingTimeUntilNextVibrate = 1000
        // safety mechanism to not vibrate too often.
        if (TOGGLE_VIBRATE && System.currentTimeMillis() - lastTime > waitingTimeUntilNextVibrate) {
            val v = getSystemService(VIBRATOR_SERVICE) as Vibrator
            // Vibrate for N milliseconds
            try {
                v.vibrate(
                    VibrationEffect.createOneShot(
                        millis.toLong(),
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } catch (e: Exception) {
                Log.d(TAG, "Failed to initialize SystemService Vibration")
                e.printStackTrace()
            }
            lastTime = System.currentTimeMillis()
        }
    }

    companion object {
        private const val TAG = "MainActivityQRCodeNCNN"
        private const val REQUEST_PERMISSIONS_CODE_BLUETOOTH_CONNECT = 11
        var TOGGLE_VIBRATE = true
        const val REQUEST_READ_WRITE_EXTERNAL_STORAGE = 112
        const val REQUEST_CAMERA = 100
        val EXTERNAL_STORAGE_PERMISSIONS = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        private fun hasPermissions(context: Context?, vararg permissions: String): Boolean {
            if (context != null && permissions != null) {
                for (permission in permissions) {
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            permission
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return false
                    }
                }
            }
            return true
        }
    }
}