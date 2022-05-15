package li.garteroboter.pren;


import static li.garteroboter.pren.ui.LogcatFragment.newLogcatFragmentInstance;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import li.garteroboter.pren.log.LogcatDataReader;
import li.garteroboter.pren.nanodet.NanodetncnnActivity;
import li.garteroboter.pren.preferences.PreferenceActivity;
import li.garteroboter.pren.socket.WebSocketManager;
import li.garteroboter.pren.socket.WebSocketManagerInstance;
import li.garteroboter.pren.ui.LogcatLine;
import li.garteroboter.pren.ui.LoggingFragment;
import li.garteroboter.pren.ui.SendImagesFragment;


public class MainActivity extends AppCompatActivity implements WebSocketManagerInstance {
    private static final String TAG = "MainActivity";


    private WebSocketManager manager = null;
    private static final int MY_CAMERA_REQUEST_CODE = 2;     // the image gallery



    // You have to change viewPager.setOffscreenPageLimit as well.
    private static final int NUM_PAGES = 3;

    private TabLayout tabLayout;
    private String[] tabNames = new String[NUM_PAGES];


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_slide_main_acivity);

        readLogcat();

        setupViewPager();

        setupButtonOnClickListeners();

        //startMainActivityNanodetNCNN();
    }

    private void readLogcat() {
        LogcatDataReader logcatDataReader = new LogcatDataReader();

        try {
            List<String> logs = logcatDataReader.read(400);
            List<LogcatLine> logcatLines = logs.stream().map(LogcatLine::new).collect(Collectors.toList());
            logcatLines.forEach(el -> System.out.println(el.toString() + '\n'));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupButtonOnClickListeners() {
        Button btnSTART = findViewById(R.id.START);
        // btnSTART.setBackgroundColor(getResources().ContextCompat.getColor(R.color.purple_500));
        btnSTART.setOnClickListener(v ->
                startNanodetNcnnActivity());

        Button settingsBtn = findViewById(R.id.idBtnSettings);
        settingsBtn.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, PreferenceActivity.class);
            startActivity(i);
        });
    }


    private void setupViewPager() {
        /*
         * The pager widget, which handles animation and allows swiping horizontally to access
         * previous
         * and next wizard steps.
         */
        ViewPager2 viewPager = findViewById(R.id.pager);

        /* The pager adapter, which provides the pages to the view pager widget. */
        FragmentStateAdapter pagerAdapter = new ScreenSlidePagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setOffscreenPageLimit(3); // important: the fragments stay in memory
        tabLayout = findViewById(R.id.tabLayout);
        tabNames = new String[]{"Logs", "Images", "Logcat"};
        if (tabLayout != null) {
            new TabLayoutMediator(
                    tabLayout,
                    viewPager,
                    (tab, position) -> {
                        tab.setText(tabNames[position]);
                        // tab.setIcon(R.drawable.ic_launcher_background);
                    }
            ).attach();
        } else {
            Log.e(TAG, "tabLayout || viewPager == null");
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainpagemenu, menu);
        return true;
    }

    public void startNanodetNcnnActivity() {
        Intent myIntent = new Intent(this, NanodetncnnActivity.class);
        startActivity(myIntent);
    }

    /**
     * Hamburger Menu selection
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        int itemId = item.getItemId();
        if (itemId == R.id.websocket) {
            intent = new Intent(this, MainActivity.class);
        } else if (itemId == R.id.nanodet) {
            intent = new Intent(this, NanodetncnnActivity.class);
        } else {
            return super.onOptionsItemSelected(item);
        }
        startActivity(intent);
        return true;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!(manager == null)) {
            manager.disconnectAll();
        }
    }

    /***
     * Fires when the user has allowed or denied camera access during runtime.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == MY_CAMERA_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "camera permission granted");
            } else {
                Log.d(TAG, "camera permission denied");
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public WebSocketManager getManager() {
        return manager;
    }

    /**
     * A simple pager adapter that represents some ScreenSlidePageFragment objects, in
     * sequence.
     */
    private static class ScreenSlidePagerAdapter extends FragmentStateAdapter {
        public ScreenSlidePagerAdapter(FragmentActivity fa) {
            super(fa);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {

            if (position == 0) {
                return LoggingFragment.newInstance();
            } else if (position == 1) {
                return SendImagesFragment.newInstance();
            } else
                return newLogcatFragmentInstance();
            }

        @Override
        public int getItemCount() {
            return NUM_PAGES;
        }
    }

}