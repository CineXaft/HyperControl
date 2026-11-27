package com.cinexaft.hypercontrol.hook.systemframework.core;

import com.cinexaft.hypercontrol.base.Entry;
import com.cinexaft.hypercontrol.base.SettingsHook.Reflect;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicInteger;

/** Signature-checked hook registration; attachment and deoptimization fail independently. */
public final class Engine {
    private static boolean installed;
    public static ClassLoader loader;
    public static final ThreadLocal<Boolean> origin = new ThreadLocal<>();
    // Avoid a ThreadLocal lookup on the common path outside Shared.capable().
    public static final AtomicInteger guarding = new AtomicInteger();
    private static final AtomicInteger successfulHooks = new AtomicInteger();
    private static final AtomicInteger failedHooks = new AtomicInteger();
    private static final AtomicInteger skippedHooks = new AtomicInteger();
    // feature | callback kind | owner | name | result | parameters | static/instance/any
    private static final String TARGETS = """
            -1|17|com.android.server.SystemServer|startOtherServices|void|com.android.server.utils.TimingsTraceAndSlog|I
            0|0|com.android.server.pm.PackageManagerServiceUtils|checkDowngrade|void|long,int,[Ljava.lang.String;,[I,android.content.pm.PackageInfoLite|S
            0|0|com.android.server.pm.PackageManagerServiceUtils|checkDowngrade|void|com.android.server.pm.PackageSetting,android.content.pm.PackageInfoLite|S
            0|0|com.android.server.pm.PackageManagerServiceUtils|checkDowngrade|void|com.android.server.pm.pkg.AndroidPackage,android.content.pm.PackageInfoLite|S
            4|2|com.android.server.pm.PackageManagerServiceUtils|verifySignatures|boolean|com.android.server.pm.PackageSetting,com.android.server.pm.SharedUserSetting,com.android.server.pm.PackageSetting,android.content.pm.SigningDetails,boolean,boolean,boolean|S
            4|3|android.util.apk.ApkSignatureVerifier|getMinimumSignatureSchemeVersionForTargetSdk|int|int|S
            4|4|android.util.apk.ApkSigningBlockUtils|parseVerityDigestAndVerifySourceLength|[B|[B,long,android.util.apk.SignatureInfo|S
            4|0|android.util.apk.ApkSigningBlockUtils|verifyIntegrityForVerityBasedAlgorithm|void|[B,java.io.RandomAccessFile,android.util.apk.SignatureInfo|S
            4|1|java.security.MessageDigest|isEqual|boolean|[B,[B|S
            4|1|android.util.jar.StrictJarVerifier|verifyMessageDigest|boolean|[B,[B|S
            4|1|android.util.jar.StrictJarVerifier|verify|boolean|java.util.jar.Attributes,java.lang.String,[B,int,int,boolean,boolean|I
            4|9|android.util.jar.StrictJarVerifier|<init>|void|java.lang.String,android.util.jar.StrictJarManifest,java.util.HashMap,boolean|C
            1|10|android.util.jar.StrictJarVerifier|verifyBytes|[Ljava.security.cert.Certificate;|[B,[B|S
            4|14|android.util.apk.ApkSignatureVerifier|verifyV1Signature|android.content.pm.parsing.result.ParseResult|android.content.pm.parsing.result.ParseInput,java.lang.String,boolean|S
            4|0|com.android.server.pm.ScanPackageUtils|assertMinSignatureSchemeIsValid|void|com.android.server.pm.pkg.AndroidPackage,int|S
            1|5|android.content.pm.SigningDetails|checkCapability|boolean|android.content.pm.SigningDetails,int|I
            1|5|android.content.pm.SigningDetails|checkCapabilityRecover|boolean|android.content.pm.SigningDetails,int|I
            3|6|android.content.pm.SigningDetails|hasCommonAncestor|boolean|android.content.pm.SigningDetails|I
            2|1|android.content.pm.SigningDetails|signaturesMatchExactly|boolean|android.content.pm.SigningDetails|I
            1|11|com.android.server.pm.KeySetManagerService|shouldCheckUpgradeKeySetLocked|boolean|com.android.server.pm.pkg.PackageStateInternal,com.android.server.pm.pkg.SharedUserApi,int|I
            1|12|com.android.server.pm.KeySetManagerService|checkUpgradeKeySetLocked|boolean|com.android.server.pm.pkg.PackageStateInternal,com.android.server.pm.pkg.AndroidPackage|I
            5|13|com.android.server.pm.InstallPackageHelper|doesSignatureMatchForPermissions|boolean|java.lang.String,com.android.internal.pm.parsing.pkg.ParsedPackage,int|I
            3|15|com.android.server.pm.SharedUserSetting|addPackage|void|com.android.server.pm.PackageSetting|I
            3|16|com.android.server.pm.SharedUserSetting|removePackage|boolean|com.android.server.pm.PackageSetting|I
            6|8|com.android.server.pm.VerifyingSession|handleStartVerify|void||I
            6|2|com.android.server.pm.VerifyingSession|isAdbVerificationEnabled|boolean|android.content.pm.PackageInfoLite,int,boolean|I
            7|2|com.nothing.server.ex.NtConfigListServiceImpl|isInstallingAppForbidden|boolean|*|A
            7|2|com.nothing.server.ex.NtConfigListServiceImpl|isStartingAppForbidden|boolean|*|A
            8|7|android.content.pm.ApplicationInfo|isPackageWhitelistedForHiddenApis|boolean||I
            9|2|android.content.res.AssetManager|containsAllocatedTable|boolean||I
            """;

    private static void attach(Executable target, int feature, int kind) {
        try {
            target.setAccessible(true);
            Reflect.attach(target, kind == 17 ? new Config.Boot() : new NHook(feature, kind, target.toString()),
                    XposedInterface.ExceptionMode.PROTECTIVE);
            successfulHooks.incrementAndGet();
            Audit.note("HOOK " + target);
        } catch (Throwable error) {
            failedHooks.incrementAndGet();
            Audit.error("Hook attachment failed: " + target, error);
            return;
        }
        deopt(target);
    }
    private static void deopt(Executable target) {
        try {
            if (!Entry.module.deoptimize(target)) Audit.note("WARN deoptimization returned false: " + target);
        } catch (Throwable error) {
            Audit.error("Deoptimization failed (hook registration unchanged): " + target, error);
        }
    }
    public static void deopt(String owner, String name) {
        try {
            int matches = 0;
            for (Method method : loader.loadClass(owner).getDeclaredMethods()) {
                if (!name.equals(method.getName())) continue;
                deopt(method);
                matches++;
            }
            if (matches == 0) throw new NoSuchMethodException(owner + "#" + name);
            Audit.note("DEOPT attempted caller: " + owner + "#" + name);
        } catch (Throwable error) {
            Audit.error(owner + "#" + name, error);
        }
    }
    public static synchronized void install(ClassLoader classLoader) {
        if (installed) return;
        loader = classLoader;
        installed = true;
        Audit.note("INSTALL core; waiting for CONFIG");
        for (String specification : TARGETS.strip().split("\n")) target(specification);
        verifyLinkedClasses();
        deopt("com.android.server.pm.ReconcilePackageUtils", "reconcilePackages");
        deopt("com.android.server.pm.PackageManagerServiceUtils", "canJoinSharedUserId");
        reportLoadingStatistics();
        Audit.note("INSTALL registration finished; inspect HOOK / FAIL / N/A separately, not a success ratio");
    }
    public static String parameters(Executable target) {
        StringJoiner names = new StringJoiner(",");
        for (Class<?> type : target.getParameterTypes()) names.add(type.getName());
        return names.toString();
    }
    private static void target(String specification) {
        try {
            String[] parts = specification.split("\\|", -1);
            int feature = Integer.parseInt(parts[0]);
            int kind = Integer.parseInt(parts[1]);
            Class<?> owner = loader.loadClass(parts[2]);
            boolean constructor = "<init>".equals(parts[3]);
            int matches = 0;
            for (Executable executable : constructor ? owner.getDeclaredConstructors() : owner.getDeclaredMethods()) {
                if (!constructor) {
                    Method method = (Method) executable;
                    if (!parts[3].equals(method.getName()) || !parts[4].equals(method.getReturnType().getName())) continue;
                    if (!"A".equals(parts[6]) && Modifier.isStatic(method.getModifiers()) != "S".equals(parts[6])) continue;
                }
                if (!"*".equals(parts[5]) && !parts[5].equals(parameters(executable))) continue;
                attach(executable, feature, kind);
                matches++;
            }
            if (matches == 0) throw new NoSuchMethodException(specification);
        } catch (Throwable error) {
            if (specification.contains("com.nothing.")) {
                skippedHooks.incrementAndGet();
                Audit.note("N/A vendor blocklist target not available on this firmware: " + specification);
            } else {
                failedHooks.incrementAndGet();
                Audit.error(specification, error);
            }
        }
    }
    public static void verifyLinkedClasses() {
        Class<?>[] linked = {R.class, Config.class, Audit.class, Config.Boot.class, NHook.class, Shared.class, Recovery.class};
        for (Class<?> type : linked) {
            try {
                type.getDeclaredMethods();
                Audit.note("VERIFY linked (not a functional test): " + type.getName());
            } catch (Throwable error) {
                Audit.error(type.getName(), error);
            }
        }
    }
    private static void reportLoadingStatistics() {
        int successful = successfulHooks.get();
        int failed = failedHooks.get();
        int skipped = skippedHooks.get();
        Audit.note("INSTALL statistics: Total=" + (successful + failed + skipped) + " Successful=" + successful
                + " Failed=" + failed + " Skipped=" + skipped);
        if (failed > 0) Audit.note("INSTALL WARNING: " + failed + " hook(s) failed to install");
    }
    public static String getLoadingStats() {
        return "successful=" + successfulHooks.get() + " failed=" + failedHooks.get() + " skipped=" + skippedHooks.get();
    }
}
