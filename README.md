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

## Dex layout, R8 and compression

Two different things were behind the earlier "six dex" and "128 KB":

1. **Dex count.** `assembleDebug` dexes project classes in incremental buckets, so it emits six
   `classes*.dex` files. `assembleRelease` runs R8 and emits exactly **one** `classes.dex`.
2. **Dex compression.** AGP 8 stores `classes.dex` uncompressed (fast install, bigger file). The
   original module shipped it deflated, so `packaging { dex { useLegacyPackaging = true } }` is set
   here to match. `resources.arsc` stays `Stored` — Android 11+ (API 30) requires it uncompressed
   and 4-byte aligned.

Measured on this project:

| build | dex files | classes.dex in APK | APK |
|---|---|---|---|
| `assembleDebug` | 6 | stored | 178 KB, debug-signed |
| `assembleRelease` before compression | 1 | 91,376 B stored | 119,291 B |
| `assembleRelease` final (R8 + deflated dex) | 1 | 43,369 B deflated | 71,283 B → **79,032 B signed** |
| supplied original APK | 1 | 40,615 B deflated | 74,748 B |

`isShrinkResources = true` is enabled, and `app/src/main/res/raw/keep.xml` keeps `@string/*`
because every UI string is resolved at runtime through
`Resources.getIdentifier(name, "string", "dev.local.hypercontrol")`. Verified after the build:
all 100 default and 98 `zh` string names still exist, with none missing and none extra.

R8 renames most classes (`a.a`, `b.c`, ...) but these names are load-bearing and pinned in
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

## Signing

Release output is unsigned. The supplied APK is signed with the public AOSP test key
(`SHA-256 a40da80a59d170caa950cf15c18c454d47a39b26989d8b640ecd745ba71bf5dc`), so a rebuild can be
signed with the same key and still be signature-compatible:

```bash
apksigner sign --key testkey.pk8 --cert testkey.x509.pem \
  --out HyperControl_1.4.8-signed.apk app-release-unsigned.apk
```

The resulting APK verifies with APK Signature Scheme v3 (the original is v3-only as well, since
minSdk is 35). Its certificate digest matches the supplied APK.

## Recovery fidelity

Java files under `app/src/main/java` were decompiled from DEX using JADX 1.5.6. This is readable recovered code, not the original authored source. JADX may reconstruct control flow imperfectly; names belonging to platform code may also be obfuscated. Keep the original smali/APK as audit reference when modifying hooks. APK metadata/assets/resources were decoded with Apktool 3.0.3.

A project that opens in Android Studio is not automatically a behavior-identical rebuild. The original APK's private signing key was not supplied; a new build cannot be installed as a signature-compatible update unless the owner's key is available. Verify the merged manifest, packaged Xposed metadata, resources, providers, flash service and target package IDs for every release.

## GitHub publication

No license has been chosen. Before making a public repository, confirm you have redistribution rights and select a license. Do not assume a license from the fact that an APK was provided. The original APK is ignored by Git (`*.apk`) and should not be committed.

The module contains system-server hooks, package verification modifications, Security Center hooks and a root kernel flasher. Review changes and test on a recoverable test device before release.
