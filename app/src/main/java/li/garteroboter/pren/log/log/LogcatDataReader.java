package li.garteroboter.pren.log.log;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import li.garteroboter.pren.log.LogcatData;

public class LogcatDataReader implements LogcatData {
    private  static final String TAG = "LogcatData";

    @Override
    public List<String> read() throws IOException {

        Process process = null; // filters logcat to only "ActivityManager:I" logs
        try {
            // Runtime.getRuntime().exec("logcat -c"); // flush the logcat first
            process = Runtime.getRuntime().exec("logcat -t 100"); // read the most recent 100 lines from logcat
        } catch (IOException e) {
            Log.e(TAG, "Runtime.getRuntime().exec failed. Printing Stacktrace");
            e.printStackTrace();
        }
        final BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));

        final List<String> log = new ArrayList<>();

        String line = "";
        Log.d(TAG,  "Reading filtered logcat");
        while ((line = bufferedReader.readLine()) != null) {
            log.add(line);
        }
        return log;
    }
}
