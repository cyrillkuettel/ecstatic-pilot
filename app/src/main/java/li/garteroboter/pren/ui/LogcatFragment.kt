package li.garteroboter.pren.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import li.garteroboter.pren.R
import li.garteroboter.pren.log.LogcatDataReader
import java.io.IOException
import java.util.stream.Collectors

/**
 * A fragment representing a list of Items.
 */
class LogcatFragment : Fragment() {

    private var columnCount = 1
    private var content: List<LogcatLine> = mutableListOf(LogcatLine("testing"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        readLogcat()
    }

    private fun readLogcat() {
        val logcatDataReader = LogcatDataReader()
        try {
            // logcatDataReader.flush()
            val logs = logcatDataReader.read(
                400
            )
            Log.v(TAG, "logs.size == $logs.size")
            content = logs.stream().map { LogcatLine(it) }.collect(Collectors.toList())
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_logcat_list, container, false)

        // Set the adapter
        if (view is RecyclerView) {
            with(view) {
                layoutManager = when {
                    columnCount <= 1 -> LinearLayoutManager(context)
                    else -> GridLayoutManager(context, columnCount)
                }
                adapter = MyItemRecyclerViewAdapter(content)
            }
        }
        return view
    }

    companion object {

        // TODO: Customize parameter argument names
        const val TAG = "LogcatFragment"

        // TODO: Customize parameter initialization
        @JvmStatic
        fun newLogcatFragmentInstance() =
            LogcatFragment().apply {
            }
    }
}