package simple.bluetooth.terminal;

import android.content.Context;
import android.graphics.Camera;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import li.garteroboter.pren.R;
import li.garteroboter.pren.nanodet.VibrationListener;
import li.garteroboter.pren.qrcode.CameraPreviewFragment;
import simple.bluetooth.terminal.screen.ScreenSlidePageFragment;

public class BlueActivity extends FragmentActivity implements VibrationListener {
    private static final Logger Log = LogManager.getLogger(BlueActivity.class);

    /**
     * The number of pages (wizard steps) to show in this demo.
     */
    private static final int NUM_PAGES = 2;

    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    private ViewPager2 viewPager;

    /**
     * The pager adapter, which provides the pages to the view pager widget.
     */
    private FragmentStateAdapter pagerAdapter;
    TabLayout tabLayout;
    final String[] tabNames = {"QR-Code", "Neural Network plant detection"};
    private static final String TAG = "ScreenSlidePagerActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_slide);

        viewPager = findViewById(R.id.pager);
        pagerAdapter = new ScreenSlidePagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setOffscreenPageLimit(2); // important: the fragments stay in memory
        tabLayout =(TabLayout) findViewById(R.id.tabLayout);

        if (tabLayout != null && viewPager!= null) {
            new TabLayoutMediator(
                    tabLayout,
                    viewPager,
                    (tab, position) -> {
                        tab.setText(tabNames[position]);
                        // tab.setIcon(R.drawable.ic_launcher_background);
                    }
            ).attach();
        } else {
            Log.info( "tabLayout  or viewPager == null");
        }


      //  getSupportFragmentManager().addOnBackStackChangedListener(this);
        if (savedInstanceState == null)
            getSupportFragmentManager().beginTransaction().add(R.id.fragmentBluetoothChain, new DevicesFragment(), "devices").commit();
        // else
           // onBackStackChanged();
    }

    /*
    @Override
    public void onBackStackChanged() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(getSupportFragmentManager().getBackStackEntryCount()>0);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

     */

    @Override
    public void onBackPressed() {
        if (viewPager.getCurrentItem() == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed();
        } else {
            // Otherwise, select the previous step.
            viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
        }
    }

    @Override
    public void startVibrating(final int millis) {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
// Vibrate for N milliseconds
        try {
            v.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE));
        } catch (Exception e) {
            Log.debug("Failed to vibrate");
            e.printStackTrace();
        }
    }

    /**
     * A simple pager adapter that represents some ScreenSlidePageFragment objects, in
     * sequence.
     */
    private class ScreenSlidePagerAdapter extends FragmentStateAdapter {
        public ScreenSlidePagerAdapter(FragmentActivity fa) {
            super(fa);
        }

        @Override
        public Fragment createFragment(int position) {
            // here you can supply custom ScreenSlidePageFragemnt, based on the position
            if (position == 0) {
                return CameraPreviewFragment.newInstance();
            } else {
                return ScreenSlidePageFragment.newInstance("This is the second Fragment");
            }
        }

        @Override
        public int getItemCount() {
            return NUM_PAGES;
        }
    }
}
