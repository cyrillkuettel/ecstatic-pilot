package li.garteroboter.pren;


import static com.google.common.truth.Truth.assertThat;



import androidx.test.filters.SmallTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import android.util.Pair;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;

@RunWith(AndroidJUnit4ClassRunner.class)
@SmallTest
public class MainActivityTest {

// IT FINALLY WORKS THANKS TO THIS:

    // https://github.com/android/testing-samples/tree/main/unit/BasicUnitAndroidTest/app/src/androidTest/java/com/example/android/testing/unittesting/basicunitandroidtest
    @Test
    public void testBlah(){
        assertThat(1).isEqualTo(1);
    }

}
