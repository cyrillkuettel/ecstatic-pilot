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
import androidx.fragment.app.activityViewModels
import li.garteroboter.pren.Constants
import li.garteroboter.pren.R
import li.garteroboter.pren.nanodet.NanodetncnnActivity
import li.garteroboter.pren.qrcode.fragments.GlobalStateViewModel
import simple.bluetooth.terminal.SerialService.SerialBinder
import simple.bluetooth.terminal.TextUtil.HexWatcher

class TerminalFragment : Fragment(), ServiceConnection,
    SerialListener {


    private val globalStateViewModel: GlobalStateViewModel by activityViewModels()


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

    /*
     * Lifecycle
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        deviceAddress = requireArguments().getString("device")
        Log.d(TAG, String.format("Found device address: %s", deviceAddress))
        //deviceAddress = "58:BF:25:81:CC:C8";
        Log.i(TAG, "setting Global reference for JNI ")
        setObjectReferenceAsGlobal(this) /*This allows accessing the instance of TerminalFragment
                                            from the C++ Layer */
    }

    override fun onDestroy() {
        if (connected != Connected.False) disconnect()
        requireActivity().stopService(Intent(activity, SerialService::class.java))
        super.onDestroy()
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
        try {
            (activity as NanodetncnnActivity?)!!.receiveTerminalFragmentReference(this)
        } catch (e: Exception) {
            Log.e(TAG, "Attempted to call public method on MainActivityNanodetNCNN, failed")
            e.printStackTrace()
        }

        receiveText =
            view.findViewById(R.id.receive_text) // TextView performance decreases with number of spans

        receiveText?.setTextColor(resources.getColor(R.color.colorRecieveText)) // set as default color to reduce number of spans
        receiveText?.setMovementMethod(ScrollingMovementMethod.getInstance())
        sendText = view.findViewById(R.id.send_text)
        hexWatcher = HexWatcher(sendText)
        hexWatcher!!.enable(hexEnabled)

        sendText?.addTextChangedListener(hexWatcher)
        sendText?.setHint(if (hexEnabled) "HEX mode" else "")


        setupButtonOnClickListener(view)
    }

    private fun setupButtonOnClickListener(view: View) {
        // Hardcoded Start/Stop Signal Buttons. Sends "start" / "stop to ESP32
        val sendStartSignalButton = view.findViewById<View>(R.id.bluetooth_send_start)
        sendStartSignalButton.setOnClickListener { v: View? ->
            send(
                Constants.START_COMMAND_ESP32
            )
        }
        val sendStopSignalButton = view.findViewById<View>(R.id.bluetooth_send_stop)
        sendStopSignalButton.setOnClickListener { v: View? ->
            send(
                Constants.STOP_COMMAND_ESP32
            )
        }

        val sendBtn = view.findViewById<View>(R.id.send_btn)
        sendBtn.setOnClickListener { v: View? ->
            send(sendText?.getText().toString())
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
        connected = Connected.False
        service!!.disconnect()
    }

    fun send(str: String) {
        if (connected != Connected.True) {
            Log.wtf(TAG, "Tried to send message but not connected!!")
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
            service!!.write(data)
        } catch (e: Exception) {
            Log.d(TAG, "exception while sending Text in method send(String str)")
            onSerialIoError(e)
        }
    }

    // here: Add Listener to start when the start command comes.
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
            receiveText!!.append(TextUtil.toCaretString(msg, newline.length != 0))
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
    }

    override fun onSerialRead(data: ByteArray) {
        receive(data)
    }

    override fun onSerialIoError(e: Exception) {
        status("connection lost: " + e.message)
        disconnect()
    }

    companion object {
        private const val TAG = "TerminalFragment"

        init {
            System.loadLibrary("nanodetncnn")
        }
    }
}