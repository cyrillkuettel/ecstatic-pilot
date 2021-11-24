package li.garteroboter.pren;

import android.os.Bundle;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import li.garteroboter.pren.databinding.ActivitySimpleUiactivityBinding;

public class simpleUIActivity extends AppCompatActivity {

private ActivitySimpleUiactivityBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_uiactivity);
    }


    public void onStartMessageESP32(View view) {
    }
}