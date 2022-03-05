package li.garteroboter.pren.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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


            settingsFragment = new SettingsFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.idFrameLayout, settingsFragment)
                    .commit();

            Button btnApply = findViewById(R.id.btnApply);
            btnApply.setOnClickListener(v -> {
                goBack();
            });

        }
    }
    private void goBack() {
        Log.d(TAG, "Commit Settings. Go back to Main.");
        Toast.makeText(this, "Settings saved",
                Toast.LENGTH_SHORT).show();
        super.onBackPressed();
    }


}

