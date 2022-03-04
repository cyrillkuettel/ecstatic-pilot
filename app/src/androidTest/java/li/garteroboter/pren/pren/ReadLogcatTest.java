package li.garteroboter.pren.pren;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.google.common.truth.Truth.assertThat;

import android.util.Log;
import android.view.View;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.filters.SmallTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.List;

import li.garteroboter.pren.MainActivity;
import li.garteroboter.pren.R;
import li.garteroboter.pren.log.LogcatData;
import li.garteroboter.pren.log.LogcatDataReader;

@RunWith(AndroidJUnit4ClassRunner.class)
@SmallTest
public class ReadLogcatTest {
    private static final String TAG = "ReadLogcatTest";

    /**
     * Use {@link ActivityScenarioRule} to create and launch the activity under test, and close it
     * after test completes.
     */
    @Rule
    public ActivityScenarioRule<MainActivity> activityScenarioRule
            = new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void testLoggingNotEmpty()  {

        onView(withId(R.id.btnOpenConnection)).perform(click());
        onView(withId(R.id.btnSendMessageToWebSocket)).perform(click());
        onView(withId(R.id.btnCloseConnection)).perform(click());
    }



    @Test
    public void testLogcat() throws IOException {
        LogcatDataReader logcatdata = new LogcatDataReader();
        logcatdata.setReadNumberOfLines(300);
        List<String> logs = logcatdata.read();
        for (String log : logs) {
            Log.v(TAG, log);
        }
        assertThat(logs).isNotEmpty();

    }




}
