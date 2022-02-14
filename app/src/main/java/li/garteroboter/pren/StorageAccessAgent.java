package li.garteroboter.pren;

/*
    Accessing app-specific files for this application.
    This class aids the process.
 */


import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;
import android.util.Log;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class StorageAccessAgent {

    private static final Logger Log = LogManager.getLogger(StorageAccessAgent.class);

    private final Context context;
    private AssetManager assetManager;

    public StorageAccessAgent(final Context context) {
        this.context = context;
        this.assetManager = context.getAssets();

    }

    /**
     * Fetches the folder test_plants and returns all names of the files in it.
     */
    public List<String> fetchNames() {
        AssetManager assetManager = context.getAssets();
        String[] filenames = null;
        try {
            filenames = assetManager.list("test_plants");
            Log.info(String.format("assetManager.list.length = %d", filenames.length));
           return Arrays.stream(filenames).collect(Collectors.toList());

        } catch (IOException e) {
            Log.error("Failed to get asset file list.", e);
        }

        return Collections.emptyList();

    }

    /**
     * Have some plant pictures ready to test.
     */
    public void copyAssets() {
        AssetManager assetManager = context.getAssets();
        String[] files = null;
        try {
            files = assetManager.list("");
            Log.info(String.format("assetManager.list = %d", files.length));

        } catch (IOException e) {
            Log.error("Failed to get asset file list.", e);
        }


        if (files != null) for (String filename : files) {
            InputStream in = null;
            OutputStream out = null;
            try {
                Log.info(String.format("filename = %s" , filename));
                in = assetManager.open(filename);
                File outFile = new File(context.getFilesDir(), filename);
                out = new FileOutputStream(outFile);
                copyFile(in, out);
            } catch(IOException e) {
                Log.error("Failed to copy asset file: " + filename, e);
            }
            finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        // NOOP
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        // NOOP
                    }
                }
            }
        }
    }

    // TODO: unwrap shallow method.
    private void copyFile(InputStream in, OutputStream out) throws IOException {
        IOUtils.copy(in, out);
        /*
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
         */
    }



}



