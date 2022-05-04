package li.garteroboter.pren.qrcode.fragments

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.io.File

/** I need to share data between fragment [IntermediateFragment] and their host activity
 * This is kind of the observer pattern.  */

class GlobalStateViewModel : ViewModel() {
    private val TAG = "GlobalStateViewModel"
    private val currentImage = MutableLiveData<File>()
    private val mutableDriveState = MutableLiveData<String>()


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
}
