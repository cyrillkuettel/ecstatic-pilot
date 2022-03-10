package li.garteroboter.pren;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import li.garteroboter.pren.nanodet.MainActivityNanodetNCNN;
import li.garteroboter.pren.qrcode.QrCodeActivity;
import li.garteroboter.pren.settings.SettingsActivity;
import li.garteroboter.pren.socket.WebSocketManager;
import li.garteroboter.pren.socket.WebSocketManagerInstance;
import simple.bluetooth.terminal.DevicesFragment;


public class MainActivity extends AppCompatActivity implements WebSocketManagerInstance {
    private static final String TAG = "MainActivity";


    private WebSocketManager manager = null;
    private static final int MY_CAMERA_REQUEST_CODE = 2;     // the image gallery

    private static final int PIXEL_CAMERA_WIDTH = 3036;  // default values when taking pictures
    private static final int PIXEL_CAMERA_HEIGHT = 4048;


    // You have to change viewPager.setOffscreenPageLimit as well.
    private static final int NUM_PAGES = 3;

    /**
     * The pager adapter, which provides the pages to the view pager widget.
     */
    private FragmentStateAdapter pagerAdapter;
    TabLayout tabLayout;
    String[] tabNames = new String[NUM_PAGES];


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_slide_main_acivity);

        /*
         * The pager widget, which handles animation and allows swiping horizontally to access
         * previous
         * and next wizard steps.
         */
        ViewPager2 viewPager = findViewById(R.id.pager);
        pagerAdapter = new ScreenSlidePagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setOffscreenPageLimit(3); // important: the fragments stay in memory
        tabLayout = findViewById(R.id.tabLayout);
        tabNames = new String[]{"Logs", "Images", "Bluetooth Terminal"};
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
            Log.i(TAG, "tabLayout  or viewPager == null");
        }

        Button btnSTART = findViewById(R.id.START);
        btnSTART.setBackgroundColor(getResources().getColor(R.color.purple_500));
        btnSTART.setOnClickListener(v ->

                startMainActivityNanodetNCNN());


        Button settingsBtn = findViewById(R.id.idBtnSettings);
        settingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // opening a new intent to open settings activity.
                Intent i = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(i);
            }
        });

        //startMainActivityNanodetNCNN();
    }

    public void createSharedPreferences() {
        // Read the preferences
        // https://stackoverflow.com/questions/7057845/save-arraylist-to-sharedpreferences
        // https://developer.android.com/guide/topics/ui/settings/use-saved-values

        Context applicationContext = getApplicationContext();
        SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(applicationContext);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("test", "value");
        editor.commit();
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainpagemenu, menu);
        return true;
    }

    public void startMainActivityNanodetNCNN() {
        Intent myIntent = new Intent(this, MainActivityNanodetNCNN.class);
        startActivity(myIntent);
    }

    /**
     * Hamburger Menu selection
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.websocket:
                intent = new Intent(this, MainActivity.class);
                break;
            case R.id.nanodet:
                intent = new Intent(this, MainActivityNanodetNCNN.class);
                break;
            case R.id.qrCodeactivity:
                intent = new Intent(this, QrCodeActivity.class);
                break;
            default:
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

        @Override
        public Fragment createFragment(int position) {

            if (position == 0) {
                Log.v(TAG, String.format("Position is %s, return LoggingFragment", position));
                return LoggingFragment.newInstance();
            } else if (position == 1) {
                Log.v(TAG, String.format("Position is %s, return SendImagesFragment", position));
                return SendImagesFragment.newInstance();
            } else
                Log.v(TAG, String.format("Position is %s, return DevicesFragment", position));
                return DevicesFragment.newInstance();
            }

        @Override
        public int getItemCount() {
            return NUM_PAGES;
        }
    }

}