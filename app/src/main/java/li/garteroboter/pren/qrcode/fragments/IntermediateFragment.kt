package li.garteroboter.pren.qrcode.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import li.garteroboter.pren.R

class IntermediateFragment: Fragment() {


    private val viewModel: StateViewModel by activityViewModels()

    private val args: IntermediateFragmentArgs by navArgs()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e(TAG, "onCreate")

        if (args.returningFrom == "CameraFragment" ) {
            // observer pattern. change the state of the HostActivity to start the other Camera again
            viewModel.setState(returningFromIntermediate)
        }
    }


    public fun navigateToCamera() {
        Log.d(TAG, "launching to camera")
        lifecycleScope.launchWhenStarted {
            Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
                IntermediateFragmentDirections.actionIntermediateToCamera())
        }
    }

    companion object {
        private val TAG = "IntermediateFragment"
        const val returningFromIntermediate = "CALLBACK_TO_SWITCH_BACK_TO_NANODET"
    }


}