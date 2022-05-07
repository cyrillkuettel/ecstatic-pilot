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

    private val globalStateViewModel: GlobalStateViewModel by activityViewModels()
    private val args: IntermediateFragmentArgs by navArgs()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e(TAG, "onCreate")

        // almost always we return from CameraFragment, except when permissions have to be handled.
        if (args.returningFrom == "CameraFragment" ) {
            // observer pattern. We use this String argument to
                // change the state of the HostActivity to start the other Camera again
            Log.i(TAG, "Returning from CameraFragment")
            globalStateViewModel.setDriveState(RETURNING_FROM_INTERMEDIATE)
        } else {
            Log.i(TAG, "args.returningFrom is not returning from CameraFragment")

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
        const val RETURNING_FROM_INTERMEDIATE = "RETURNING_FROM_INTERMEDIATE"
        private val TAG = "IntermediateFragment"
    }


}