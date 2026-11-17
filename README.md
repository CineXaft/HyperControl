# HyperControl 1.7.0 — recovered Android Studio project

This is an Android Studio/Gradle project reconstructed from the APK supplied by the user (`HyperControl_1.4.8.apk`). It turns the DEX back into readable Java and restores the APK resources/assets and LSPosed metadata into the usual project layout.

## Identity and version

- Application ID, Java namespace and resource package: `com.cinexaft.hypercontrol`
- LSPosed entry: `com.cinexaft.hypercontrol.Entry` (see `META-INF/xposed/java_init.list`)
- Current build: `1.7.0`, versionCode `10700` (reconstructed from `1.4.8`, versionCode `10408`)
- Source APK SHA-256: recorded in [`reference/APK-SHA256.txt`](reference/APK-SHA256.txt)
- LSPosed metadata/scope: `app/src/main/resources/META-INF/xposed/`

The supplied 1.4.8 APK shipped its Java code under `dev.local.hypercontrol` while its application
ID was already `com.cinexaft.hypercontrol`. This reconstruction unifies both on
`com.cinexaft.hypercontrol`, including the manifest component names, the Xposed entry point and
the resource package that `Strings.get()` resolves by name.

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

## What this build changed

1.4.9 drops the Security Center feature. Its three hooks (score zeroing, permission-interception
bypass, ADB-verify bypass) depended on private Security Center internals that change between
releases, and could not be kept working across versions, so the whole `securitycenter` package,
its settings page and its strings are gone. `com.miui.securitycenter` stays in `scope.list` only
because the passkey feature still needs `Settings.Secure` writes from that process.

Also removed as dead weight:

- `core.R` carried 100 hard-coded resource id constants copied out of the resource table; nothing
  referenced them (the flasher uses `android.R`), so the whole inner class is gone.
- The Security Center strings, their `public.xml` ids, and the catalog row/route that opened them.

Shared plumbing now lives in `Hooks`:

| helper | replaces |
|---|---|
| `byNameCount(Class, name, count)` | `ClockExpand.m()`, per-class ad-hoc lookup loops |
| `bySignature(loader, class, name, params, returnType)` | `Runtime.hook()`'s load/lookup/verify block |
| `install(method, hooker[, tag])` | `ClockExpand.h()` and repeated deopt+hook+log triples |
| `replaceArg(args, index, value)` | hand-built `new Object[]{...}` arrays in `ClockHook` (one of which hard-coded `new Object[5]`) |

Hook overhead work in the hot paths:

- `Audit.hit()` was `synchronized`, so every interception inside PackageManagerService took a
  global monitor. It now uses `AtomicIntegerArray` and keeps the same power-of-two logging.
- `NHook.intercept()` no longer reads a `ThreadLocal` on every call. `Engine.guarding` is a plain
  volatile int that is zero unless some thread is inside `Shared.capable()`, so the common path is
  one int compare. The ThreadLocal is still consulted when the counter is non-zero, so the
  re-entrancy semantics are unchanged.
- `Runtime.install()`'s five-way boolean OR chain became a `handled()` predicate plus a switch;
  a signature mismatch now skips one hook with a warning instead of aborting the rest.

### 1.5.0 — status bar icon swap

`SwapHook` adds the "Swap mobile and Wi-Fi icons" switch under *System UI*. It does not touch
layout code: HyperOS lays the icon container out from the right edge in reverse child order, so
the child order *is* the visual order and exchanging the two children is enough. The exchange is
posted after the container reports a new child (its `onViewAdded`), which is the only point where
both views are guaranteed to exist, and it is idempotent — after the first swap the check sees
Wi-Fi at the lower index and does nothing. A firmware that renames or drops the container simply
logs a warning and leaves the status bar alone.

### 1.6.0 — DexKit and the focus-auth unlock

`com.xiaomi.xmsf` is now a hook target, and its one switch needs a DEX index: the auth module that
ships with this ROM is fully renamed by R8 (`com.xiaomi.xms.auth.a … n`), so no class or method name
from the original module survives. `Dex.java` owns the index — it unpacks `libdexkit.so` from this
APK into the host's code cache once per process, loads it by absolute path, and keeps a
`DexKitBridge` per process. Nothing is loaded unless the switch is already on when the process
starts, which is why changing it needs a restart of the service process.

`XmsAuth.java` then looks for the session class structurally: the class whose methods reference the
literal `"Auth is successful"`, and inside it the zero-argument and one-argument methods that both
return a `Bundle`. Intercepting the one-argument method and returning the zero-argument result makes
a failed authentication report success. The error object is only read (for the log line) — nothing
downstream sees it once the result is replaced. If the success builder throws (a session that never
reached a usable state), the hook falls back to the original error instead of converting it into a
crash.

Verified on the target device by running the module's exact query through `app_process` against the
installed `auth-master.apk`: the search returns `com.xiaomi.xms.auth.i` with `b(int param)`,
`c(three params, ignored)` and `h()`, i.e. the error builder, an unrelated helper and the success
builder. The same run used the R8-minified release APK as its classpath, so the keep rules for
DexKit, flatbuffers and the Kotlin stdlib are proven sufficient.

Cost of the index, measured: the APK goes from 79 KB to 350 KB (dex 89 KB -> 281 KB plus a 395 KB
arm64 native engine, both deflated), and the index itself takes ~30 ms to build over three dex
files. The dex side is dominated by DexKit and the Kotlin runtime it is written in, not by this
module's own code (which stayed at ~90 KB).

The first version of these keep rules pinned `org.luckypray.dexkit.**` with `-keep`, which left all
648 classes in place - 451 of them DexKit's query DSL that this module never builds. `-keepnames`
(`-keep,allowshrinking`) keeps every *name* while letting R8 delete the unreachable classes, which
is what JNI and DexKit's own reflection actually require:

| rule | why |
|---|---|
| `-keepnames org.luckypray.dexkit.**` | the .so exports `Java_org_luckypray_dexkit_DexKitBridge_nativeXxx`, and DexKit's helpers walk their own data classes by reflection |
| `-keepnames com.google.flatbuffers.**` | same, for the schema readers behind the query byte arrays |
| `-keepclasseswithmembernames` for `native` | belt and braces for the JNI entry points |

Result: 648 -> 274 classes, dex 887 KB -> 281 KB, APK 514 KB -> 350 KB, with no behavioural
difference - verified by rerunning the three queries against the *shrunk* dex through
`app_process`, which is the same code path the module takes at runtime.

### 1.7.0 — Security Center, this time found by structure

The three Security Center features are back, but nothing about them is pinned to a release any
more. The 1.4.x implementation hard-coded `ScoreManager.q()/o()`, scraped a `Log.d` message to
discover the score method, hooked every `String.equals` in the process, and guessed at
`InterceptBaseFragment`'s `protected ()int` methods; all four guesses are wrong on this ROM
(`ScoreManager.o()` is still private, `f0()` became the layout id, and the ADB activity now has a
device/region early exit). `securitycenter/SecurityCenter.java` re-derives all of it:

| switch | how the target is found | what it does |
|---|---|---|
| Zero predicted deduction | the method that uses the literal `getMinusPredictScore` | returns 0, so `100 - clamp(...)` stays at 100 |
| Skip interception countdown | every `protected`, non-abstract, no-argument `int` method in the `InterceptBaseFragment` hierarchy | returns 0, so the dialog button is enabled immediately |
| Skip ADB install verification | the `AdbInstallVerifyActivity` subclass, its `onCreate(Bundle)` | calls the parent `onCreate` and `finish()`, skipping the account and network round trip |

Two details worth keeping in mind if these are ever touched again:

- The layout id is also an `int`, so the `protected` filter is what separates `c0()` (countdown,
  protected) from `f0()` (layout, public). Dropping that filter inflates the wrong resource.
- The name filter also matches `InterceptBaseFragment$a/$b` and `AdbInstallVerifyActivity$a/$b`, so
  inner classes are skipped explicitly.

The old `String.equals` hook is gone: the ADB activity is now left entirely, which is both less
invasive and cheaper, since no string comparison in the Security Center process pays for it.

Verified by running these exact queries through `app_process` against the installed
`MIUISecurityCenter.apk` (11.1.0, 3 dex, 61 MB): score resolves to `ScoreManager.o() ()int` in
31 ms, the fragment search returns 5 classes of which `c0()` is `protected` (0x4) and `f0()` is
`public`/`abstract` (0x401/0x1), and the ADB class exposes `onCreate(Bundle)`. The index is only
built when at least one of the three switches is on, and it is shared with the XMS feature.

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

R8 renames 37 of the 43 remaining classes (`a.a`, `b.c`, ...). Six keep their original names
because the name itself is the contract, and none of those can be rewritten by R8:

| kept name | why |
|---|---|
| `...hypercontrol.Entry` | listed in `META-INF/xposed/java_init.list`, a resource file R8 cannot edit |
| `...ui.NativeScreen` | passed to Settings as a fragment-name string; `Hook` compares and resolves it |
| `...ui.PageBridge` | loaded via `ClassLoader.loadClass("...ui.PageBridge")` |
| `...RestartProvider` | declared in `AndroidManifest.xml` |
| `...flasher.KernelProvider` | declared in `AndroidManifest.xml` |
| `...flasher.FlashService` | declared in `AndroidManifest.xml` |

`-adaptclassstrings` was tried and rejected: R8 left the class-name literals untouched while still
renaming the classes, which would have made `loadClass` fail at runtime. The rules file documents
that finding.

`core.*` no longer needs pinning because it is no longer looked up by name — the old
`Engine.verifyLinkedClasses()` built class names by string concatenation, which no obfuscator can
follow; it now iterates real `Class` objects, so R8 can rename them.

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
