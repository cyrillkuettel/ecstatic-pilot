package li.garteroboter.pren;


import static com.google.common.truth.Truth.assertThat;

import androidx.test.filters.SmallTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4ClassRunner.class)
@SmallTest
public class MyTest {

// FUCK THIS SHIT
    // Having spent 1.5 hours to configure this. Now it runs, but shows Tests passed 0 Tests passed
    @SmallTest
    public void testBlah(){
        assertThat(1).isEqualTo(1);
    }

}
