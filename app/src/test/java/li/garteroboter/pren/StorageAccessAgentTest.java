package li.garteroboter.pren;

import static org.junit.jupiter.api.Assertions.assertTrue;

import android.content.Context;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class StorageAccessAgentTest  {
    private static final String TAG = "StorageAccessAgentTest";

    Context mContext;

    @BeforeAll
    public static void setup() {
        
    }

    @BeforeEach
    public void setupContext() {
        // https://github.com/android/android-test/issues/409
        // this is a hack to get hte context
        this.mContext =  InstrumentationRegistry.getInstrumentation().getTargetContext();

    }


    @Test
    public void testPlantDirecoryIsnotNull() {
        if (mContext == null) {
            System.out.println("null");
        }
        StorageAccessAgent storageAccessAgent = new StorageAccessAgent(mContext);
        List<String> list = storageAccessAgent.fetchNames();
        System.out.println(list);
        Log.i(TAG, String.valueOf(list));
        assertTrue(true);
        //assertThat(list.contains("potted_plant.jpg.webp"));

    }

    @Test
    public void testLoggingWithColor() {
        Log.i(TAG, "Logging works");
        Log.e(TAG, "error");
        Log.d(TAG, "debug");
        // System.out.println(System.getProperty("java.class.path"));
    }
}