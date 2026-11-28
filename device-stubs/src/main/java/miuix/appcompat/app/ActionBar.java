package miuix.appcompat.app;

import android.view.View;

/** Compile-only MIUI ActionBar API shim. The device runtime provides the actual class. */
public class ActionBar {
    public void setEndView(View view) { }
    public void addActionBarTransitionListener(ActionBarTransitionListener listener) { }
    public void removeActionBarTransitionListener(ActionBarTransitionListener listener) { }
}
