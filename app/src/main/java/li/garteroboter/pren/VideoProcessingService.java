package li.garteroboter.pren;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Locale;
import android.util.Size;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/*
        Reference: http://stackoverflow.com/questions/28003186/capture-picture-without-preview-using-camera2-api
        Problem
        1.  BufferQueue has been abandoned  from ImageCapture
        */
public class VideoProcessingService extends Service {

    private static final Logger Log = LogManager.getLogger(VideoProcessingService.class);

    protected static final int CAMERACHOICE = CameraCharacteristics.LENS_FACING_FRONT;
    protected static CameraCharacteristics characteristics;
    private static final int CODE_PERM_CAMERA = 3;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession session;
    protected ImageReader imageReader;

    private static boolean SENT_IMAGE = false;

    protected CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.info("CameraDevice.StateCallback onOpened");
            cameraDevice = camera;
            try {
                cameraDevice.createCaptureSession(Arrays.asList(imageReader.getSurface()), sessionStateCallback, null);
            } catch (CameraAccessException e){
                Log.error(e.getMessage());
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.warn("CameraDevice.StateCallback onDisconnected");
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.error("CameraDevice.StateCallback onError " + error);
        }
    };

    protected CameraCaptureSession.StateCallback sessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            Log.info("CameraCaptureSession.StateCallback onConfigured");
            VideoProcessingService.this.session = session;
            try {
                session.setRepeatingRequest(createCaptureRequest(), null, null);
            } catch (CameraAccessException e) {
                Log.error(e.getMessage());
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            Log.info("onConfigureFailed");

        }
    };

    protected ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Log.info("onImageAvailable");
            // mTextureView.onPreviewFrame(reader.acquireNextImage().getPlanes([0].getBuffer().array());

            if (!SENT_IMAGE) { // To get in working: only process the image once, not multiple times.  (at least for testing)
                Image img = reader.acquireLatestImage();
                Log.debug(String.format(Locale.getDefault(), "image w = %d; h = %d", img.getWidth(), img.getHeight()));
                    try {
                        SENT_IMAGE = true;
                        processImage(img);
                        img.close();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
            }
        }
    };

    /**
     *  Process image data as desired.
     */
    private void processImage(Image image) throws InterruptedException {

        Log.info("processImage fired!");

        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

        String savePath = "";
        try {
            savePath = saveToInternalStorage(bitmap);
            Log.info(String.format("Saved the file to following directory: %s", savePath));
        } catch (Exception e) {
            Log.warn("Error saving file to directory");
            e.printStackTrace();
        }

        // now working just quite how I want to
        /*
        Imageview imageView = findViewById(R.id.imageView22);
        mImageView.setImageBitmap(bitmap);
        */

        //  The following code is a whole different experiment altogether. Highly dubious....
        /*
        byte[] imageTosend = YUV_420_888toNV21(image);

        String remoteURI =   "ws://pren.garteroboter.li:80/ws/";
        String localURI =   "ws://192.168.188.38:80/ws/";

        WebSocketManager manager = new WebSocketManager(localURI);
        Thread openWebSocketThread = new Thread() {
            public void run() {
                manager.createAndOpenWebSocketConnection(Sockets.Binary);
                manager.sendBytes(imageTosend);
            }
        };
        openWebSocketThread.start();
         */

    }

    /**
     * there is also a class
     * https://stackoverflow.com/questions/17674634/saving-and-reading-bitmaps-images-from-internal-memory-in-android
     */
    private String saveToInternalStorage(Bitmap bitmapImage){
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        // path to /data/data/yourapp/app_data/imageDir
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        // Create imageDir
        File mypath=new File(directory,"profile.jpg");

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return directory.getAbsolutePath();
    }


    public void readyCamera() {
        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            String pickedCamera = getCamera(manager);
            Log.info(String.format("getCamera numero %s", pickedCamera));

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.

                Log.error("no permission fuck!! ");
                return;
            }
            manager.openCamera(pickedCamera, cameraStateCallback, null);


            Size[] jpegSizes = null;
            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            } else {
                Log.info("CameraCharacteristics is null! ");
            }
            int width = jpegSizes[0].getWidth();
            int height = jpegSizes[0].getHeight();
            Log.info(String.format("width=%d", width));
            Log.info(String.format("height=%d", height));

            imageReader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 2 /* images buffered */);
            imageReader.setOnImageAvailableListener(onImageAvailableListener, null);
            imageReader.getSurface();
            Log.info("imageReader created");
        } catch (CameraAccessException e){
            Log.error(e.getMessage());
        }
    }


    /**
     *  Return the Camera Id which matches the field CAMERACHOICE.
     */
    public String getCamera(CameraManager manager){
        try {
            for (String cameraId : manager.getCameraIdList()) {
                characteristics = manager.getCameraCharacteristics(cameraId);

                int cOrientation = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (cOrientation == CAMERACHOICE) {
                    return cameraId;
                }
            }
        } catch (CameraAccessException e){
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.info("onStartCommand flags " + flags + " startId " + startId);

        readyCamera();

        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public void onDestroy() {
        try {
            session.abortCaptures();
        } catch (CameraAccessException e){
            Log.error(e.getMessage());
        }
        session.close();
    }



    private Bitmap getBitMapFromImageObject(Image input)  {
        ByteBuffer buffer = input.getPlanes()[0].getBuffer();
        Log.info(String.valueOf("ByteBuffer buffer capacity = " +  buffer.capacity()));
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        Bitmap myBitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length,null);
        return myBitmap;
    }


    // convert from bitmap to byte array
    private byte[] getBytesFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream);
        return stream.toByteArray();
    }


    protected CaptureRequest createCaptureRequest() {
        try {
            CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            builder.addTarget(imageReader.getSurface());
            return builder.build();
        } catch (CameraAccessException e) {
            Log.error(e.getMessage());
            return null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
