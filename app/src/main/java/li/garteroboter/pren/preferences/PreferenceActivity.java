package li.garteroboter.pren.preferences;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import li.garteroboter.pren.R;


public class PreferenceActivity extends AppCompatActivity {

    private final String TAG = "SettingsActivity";
    private PreferenceFragment preferenceFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getSupportActionBar().setTitle("Settings");

        if (findViewById(R.id.idFrameLayout) != null) {
            if (savedInstanceState != null) {
                return;
            }


            preferenceFragment = new PreferenceFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.idFrameLayout, preferenceFragment)
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

