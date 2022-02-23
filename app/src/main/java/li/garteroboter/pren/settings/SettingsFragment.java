package li.garteroboter.pren;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsFragment extends PreferenceFragmentCompat {
    private static final String TAG = "SettingsFragment";


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Log.v(TAG, "onCreatePreferences");
        setPreferencesFromResource(R.xml.preferences, rootKey);


        /*
        Preference preference = findPreference("remind_me_for_bed_time");
        Log.v(TAG , (String) preference.getTitle());

         */

    }



}