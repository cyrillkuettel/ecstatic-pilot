package li.garteroboter.pren.log;

import java.io.IOException;
import java.util.List;

public interface LogcatData {

    /**
     * Read the current output of the Logcat at runtime.
     * @return Appendded String.
     */
    List<String> read(long readNumberOfLines) throws IOException;

    /**
     * Flush the Logs. This will erase history.
     * I suppose it makes sense to call this when the app is first started.
     */
    void flush();

}
