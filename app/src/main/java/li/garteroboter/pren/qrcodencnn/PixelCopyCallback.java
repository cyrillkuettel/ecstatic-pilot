package li.garteroboter.pren.qrcodencnn;

import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import li.garteroboter.pren.qrcodencnn.MainActivityQRCodeNCNN;

public class PixelCopyCallback implements MainActivityQRCodeNCNN.PostTake {
    private static final String TAG = "PixelCopyCallback";

    String hasPermission = "PENDING";
    @Override
    public void onSuccess(Bitmap bitmap) {
        Log.d(TAG, "onSuccess!");
        if (hasPermission.equals("YES")) {
            savebitmap(bitmap);
        } else {
            Log.e(TAG, String.format("has permission = %s", hasPermission));
        }
    }

    @Override
    public void onFailure(int error) {
        Log.e(TAG, "onFailure!");

    }

    public void savebitmap(Bitmap bmp)  {

        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 60, bytes);
            File f = new File(Environment.getExternalStorageDirectory()
                    + File.separator + "testimage_zhb.jpg");
            f.createNewFile();
            FileOutputStream fo = new FileOutputStream(f);
            fo.write(bytes.toByteArray());
            fo.close();
        } catch (IOException e) {
            Log.d(TAG, "failed to save bitmap");
            e.printStackTrace();
        }

    }
}
