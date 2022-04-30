package li.garteroboter.pren.nanodet

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.activity.viewModels
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.NavHostFragment
import androidx.preference.PreferenceManager
import li.garteroboter.pren.Constants.STOP_COMMAND_ESP32
import li.garteroboter.pren.R
import li.garteroboter.pren.databinding.ActivityNanodetncnnBinding
import li.garteroboter.pren.preferences.bundle.CustomSettingsBundle
import li.garteroboter.pren.preferences.bundle.SettingsBundle
import li.garteroboter.pren.qrcode.QrcodeActivity
import li.garteroboter.pren.qrcode.fragments.IntermediateFragment
import li.garteroboter.pren.qrcode.fragments.StateViewModel
import simple.bluetooth.terminal.DevicesFragment
import simple.bluetooth.terminal.TerminalFragment
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger


class NanodetncnnActivity : AppCompatActivity(), SurfaceHolder.Callback, PlaySoundListener {

    private val viewModel: StateViewModel by viewModels()


    private var lastTimePlantCallback: Long = 0
    private val waitingTimePlantCallback = 5000 // to configure the bluetooth calls
    private val atomicCounter = AtomicInteger(0)
    private val nanodetncnn = NanoDetNcnn()
    private var useBluetooth = false
    private var current_model = 0
    private var current_cpugpu = 0
    private var cameraView: SurfaceView? = null
    private var ringtone: Ringtone? = null
    private var terminalFragment: TerminalFragment? = null
    private var transitionToQRActivityEnabled = true

    private var currentSurfaceViewWidth = 0;
    private var currentSurfaceViewHeight = 0;

    private lateinit var binding: ActivityNanodetncnnBinding


    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNanodetncnnBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val settingsBundle = generatePreferenceBundle()
        useBluetooth = settingsBundle.isUsingBluetooth

        val drive: String?
        if (savedInstanceState == null) {
            val extras = intent.extras
            if (extras == null) {
                drive = null
            } else {
                drive = extras.getString("drive")
                if (bluetoothCheck(terminalFragment)) {
                    terminalFragment!!.send(drive)
                } else {
                    Log.e(TAG, "terminalFragment null")
                }
            }


            val devicesFragment = DevicesFragment.newInstance()
            devicesFragment.arguments = setupBluetoothPermission()
            supportFragmentManager.beginTransaction().add(
                R.id.fragmentBluetoothChain,
                devicesFragment, "devices"
            ).commit()


        } else {
            Log.d(TAG, "savedInstanceState != NULL")
            drive = savedInstanceState.getSerializable("drive") as String?
            if (bluetoothCheck(terminalFragment)) {
                terminalFragment!!.send(drive)
            } else {
                Log.e(TAG, "terminalFragment null")
            }
        }

        viewModel.getCurrentState().observe(this, Observer { state ->
            Log.i(TAG, "viewModel.getCurrentState().observe")
            reOpenNanodetCamera()
        })



        // creates a reference to the currently active instance
        // of MainActivityNanodetNCNN in the C++ layer
        nanodetncnn.setObjectReferenceAsGlobal(this)

        nanodetncnn.injectBluetoothSettings(useBluetooth)
        nanodetncnn.injectFPSPreferences(settingsBundle.isShowFPS)
        nanodetncnn.injectProbThresholdSettings(settingsBundle.prob_threshold)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        cameraView = binding.cameraview
        cameraView!!.holder.setFormat(PixelFormat.RGBA_8888)
        cameraView!!.holder.addCallback(this)

        val spinnerModel = binding.spinnerModel
        setupSpinnerOnClick(spinnerModel)

        val spinnerCPUGPU = binding.spinnerCPUGPU
        setupSpinnerCPUGPUOnClick(spinnerCPUGPU)

        binding.mainButtonSwitchCameraSource.setOnClickListener{ closeNanodetCamera()}


        initializePreferences()
        val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        ringtone = RingtoneManager.getRingtone(applicationContext, notification)


        setupAtomicCounterInterval()

        reload()


    }




    override fun onRequestPermissionsResult(
        requestCode: Int,
        @NonNull permissions: Array<String>,
        @NonNull grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS_CODE_BLUETOOTH_CONNECT) {
            val args = Bundle()
            args.putString("autoConnect", "true")
            val devicesFragment = DevicesFragment.newInstance()
            devicesFragment.arguments = args
            supportFragmentManager.beginTransaction().add(
                R.id.fragmentBluetoothChain,
                devicesFragment, "devices"
            ).commit()
        }
    }

    private fun reload() {
        val ret_init = nanodetncnn.loadModel(assets, current_model, current_cpugpu)
        if (!ret_init) {
            Log.e("MainActivity", "nanodetncnn loadModel failed")
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        nanodetncnn.setOutputWindow(holder.surface)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {}
    override fun surfaceDestroyed(holder: SurfaceHolder) {}

    /** This method is called by the native layer.
     * It is in a sense the most important part of this application.
     * Note that this is effectively called by a different thread.
     * It _is_ a different thread. that also means you cannot change the UI from this method directly */
    fun plantVaseDetectedCallback(helloFromTheOtherSide: String?) {
        val _count = atomicCounter.incrementAndGet()
        if (_count != 0) {
            // Log.v(TAG,String.format("current number of confirmations = %d", _count));
        }
        if (_count >= 3) { // count = number of confirmations. The lower, the faster
            atomicCounter.set(0) // reset the counter back
            if (lastTimeWasNSecondsAGo()) {
                lastTimePlantCallback = System.currentTimeMillis()
                Log.d(
                    TAG,
                    String.format("Accept potted plant detection with %d confirmations", _count)
                )
                synchronized(this) {
                    if (bluetoothCheck(terminalFragment)) {
                        terminalFragment!!.send(STOP_COMMAND_ESP32)
                    }
                }
                startRingtone()
                startQRActivityIfEnabled()
            }
        }
    }

    private fun lastTimeWasNSecondsAGo(): Boolean {
        return System.currentTimeMillis() - lastTimePlantCallback > waitingTimePlantCallback
    }

    private fun bluetoothCheck(terminalFragment: TerminalFragment?): Boolean {
        if (useBluetooth && terminalFragment != null) {
            return true
        }
        if (terminalFragment == null) {
            Log.d(TAG, "bluetoothCheck failed, terminalFragment == null ")
        }
        return false
    }

    fun startQRActivityIfEnabled() {
        if (transitionToQRActivityEnabled) {
            val myIntent = Intent(this, QrcodeActivity::class.java)
            myIntent.addFlags(FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(myIntent)
        }
    }

    private fun setupAtomicCounterInterval() {
        // To be absolutely certain we have a plant detected, and not a false positive, I
        // implemented a little extra check.
        // It will watch for a fast burst of plantVaseDetectedCallback.
        // To detect that burst of callbacks in a short period of time, I use an atomicCounter.
        // This atomicCounter tracks the number of plantVaseDetectedCallback in a given interval.
        // After the interval has passed, simply reset the atomicCounter back to zero.
        val resetAtomicCounterEveryNSeconds = Runnable {
            Log.v(TAG, "resetting counter")
            atomicCounter.incrementAndGet()
        }
        val executor = Executors.newScheduledThreadPool(1)
        executor.scheduleAtFixedRate(
            resetAtomicCounterEveryNSeconds,
            0,
            3,
            TimeUnit.SECONDS
        )
    }

    private fun closeNanodetCamera() {
        nanodetncnn.closeCamera()

        shrinkSufaceView()

        val navHostFragment = binding.fragmentContainer.getFragment<NavHostFragment>()
        val fragment: Fragment = navHostFragment.childFragmentManager.fragments[0]
        val intermediateFragment = fragment as IntermediateFragment
        intermediateFragment.navigateToCamera()

    }

    private fun shrinkSufaceView() {
        currentSurfaceViewWidth = cameraView!!.layoutParams.width // save to restore later
        currentSurfaceViewHeight = cameraView!!.layoutParams.height

        cameraView!!.layoutParams = LinearLayout.LayoutParams( currentSurfaceViewWidth, 1) // shrink
    }


    private fun reOpenNanodetCamera() {
        unShrinkSufaceView()
        nanodetncnn.openCamera(1)
    }

    private fun unShrinkSufaceView() {
         cameraView!!.layoutParams.width = currentSurfaceViewWidth
         cameraView!!.layoutParams.height = currentSurfaceViewHeight

         cameraView!!.layoutParams = LinearLayout.LayoutParams( currentSurfaceViewWidth, currentSurfaceViewHeight)
    }




    /** Called from terminal Fragment  */
    fun receiveTerminalFragmentReference(terminalFragment: TerminalFragment?) {
        if (bluetoothCheck(terminalFragment)) {
            this.terminalFragment = terminalFragment
        } else {
            Log.e(
                TAG,
                "receiveTerminalFragmentReference failed to get reference to terminalFragment"
            )
        }
    }


    private fun setupBluetoothPermission(): Bundle {
        val args = Bundle()
        if (useBluetooth) {
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
                != PackageManager.PERMISSION_GRANTED
            ) {
                Log.i(TAG, " != PackageManager.PERMISSION_GRANTED")
                // TODO: is it not possible to use ActivityCompat.requestPermissions like in onResume()
                requestPermissions(
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                    REQUEST_PERMISSIONS_CODE_BLUETOOTH_CONNECT
                )
            } else {
                args.putString("autoConnect", "true")
            }
        } else {
            args.putString("autoConnect", "false")
        }
        return args
    }

    private fun setupSpinnerCPUGPUOnClick(spinnerCPUGPU: Spinner) {
        spinnerCPUGPU.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                arg0: AdapterView<*>?,
                arg1: View,
                position: Int,
                id: Long
            ) {
                if (position != current_cpugpu) {
                    current_cpugpu = position
                    reload()
                }
            }

            override fun onNothingSelected(arg0: AdapterView<*>?) {}
        }
    }

    private fun setupSpinnerOnClick(spinnerModel: Spinner) {
        spinnerModel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                arg0: AdapterView<*>?,
                arg1: View,
                position: Int,
                id: Long
            ) {
                if (position != current_model) {
                    current_model = position
                    reload()
                }
            }

            override fun onNothingSelected(arg0: AdapterView<*>?) {}
        }
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
        nanodetncnn.openCamera(1) // always open front camera
    }


    override fun startRingtone() {
        if (TOGGLE_RINGTONE) {
            try {
                ringtone!!.play()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // creates an object, which is a container for some preferences. This bundle object is then
    // passed to the native layer)
    private fun generatePreferenceBundle(): SettingsBundle {
        val preferences = PreferenceManager.getDefaultSharedPreferences(
            applicationContext
        )
        val useBluetooth = preferences.getBoolean("key_bluetooth", false)
        val drawFps = preferences.getBoolean("key_fps", false)
        val _value = preferences.getString("key_prob_threshold", "0.40")
        val probThreshold = _value!!.toFloat()
        val plantCount = preferences.getInt("number_picker_preference", 4)
        return CustomSettingsBundle(useBluetooth, drawFps, probThreshold)
    }


    private fun initializePreferences() {

        val preferences = PreferenceManager.getDefaultSharedPreferences(
            applicationContext
        )
        // load preferences ot local variable
        transitionToQRActivityEnabled = preferences.getBoolean("key_start_transition", false)
    }

    public override fun onPause() {
        super.onPause()
        nanodetncnn.closeCamera()
    }

    companion object {
        const val REQUEST_CAMERA = 100
        private const val TAG = "MainActivityNanodetNCNN"
        var TOGGLE_RINGTONE = true
        private const val REQUEST_PERMISSIONS_CODE_BLUETOOTH_CONNECT = 11


        /** Use external media if it is available, our app's file directory otherwise */
        fun getOutputDirectory(context: Context): File {
            val appContext = context.applicationContext
            val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
                File(it, appContext.resources.getString(R.string.app_name)).apply { mkdirs() } }
            return if (mediaDir != null && mediaDir.exists())
                mediaDir else appContext.filesDir
        }
    }
}