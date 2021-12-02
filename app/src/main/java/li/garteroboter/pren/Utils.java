package li.garteroboter.pren;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

public class Utils {

    public static void LogAndToast(Context mContext, String message) {
        // TODO: Make it possible to run this from everywhere. Runnable?
        Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
    }

}

