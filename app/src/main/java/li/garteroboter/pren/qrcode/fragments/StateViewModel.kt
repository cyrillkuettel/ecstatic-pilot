package li.garteroboter.pren.qrcode.fragments

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/** I need to share data between fragment [IntermediateFragment] and their host activity
 * This is kind of the observer pattern.  */

class StateViewModel : ViewModel() {

    private val mutableState = MutableLiveData<String>()


    fun setState(state: String) {
        mutableState.value = state
    }

    fun getCurrentState() : MutableLiveData<String> {
        return mutableState
    }
}
