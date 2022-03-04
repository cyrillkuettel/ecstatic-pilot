package li.garteroboter.pren.settings.container;

/**
 * this is a simple Dataclass
 * Store all the Settings. Main purpose: for testing 
 */
public final class CustomSettingsBundle implements SettingsBundle {

    private boolean usingBluetooth;
    private boolean showFPS;
    
    public CustomSettingsBundle(boolean usingBluetooth, boolean showFPS) {
        usingBluetooth = usingBluetooth;
        showFPS = showFPS;
    }
    
    public CustomSettingsBundle() {}

    @Override
    public boolean isUsingBluetooth() {
        return usingBluetooth;
    }

    @Override
    public boolean isShowFPS() {
        return showFPS;
    }

    public void setUsingBluetooth(boolean usingBluetooth) {
        this.usingBluetooth = usingBluetooth;
    }

    public void setShowFPS(boolean showFPS) {
        this.showFPS = showFPS;
    }

    @Override
    public String toString() {
        return "CustomSettingsBundle{" +
                "usingBluetooth=" + usingBluetooth +
                ", showFPS=" + showFPS +
                '}';
    }
}
