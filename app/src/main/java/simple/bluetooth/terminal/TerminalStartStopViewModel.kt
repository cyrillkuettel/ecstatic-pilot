package simple.bluetooth.terminal

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TerminalStartStopViewModel : ViewModel() {

    private val TAG = "StartStopViewModel"
    private val currentCommand = MutableLiveData<String>()

    fun setCommand(state: String) {
        Log.d(TAG, "setCommand: $state")
        currentCommand.value = state
    }

    fun getNextCommand() : MutableLiveData<String> {
        return currentCommand
    }
}