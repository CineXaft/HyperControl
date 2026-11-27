# HyperControl - R8 configuration
# Preserve the exact externally resolved contracts, not every implementation member.
# These class names are read by the Settings host, Xposed metadata or Android manifest.

# FragmentFactory resolves this class by name and constructs it reflectively.
# Preserve lifecycle methods invoked by Settings/AndroidX, including the private host API.
-keep,allowoptimization class com.cinexaft.hypercontrol.base.NativeScreen {
    public <init>();
    public void onCreateOptionsMenu(android.view.Menu, android.view.MenuInflater);
    public int getMetricsCategory();
    public void onCreatePreferences(android.os.Bundle, java.lang.String);
    public boolean onPreferenceTreeClick(androidx.preference.PreferenceScreen, androidx.preference.Preference);
    public void onStart();
}

# SettingsHook.loadScreen() resolves this exact static method by reflection.
-keep,allowoptimization class com.cinexaft.hypercontrol.base.PageBridge {
    public static void show(android.app.Activity, android.os.Bundle, java.lang.CharSequence);
}

# META-INF/xposed/java_init.list names the module; the framework invokes these callbacks.
-keep,allowoptimization class com.cinexaft.hypercontrol.base.Entry {
    public <init>();
    public void onModuleLoaded(io.github.libxposed.api.XposedModuleInterface$ModuleLoadedParam);
    public void onPackageLoaded(io.github.libxposed.api.XposedModuleInterface$PackageLoadedParam);
    public void onPackageReady(io.github.libxposed.api.XposedModuleInterface$PackageReadyParam);
    public void onSystemServerStarting(io.github.libxposed.api.XposedModuleInterface$SystemServerStartingParam);
}

# Android instantiates the manifest provider and invokes its ContentProvider callbacks.
-keep,allowoptimization class com.cinexaft.hypercontrol.base.RestartProvider {
    public <init>();
    public boolean onCreate();
    public android.os.Bundle call(java.lang.String, java.lang.String, android.os.Bundle);
    public android.database.Cursor query(android.net.Uri, java.lang.String[], java.lang.String, java.lang.String[], java.lang.String);
    public java.lang.String getType(android.net.Uri);
    public android.net.Uri insert(android.net.Uri, android.content.ContentValues);
    public int delete(android.net.Uri, java.lang.String, java.lang.String[]);
    public int update(android.net.Uri, android.content.ContentValues, java.lang.String, java.lang.String[]);
}

# Keep the Hooker callback contract. Do not relax JNI contracts to save space.
-keepclassmembers class * implements io.github.libxposed.api.XposedInterface$Hooker {
    java.lang.Object intercept(io.github.libxposed.api.XposedInterface$Chain);
}
-keepclasseswithmembers,includedescriptorclasses class org.luckypray.dexkit.** {
    native <methods>;
}

# WechatPad compares a host coroutine parameter against this exact type name.
-keepnames interface kotlin.coroutines.Continuation

# No runtime code reads kotlin.Metadata. Let R8 remove it instead of pinning it.
# The default proguard-android-optimize.txt already preserves runtime annotations,
# Signature, InnerClasses and EnclosingMethod; do not add a blanket *Annotation* rule.
-dontwarn kotlin.**
-dontwarn org.jetbrains.annotations.**
