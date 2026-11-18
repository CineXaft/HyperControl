# Source reconstruction report

- Input: the HyperControl 1.4.8 APK as published.
- SHA-256: `b48b48b400af6533481901e79d9ff89fc257043b4e2a20116d04aad958358a8d`.
- DEX: one `classes.dex`; APK contained 58 recovered `.java` source files.
- JADX: 1.5.6, invoked with `--show-bad-code --no-res`.
- Apktool: 3.0.3; AndroidManifest, resource XML, assets and `META-INF/xposed` decoded.
- No APK signatures/private signing key are part of the source project.

The output is a decompilation-based reconstruction of the author's own build: a clean Gradle build does not reproduce the binary byte-for-byte, and the decompiled Java is not bytecode-identical to the original sources. Device-private APIs are represented in the `device-stubs` module only for compile-time use.
