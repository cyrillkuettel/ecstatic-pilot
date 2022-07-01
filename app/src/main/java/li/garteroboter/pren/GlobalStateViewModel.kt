package li.garteroboter.pren

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import li.garteroboter.pren.Constants.STOP_FINISH_LINE
import li.garteroboter.pren.qrcode.identification.RetroFitWrapper
import java.io.File
import kotlin.concurrent.thread

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

    fun setCurrentSpecies(species: String) {
        currentSpecies.value = species
    }

    fun getCurrentSpecies() : MutableLiveData<String> {
        return currentSpecies
    }

    fun setCurrentLog(newLog: LogType) {
        Log.d(TAG, "setCurrentLog: $newLog")
        currentLog.value = newLog
    }

    fun getCurrentLog() : MutableLiveData<LogType> {
        return currentLog
    }



    fun startAPICall(file: File) {
        thread(start = true) {
            var speciesName = "failed"
            try {
                val savedUri = file.toString()
                speciesName = startApiCallForSpecies(savedUri)
                setCurrentSpecies(speciesName)

            } catch (e: InterruptedException) {
                Log.d(TAG, "caught Interrupted exception!")
            } finally {
                Log.v(TAG, "updating database with scientific name $speciesName")
            }
        }
    }

    fun startApiCallForSpecies(savedUri: String): String {
        Log.i(TAG, "startApiCall")
        val retroFitWrapper = RetroFitWrapper(getAPIKey())
        val name = retroFitWrapper.requestLocalPlantIdentificationSynchronously(savedUri)
        return name;
    }

    private fun getAPIKey(): String {
        val applicationInfo: ApplicationInfo = context.packageManager
            .getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            )
        val key = applicationInfo.metaData["plantapi"]
        return key.toString()
    }


    enum class LogType(val state: String) {
        STARTED("Roboter started"),
        FINISHED("Roboter halts."),
        OBJECT_DETECTION_TRIGGERED("Object detection: Potted Plant"),
        PLANT_SPECIES_DETECTED("Species detected."),

        QR_CODE_DETECTED("QR_CODE_DETECTED")
    }
    companion object {
        const val TAG = "GlobalStateViewModel"
    }
}
