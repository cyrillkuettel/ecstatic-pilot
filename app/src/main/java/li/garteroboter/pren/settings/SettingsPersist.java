package li.garteroboter.pren.settings;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

public class SettingsPersist extends Application {
    private static Context mAppContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mAppContext = getApplicationContext();
    }

    public static Context getmAppContext() {
        return mAppContext;
    }

    public static SharedPreferences getPreferences(){
        return mAppContext.getSharedPreferences("my_app_preferences",MODE_MULTI_PROCESS);
    }
}
