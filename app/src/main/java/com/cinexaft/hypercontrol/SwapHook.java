package com.cinexaft.hypercontrol;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import io.github.libxposed.api.XposedInterface;

/**
 * Swaps the status bar positions of the Wi-Fi and mobile signal icons.
 *
 * <p>The icon container lays its children out from the right edge in reverse order, so the child
 * order <em>is</em> the visual order: index 0 is the left-most icon. Exchanging the two views
 * inside that container therefore swaps them without touching layout code, and it keeps working
 * even if a firmware changes the icon sizes, the drawables or the spacing.
 *
 * <p>The check runs after the container reported a new child, because that is the only moment the
 * two views can both be located. It is posted instead of running inside {@code addView}, so the
 * container finishes its own bookkeeping first, and it is idempotent: once Wi-Fi sits left of the
 * mobile icon, further calls do nothing.
 */
public final class SwapHook implements XposedInterface.Hooker {

    /** The container that owns both icons; its class is also the hook target. */
    private static final String CONTAINER =
            "com.android.systemui.statusbar.views.MiuiStatusIconContainer";
    private static final String WIFI = "wifi_combo";
    private static final String MOBILE = "mobile_combo";

    /** Cached ids of wifi_combo and mobile_combo; 0 means "not looked up yet". */
    private static final int[] ids = new int[2];

    private static Class<?> container;

    public static void install(ClassLoader classLoader) {
        try {
            container = classLoader.loadClass(CONTAINER);
        } catch (Throwable error) {
            Log.w(Hooks.TAG, "no icon container, icon swap unavailable");
            return;
        }
        Hooks.install(Hooks.byNameCount(container, "onViewAdded", 1), new SwapHook());
    }

    /** Returns the child index of a view id, or -1 when the container has no such child. */
    private static int indexOf(ViewGroup parent, int id) {
        if (id <= 0) {
            return -1;
        }
        for (int i = 0; i < parent.getChildCount(); i++) {
            if (parent.getChildAt(i).getId() == id) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Puts the Wi-Fi view left of the mobile view by exchanging their two slots.
     *
     * <p>The firmware lists mobile before wifi, so mobile already has the lower index; exchanging
     * the slots keeps any icon that happens to sit between them in place. Only that one order is
     * enforced, which makes a repeated call a no-op.
     */
    private static void order(ViewGroup parent) {
        int wifi = indexOf(parent, Platform.id(parent, WIFI, ids, 0));
        int mobile = indexOf(parent, Platform.id(parent, MOBILE, ids, 1));
        if (wifi < 0 || mobile < 0 || wifi < mobile) {
            return;
        }
        View wifiView = parent.getChildAt(wifi);
        View mobileView = parent.getChildAt(mobile);
        parent.removeView(wifiView);
        parent.removeView(mobileView);
        parent.addView(wifiView, mobile);
        parent.addView(mobileView, wifi);
        Log.i(Hooks.TAG, "swapped wifi and mobile icons");
    }

    public Object intercept(XposedInterface.Chain chain) throws Throwable {
        Object result = chain.proceed();
        if (container == null || !State.enabled(12)) {
            return result;
        }
        Object host = chain.getThisObject();
        if (!(host instanceof ViewGroup) || !container.isInstance(host)) {
            return result;
        }
        final ViewGroup parent = (ViewGroup) host;
        parent.post(new Runnable() {
            public void run() {
                try {
                    order(parent);
                } catch (Throwable error) {
                    Reflect.log(error);
                }
            }
        });
        return result;
    }
}
