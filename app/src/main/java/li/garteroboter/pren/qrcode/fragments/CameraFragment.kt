/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package li.garteroboter.pren.qrcode.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.core.ImageCapture.Metadata
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.Navigation
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowMetricsCalculator
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import li.garteroboter.pren.GlobalStateViewModel
import li.garteroboter.pren.GlobalStateViewModel.LogType
import li.garteroboter.pren.R
import li.garteroboter.pren.databinding.CameraUiContainerBinding
import li.garteroboter.pren.databinding.FragmentCameraBinding
import li.garteroboter.pren.nanodet.NanodetncnnActivity
import li.garteroboter.pren.qrcode.database.PlantRoomDatabase.Companion.getDatabase
import li.garteroboter.pren.qrcode.qrcode.QRCodeImageAnalyzer
import li.garteroboter.pren.qrcode.utils.ANIMATION_FAST_MILLIS
import li.garteroboter.pren.qrcode.utils.ANIMATION_SLOW_MILLIS
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.schedule
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import li.garteroboter.pren.qrcode.qrcode.QRCodeFoundListener as QRCodeFoundListener1


/**
 * Main fragment for QR-Code detection and programmatically taking photo, and species detection.
 *
 * - ImageAnalysis: [QRCodeImageAnalyzer] can check QR-Codes. For this, imageAnalysis.Analyzer
 * function is implemented here, where it can access image data for application analysis via an ImageProxy.
 * - Photo taking by manually controlling the camera.
 *  -> Photo added to the [GlobalStateViewModel] which results in uploading it to out webserver.
 *
 *  Additionally, Species detection has been implemented using the Pl@nt net API with Retrofit
 */
class CameraFragment : Fragment() {

    private val globalStateViewModel: GlobalStateViewModel by activityViewModels()

    private var _fragmentCameraBinding: FragmentCameraBinding? = null
    private val fragmentCameraBinding get() = _fragmentCameraBinding!!

    /** Timer for fragment lifetime.  */
    private var timerForFragmentTermination: TimerTask? = null


    private val imageTaken: AtomicBoolean = AtomicBoolean(false)
    private val navigateBackHasBeenCalled: AtomicBoolean = AtomicBoolean(false)
    private val qrDetected: AtomicBoolean = AtomicBoolean(false)

    @Volatile
    private var qrString: String = ""


    private lateinit var outputDirectory: File
    private lateinit var broadcastManager: LocalBroadcastManager
    private var displayId: Int = -1
    private var lensFacingBack: Int = CameraSelector.LENS_FACING_BACK
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraUiContainerBinding: CameraUiContainerBinding? = null


    @Synchronized
    fun setQRString(qrCode: String?) {
        if (qrCode != null) {
            qrString = qrCode
        }
    }

    @Synchronized
    fun qrCodeAlreadySet(): Boolean {
        return qrString != ""
    }

    private lateinit var windowInfoTracker: WindowInfoTracker


    private val displayManager by lazy {
        requireContext().getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }


    /** Blocking camera operations are performed using this executor */
    private lateinit var cameraExecutor: ExecutorService // similar to java.util.concurrent.executor


    /**
     * We need a display listener for orientation changes that do not trigger a configuration
     * change, for example if we choose to override config change in manifest or for 180-degree
     * orientation changes.
     */
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) = view?.let { view ->
            if (displayId == this@CameraFragment.displayId) {
                Log.d(TAG, "Rotation changed: ${view.display.rotation}")
                imageCapture?.targetRotation = view.display.rotation
                imageAnalyzer?.targetRotation = view.display.rotation
            }
        } ?: Unit
    }

    override fun onResume() {
        super.onResume()
        // Make sure that all permissions are still present, since the
        // user could have removed them while the app was in paused state.
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
                CameraFragmentDirections.actionCameraToPermissions()
            )
        }
        globalStateViewModel.setCurrentLog(LogType.OBJECT_DETECTION_TRIGGERED)
        timerForFragmentTermination = Timer("adieu", false).schedule(QR_CODE_WAITING_TIME) {
            navigateBack_OnUIThread()
        }
        takePhotoDelayed(1000)
    }

    fun takePhotoDelayed(timeoutMillis: Long) {
        thread(start = true) {
            Thread.sleep(timeoutMillis)
            // Camera needs to be initialized. The sleep call is necessary,
            // otherwise it does not work. Better would be callback method as soon as CameraDevice
            // initialization is finished.
            // the delay is very important, it is needed to adjust parameters like brightness internally
            takePhotoOnce(::setCurrentImage)
        }
    }

    fun placeHolder(input: File) {
        Log.i(TAG, "placeHolder")
    }

    fun onImageSavedPrepareUpload(input: File) {
        Log.i(TAG, "onImageSaved")
        globalStateViewModel.setCurrentImage(input)
    }

    override fun onDestroyView() {
        _fragmentCameraBinding = null
        super.onDestroyView()

        // Shut down our background executor
        cameraExecutor.shutdown()

        // Unregister the broadcast receivers and listeners
        displayManager.unregisterDisplayListener(displayListener)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _fragmentCameraBinding = FragmentCameraBinding.inflate(inflater, container, false)
        return fragmentCameraBinding.root
    }

    private fun setGalleryThumbnail(uri: Uri) {
        // Run the operations in the view's thread
        cameraUiContainerBinding?.photoViewButton?.let { photoViewButton ->
            photoViewButton.post {
                // Remove thumbnail padding
                photoViewButton.setPadding(resources.getDimension(R.dimen.stroke_small).toInt())

                // Load thumbnail into circular button using Glide
                Glide.with(photoViewButton)
                    .load(uri)
                    .apply(RequestOptions.circleCropTransform())
                    .into(photoViewButton)
            }
        }
    }

    /** This is a function used for debugging purposes.
     * Delete the database for every new test. (Called once per Fragment Lifetime) */
    private fun clearAllTablesThread(): Thread {
        Log.i(TAG, "clearAllTablesThread")
        return object : Thread("clearAllTables") {
            override fun run() {
                try {
                    val db = context?.let { it -> getDatabase(it) }
                    db?.clearAllTables()
                } catch (e: InterruptedException) {
                    Log.d(TAG, "caught Interrupted exception!")
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        // Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        broadcastManager = LocalBroadcastManager.getInstance(view.context)


        // Every time the orientation of device changes, update rotation for use cases
        displayManager.registerDisplayListener(displayListener, null)

        //Initialize WindowManager to retrieve display metrics
        // windowManager = WindowManager(view.context)
        windowInfoTracker = WindowInfoTracker.getOrCreate(requireActivity())


        // Determine the output directory
        outputDirectory = NanodetncnnActivity.getOutputDirectory(requireContext())

        // Wait for the views to be properly laid out
        fragmentCameraBinding.previewView.post {

            // Keep track of the display in which this view is attached
            displayId = fragmentCameraBinding.previewView.display.displayId

            // Build UI controls
            updateCameraUi()

            // Set up the camera and its use cases
            setUpCamera()
        }

    }

    /**
     * Inflate camera controls and update the UI manually upon config changes to avoid removing
     * and re-adding the view finder from the view hierarchy; this provides a seamless rotation
     * transition on devices that support it.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Rebind the camera with the updated display metrics
        bindCameraUseCases()
    }

    /** Initialize CameraX, and prepare to bind the camera use cases  */
    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(Runnable {

            cameraProvider = cameraProviderFuture.get()

            // For our use-case, it's always LENS_FACING_BACK
            lensFacingBack = when {
                hasBackCamera() -> CameraSelector.LENS_FACING_BACK
                hasFrontCamera() -> CameraSelector.LENS_FACING_FRONT
                else -> throw IllegalStateException("Back and front camera are unavailable")
            }

            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    /** Declare and bind preview, capture and analysis use cases */
    private fun bindCameraUseCases() {

        // Get screen metrics used to setup camera for full screen resolution
        val wmc = WindowMetricsCalculator.getOrCreate()
        val metricsRect: Rect = wmc.computeMaximumWindowMetrics(requireActivity()).bounds
        val maxWidth = metricsRect.width()
        val maxHeight = metricsRect.height()


        val screenAspectRatio = aspectRatio(maxWidth, maxHeight)
        Log.d(TAG, "Preview aspect ratio: $screenAspectRatio")

        val rotation = fragmentCameraBinding.previewView.display.rotation

        // CameraProvider
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        // CameraSelector
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacingBack).build()

        // Preview
        preview = Preview.Builder()
            // We request aspect ratio but no resolution
            .setTargetAspectRatio(screenAspectRatio)
            // Set initial target rotation
            .setTargetRotation(rotation)
            .build()

        // ImageCapture
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            // We request aspect ratio but no resolution to match preview config, but letting
            // CameraX optimize for whatever specific resolution best fits our use cases
            .setTargetAspectRatio(screenAspectRatio)
            // Set initial target rotation, we will have to call this again if rotation changes
            // during the lifecycle of this use case
            .setTargetRotation(rotation)

            .build()

        imageAnalyzer = ImageAnalysis.Builder()
            // .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            //.setTargetResolution(Size(700, 700)) // 1280, 720)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // TODO: experiment with this
            .build()
            // The analyzer can then be assigned to the instance
            .also {
                it.setAnalyzer(
                    cameraExecutor,
                    QRCodeImageAnalyzer(object : QRCodeFoundListener1 {
                        override fun onQRCodeFound(qrCode: String?) {
                            Log.e(TAG, "onQRCodeFound")
                            cameraUiContainerBinding?.textViewDebugging!!.text = "QR-Code detected"
                            actionOnQrCode(qrCode)
                        }

                        /**
                         * This block is synchronized because it's a simple solution.
                         * It's not entirely necessary, but then you would have to set some AtomicBoolean
                         * variables to prevent this Callback from calling methods twice which should
                         * not be called twice.
                         * This introduces whole new level of complexity which we're trying to avoid.
                         */
                       // @Synchronized
                        private fun actionOnQrCode(qrCode: String?) {
                            /** QR-Code has been detected.
                             *
                             * If the last plant position plant has _not_ yet been reached, we can
                             * now navigateBack. How to know it's the last position? Here we get
                             * into the real of pure speculation.
                             *
                             *
                             * */
                            if (qrDetected.get()) return
                            if (qrDetected.compareAndSet(false, true)) {
                                Log.e(TAG, "onQRCodeFound")
                                /** Extend the CameraFragment LifeTime for a few seconds in order
                                 * to avoid exiting the Fragment too early while we're still in the
                                 * process of taking an image. (This would result in a crash) */
                                timerForFragmentTermination = Timer("adieu", false)
                                    .schedule(QR_CODE_WAITING_TIME) {
                                        navigateBack_OnUIThread()
                                    }
                            /*
                                requireActivity().runOnUiThread {
                                    globalStateViewModel.setCurrentLog(LogType.QR_CODE_DETECTED)
                             }
                             */

                                if (qrCode?.uppercase()?.contains("STOP") == true) {
                                    globalStateViewModel.stop()

                                }
                                if (qrCodeAlreadySet()) {
                                    Log.e(TAG, "qrCodeAlreadySet")

                                    Log.e(TAG, "starting timerForFragmentTermination")
                                    navigateBack_OnUIThread()
                                } else {
                                    Log.d(TAG, "This is a new QR-Code.")
                                    setQRString(qrCode)
                                    takePhotoOnce(::setCurrentImage)
                                }
                            }
                        }
                        override fun qrCodeNotFound() {
                            // nope
                        }
                    })
                )
            }

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageCapture, imageAnalyzer
            )

            // widest zoom angle possible
            try {
                val zoomState = camera!!.cameraInfo.zoomState.value
                val zoom = zoomState?.minZoomRatio
                if (zoom != null) {
                    camera!!.cameraControl.setZoomRatio(zoom)
                }

            }catch (exc: Exception) {
                Log.e(TAG, "setZoomRatio failed", exc)

            }

            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(fragmentCameraBinding.previewView.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }





    /**
     *  [androidx.camera.core.ImageAnalysis.Builder] requires enum value of
     *  [androidx.camera.core.AspectRatio]. Currently it has values of 4:3 & 16:9.
     *
     *  Detecting the most suitable ratio for dimensions provided in @params by counting absolute
     *  of preview ratio to one of the provided values.
     *
     *  @param width - preview width
     *  @param height - preview height
     *  @return suitable aspect ratio
     */
    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    /** Method used to re-draw the camera UI controls, called every time configuration changes. */
    private fun updateCameraUi() {

        // Remove previous UI if any
        cameraUiContainerBinding?.root?.let {
            fragmentCameraBinding.root.removeView(it)
        }

        cameraUiContainerBinding = CameraUiContainerBinding.inflate(
            LayoutInflater.from(requireContext()),
            fragmentCameraBinding.root,
            true
        )

        // In the background, load latest photo taken (if any) for gallery thumbnail
        /*
        lifecycleScope.launch(Dispatchers.IO) {
            outputDirectory.listFiles { file ->
                EXTENSION_WHITELIST.contains(file.extension.uppercase(Locale.ROOT))
            }?.maxOrNull()?.let {
                setGalleryThumbnail(Uri.fromFile(it))
            }
        }

         */

        cameraUiContainerBinding?.cameraCaptureButton?.setOnClickListener {
            takePhotoOnce(::setCurrentImage)
        }

        // Listener for button used to view the most recent photo
        cameraUiContainerBinding?.photoViewButton?.setOnClickListener {
            // Only navigate when the gallery has photos
            if (true == outputDirectory.listFiles()?.isNotEmpty()) {
                Navigation.findNavController(
                    requireActivity(), R.id.fragment_container
                ).navigate(
                    CameraFragmentDirections
                        .actionCameraToGallery(outputDirectory.absolutePath)
                )
            }
        }
    }

    /** Different threads can call this method. Since  calling this method more than
     * once (per lifetime) is prone to cause crashes, we set a AtomicBoolean Flag to ensure it's
     * called once and only once. */
    @Synchronized
    private fun navigateBack_OnUIThread() {
        if (navigateBackHasBeenCalled.get()) return
        if (navigateBackHasBeenCalled.compareAndSet(false, true)) {
            navigateBack()
        }
    }

    private fun navigateBack() {
        Log.e(TAG,"navigateBack()")
        requireActivity().runOnUiThread {
            try {
                Log.d(
                    TAG, "navigateBack() has been called. " +
                            "Therefore, callsToNavigateBack.compareAndSet(false, true) is called"
                )
                Navigation.findNavController(requireActivity(), R.id.fragment_container)
                    .navigate(
                        CameraFragmentDirections.actionCameraToIntermediate("CameraFragment")
                    )
            } catch (e: Exception) {
                Log.v(TAG, e.toString())
            }
        }
    }


    private fun takePhotoOnce(myActionOnImageSaved: (savedImage: File) -> Unit) {
        if (navigateBackHasBeenCalled.get()) return // to prevent error of "Photo capture failed: Camera is closed"
        if (imageTaken.get()) return // only call once
        if (imageTaken.compareAndSet(false, true)) {
            Log.i(TAG, "takePhotoOnce")

            // Get a stable reference of the modifiable image capture use case
            imageCapture?.let { imageCapture ->
                Log.d(TAG, "starting capture process")
                // Create output file to hold the image
                val photoFile = createFile(outputDirectory, FILENAME, PHOTO_EXTENSION)

                // Setup image capture metadata
                val metadata = Metadata().apply {

                    // Mirror image when using the front camera
                    isReversedHorizontal = lensFacingBack == CameraSelector.LENS_FACING_FRONT
                }

                // Create output options object which contains file + metadata
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile)
                    .setMetadata(metadata)
                    .build()

                // Setup image capture listener which is triggered after photo has been taken
                imageCapture.takePicture(
                    outputOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
                        override fun onError(exc: ImageCaptureException) {
                            Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                        }

                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            val savedUri = output.savedUri ?: Uri.fromFile(photoFile)

                            Log.d(TAG, "Photo capture succeeded: $savedUri")


                            myActionOnImageSaved(photoFile)

                        }
                    })

                displayFlashAnimation()
            }
        }
    }


    fun setCurrentImage(file: File) {
        Log.i(TAG, "globalStateViewModel.setCurrentImage(file)")
        activity?.runOnUiThread {
            globalStateViewModel.setCurrentImage(file)
        }
    }





    /** Returns true if the device has an available back camera. False otherwise */
    private fun hasBackCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ?: false
    }

    /** Returns true if the device has an available front camera. False otherwise */
    private fun hasFrontCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ?: false
    }


    private fun displayFlashAnimation(color: Int = Color.WHITE) {
        // We can only change the foreground Drawable using API level 23+ API
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            // Display flash animation to indicate that photo was captured
            fragmentCameraBinding.root.postDelayed({
                fragmentCameraBinding.root.foreground = ColorDrawable(color)
                fragmentCameraBinding.root.postDelayed(
                    { fragmentCameraBinding.root.foreground = null }, ANIMATION_FAST_MILLIS
                )
            }, ANIMATION_SLOW_MILLIS)
        }
    }


    companion object {
        /** Maximum activity Lifetime */
        private const val QR_CODE_WAITING_TIME: Long = 5000 // maximum allowed fragment Lifetime

        private const val TAG = "CameraFragment"
        private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val PHOTO_EXTENSION = ".jpg"
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0

        /** Helper function used to create a timestamped file */
        private fun createFile(baseFolder: File, format: String, extension: String) =
            File(
                baseFolder, SimpleDateFormat(format, Locale.US)
                    .format(System.currentTimeMillis()) + extension
            )
    }
}
