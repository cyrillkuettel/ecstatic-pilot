package li.garteroboter.pren;

import static com.google.common.truth.Truth.assertThat;
import androidx.test.filters.SmallTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.List;

import li.garteroboter.pren.log.LogcatDataReader;
import li.garteroboter.pren.log.LogcatData;

@RunWith(AndroidJUnit4ClassRunner.class)
@SmallTest
public class ReadLogcatTest {
    private static final String TAG = "ReadLogcatTest";

    @Test
    public void testLoggingNotEmpty() throws IOException {
        LogcatData logcatdata = new LogcatDataReader();
        List<String> logs = logcatdata.read();
        System.out.println(logs);
        assertThat(logs).isNotEmpty();

    }

}
