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
import java.util.function.Predicate;
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
           return Arrays.stream(filenames).collect(Collectors.toList());

        } catch (IOException e) {
            Log.error("Failed to get asset file list.", e);
        }
        return Collections.emptyList();
    }

    public List<File> getAllPlantImages() {
        Predicate<File> FileNameContainsWEBP = file -> file.getName().contains(".webp");

        final File[] files = context.getFilesDir().listFiles();
        Arrays.stream(files)
                .filter(FileNameContainsWEBP)
                .map(File::getName)
                .forEach(System.out::println);

        return Arrays.stream(files)
                .filter(FileNameContainsWEBP)
                .collect(Collectors.toList());

    }


    public void filterAllDirectoriesInAssets() {
        final File[] files = context.getFilesDir().listFiles();
        List<File> directories = Arrays.stream(files)
                .filter(File::isDirectory).collect(Collectors.toList());
        Log.info(String.format("directories.size() =  %d", directories.size()));
    }






    // only has to run one time per Activity Lifecycle
    // only for testing purposes
    public void copyPlantsToInternalDirectory(final String[] pottedPlantImageFiles) {
        for (String filenameWithAssetsSubFolder : pottedPlantImageFiles) {
            final String basename = filenameWithAssetsSubFolder;
            filenameWithAssetsSubFolder = "test_plants/" + filenameWithAssetsSubFolder;
            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                Log.info(String.format("attempting to copy filename = %s", filenameWithAssetsSubFolder));
                inputStream = assetManager.open(filenameWithAssetsSubFolder);
                File outFile = new File(context.getFilesDir(), basename);
                outputStream = new FileOutputStream(outFile);
                IOUtils.copy(inputStream, outputStream);
                Log.info("Copied 1 file");
            } catch (IOException e) {
                Log.error("Failed to copy asset file: " + filenameWithAssetsSubFolder, e);
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        // NOOP
                    }
                }
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        // NOOP
                    }
                }
            }
        }
    }
}



