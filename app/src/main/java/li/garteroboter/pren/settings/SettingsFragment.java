package li.garteroboter.pren.settings;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import java.util.Map;

import li.garteroboter.pren.R;

public class SettingsFragment extends PreferenceFragmentCompat {
    private static final String TAG = "SettingsFragment";

    public static SettingsFragment newInstance() {
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Log.v(TAG, "onCreatePreferences");
        setPreferencesFromResource(R.xml.preferences, rootKey);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        Map<String, ?> map;

        map = preferences.getAll();

        for (Object item : map.entrySet()) {
            Log.v(TAG, item.toString());
        }
        Log.v(TAG, String.valueOf(map.size()));


/*
        SharedPreferences sharedPref = null;
        try {
            sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(getString(R.string.show_an_approximation_of_fps), "test");
        editor.commit();

        Preference preference = findPreference("remind_me_for_bed_time");
        Log.v(TAG , (String) preference.getTitle());

         */

    }



}