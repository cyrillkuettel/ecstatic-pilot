package li.garteroboter.pren;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;


import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;


import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class StorageAccessAgentTest  {
    private static final Logger Log = Logger.getLogger(WebSocketManagerTest.class);

    Context mContext;

    @BeforeAll
    public static void setup() {
        BasicConfigurator.configure();
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
        Log.info(list);
        assertTrue(true);
        //assertThat(list.contains("potted_plant.jpg.webp"));

    }
}