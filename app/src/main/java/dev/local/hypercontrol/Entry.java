package dev.local.hypercontrol;

import android.os.Bundle;
import android.util.Log;
import dev.local.hypercontrol.core.Engine;
import dev.local.hypercontrol.passkey.Installer;
import dev.local.hypercontrol.passkey.Policy;
import dev.local.hypercontrol.secure.FlagSecure;
import dev.local.hypercontrol.securitycenter.SecurityCenter;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class Entry extends XposedModule {
    public static String apk;
    private static boolean installed;
    public static Entry module;

    public synchronized void onPackageLoaded(XposedModuleInterface.PackageLoadedParam packageLoadedParam) {
        module = this;
        String packageName = packageLoadedParam.getPackageName();
        ClassLoader defaultClassLoader = packageLoadedParam.getDefaultClassLoader();
        if (!"com.android.settings".equals(packageName)) {
            try {
                if ("com.miui.securitycenter".equals(packageName)) {
                    SecurityCenter.install(defaultClassLoader);
                }
                Runtime.install(packageName, defaultClassLoader);
                return;
            } catch (Throwable th) {
                Reflect.log(th);
                return;
            }
        }
        if (installed) {
            return;
        }
        installed = true;
        try {
            Hook.host = defaultClassLoader;
            Reflect.hook(defaultClassLoader.loadClass("com.android.settings.MiuiSettings").getDeclaredMethod("updateHeaderList", List.class), new Hook(0));
            Reflect.hook(defaultClassLoader.loadClass("com.android.settings.SettingsActivity").getDeclaredMethod("switchToFragment", String.class, Bundle.class, Boolean.TYPE, Integer.TYPE, CharSequence.class), new Hook(1));
            Reflect.hook(defaultClassLoader.loadClass("androidx.fragment.app.FragmentFactory").getDeclaredMethod("loadFragmentClass", ClassLoader.class, String.class), new Hook(2));
        } catch (Throwable th2) {
            Reflect.log(th2);
        }
    }

    public void onPackageReady(XposedModuleInterface.PackageReadyParam packageReadyParam) {
        if (packageReadyParam.isFirstPackage()) {
            try {
                module = this;
                Installer.packages(this, packageReadyParam.getPackageName(), packageReadyParam.getClassLoader());
            } catch (Throwable th) {
                Policy.warn("Passkey package hooks unavailable", th);
            }
        }
    }

    public void onSystemServerStarting(XposedModuleInterface.SystemServerStartingParam systemServerStartingParam) {
        module = this;
        ClassLoader classLoader = systemServerStartingParam.getClassLoader();
        try {
            Installer.server(this, classLoader);
        } catch (Throwable th) {
            Policy.warn("Passkey system hooks unavailable", th);
        }
        Log.i("HyperControl.Core", "ENTRY system_server");
        try {
            Engine.install(classLoader);
        } catch (Throwable th2) {
            Log.e("HyperControl.Core", "ENTRY core installation aborted", th2);
        }
        try {
            FlagSecure.install(classLoader);
        } catch (Throwable th3) {
            Reflect.log(th3);
        }
        try {
            SafeVolume.install(classLoader);
        } catch (Throwable th4) {
            Log.e("HyperControl.Volume", "ENTRY volume installation aborted", th4);
        }
    }
}
