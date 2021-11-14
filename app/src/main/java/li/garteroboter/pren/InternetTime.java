package li.garteroboter.pren;

import android.util.Log;

import java.io.IOException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Callable;

import org.apache.commons.net.time.TimeTCPClient;


public final class InternetTime implements Callable<String> {
    private static final String TAG = InternetTime.class.getSimpleName();

    public final String getTime() {
        return "";
    }

    @Override
    public String call() throws Exception {
        // uses the Network Time Protocol ( NTP )

        try {
            TimeTCPClient client = new TimeTCPClient();
            try {
                // Set timeout of 60 seconds
                client.setDefaultTimeout(60000);
                // Connecting to time server
                // Other time servers can be found at : http://tf.nist.gov/tf-cgi/servers.cgi#
                // Make sure that your program NEVER queries a server more frequently than once every 4 seconds
                client.connect("time.nist.gov");
                Date date = client.getDate();
                Format formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String s = formatter.format(date);
                return s;

            } finally {
                client.disconnect();
            }
        } catch (IOException e) {

           e.printStackTrace();
           return "failed";
        }

       // return "there was some intervention somewhere";
    }
}