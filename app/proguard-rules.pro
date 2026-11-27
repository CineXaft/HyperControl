# HyperControl - R8 configuration
#
# NOTE: R8 exposes -adaptclassstrings, but it does NOT rewrite the module's own class-name
# literals here (verified: after enabling it the literals stayed unchanged while the classes were
# renamed, which would break loadClass at runtime). Those two classes are therefore pinned by name
# instead. Everything else is obfuscated.

# Settings resolves this fragment through a name string and SettingsHook returns the
# class object, so the literal "com.cinexaft.hypercontrol.base.NativeScreen" must stay resolvable.
-keep class com.cinexaft.hypercontrol.base.NativeScreen { *; }

# Loaded through ClassLoader.loadClass("com.cinexaft.hypercontrol.base.PageBridge").
-keep class com.cinexaft.hypercontrol.base.PageBridge { *; }

# LSPosed reads the entry class name from META-INF/xposed/java_init.list, a resource file that
# R8 cannot rewrite. This one name must survive obfuscation.
-keep class com.cinexaft.hypercontrol.base.Entry { *; }

# Components referenced from AndroidManifest.xml. The manifest is binary XML produced by AAPT2
# and is not rewritten by R8, so these names must also survive.
-keep class com.cinexaft.hypercontrol.base.RestartProvider { *; }

# XposedInterface$Hooker is a *library* interface, so R8 already cannot rename intercept().
# Stating it explicitly documents the contract and guards against a future API change.
-keepclassmembers class * implements io.github.libxposed.api.XposedInterface$Hooker {
    java.lang.Object intercept(io.github.libxposed.api.XposedInterface$Chain);
}

# LSPosed uses annotations/enclosing metadata when reporting hook failures.
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature


# Keep JNI entry points from being renamed/removed; allow DexKit and FlatBuffers classes
# themselves to shrink and obfuscate. DexKit 2.3.0 uses serialized/native IDs, not Java names.
-keepclasseswithmembers,includedescriptorclasses class org.luckypray.dexkit.** {
    native <methods>;
}
# TabletHook compares this host parameter type through Continuation::class.java.name.
-keepnames interface kotlin.coroutines.Continuation
-keep class kotlin.Metadata { *; }
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-dontwarn kotlin.**
-dontwarn org.jetbrains.annotations.**
