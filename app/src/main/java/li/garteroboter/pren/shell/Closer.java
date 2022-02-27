package li.garteroboter.pren.shell;

import android.util.Log;

import java.io.Closeable;
import java.net.DatagramSocket;
import java.net.Socket;

// https://stackoverflow.com/questions/20932102/execute-shell-command-from-android
// The utility to silently close a number of Closeables
public class Closer {
    private static final String TAG = "Close";
    // closeAll()
    public static void closeSilently(Object... xs) {
        // Note: on Android API levels prior to 19 Socket does not implement Closeable
        for (Object x : xs) {
            if (x != null) {
                try {
                    Log.d(TAG, "closing: "+x);
                    if (x instanceof Closeable) {
                        ((Closeable)x).close();
                    } else if (x instanceof Socket) {
                        ((Socket)x).close();
                    } else if (x instanceof DatagramSocket) {
                        ((DatagramSocket)x).close();
                    } else {
                        Log.d(TAG, "cannot close: "+x);
                        throw new RuntimeException("cannot close "+x);
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
