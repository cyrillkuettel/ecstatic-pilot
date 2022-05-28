package li.garteroboter.pren.qrcode.fragments

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import java.io.File

/** This ViewModel is used to track the current state of the Roboter.
 *
 * fragment [CameraFragment] updates images and Logs to this fragment
 * fragment [IntermediateFragment] updates changes of which camera implementation to use
 * Activity [NanodetncnnActivity] observes this viewModel
 * fragment [TerminalFragment] only updates this to set the inital start command

 */

class GlobalStateViewModel(application: Application) : AndroidViewModel(application)  {
    private val TAG = "GlobalStateViewModel"
    private val currentImage = MutableLiveData<File>()
    private val currentSpecies = MutableLiveData<String>()
    private val mutableDriveState = MutableLiveData<String>()
    private val _triggerNavigateToCameraFragment = MutableLiveData<Boolean>(false)
    private val currentLog = MutableLiveData<LogType>()

    fun set_triggerNavigateToCameraFragment(value: Boolean) {
        Log.d(TAG, "set_triggerNavigateToCameraFragment")
        _triggerNavigateToCameraFragment.value = value
    }

    fun get_triggerNavigateToCameraFragment(): MutableLiveData<Boolean> {
        return _triggerNavigateToCameraFragment
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

    fun getCurrentDriveState() : MutableLiveData<String> {
        return mutableDriveState
    }

    fun setCurrentSpecies(species: String) {
        currentSpecies.value = species
    }

    fun getCurrentSpecies() : MutableLiveData<String> {
        return currentSpecies
    }

    fun setCurrentLog(newLog: LogType ) {
        currentLog.value = newLog
    }

    fun getCurrentLog() : MutableLiveData<LogType> {
        return currentLog
    }


    enum class LogType(val state: String) {
        STARTED("STARTED"),
        FINISHED("FINISHED"),
        OBJECT_DETECTION_TRIGGERED("OBJECT_DETECTION_TRIGGERED"),
        PLANT_SPECIES_DETECTED("PLANT_SPECIES_DETECTED"),
        NO_PLANT_SPECIES_DETECTED(
            "NO_PLANT_SPECIES_DETECTED"),

        QR_CODE_DETECTED("QR_CODE_DETECTED")
    }
}
