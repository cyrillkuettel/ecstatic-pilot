package li.garteroboter.pren.preferences.bundle;

import androidx.annotation.NonNull;

/**
 * this is a simple Dataclass
 * Store all the Settings. Main purpose: for testing 
 */
public final class CustomSettingsBundle implements SettingsBundle {

    private  int confirmations;
    private  boolean usingBluetooth;
    private  boolean showFPS;
    private  float prob_threshold = 0.4f;
    private  boolean switchToQr;

    public CustomSettingsBundle(boolean usingBluetooth, boolean showFPS, float prob_threshold, boolean switchToQr, int confirmations) {
        this.usingBluetooth = usingBluetooth;
        this.showFPS = showFPS;
        this.prob_threshold = prob_threshold;
        this.switchToQr = switchToQr;
        this.confirmations = confirmations;
    }
    


    @Override
    public boolean isUsingBluetooth() {
        return usingBluetooth;
    }

    @Override
    public boolean isShowFPS() {
        return showFPS;
    }

    @Override
    public float getProb_threshold() {
       return prob_threshold;
    }

    @Override
    public int getConfirmations() {
        return confirmations;
    }

    public void setUsingBluetooth(boolean usingBluetooth) {
        this.usingBluetooth = usingBluetooth;
    }

    public void setShowFPS(boolean showFPS) {
        this.showFPS = showFPS;
    }

    @NonNull
    @Override
    public String toString() {
        return "CustomSettingsBundle{" +
                "usingBluetooth=" + usingBluetooth +
                ", showFPS=" + showFPS +
                '}';
    }

    public boolean isSwitchToQr() {
        return switchToQr;
    }

}
