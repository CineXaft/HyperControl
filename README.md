# HyperControl 1.4.8 — recovered Android Studio project

This is an Android Studio/Gradle project reconstructed from the APK supplied by the user (`HyperControl_1.4.8.apk`). It turns the DEX back into readable Java and restores the APK resources/assets and LSPosed metadata into the usual project layout.

## Identity and version

- APK application ID: `com.cinexaft.hypercontrol`
- Java namespace and Xposed entry: `dev.local.hypercontrol.Entry`
- APK version: `1.4.8`, versionCode `10408`
- Source APK SHA-256: recorded in [`reference/APK-SHA256.txt`](reference/APK-SHA256.txt)
- LSPosed metadata/scope: `app/src/main/resources/META-INF/xposed/`

The application ID and Java namespace intentionally differ, matching the supplied APK.

## Current build status

The project configuration and Java source tree are in place. On the ARM64 Linux environment used for recovery, Gradle configuration and resource processing start successfully, but the full Android Gradle build is not yet green: AGP's AAPT2 daemon expects a native executable, while the downloaded Google build-tools AAPT2 is x86_64 and must be run through QEMU. The project includes the SDK/build-tool setup and wrapper notes for continuing on an x86_64 development host or after configuring a native/transparent AAPT2 toolchain. This is an environment/toolchain limitation, not evidence that the recovered Java is equivalent to the original source.

The recovered project should therefore be treated as **Android Studio-readable and GitHub-ready**, not as a verified release-buildable replacement yet.


1. Open this folder as a Gradle project.
2. Use JDK 17 or newer and install Android SDK Platform 35 (or the platform required by the build).
3. Let Android Studio download the Gradle and Maven dependencies.
4. Select the `app` configuration to compile/debug.

The APK metadata specifies target SDK 37 while the initial project compile SDK is 35. The supplied APK was produced with a newer SDK than the recovered local SDK; a release build should use the matching SDK/AGP combo rather than quietly changing target behavior.

`device-stubs` provides compile-only stand-ins for private Android/MIUI classes (SettingsPreferenceFragment, MIUI SettingsLib preferences, hidden framework APIs and MIUI AlertDialog). These are only for editor/compiler resolution. The real OS supplies those classes on the supported Xiaomi/Redmi ROMs. **Never package or ship the stubs as runtime code.**

## Recovery fidelity

Java files under `app/src/main/java` were decompiled from DEX using JADX 1.5.6. This is readable recovered code, not the original authored source. JADX may reconstruct control flow imperfectly; names belonging to platform code may also be obfuscated. Keep the original smali/APK as audit reference when modifying hooks. APK metadata/assets/resources were decoded with Apktool 3.0.3.

A project that opens in Android Studio is not automatically a behavior-identical rebuild. The original APK's private signing key was not supplied; a new build cannot be installed as a signature-compatible update unless the owner's key is available. Verify the merged manifest, packaged Xposed metadata, resources, providers, flash service and target package IDs for every release.

## GitHub publication

No license has been chosen. Before making a public repository, confirm you have redistribution rights and select a license. Do not assume a license from the fact that an APK was provided. The original APK is ignored by Git (`*.apk`) and should not be committed.

The module contains system-server hooks, package verification modifications, Security Center hooks and a root kernel flasher. Review changes and test on a recoverable test device before release.
