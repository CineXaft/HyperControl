# HyperControl - R8 configuration
#
# NOTE: R8 exposes -adaptclassstrings, but it does NOT rewrite the module's own class-name
# literals here (verified: after enabling it the literals stayed unchanged while the classes were
# renamed, which would break loadClass at runtime). Those two classes are therefore pinned by name
# instead. Everything else is obfuscated.

# Settings resolves this fragment through a name string and Hook.loadFragmentClass() returns the
# class object, so the literal "com.cinexaft.hypercontrol.ui.NativeScreen" must stay resolvable.
-keep class com.cinexaft.hypercontrol.ui.NativeScreen { *; }

# Loaded through ClassLoader.loadClass("com.cinexaft.hypercontrol.ui.PageBridge").
-keep class com.cinexaft.hypercontrol.ui.PageBridge { *; }

# LSPosed reads the entry class name from META-INF/xposed/java_init.list, a resource file that
# R8 cannot rewrite. This one name must survive obfuscation.
-keep class com.cinexaft.hypercontrol.Entry { *; }

# Components referenced from AndroidManifest.xml. The manifest is binary XML produced by AAPT2
# and is not rewritten by R8, so these names must also survive.
-keep class com.cinexaft.hypercontrol.RestartProvider { *; }
-keep class com.cinexaft.hypercontrol.flasher.KernelProvider { *; }
-keep class com.cinexaft.hypercontrol.flasher.FlashService { *; }

# XposedInterface$Hooker is a *library* interface, so R8 already cannot rename intercept().
# Stating it explicitly documents the contract and guards against a future API change.
-keepclassmembers class * implements io.github.libxposed.api.XposedInterface$Hooker {
    java.lang.Object intercept(io.github.libxposed.api.XposedInterface$Chain);
}

# LSPosed uses annotations/enclosing metadata when reporting hook failures.
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature

# DexKit is a native parser driven from Java. Two different constraints apply, and they are not
# the same thing:
#
#  * the .so exports Java_org_luckypray_dexkit_DexKitBridge_nativeXxx, so that class name and those
#    native method names must survive verbatim - otherwise the first call is an UnsatisfiedLinkError;
#  * several of its own helpers walk their data classes by reflection, so names must stay readable.
#
# Neither of those prevents R8 from *shrinking* the library: the query DSL ships hundreds of
# matchers this module never builds, and they can go. -keepnames (== -keep,allowshrinking) keeps
# every name while still removing what is unreachable, which is exactly the combination needed.
-keepnames class org.luckypray.dexkit.** { *; }
-keepnames class com.google.flatbuffers.** { *; }
-keepclasseswithmembernames,includedescriptorclasses class * { native <methods>; }
# DexKit is written in Kotlin; the stdlib it links against must keep its annotations and
# metadata, and its own optional dependencies are not needed here.
-keep class kotlin.Metadata { *; }
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-dontwarn kotlin.**
-dontwarn org.jetbrains.annotations.**
