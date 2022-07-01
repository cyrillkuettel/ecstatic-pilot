package simple.bluetooth.terminal

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.method.ScrollingMovementMethod
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import li.garteroboter.pren.Constants
import li.garteroboter.pren.Constants.RECEIVED_CHAR_START_COMMAND_ESP32
import li.garteroboter.pren.GlobalStateViewModel
import li.garteroboter.pren.R
import simple.bluetooth.terminal.SerialService.SerialBinder
import simple.bluetooth.terminal.TextUtil.HexWatcher


class TerminalFragment : Fragment(), ServiceConnection, SerialListener {

    /** ViewModel used for output of information*/
    private val globalStateViewModel: GlobalStateViewModel by activityViewModels()

    /** ViewModel used for input of information*/
    private val terminalStartStopViewModel: TerminalStartStopViewModel  by activityViewModels()

    private enum class Connected {
        False, Pending, True
    }

    private var deviceAddress: String? = null
    private var service: SerialService? = null
    private var receiveText: TextView? = null
    private var sendText: TextView? = null
    private var hexWatcher: HexWatcher? = null
    private var connected = Connected.False
    private var initialStart = true
    private val hexEnabled = false
    private var pendingNewline = false
    private val newline = TextUtil.newline_crlf

    external fun setObjectReferenceAsGlobal(terminalFragment: TerminalFragment?): Boolean


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        deviceAddress = requireArguments().getString("device")
        Log.d(TAG, String.format("Found device address: %s", deviceAddress))

        // Log.i(TAG, "setting Global reference for JNI ")
        // setObjectReferenceAsGlobal(this) /* This allows accessing the instance of TerminalFragment from the C++ Layer */

    }

    override fun onDestroy() {
        super.onDestroy()
        Log.e(TAG, "onDestroy()")
        if (connected != Connected.False) disconnect()
        requireActivity().stopService(Intent(activity, SerialService::class.java))

    }

    override fun onStart() {
        super.onStart()
        if (service != null) service!!.attach(this) else requireActivity().startService(
            Intent(
                activity,
                SerialService::class.java
            )
        )
        // prevents service destroy on unbind from recreated activity caused by orientation change
    }

    override fun onStop() {
        if (service != null && !requireActivity().isChangingConfigurations) service!!.detach()
        super.onStop()
    }

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        requireActivity().bindService(
            Intent(getActivity(), SerialService::class.java),
            this,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onDetach() {
        try {
            requireActivity().unbindService(this)
        } catch (ignored: Exception) {
        }
        super.onDetach()
    }

    override fun onResume() {
        super.onResume()
        if (initialStart && service != null) {
            initialStart = false
            requireActivity().runOnUiThread { connect() }
        }
    }

    override fun onServiceConnected(name: ComponentName, binder: IBinder) {
        service = (binder as SerialBinder).service
        service?.attach(this)
        if (initialStart && isResumed) {
            initialStart = false
            requireActivity().runOnUiThread { connect() }
        }
    }

    override fun onServiceDisconnected(name: ComponentName) {
        service = null
    }

    /*
     * UI
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_terminal, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        receiveText =
            view.findViewById(R.id.receive_text) // TextView performance decreases with number of spans

        receiveText?.setTextColor(resources.getColor(R.color.colorRecieveText)) // set as default color to reduce number of spans
        receiveText?.setMovementMethod(ScrollingMovementMethod.getInstance())
        sendText = view.findViewById(R.id.send_text)
        hexWatcher = HexWatcher(sendText)
        hexWatcher!!.enable(hexEnabled)

        sendText?.addTextChangedListener(hexWatcher)
        sendText?.setHint(if (hexEnabled) "HEX mode" else "")

        observeViewModel(view)

    }

    private fun observeViewModel(view: View) {
        setupButtonOnClickListener(view)
        terminalStartStopViewModel.getNextCommand().observe(viewLifecycleOwner, Observer { command ->
            Log.v(TAG, " startStopViewModel.getNextCommand().observe")
            send(command)
        })
    }

    private fun setupButtonOnClickListener(view: View) {
        val simulateStartSignalButton = view.findViewById<View>(R.id.bluetooth_send_start)
        simulateStartSignalButton.setOnClickListener { v: View? ->
            send(
                Constants.START_COMMAND_ESP32
            )
            // this is used for testing, simulate start
            globalStateViewModel.ROBOTER_DRIVING = true
        }

        val sendBtn = view.findViewById<View>(R.id.send_btn)
        sendBtn.setOnClickListener { v: View? ->
            send(sendText?.text.toString())
        }
    }

    /*
     * Serial + UI
     */
    private fun connect() {
        try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
            status("connecting...")
            connected = Connected.Pending
            val socket = SerialSocket(requireActivity().applicationContext, device)
            service!!.connect(socket)
        } catch (e: Exception) {
            onSerialConnectError(e)
        }
    }

    private fun disconnect() {
        Log.d(TAG, "disconnect()")
        connected = Connected.False
        service!!.disconnect()
    }

    fun send(str: String) {
        if (connected != Connected.True) {
            Log.e(TAG, "Tried to send message but not connected!!")
            return
        }
        try {
            val msg: String
            val data: ByteArray
            if (hexEnabled) {
                val sb = StringBuilder()
                TextUtil.toHexString(sb, TextUtil.fromHexString(str))
                TextUtil.toHexString(sb, newline.toByteArray())
                msg = sb.toString()
                data = TextUtil.fromHexString(msg)
            } else {
                msg = str
                data = (str + newline).toByteArray()
                Log.v(TAG, "sent char $data")
                service!!.write(data)

            }
            val spn = SpannableStringBuilder(
                """
                      $msg
                      
                      """.trimIndent()
            )
            spn.setSpan(
                ForegroundColorSpan(resources.getColor(R.color.colorSendText)),
                0,
                spn.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            receiveText!!.append(spn)
        } catch (e: Exception) {
            Log.d(TAG, "exception while sending Text in method send(String str)")
            onSerialIoError(e)
        }
    }

    private fun receive(data: ByteArray) {
        if (hexEnabled) {
            receiveText!!.append(
                """
                    ${TextUtil.toHexString(data)}
                    
                    """.trimIndent()
            )
        } else {
            var msg = String(data)

            if (newline == TextUtil.newline_crlf && msg.length > 0) {
                // don't show CR as ^M if directly before LF
                msg = msg.replace(TextUtil.newline_crlf, TextUtil.newline_lf)
                // special handling if CR and LF come in separate fragments
                if (pendingNewline && msg[0] == '\n') {
                    val edt = receiveText!!.editableText
                    if (edt != null && edt.length > 1) edt.replace(edt.length - 2, edt.length, "")
                }
                pendingNewline = msg[msg.length - 1] == '\r'
            }
            val content: CharSequence =  TextUtil.toCaretString(msg, newline.isNotEmpty())

            if (content.contains(RECEIVED_CHAR_START_COMMAND_ESP32)) {
                Log.d(TAG, content.toString())
                 globalStateViewModel.setDriveState(RECEIVED_CHAR_START_COMMAND_ESP32)

            }
            receiveText!!.append(content)
        }
    }

    private fun status(str: String) {
        val spn = SpannableStringBuilder(
            """
                  $str
                  
                  """.trimIndent()
        )
        spn.setSpan(
            ForegroundColorSpan(resources.getColor(R.color.colorStatusText)),
            0,
            spn.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        receiveText!!.append(spn)
    }

    /*
     * SerialListener
     */
    override fun onSerialConnect() {
        status("connected")
        connected = Connected.True
    }

    override fun onSerialConnectError(e: Exception) {
        status("connection failed: " + e.message)
        disconnect()
        startRecoverySequence()
    }

    override fun onSerialRead(data: ByteArray) {
        receive(data)
    }

    override fun onSerialIoError(e: Exception) {
        status("connection lost: " + e.message)
        disconnect()
        startRecoverySequence()
    }

    private fun startRecoverySequence() {

        /** If things really go wrong, that is, we receive some type of Serial error,
         *  attempt to reconnect. Reconnect by returning back to DevicesFragment.
         *
         * There, implement a function to automatically re-connect */
        Log.e(TAG, "startRecoverySequence")
        val fragmentManager: FragmentManager = requireActivity().supportFragmentManager
        val result = fragmentManager.popBackStackImmediate("terminal", POP_BACK_STACK_INCLUSIVE)
        if (!result) {
            Log.e(TAG, "There is nothing to pop")
        }
    }

    companion object {
        private const val TAG = "TerminalFragment"
        private const val POP_BACK_STACK_INCLUSIVE = 1

        // init {
          //  System.loadLibrary("nanodetncnn")
        // }
    }
}