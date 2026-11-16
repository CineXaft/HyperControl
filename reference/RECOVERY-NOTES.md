# Recovery notes

- JADX output is stored as the starting Java source because the user described the module as not obfuscated.
- This is a reconstruction, not a source-to-source round trip.
- The original app ID is `com.cinexaft.hypercontrol`; Java and Xposed entry classes remain in `dev.local.hypercontrol`.
- Original Xposed metadata is under `app/src/main/resources/META-INF/xposed/`.
- Target-ROM Settings, SettingsLib, MIUI preferences, hidden framework APIs, and MIUI AlertDialog types are represented by compile-only shims in `device-stubs/`. Those stubs must never be bundled into a release APK.
- The supplied APK was not copied into this repository. Its SHA-256 is recorded in `APK-SHA256.txt`.
