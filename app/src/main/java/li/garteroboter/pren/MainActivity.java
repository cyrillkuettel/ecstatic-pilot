package li.garteroboter.pren;

import static li.garteroboter.pren.Utils.LogAndToast;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;




import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

import li.garteroboter.pren.log.LogcatData;
import li.garteroboter.pren.log.LogcatDataReader;
import simple.bluetooth.terminal.BlueActivity;
import simple.bluetooth.terminal.screen.ScreenSlidePageFragment;


public class MainActivity extends AppCompatActivity implements WebSocketManagerInstance {
    private static final String TAG = "MainActivity";

    private static final String ABSOLUTE_APK_PATH = "https://github.com/cyrillkuettel/ecstatic" +
            "-pilot/blob/master/app/build/outputs/apk/debug/app-debug.apk?raw=true";
    private WebSocketManager manager = null;
    final Context mainContext = MainActivity.this;

    private static final int MY_CAMERA_REQUEST_CODE = 2;
    public static final int PICK_IMAGE = 1; // so you can recognize when the user comes back from
    // the image gallery

    private static final int PIXEL_CAMERA_WIDTH = 3036;  // default values when taking pictures
    // with google camera.
    private static final int PIXEL_CAMERA_HEIGHT = 4048;

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
    final String[] tabNames = {"Logs", "Images"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_slide_main_acivity);

        viewPager = findViewById(R.id.pager);
        pagerAdapter = new ScreenSlidePagerAdapter(this);
        Log.d(TAG, "creating ScreenSlidePagerAdapter");
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
            Log.i(TAG,  "tabLayout  or viewPager == null");
        }
/*





        Button updateApp = (Button) findViewById(R.id.updateApp);
        updateApp.setOnClickListener(v -> {
            showUpdateMessageBox();

        });

        Button btnSelectImageAndSend = (Button) findViewById(R.id.btnSelectImageAndSend);
        btnSelectImageAndSend.setEnabled(false);
        btnSelectImageAndSend.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
        });

        Button btnSendImagesFromDisk = (Button) findViewById(R.id.btnSendImagesFromDisk);
        btnSendImagesFromDisk.setEnabled(false);
        btnSendImagesFromDisk.setOnClickListener(v -> {
            testSendArrayOfPlants();

        });

        Button btnOpenByteSocketConnection =
                (Button) findViewById(R.id.btnOpenByteSocketConnection);
        btnOpenByteSocketConnection.setOnClickListener(v -> {
            if (manager != null) {
                manager.disconnectAll();
            } else {
                LogAndToast(mainContext, "Opening new Socket connection");
            }
            Spinner hostDropdown = findViewById(R.id.dropdown_menu);
            manager = new WebSocketManager(this, hostDropdown.getSelectedItem().toString());

            // TODO: change this so be more optimal
            new Thread(() -> manager.createAndOpenWebSocketConnection(SocketType.Bytes)).start();
            btnSelectImageAndSend.setEnabled(true);
            btnSendImagesFromDisk.setEnabled(true);
        });


        Button btnGetAbsoluteFilePath = (Button) findViewById(R.id.btnGetAbsoluteFilePath);
        btnGetAbsoluteFilePath.setOnClickListener(v -> {
            Log.i(TAG, "Attempt logcat read");
            testReadingLogcatOutput();
        });

 */

    }




    public void testReadingLogcatOutput() {
        LogcatData logcatreader = new LogcatDataReader();

        try {

            System.out.println(logcatreader.read());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void testSendArrayOfPlants() {
        File[] files = mainContext.getFilesDir().listFiles();
        List<String> file_names = Arrays.stream(files)
                .map(f -> f.getName().toString()).collect(Collectors.toList());

        StorageAccessAgent storageAccessAgent = new StorageAccessAgent(mainContext);
        List<String> plants = storageAccessAgent.fetchNames();

        storageAccessAgent.copyPlantsToInternalDirectory(plants.toArray(new String[0]));

        List<File> plantImages = storageAccessAgent.getAllPlantImages();
        plantImages.forEach(this::sendSinglePlantImageFromInternalDirectory);
    }

    public void sendSinglePlantImageFromInternalDirectory(final File file) {
        try {
            byte[] bytes = FileUtils.readFileToByteArray(file);
            try {
                manager.sendBytes(bytes);
            } catch (Exception e) {
                Log.e(TAG, "Error while sending bytes");
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void vibrate() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
// Vibrate for 500 milliseconds
        v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                //Display an error
                Log.e(TAG, "Image data from Intent is null");
                return;
            }
            byte[] bytes = selectImageWithIntent(data);
            Log.i(TAG, String.format("Sending %s bytes", bytes.length));

            try {
                manager.sendBytes(bytes);
            } catch (Exception e) {
                Log.e(TAG, "Error while sending bytes");
                e.printStackTrace();
            }
        }
    }

    public byte[] selectImageWithIntent(final Intent data) {
        byte[] bytes = new byte[0];
        try {
            InputStream inputStream =
                    mainContext.getContentResolver().openInputStream(data.getData());
            bytes = IOUtils.toByteArray(inputStream);

        } catch (IOException e) {
            Log.e(TAG, "Something went wrong when reading InputStream to bytes. Maybe context?");
            e.printStackTrace();
        }
        return bytes;
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
            case R.id.bluetooth2:
                intent = new Intent(this, BlueActivity.class);
                break;
            case R.id.nanodet:
                intent = new Intent(this, li.garteroboter.pren.nanodet.MainActivity2.class);
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
     *
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == MY_CAMERA_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
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
            // here you can supply custom ScreenSlidePageFragemnt, based on the position
            // send logs
            // send images
            // test timer
            // settings:
                // options:
                    //
            Log.d(TAG, "creating fragments");
            if (position == 0) {
                Log.d(TAG, "returning the first fragment");
                return LoggingFragment.newInstance("This is the first Fragment");
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