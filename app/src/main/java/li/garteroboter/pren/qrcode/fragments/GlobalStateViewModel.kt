package li.garteroboter.pren.qrcode.fragments

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.io.File

/** This ViewModel is used to track the current state of the Roboter
 *
 * fragment [CameraFragment] updates images and Logs to this fragment
 * fragment [IntermediateFragment] updates changes of which camera implementation to use
 * Activity [NanodetncnnActivity] observes this viewModel
 * fragment [TerminalFragment] only updates this to set the inital start command

 */

class GlobalStateViewModel : ViewModel() {
    private val TAG = "GlobalStateViewModel"
    private val currentImage = MutableLiveData<File>()

    private val mutableDriveState = MutableLiveData<String>()

    private val currentLog = MutableLiveData<String>()

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

    fun getCurrentDriveState() : MutableLiveData<String> {
        return mutableDriveState
    }

    enum class LogType(val state: String) {
        STARTED("STARTED"),
        FINISHED("FINISHED"),
        OBJECT_DETECTION_TRIGGERED("OBJECT_DETECTION_TRIGGERED"),
        PLANT_SPECIES_DETECTED("PLANT_SPECIES_DETECTED"),
        QR_CODE_DETECTED("QR_CODE_DETECTED")
    }
}
