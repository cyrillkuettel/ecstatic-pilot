package li.garteroboter.pren;


import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
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

    private static final String ABSOLUTE_APK_PATH = "https://github.com/cyrillkuettel/ecstatic" +
            "-pilot/blob/master/app/build/intermediates/apk/debug/app-debug.apk?raw=true";

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

        Button updateApp = findViewById(R.id.updateApp);
        updateApp.setOnClickListener(v -> showUpdateMessageBox());


        Button settingsBtn = findViewById(R.id.idBtnSettings);
        settingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // opening a new intent to open settings activity.
                Intent i = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(i);
            }
        });

        /*
        SharedPreferences sharedPreferences =
                PreferenceManager.

        String name = sharedPreferences.getString("key_show_an_approximation_of_fps", "");

        Log.v(TAG, String.format("Printing fps on off value: %s", name));

        Intent myIntent = new Intent(this, MainActivityQRCodeNCNN.class);
        startActivity(myIntent);

         */
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainpagemenu, menu);
        return true;
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


    public final void showUpdateMessageBox() {

        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Update Vorgehensweise");
        final TextView messageForUpdateProcedure = new TextView(this);
        final String defaultMessage = "Du wirst jetzt weitergeleitet, um die neuste Version des " +
                "Apps als apk herunterzuladen. \n *Wichtig* Zuerst dieses App deinstallieren, bevor " +
                "das " +
                "neue App installiert wird! Wenn die Meldung \"App nicht installiert\" kommt, " +
                "hilft " +
                "meistens ein Neustart des Geräts. ";
        messageForUpdateProcedure.setText(defaultMessage);
        alert.setView(messageForUpdateProcedure);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(ABSOLUTE_APK_PATH));
                startActivity(browserIntent);
            }
        });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.cancel();
            }
        });
        alert.show();

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