# HyperControl — R8 keep rules
#
# The module is injected into other processes, so several classes are looked up by NAME at
# runtime (LSPosed entry point, Settings fragment host, dynamic class loader, Class.forName).
# Renaming them silently breaks the module, so they must keep their original names.

# LSPosed entry point (META-INF/xposed/java_init.list looks this name up)
-keep class dev.local.hypercontrol.Entry { *; }

# Instantiated by the Settings framework from a ":settings:show_fragment" string
-keep class dev.local.hypercontrol.ui.NativeScreen { *; }

# Loaded with Class.forName/loadClass and invoked reflectively (method "show")
-keep class dev.local.hypercontrol.ui.PageBridge { *; }

# Verified by name in Engine.verifyLinkedClasses()
-keep class dev.local.hypercontrol.core.R { *; }
-keep class dev.local.hypercontrol.core.Config { *; }
-keep class dev.local.hypercontrol.core.Audit { *; }
-keep class dev.local.hypercontrol.core.Boot { *; }
-keep class dev.local.hypercontrol.core.NHook { *; }
-keep class dev.local.hypercontrol.core.Shared { *; }
-keep class dev.local.hypercontrol.core.Recovery { *; }

# Declared in AndroidManifest.xml (AGP normally keeps these; explicit for clarity)
-keep class dev.local.hypercontrol.RestartProvider { *; }
-keep class dev.local.hypercontrol.flasher.KernelProvider { *; }
-keep class dev.local.hypercontrol.flasher.FlashService { *; }

# XposedInterface$Hooker is a *library* type from the LSPosed API, so R8 cannot rename the
# interface method. Keep the implementing member name, but allow the class itself to be renamed.
-keepclassmembers class * implements io.github.libxposed.api.XposedInterface$Hooker {
    java.lang.Object intercept(io.github.libxposed.api.XposedInterface$Chain);
}

# Keep annotations/enclosing info that LSPosed uses when reporting hook errors.
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature
