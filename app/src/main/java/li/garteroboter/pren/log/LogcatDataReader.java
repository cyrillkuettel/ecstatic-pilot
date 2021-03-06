package li.garteroboter.pren.log;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class LogcatDataReader implements LogcatData {

    private static final String TAG = "LogcatData";


    @Override
    public  List<String> read(long readNumberOfLines) throws IOException {
         final List<String> log = new ArrayList<>();

        Process process = null;
        try {
            // Runtime.getRuntime().exec("logcat -c"); // flush the logcat first
            // read the most recent X lines from logcat
            String command =  "logcat -t" + readNumberOfLines;
            process = Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            Log.e(TAG, "Runtime.getRuntime().exec failed. Printing Stacktrace");
            e.printStackTrace();
        }

        final BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));

        String line;
        Log.d(TAG,  "Reading filtered logcat");
        while ((line = bufferedReader.readLine()) != null) {
            log.add(line);
        }
        return log;
    }

    public void flush() {
        try {
            Runtime.getRuntime().exec("logcat -c"); // flush logcat
        } catch (IOException e) {
            Log.e(TAG, "Attempted logcat -c to flush. This failed. Printing Stacktrace");
            e.printStackTrace();
        }
    }
}
