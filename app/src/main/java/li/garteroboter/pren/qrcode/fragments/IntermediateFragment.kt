package li.garteroboter.pren.qrcode.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import li.garteroboter.pren.GlobalStateViewModel
import li.garteroboter.pren.R
import li.garteroboter.pren.databinding.FragmentIntermediateBinding

class IntermediateFragment: Fragment() {
    /** This fragment intentionally does not have a view. */
    private val globalStateViewModel: GlobalStateViewModel by activityViewModels()
    private val args: IntermediateFragmentArgs by navArgs()

    private var _binding: FragmentIntermediateBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e(TAG, "onCreate IntermediateFragment")

        // almost always we return from CameraFragment, except when permissions have to be handled.
        if (args.returningFrom == "CameraFragment" ) {
            // observer pattern. We use this String argument to
            // change the state of the HostActivity to restart the NdkCamera
            Log.i(TAG, "Returning from CameraFragment")
            globalStateViewModel.setDriveState(RETURNING_FROM_INTERMEDIATE)
        } else {
            Log.i(TAG, "args.returningFrom is not returning from CameraFragment")

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentIntermediateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        globalStateViewModel.get_triggerNavigateToCameraFragment().observe(viewLifecycleOwner) { value ->
            Log.d(TAG, "get_triggerNavigateToCameraFragment().observe")
            if (value) {
                globalStateViewModel.set_triggerNavigateToCameraFragment(false) // set to false again to prevent loops
                navigateToCameraFragment()
            }
        }

    }

    fun navigateToCameraFragment() {
        Log.d(TAG, "launching to camera")

         lifecycleScope.launchWhenStarted {
            Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
                IntermediateFragmentDirections.actionIntermediateToCamera())
         }
    }

    override fun onResume() {
        super.onResume()
        Log.e(TAG, "onResume")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val RETURNING_FROM_INTERMEDIATE = "RETURNING_FROM_INTERMEDIATE"
        private val TAG = "IntermediateFragment"
    }


}