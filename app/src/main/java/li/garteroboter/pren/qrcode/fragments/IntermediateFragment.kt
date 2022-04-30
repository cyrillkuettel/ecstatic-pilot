package li.garteroboter.pren.qrcode.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import li.garteroboter.pren.R

class IntermediateFragment: Fragment() {
    private val TAG = "IntermediateFragment"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e(TAG, "onCreate")
    }


    public fun navigateToCamera() {
        Log.d(TAG, "launching to camera")
        lifecycleScope.launchWhenStarted {
            Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
                IntermediateFragmentDirections.actionIntermediateToCamera())
        }
    }


}