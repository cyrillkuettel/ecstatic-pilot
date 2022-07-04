package li.garteroboter.pren.nanodet

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.PixelFormat
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import kotlinx.coroutines.launch
import li.garteroboter.pren.Constants.*
import li.garteroboter.pren.GlobalStateViewModel
import li.garteroboter.pren.databinding.ActivityNanodetncnnBinding
import li.garteroboter.pren.network.GlobalStateListener
import li.garteroboter.pren.network.SocketType
import li.garteroboter.pren.network.WebSocketManager
import li.garteroboter.pren.preferences.bundle.CustomSettingsBundle
import li.garteroboter.pren.qrcode.fragments.IntermediateFragment.Companion.RETURNING_FROM_INTERMEDIATE
import org.apache.commons.io.FileUtils
import simple.bluetooth.terminal.DevicesFragment
import simple.bluetooth.terminal.TerminalStartStopViewModel
import java.io.File
import java.lang.System.exit
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.schedule


class NanodetncnnActivity : AppCompatActivity(), SurfaceHolder.Callback, PlaySoundListener, GlobalStateListener {



    private val globalStateViewModel: GlobalStateViewModel by viewModels()
    private val terminalStartStopViewModel: TerminalStartStopViewModel  by viewModels()

    private lateinit var binding: ActivityNanodetncnnBinding
    private var lastTimePlantCallback: Long = 0

    private val atomicCounter = AtomicInteger(0)
    private val nanodetncnn = NanoDetNcnn()

    private var currentModel = 0
    private var currentCPUGPU = 0
    private var cameraView: SurfaceView? = null

    private var timerForDelay: TimerTask? = null

    private val ringtone by lazy {
        RingtoneManager.getRingtone(applicationContext, RingtoneManager.getActualDefaultRingtoneUri(
            applicationContext, RingtoneManager.TYPE_NOTIFICATION))
    }

    private var currentSurfaceViewWidth = 0
    private var currentSurfaceViewHeight = 0

    /** Initialization by lazy { ... } is thread-safe by default */
    private val websocketManagerByte: WebSocketManager by lazy {
        WebSocketManager(this@NanodetncnnActivity, HOSTNAME, SocketType.Binary).apply {
            lifecycleScope.launch {
                createAndOpenWebSocketConnection()
            }
        }
    }

    private val websocketManagerText: WebSocketManager by lazy {
        WebSocketManager(this@NanodetncnnActivity, HOSTNAME, SocketType.Text).apply {
            lifecycleScope.launch {
                createAndOpenWebSocketConnection()
            }
        }
    }

    private val websocketManagerCommand: WebSocketManager by lazy {
        WebSocketManager(this@NanodetncnnActivity, HOSTNAME, SocketType.Command).apply {
            lifecycleScope.launch {
                createAndOpenWebSocketConnection()
            }
        }
    }


    // preferences
    private var useBluetooth = false
    private var numerOfConfirmations = 3 // accept Plant detection result after N confirmations
    private val waitingTimePlantCallback = 5000 // to configure the bluetooth calls
    private var plantCount = -1
    private var switchQr = true // determine if QR-Code detection is enabled
    private var prob_threshhold = -1f

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNanodetncnnBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val settingsBundle = generatePreferenceBundle()

        setupSettings(settingsBundle)

        injectPreferences(settingsBundle)

        setupFragmentBluetoothChain()

        observeViewModels()

        setupUI()

        setupAtomicCounterInterval()


        // seems to be needed to initialize the lazy object
        websocketManagerCommand.sendText("initialize")
        reload()
    }

    private fun setupUI() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        cameraView = binding.cameraview
        cameraView!!.holder.setFormat(PixelFormat.RGBA_8888)
        cameraView!!.holder.addCallback(this)
        setupSpinnerOnClick(binding.spinnerModel)
        setupSpinnerCPUGPUOnClick(binding.spinnerCPUGPU)
        binding.mainButtonQrCode.setOnClickListener { navigateToCameraFragment() }
        binding.mainButtonExit.setOnClickListener { exit() }
    }


    private fun setupSettings(settingsBundle: CustomSettingsBundle) {
        useBluetooth = settingsBundle.isUsingBluetooth
        if (!useBluetooth) {
            Log.e(TAG, "not using bluetooth")
            globalStateViewModel.ROBOTER_DRIVING = true // if the start command is not received
            // via bluetooth, just start as soon as this Activity starts.
        }
        numerOfConfirmations = settingsBundle.confirmations
        plantCount = settingsBundle.plantCount
        switchQr = settingsBundle.isSwitchToQr
        prob_threshhold = settingsBundle.prob_threshold
    }


    /** Restart [NanodetncnnActivity], assuming MainActivity creates [NanodetncnnActivity] in onCreate]
     * Effect: Flushing ViewModel, depending on the scope of the ViewModel. */
    private fun exit() {
        //finish()
         /**
          * Terminates the currently running java virtual machine.
          *  finish() is a Possibility, although a considerable and dramatic form is : exit() .*/
        exit(0)

    }


    private fun observeViewModels() {


        globalStateViewModel.getCurrentImage().observe(this, Observer { image ->
            Log.i(TAG, "viewModel.getCurrentImage().observe")
            uploadPlantFromFile(image)
        })
        globalStateViewModel.getCurrentSpecies().observe(this, Observer { speciesName ->
            Log.i(TAG, "viewModel.getCurrentSpecies().observe")
            binding.textViewCurrentSpecies.text = speciesName
            // websocketManagerText.sendText(speciesName)
        })
        globalStateViewModel.getCurrentLog().observe(this, Observer { log ->
            Log.e(TAG,"globalStateViewModel.getCurrentLog().observe" )
            if (log == GlobalStateViewModel.LogType.QR_CODE_DETECTED) {
                Log.d(TAG, "globalStateViewModel.getCurrentLog().observe triggered LogType.QR_CODE_DETECTED")
                binding.debuggingMessages.text = log.state
            }
            websocketManagerText.sendText(log.state)
        })



        globalStateViewModel.getMutableDriveState().observe(this, Observer { currentGlobalScope ->
            Log.i(TAG, "globalStateViewModel.getCurrentDriveState().observe(this")

        /**  Initialization First start signal: <Drive> */
        if (currentGlobalScope.equals(RECEIVED_CHAR_START_COMMAND_ESP32)) {
            globalStateViewModel.ROBOTER_DRIVING = true
            terminalStartStopViewModel.setCommand(START_COMMAND_ESP32)
            Log.v(TAG, "state == START_COMMAND_ESP32")
            websocketManagerText.sendText("received start command")
            websocketManagerText.startTimer()
        } else if (currentGlobalScope.equals(RETURNING_FROM_INTERMEDIATE)) {
            /** Here we are returning from CameraFragment. Either we have successfully read the
             * QR-Code, or not,
             * in any case, resume driving. */
            terminalStartStopViewModel.setCommand(START_COMMAND_ESP32)
            globalStateViewModel.setCurrentLog(GlobalStateViewModel.LogType.RESUME)

            /** We have to wait a bit before detecting again, to prevent detecting the same plant twice */
            Log.e(TAG, "Setting timer")
            globalStateViewModel.ROBOTER_DRIVING = false // wait a bit
            timerForDelay = Timer("delay_object_detection", false)
                .schedule(OBJECT_DETECTION_DELAY_MILLIS) {
                    runOnUiThread {
                        globalStateViewModel.ROBOTER_DRIVING = true
                        reOpenNanodetCamera()
                    }
            }
            /** Finish Line */
        } else if (currentGlobalScope.equals(STOP_FINISH_LINE)) {
            terminalStartStopViewModel.setCommand(STOP_COMMAND_ESP32)
            websocketManagerText.stopTimer()
            globalStateViewModel.setCurrentLog(GlobalStateViewModel.LogType.STOP)
            Thread.sleep(500)
            exit()
        }
    })
    }


    /**
     * Converts an image File Path to bytes, afterwards
     * fetching binary data through the currently running instance of [WebSocketManager]
     */
    private fun uploadPlantFromFile(file: File?) {
        Log.d(TAG,"uploadPlantFromFile" )
        file?.let { it ->
            try {
                val bytes = FileUtils.readFileToByteArray(it)
                websocketManagerByte.sendBytes(bytes)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    /** Monkeypatching the native Layer.
     * Override hyperparamter in object detection in the C++ Layer.

    ╔═══════════╤═════╗                      ╔═════════════════════╤═════╗
    ║ C++ Layer │ NDK ║<---------------------║ Android / JVM Layer │ SDK ║
    ╚═══════════╧═════╝                      ╚═════════════════════╧═════╝
*/
    private fun injectPreferences(settingsBundle: CustomSettingsBundle) {
        nanodetncnn.setObjectReferenceAsGlobal(this)
        nanodetncnn.injectBluetoothSettings(settingsBundle.isUsingBluetooth)
        nanodetncnn.injectFPSPreferences(settingsBundle.isShowFPS)
        nanodetncnn.injectProbThresholdSettings(settingsBundle.prob_threshold)
    }

    private fun setupFragmentBluetoothChain() {
        lifecycleScope.launch {
            val devicesFragment = DevicesFragment.newInstance()
            val autoConnectBluetooth = setupBluetoothPermission()
            devicesFragment.arguments = autoConnectBluetooth

            supportFragmentManager.beginTransaction().add(
                li.garteroboter.pren.R.id.fragmentBluetoothChain,
                devicesFragment, DEVICES_FRAGMENT_TAG
            ).commit()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS_CODE_BLUETOOTH_CONNECT) {
            val args = Bundle()
            args.putString("autoConnect", "true")
            val devicesFragment = DevicesFragment.newInstance()
            devicesFragment.arguments = args
            supportFragmentManager.beginTransaction().add(
                li.garteroboter.pren.R.id.fragmentBluetoothChain,
                devicesFragment, "devices"
            ).commit()
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

                ActivityCompat.requestPermissions(
                    this,
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

    private fun reload() {
        val retryInit = nanodetncnn.loadModel(assets, currentModel, currentCPUGPU)
        if (!retryInit) {
            Log.e("MainActivity", "nanodetncnn loadModel failed")
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        nanodetncnn.setOutputWindow(holder.surface)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {}
    override fun surfaceDestroyed(holder: SurfaceHolder) {}

    /**
           ╔═══════════╤═════╗      Object detected     ╔═════════════════════╤═════╗
           ║ C++ Layer │ NDK ║------------------------->║ Android / JVM Layer │ SDK ║
           ╚═══════════╧═════╝                          ╚═════════════════════╧═════╝
     * */

    fun plantVaseDetectedCallback(objectLabel: String?, probability: String?) {
        val count = atomicCounter.incrementAndGet()
        if (!globalStateViewModel.ROBOTER_DRIVING) {
            Log.e(TAG,"globalStateViewModel.ROBOTER_STARTED == false" )
            return
        }
        if (count != 0) {
            // Log.v(TAG,String.format("current number of confirmations = %d", _count))
        }
        if (count >= numerOfConfirmations) { // count = number of confirmations. The lower, the faster
            atomicCounter.set(0) // reset the counter back
            if (lastTimeWasNSecondsAGo()) {
                runOnUiThread(Runnable {
                    terminalStartStopViewModel.setCommand(STOP_COMMAND_ESP32)  // stop driving
                    if (switchQr) {
                        navigateToCameraFragment()
                    }
                    updateDescription(
                        "detected %s, latest probability == %s".format(
                            objectLabel,
                            probability
                        )
                    )
                })
                lastTimePlantCallback = System.currentTimeMillis()
                Log.d(TAG, "Accept potted plant detection with $count confirmations")
                startRingtone()
            }
        }
    }

    private fun lastTimeWasNSecondsAGo(): Boolean {
        return System.currentTimeMillis() - lastTimePlantCallback > waitingTimePlantCallback
    }


    private fun updateDescription(text: String) {
        binding.textViewDetectedObjectLabel.text = text
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
            resetAtomicCounterEveryNSeconds, 0, 3, TimeUnit.SECONDS
        )
    }

    private fun navigateToCameraFragment() {
        nanodetncnn.closeCamera()
        shrinkSurfaceView()
        globalStateViewModel.set_triggerNavigateToCameraFragment(true)
    }

    /** This is a little trick I use so that the CameraFragment gets displayed. */
    private fun shrinkSurfaceView() {
        currentSurfaceViewWidth = cameraView!!.layoutParams.width // save to restore later
        currentSurfaceViewHeight = cameraView!!.layoutParams.height

        cameraView!!.layoutParams =
            LinearLayout.LayoutParams(currentSurfaceViewWidth, 1) // shrink
    }



    private fun reOpenNanodetCamera() {
        Log.i(TAG, "reOpenNanodetCamera")
        unShrinkSufaceView()
        nanodetncnn.openCamera(CAMERA_ORIENTATION)
    }

    private fun unShrinkSufaceView() {
        cameraView!!.layoutParams.width = currentSurfaceViewWidth
        cameraView!!.layoutParams.height = currentSurfaceViewHeight

        cameraView!!.layoutParams =
            LinearLayout.LayoutParams(currentSurfaceViewWidth, currentSurfaceViewHeight)
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
        nanodetncnn.openCamera(CAMERA_ORIENTATION)
    }

    override fun startRingtone() {
        if (TOGGLE_RINGTONE) {
            try {
                ringtone.play()
                Log.e(TAG, "played the ringtone.")

            } catch (e: Exception) {
                Log.e(TAG, "Playing the ringtone failed.")
                e.printStackTrace()
            }
        }
    }

    private fun setupSpinnerCPUGPUOnClick(spinnerCPUGPU: Spinner) {
        spinnerCPUGPU.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                arg0: AdapterView<*>?,
                arg1: View,
                position: Int,
                id: Long
            ) {
                if (position != currentCPUGPU) {
                    currentCPUGPU = position
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
                if (position != currentModel) {
                    currentModel = position
                    reload()
                }
            }

            override fun onNothingSelected(arg0: AdapterView<*>?) {}
        }
    }

    // creates an object, which is a container for some preferences.
    private fun generatePreferenceBundle(): CustomSettingsBundle {
        val preferences = PreferenceManager.getDefaultSharedPreferences(
            applicationContext
        )
        val useBluetooth = preferences.getBoolean("key_bluetooth", false)
        val drawFps = preferences.getBoolean("key_fps", true)
        val _value = preferences.getString("key_prob_threshold", "0.40")
        val probThreshold = _value!!.toFloat()
        val plantCount = preferences.getInt("number_picker_preference", 6)
        val switchToQr = preferences.getBoolean("key_start_transition", true)
        val _confirmations = preferences.getString("confirmation_number_picker", "3")
        val confirmations = _confirmations!!.toInt()
        return CustomSettingsBundle(
            useBluetooth,
            drawFps,
            probThreshold,
            switchToQr,
            confirmations,
            plantCount
        )
    }

    public override fun onPause() {
        super.onPause()
        nanodetncnn.closeCamera()
    }



    companion object {
        const val REQUEST_CAMERA = 100
        const val CAMERA_ORIENTATION = 1
        const val OBJECT_DETECTION_DELAY_MILLIS: Long = 2000
        const val DEVICES_FRAGMENT_TAG = "devices"
        private const val TAG = "NanodetncnnActivity"
        const val HOSTNAME = "wss://pren.garteroboter.li:443/ws/"
        var TOGGLE_RINGTONE = true
        private const val REQUEST_PERMISSIONS_CODE_BLUETOOTH_CONNECT = 11

        /** Use external media if it is available */
        fun getOutputDirectory(context: Context): File {
            val appContext = context.applicationContext
            val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
                File(it, appContext.resources.getString(li.garteroboter.pren.R.string.app_name)).apply { mkdirs() }
            }
            return if (mediaDir != null && mediaDir.exists())
                mediaDir else appContext.filesDir
        }
    }

    override fun triggerStop() {
        Log.d(TAG, "triggerStop")
        runOnUiThread {  // this is necessary because we're calling this method from a different thread
            globalStateViewModel.stop()
        }
    }

}