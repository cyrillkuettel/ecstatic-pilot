package li.garteroboter.pren.ui;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import li.garteroboter.pren.R;
import li.garteroboter.pren.StorageAccessAgent;
import li.garteroboter.pren.network.SocketType;
import li.garteroboter.pren.network.WebSocketManager;
import li.garteroboter.pren.network.WebSocketManagerInstance;


/**
 * this is the tab for testing the sending of images to the webserver.
 */
public class SendImagesFragment extends Fragment {
    private static final String TAG = "SendImagesFragment";
    public static final int PICK_IMAGE = 1; // code to identify action in onActivityResult

    private WebSocketManager manager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    public static SendImagesFragment newInstance() {
        SendImagesFragment fragment = new SendImagesFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            WebSocketManagerInstance instance = (WebSocketManagerInstance) context;
            manager = instance.getManager();
        } catch (ClassCastException castException) {
            /** The activity does not implement the listener. */
            Log.e(TAG, "Failed to implement WebSocketManagerInstance");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_websocket_send_images, container, false);
        generateDropDownItems(view);


        Button btnSelectImageAndSend = view.findViewById(R.id.btnSelectImageAndSend);
        btnSelectImageAndSend.setEnabled(false);
        btnSelectImageAndSend.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
        });

        Button btnSendImagesFromDisk = view.findViewById(R.id.btnSendImagesFromDisk);
        btnSendImagesFromDisk.setEnabled(false);
        btnSendImagesFromDisk.setOnClickListener(v -> {
            testSendArrayOfPlants();

        });

        Button btnOpenByteSocketConnection =
                view.findViewById(R.id.btnOpenByteSocketConnection);
        btnOpenByteSocketConnection.setOnClickListener(v -> {
            Spinner hostnameDropdown = view.findViewById(R.id.dropdown_menu_byte);
            Log.e(TAG, hostnameDropdown.getSelectedItem().toString());
            reOpenSocket(hostnameDropdown.getSelectedItem().toString());

            btnSelectImageAndSend.setEnabled(true);
            btnSendImagesFromDisk.setEnabled(true);
        });

        Button btnSendImagesToLocal =
                view.findViewById(R.id.btnTestLocalImagesSend);
        btnSendImagesToLocal.setOnClickListener( v -> {
            reOpenSocket("ws://192.168.188.38:8000/ws/");
            try {
                Thread.sleep(400);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            uploadSinglePlantTest("lavender_rotated");
        });

        Button btnSendThreeImagesToLocal =
                view.findViewById(R.id.btnSendThreeImagesToLocal);
        btnSendThreeImagesToLocal.setOnClickListener( v -> {
            reOpenSocket("ws://192.168.188.38:8000/ws/");
            try {
                Thread.sleep(400);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            uploadSinglePlantTest("three_plants");
        });

        return view;
    }

    public void reOpenSocket(final String hostname) {
        if (manager != null) {
            manager.disconnectAll();
        } else {
            Log.i(TAG, "Opening new Socket connection");
        }
        manager = new WebSocketManager(getContext(), hostname, SocketType.Binary);
        // TODO: change this so be more optimal
        new Thread(() -> manager.createAndOpenWebSocketConnection()).start();
    }

    public final void generateDropDownItems(View view) {
        Spinner spinnerHostname = view.findViewById(R.id.dropdown_menu_byte);

        ArrayAdapter<CharSequence> adapter = null;
        try {
            adapter = ArrayAdapter.createFromResource(
                    getContext(), R.array.uri, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerHostname.setAdapter(adapter);
        } catch (Exception e) {
            Log.e(TAG, "Failed creating spinner items");
            e.printStackTrace();
        }
    }

    public List<File> getTestImages()  {
        File[] files = getContext().getFilesDir().listFiles();
        List<String> file_names = Arrays.stream(files)
                .map(f -> f.getName()).collect(Collectors.toList());

        StorageAccessAgent storageAccessAgent = new StorageAccessAgent(getContext());
        List<String> plants = storageAccessAgent.fetchNames();

        storageAccessAgent.copyPlantsToInternalDirectory(plants.toArray(new String[0]));

        return storageAccessAgent.getAllPlantImages();
    }

    public void testSendArrayOfPlants() {
        List<File> plantImages = getTestImages();
        plantImages.forEach(this::sendSinglePlantImageFromInternalDirectory);
    }

    public void uploadSinglePlantTest(String filter) {
        List<File> plantImages = getTestImages();
        List<File> rotated = plantImages.stream()
                .filter(element -> element.getName()
                        .contains(filter))
                .collect(Collectors.toList());
        if (rotated.size() > 0) {
            sendSinglePlantImageFromInternalDirectory(rotated.get(0));
        } else {
            throw new IndexOutOfBoundsException("Index out of bounds in uploadSinglePlantTest");
        }

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

            try (InputStream inputStream =
                         getContext().getContentResolver().openInputStream(data.getData())) {
                bytes = IOUtils.toByteArray(inputStream);
            } catch (Exception ex) {
                Log.e(TAG, "Failed to open input stream of Intent data. Might be because " +
                        "of getContext().getContentResolver() ");
            }
        return bytes;
    }

    public SendImagesFragment() {
        // Required empty public constructor
    }
}

