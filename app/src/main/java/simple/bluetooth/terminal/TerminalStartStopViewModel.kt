package simple.bluetooth.terminal

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TerminalStartStopViewModel : ViewModel() {

    private val TAG = "StartStopViewModel"
    private val currentCommand = MutableLiveData<String>()

    fun setCommand(state: String) {
        currentCommand.value = state
    }

    fun getNextCommand() : MutableLiveData<String> {
        return currentCommand
    }
}