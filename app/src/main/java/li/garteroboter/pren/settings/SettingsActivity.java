package li.garteroboter.pren.settings;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.SwitchPreference;
import androidx.preference.PreferenceFragmentCompat;

import li.garteroboter.pren.R;


public class SettingsActivity extends AppCompatActivity {

    private final String TAG = "SettingsActivity";
    private SettingsFragment settingsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getSupportActionBar().setTitle("Settings");

        if (findViewById(R.id.idFrameLayout) != null) {
            if (savedInstanceState != null) {
                return;
            }

            // https://stackoverflow.com/questions/11316560/sharedpreferences-from-different-activity
            Context applicationContext = getApplicationContext();

            settingsFragment = new SettingsFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.idFrameLayout, settingsFragment)
                    .commit();

            Button btnApply = findViewById(R.id.btnApply);
            btnApply.setOnClickListener(v -> {
                commitSettings();
            });

        }
    }
    public void commitSettings() {

    }
}

