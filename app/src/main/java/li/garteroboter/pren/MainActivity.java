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
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import li.garteroboter.pren.nanodet.MainActivityNanodetNCNN;
import simple.bluetooth.terminal.BlueActivity;


public class MainActivity extends AppCompatActivity implements WebSocketManagerInstance {
    private static final String TAG = "MainActivity";

    private static final String ABSOLUTE_APK_PATH = "https://github.com/cyrillkuettel/ecstatic" +
            "-pilot/blob/master/app/build/outputs/apk/debug/app-debug.apk?raw=true";
    private static final int MY_CAMERA_REQUEST_CODE = 2;     // the image gallery
    private static final int PIXEL_CAMERA_WIDTH = 3036;  // default values when taking pictures
    private static final int PIXEL_CAMERA_HEIGHT = 4048;
    private static final int NUM_PAGES = 2;
    final String[] tabNames = {"Logs", "Images"};
    TabLayout tabLayout;
    private WebSocketManager manager = null;
    /**
     * The pager adapter, which provides the pages to the view pager widget.
     */
    private FragmentStateAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_slide_main_acivity);

        /**
         * The pager widget, which handles animation and allows swiping horizontally to access
         * previous
         * and next wizard steps.
         */
        ViewPager2 viewPager = findViewById(R.id.pager);
        pagerAdapter = new ScreenSlidePagerAdapter(this);
        Log.d(TAG, "creating ScreenSlidePagerAdapter");
        viewPager.setAdapter(pagerAdapter);
        viewPager.setOffscreenPageLimit(2); // important: the fragments stay in memory
        tabLayout = findViewById(R.id.tabLayout);

        if (tabLayout != null && viewPager != null) {
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
        updateApp.setOnClickListener(v -> {
            showUpdateMessageBox();
        });

        Intent myIntent = new Intent(this, MainActivityNanodetNCNN.class);
        startActivity(myIntent);
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
            case R.id.Main:
                intent = new Intent(this, MainActivity.class);
                break;
            case R.id.deprecated_fragment_QR:
                intent = new Intent(this, BlueActivity.class);
                break;
            case R.id.nanodet_Activity:
                intent = new Intent(this, li.garteroboter.pren.nanodet.MainActivityNanodetNCNN.class);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        startActivity(intent);
        return true;
    }


    public final void showUpdateMessageBox() {

        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Achtung: Update Vorgehensweise");
        final TextView messageForUpdateProcedure = new TextView(this);
        final String defaultMessage = String.format("Zuerst dieses App deinstallieren, bevor das " +
                "apk installiert wird! Wenn die Meldung \"App nicht installiert\" kommt, hilft " +
                "meistens ein Neustart des Geräts. ");
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
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
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
            // send logs
            // send images
            // settings:
            if (position == 0) {
                Log.v(TAG, String.format("Position is %s, return LoggingFragment", position));
                return LoggingFragment.newInstance();
            } else {
                Log.v(TAG, String.format("Position is %s, return SendImagesFragment", position));
                return SendImagesFragment.newInstance();
            }
        }

        @Override
        public int getItemCount() {
            return NUM_PAGES;
        }
    }

}