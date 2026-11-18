Provenance and attribution
==========================

This repository is a *reconstruction*: its Java sources were produced by decompiling a
third-party APK (`HyperControl_1.4.8.apk`, package `com.cinexaft.hypercontrol`) that was supplied
to the maintainer as a binary. It is not the original author's source code.

Upstream
--------

- Original package: `com.cinexaft.hypercontrol` ("HyperControl"), version 1.4.8 / versionCode
  10408, targetSdk 37, distributed as an APK only.
- No public source repository, and no license file, was found for that package. The name
  `com.cinexaft` is the only author identifier available; it has not been contacted.
- The APK itself is not redistributed in this repository (see `.gitignore`, `*.apk`). Only its
  SHA-256 is recorded in `reference/APK-SHA256.txt`.

What that means for reuse
-------------------------

Decompiling software to study or to fix it locally is one thing; republishing the result under a
license of your own is another. As the upstream code carries no license grant, this repository
does **not** assert one. `LICENSE-STATUS.md` tracks the question, and no license file is added
until it is answered — either by the original author granting permission, or by the maintainer
deciding to publish only their own changes rather than the whole reconstruction.

Third-party components (these do have licenses, and they are respected here)
---------------------------------------------------------------------------

- DexKit (`org.luckypray:dexkit`) — LGPL-3.0 / Apache-2.0, © LuckyPray. Shipped unmodified as a
  Maven dependency; its native library is packaged as-is.
- libxposed API (`io.github.libxposed:api`) — Apache-2.0. Compile-only, supplied at runtime by
  the framework.
- AndroidX (appcompat, fragment, preference) — Apache-2.0. Compile-only, supplied at runtime by
  `com.android.settings`.
- Class names and behaviour described in `SOURCE_RECONSTRUCTION.md` derive from the APK above.

If you are the original author and want this taken down, changed, or relicensed, open an issue.
