package li.garteroboter.pren

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import li.garteroboter.pren.Constants.STOP_FINISH_LINE
import java.io.File

/** This ViewModel is used to track the current state of the Roboter.
 *
 * fragment [CameraFragment] updates images and Logs to this fragment
 * fragment [IntermediateFragment] updates changes of which camera implementation to use
 * Activity [NanodetncnnActivity] observes this viewModel
 * fragment [TerminalFragment] only updates this to set the inital start command

 */

class GlobalStateViewModel(application: Application) : AndroidViewModel(application) {

    /** Object detection result is dismissed as long as this flag this flag is set to true. */
    var ROBOTER_DRIVING = false

    private val context = getApplication<Application>().applicationContext

    private val currentImage = MutableLiveData<File>()
    private val currentSpecies = MutableLiveData<String>()
    private val mutableDriveState = MutableLiveData<String>()
    private val triggerNavigateToCameraFragment = MutableLiveData<Boolean>(false)
    private val currentLog = MutableLiveData<LogType>()

    private val currentDeubggingLog = MutableLiveData<String>()

    /** Stops the roboter, finish line. */
    fun stop() {
        Log.e(TAG ,"stop()")
        ROBOTER_DRIVING = false
        mutableDriveState.value = STOP_FINISH_LINE
    }


    fun set_triggerNavigateToCameraFragment(value: Boolean) {
        Log.d(TAG, "set_triggerNavigateToCameraFragment")
        triggerNavigateToCameraFragment.value = value
    }

    fun get_triggerNavigateToCameraFragment(): MutableLiveData<Boolean> {
        return triggerNavigateToCameraFragment
    }

    fun setCurrentImage(image: File) {
        Log.i(TAG, "setCurrentImage: postvalue")
        currentImage.postValue(image)
    }

    fun getCurrentImage() : MutableLiveData<File> {
        return currentImage
    }

    fun setDriveState(state: String) {
        mutableDriveState.value = state
    }

    fun getMutableDriveState() : MutableLiveData<String> {
        return mutableDriveState
    }


    fun setCurrentLog(newLog: LogType) {
        Log.d(TAG, "setCurrentLog: $newLog")
        currentLog.value = newLog
    }

    fun getCurrentLog() : MutableLiveData<LogType> {
        return currentLog
    }






    enum class LogType(val state: String) {
        STARTED("Roboter gestartet"),
        STOP("Roboter stopp. Ende Gelände."),
        RESUME("Weiterfahren."),
        OBJECT_DETECTION_TRIGGERED("Object detection: Potted Plant"),
        PLANT_SPECIES_DETECTED("Spezies erkannt."),
        QR_CODE_DETECTED("QR-Code erkannt"),
        ZOOM("Camera Zoom initialisiert ")
    }
    companion object {
        const val TAG = "GlobalStateViewModel"
    }
}
