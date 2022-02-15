package li.garteroboter.pren.log;

import java.io.IOException;
import java.util.List;

public interface LogcatData {

    /**
     * Read the current output of the Logcat at runtime.
     * @return Appendded String.
     */
    List<String> read() throws IOException;

}
