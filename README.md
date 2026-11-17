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

Verified on an ARM64 Linux host with JDK 21, Android SDK Platform 35 and Gradle 8.11.1:

```bash
JAVA_HOME=<jdk17+> ANDROID_HOME=<sdk> ./gradlew \
  -Pandroid.aapt2FromMavenOverride=<aapt2 wrapper> :app:assembleDebug
```

`BUILD SUCCESSFUL` - the debug APK contains all 58 classes from the original DEX (plus
Gradle-generated `R` classes), and its Xposed metadata, assets and manifest components match the
supplied APK. It is debug-signed, so it cannot update the published module.

The ARM64 host needed a QEMU wrapper for Google's x86_64 AAPT2; see `bin/aapt2-qemu` and
`-Pandroid.aapt2FromMavenOverride=`. On an x86_64 host none of that is required.

## Open in Android Studio

1. Open this folder as a Gradle project.
2. Use JDK 17 or newer and install Android SDK Platform 35 (or the platform required by the build).
3. Let Android Studio download the Gradle and Maven dependencies.
4. Select the `app` configuration to compile/debug.

The APK metadata specifies target SDK 37 while the initial project compile SDK is 35. The supplied APK was produced with a newer SDK than the recovered local SDK; a release build should use the matching SDK/AGP combo rather than quietly changing target behavior.

`device-stubs` provides compile-only stand-ins for private Android/MIUI classes (SettingsPreferenceFragment, MIUI SettingsLib preferences, hidden framework APIs and MIUI AlertDialog). These are only for editor/compiler resolution. The real OS supplies those classes on the supported Xiaomi/Redmi ROMs. **Never package or ship the stubs as runtime code.**

## Dex layout and R8

`assembleDebug` produces **six** `classes*.dex` files. That is normal AGP behaviour, not a
packaging bug: without minification the project classes are dexed in incremental buckets and the
debug pipeline keeps the buckets separate (`classes.dex`, `classes2.dex`, ...) so rebuilds stay
fast. The total is still only ~150 KB.

`assembleRelease` runs R8 (`isMinifyEnabled = true`) and emits a **single** `classes.dex`:

| build | dex files | APK |
|---|---|---|
| `assembleDebug` (no R8) | 6 | 178 KB, debug-signed |
| `assembleRelease` (R8) | 1 | 117 KB, unsigned |

`isShrinkResources` is deliberately `false`: every UI string is looked up at runtime with
`Resources.getIdentifier(name, "string", "dev.local.hypercontrol")`, so resource shrinking would
delete strings the module still asks for by name.

R8 renames most classes (`a.a`, `b.c`, ...), but a few names are load-bearing and are pinned in
`app/proguard-rules.pro`:

- `dev.local.hypercontrol.Entry` — looked up by name from `META-INF/xposed/java_init.list`.
- `dev.local.hypercontrol.ui.NativeScreen` and `ui.PageBridge` — instantiated/loaded from string
  literals (`":settings:show_fragment"`, `Class.forName`).
- `dev.local.hypercontrol.core.{R,Config,Audit,Boot,NHook,Shared,Recovery}` — verified by name in
  `Engine.verifyLinkedClasses()`.
- Manifest components (`RestartProvider`, flasher `KernelProvider`/`FlashService`).
- The `intercept(...)` member of every `XposedInterface.Hooker`: the interface comes from the
  LSPosed API as a *library* type, so its method reference cannot be rewritten. The classes
  themselves are still renamed.

The release APK is unsigned. Sign it with your own key; it can never be a signature-compatible
update for the published module unless the original key is used.

## Recovery fidelity

Java files under `app/src/main/java` were decompiled from DEX using JADX 1.5.6. This is readable recovered code, not the original authored source. JADX may reconstruct control flow imperfectly; names belonging to platform code may also be obfuscated. Keep the original smali/APK as audit reference when modifying hooks. APK metadata/assets/resources were decoded with Apktool 3.0.3.

A project that opens in Android Studio is not automatically a behavior-identical rebuild. The original APK's private signing key was not supplied; a new build cannot be installed as a signature-compatible update unless the owner's key is available. Verify the merged manifest, packaged Xposed metadata, resources, providers, flash service and target package IDs for every release.

## GitHub publication

No license has been chosen. Before making a public repository, confirm you have redistribution rights and select a license. Do not assume a license from the fact that an APK was provided. The original APK is ignored by Git (`*.apk`) and should not be committed.

The module contains system-server hooks, package verification modifications, Security Center hooks and a root kernel flasher. Review changes and test on a recoverable test device before release.
