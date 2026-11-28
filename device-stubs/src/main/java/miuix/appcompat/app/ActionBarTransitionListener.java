package miuix.appcompat.app;

/** Compile-only MIUIX transition contract; the Settings host provides the implementation. */
public interface ActionBarTransitionListener {
    void onActionBarMove(float from, float to);
    void onActionBarResizing(int state, float progress, int height);
    void onExpandStateChanged(int state);
    default void onOverlayMaskStateChanged(boolean overlayMask) { }
}
