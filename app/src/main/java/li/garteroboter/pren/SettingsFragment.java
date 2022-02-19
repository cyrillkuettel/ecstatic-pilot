package li.garteroboter.pren;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.preference.*;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsFragment extends PreferenceFragmentCompat {
    private static final String TAG = "SettingsFragment";

    private ListPreference mListPreference;

    public static SettingsFragment newInstance() {
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Indicate here the XML resource you created above that holds the preferences
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }



    /*
    // commented out
    https://stackoverflow.com/questions/43131537/recyclerview-exception-using-preferencefragmentcompat-without-any-recyclerview
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mListPreference = (ListPreference)  getPreferenceManager().findPreference("ESP-MAC");
        if (mListPreference != null) {
            mListPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    // your code here
                    return true;
                }
            });
        } else {
            Log.e(TAG, "mListPreference is Null ");
        }
        return inflater.inflate(R.layout.settings_layout, container, false);
    }

     */


    public SettingsFragment() {
        // required empty constructor
    }
}