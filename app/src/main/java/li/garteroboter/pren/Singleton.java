package li.garteroboter.pren;

import android.util.Log;

/**
 * This class can store global variables across Activities.
 * Does not work when native layer crashes
 *
 */
public class Singleton {
    private boolean FIRST_TIME_MAINACTIVITY = true;

    private static Singleton instance = null;
    protected Singleton() {
        Log.d("Singleton", "Singleton Constructor");
        // Exists only to defeat instantiation.
    }
    public static Singleton getInstance() {
        if(instance == null) {
            instance = new Singleton();
        }
        return instance;
    }
    public void setFIRST_TIME_MAINACTIVITY(final boolean value) {
        this.FIRST_TIME_MAINACTIVITY = false;
    }

    public boolean isFIRST_TIME_MAINACTIVITY() {
        return FIRST_TIME_MAINACTIVITY;
    }
}