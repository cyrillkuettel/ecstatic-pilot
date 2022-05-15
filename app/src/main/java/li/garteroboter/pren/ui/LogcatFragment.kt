package li.garteroboter.pren.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import li.garteroboter.pren.R
import java.util.stream.Collectors

/**
 * A fragment representing a list of Items.
 */
class LogcatFragment : Fragment() {

    private var columnCount = 1
    private var content: List<LogcatLine> = mutableListOf(LogcatLine("testing"))

    private var logs: ArrayList<String> = ArrayList()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { argument ->
            logs = argument.getStringArrayList(LOGCATDUMP) as ArrayList<String>
            content = logs.stream().map {

                // val char = it[32]
                // TODO: LogcatType
                // Log.v(TAG, "CHAR = $char")

                LogcatLine(it)
            }.collect(Collectors.toList())
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

    enum class LogcatType {
        // TODO: map Color to Log
        // INFO()
    }

    companion object {

        const val TAG = "LogcatFragment"
        const val LOGCATDUMP = "LogcatDump"

        @JvmStatic
        fun newLogcatFragmentInstance(list: ArrayList<String>) =
            LogcatFragment().apply {
                arguments = Bundle().apply {
                    putStringArrayList(LOGCATDUMP, list)
                }
            }
    }
}