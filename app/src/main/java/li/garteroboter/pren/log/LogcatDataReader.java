package li.garteroboter.pren.log;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class LogcatDataReader implements LogcatData {

    private  static final String TAG = "LogcatData";

    private long readNumberOfLines = 100;

    private final List<String> log = new ArrayList<>();

    @Override
    public List<String> read() throws IOException {

        Process process = null;
        try {
            Runtime.getRuntime().exec("logcat -c"); // flush the logcat first
            // read the most recent X lines from logcat
            String command =  "logcat -t" + String.valueOf(readNumberOfLines);
            process = Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            Log.e(TAG, "Runtime.getRuntime().exec failed. Printing Stacktrace");
            e.printStackTrace();
        }
        final BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));



        String line = "";
        Log.d(TAG,  "Reading filtered logcat");
        while ((line = bufferedReader.readLine()) != null) {
            log.add(line);
        }
        return log;
    }


    public void setReadNumberOfLines(long readNumberOfLines) {
        this.readNumberOfLines = readNumberOfLines;
    }
}
