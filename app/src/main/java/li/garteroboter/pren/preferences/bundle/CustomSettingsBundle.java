package li.garteroboter.pren.preferences.bundle;

import androidx.annotation.NonNull;

/**
 * this is a simple Dataclass
 * Store all the Settings. Main purpose: for testing 
 */
public final class CustomSettingsBundle  {

    private final int confirmations;
    private  boolean usingBluetooth;
    private  boolean showFPS;
    private final float prob_threshold;
    private final boolean switchToQr;
    private final int plantCount;

    public CustomSettingsBundle(boolean usingBluetooth, boolean showFPS, float prob_threshold, boolean switchToQr, int confirmations, int plantCount) {
        this.usingBluetooth = usingBluetooth;
        this.showFPS = showFPS;
        this.prob_threshold = prob_threshold;
        this.switchToQr = switchToQr;
        this.confirmations = confirmations;
        this.plantCount = plantCount;
    }
    



    public boolean isUsingBluetooth() {
        return usingBluetooth;
    }


    public boolean isShowFPS() {
        return showFPS;
    }

    public float getProb_threshold() {
       return prob_threshold;
    }


    public int getConfirmations() {
        return confirmations;
    }


    public int getPlantCount() {
        return plantCount;
    }

    public boolean isSwitchToQr() {
        return switchToQr;
    }


    @NonNull
    @Override
    public String toString() {
        return "CustomSettingsBundle{" +
                "usingBluetooth=" + usingBluetooth +
                ", showFPS=" + showFPS +
                '}';
    }



}
