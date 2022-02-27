package li.garteroboter.pren.pren;


import static com.google.common.truth.Truth.assertThat;

import android.util.Log;

import androidx.test.filters.SmallTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4ClassRunner.class)
@SmallTest
public class MainActivityTest {
    private static final String TAG = "MainActivityTest";
// IT FINALLY WORKS THANKS TO THIS:
    // https://github.com/android/testing-samples/tree/main/unit/BasicUnitAndroidTest/app/src/androidTest/java/com/example/android/testing/unittesting/basicunitandroidtest
    @Test
    public void testBlah(){
        assertThat(1).isEqualTo(1);
    }

    /*
    Alright let's think about what we're going to do .
    Wasted huge amounts of time just so you're able to develop on the laptop.
    Mainly Fixing Android Studio issues.
    I went through the cubersome taks of refactoring to Logcat again.
    Well, at the very least we have colors now.

    I will implement a ViewPager I think. TDD ?
    BUT FIRST it's important to set priorities. I want to read the Logcat output at runtime.
     */
    @Test
    public void testLoggingWithColor() {
        Log.i(TAG, "Logging works");
        Log.e(TAG, "error");
        Log.d(TAG, "debug");
        // System.out.println(System.getProperty("java.class.path"));
    }


}
