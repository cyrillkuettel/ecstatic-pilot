package li.garteroboter.pren.qrcodencnn.image;

import android.view.SurfaceView;

/**
 * I'm using an interface to represent an area of methods that are copying image data from the
 * SurfaceView. It's possible that I will switch out the implementation in the future.
 * The ImageProcessor mainly does two things.
 * 1.) Get Access to the Image on the SurfaceView
 * 2.) Start the copying Process, with an attached Listener
 */
public interface ImageProcessor {

    void startCopyOnLockAcquired(SurfaceView view);

    void copyBitmapAndAttachListener(SurfaceView view, PostTake callback);

    void setHasPermissionToSave(boolean b);

    void start();
}
