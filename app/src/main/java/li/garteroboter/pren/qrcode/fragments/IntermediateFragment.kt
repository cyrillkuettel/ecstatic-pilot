package li.garteroboter.pren.qrcode.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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
            Log.i(TAG, "Returning from CameraFragment")
            viewModel.setState(returningFromIntermediate)
        } else {
            Log.i(TAG, "args is not returningfrom CameraFragment")

        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.e(TAG, "onViewCreated")

    }

    override fun onDestroy() {
        super.onDestroy()
        Log.e(TAG, "onDestroy")

    }


    public fun navigateToCamera() {
        Log.d(TAG, "launching to camera")
        // lifecycleScope.launchWhenStarted {
            Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
                IntermediateFragmentDirections.actionIntermediateToCamera())
        // }
    }

    companion object {
        private val TAG = "IntermediateFragment"
        const val returningFromIntermediate = "CALLBACK_TO_SWITCH_BACK_TO_NANODET"
    }


}