package dev.local.hypercontrol.core;

import dev.local.hypercontrol.Entry;
import dev.local.hypercontrol.Reflect;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/* JADX INFO: loaded from: classes.dex */
public final class Engine {
    private static boolean installed;
    public static ClassLoader loader;
    public static final ThreadLocal origin = new ThreadLocal();
    public static final ThreadLocal keyContext = new ThreadLocal();

    private static void attach(Executable executable, int i, int i2, String str) {
        executable.setAccessible(true);
        Reflect.attach(executable, i2 == 17 ? new Boot() : new NHook(i, i2, str), XposedInterface.ExceptionMode.PROTECTIVE);
        Audit.hooked(i, str);
        if (Entry.module.deoptimize(executable)) {
            return;
        }
        Audit.note("WARN target deoptimization returned false: ".concat(str));
    }

    public static void deopt(String str, String str2) {
        try {
            int i = 0;
            for (Method method : loader.loadClass(str).getDeclaredMethods()) {
                if (str2.equals(method.getName())) {
                    if (!Entry.module.deoptimize(method)) {
                        Audit.note("WARN caller deoptimization failed: ".concat(method.toString()));
                    }
                    i++;
                }
            }
            if (i == 0) {
                throw new NoSuchMethodException(str2);
            }
            Audit.note("DEOPT attempted caller: ".concat(str2));
        } catch (Throwable th) {
            Audit.error(str2, th);
        }
    }

    public static synchronized void install(ClassLoader classLoader) {
        if (installed) {
            return;
        }
        loader = classLoader;
        installed = true;
        Audit.note("INSTALL core; waiting for CONFIG");
        for (String str : "-1|17|com.android.server.SystemServer|startOtherServices|void|com.android.server.utils.TimingsTraceAndSlog|I\n0|0|com.android.server.pm.PackageManagerServiceUtils|checkDowngrade|void|long,int,[Ljava.lang.String;,[I,android.content.pm.PackageInfoLite|S\n0|0|com.android.server.pm.PackageManagerServiceUtils|checkDowngrade|void|com.android.server.pm.PackageSetting,android.content.pm.PackageInfoLite|S\n0|0|com.android.server.pm.PackageManagerServiceUtils|checkDowngrade|void|com.android.server.pm.pkg.AndroidPackage,android.content.pm.PackageInfoLite|S\n4|2|com.android.server.pm.PackageManagerServiceUtils|verifySignatures|boolean|com.android.server.pm.PackageSetting,com.android.server.pm.SharedUserSetting,com.android.server.pm.PackageSetting,android.content.pm.SigningDetails,boolean,boolean,boolean|S\n4|3|android.util.apk.ApkSignatureVerifier|getMinimumSignatureSchemeVersionForTargetSdk|int|int|S\n4|4|android.util.apk.ApkSigningBlockUtils|parseVerityDigestAndVerifySourceLength|[B|[B,long,android.util.apk.SignatureInfo|S\n4|0|android.util.apk.ApkSigningBlockUtils|verifyIntegrityForVerityBasedAlgorithm|void|[B,java.io.RandomAccessFile,android.util.apk.SignatureInfo|S\n4|1|java.security.MessageDigest|isEqual|boolean|[B,[B|S\n4|1|android.util.jar.StrictJarVerifier|verifyMessageDigest|boolean|[B,[B|S\n4|1|android.util.jar.StrictJarVerifier|verify|boolean|java.util.jar.Attributes,java.lang.String,[B,int,int,boolean,boolean|I\n4|9|android.util.jar.StrictJarVerifier|<init>|void|java.lang.String,android.util.jar.StrictJarManifest,java.util.HashMap,boolean|C\n1|10|android.util.jar.StrictJarVerifier|verifyBytes|[Ljava.security.cert.Certificate;|[B,[B|S\n4|14|android.util.apk.ApkSignatureVerifier|verifyV1Signature|android.content.pm.parsing.result.ParseResult|android.content.pm.parsing.result.ParseInput,java.lang.String,boolean|S\n4|0|com.android.server.pm.ScanPackageUtils|assertMinSignatureSchemeIsValid|void|com.android.server.pm.pkg.AndroidPackage,int|S\n1|5|android.content.pm.SigningDetails|checkCapability|boolean|android.content.pm.SigningDetails,int|I\n1|5|android.content.pm.SigningDetails|checkCapabilityRecover|boolean|android.content.pm.SigningDetails,int|I\n3|6|android.content.pm.SigningDetails|hasCommonAncestor|boolean|android.content.pm.SigningDetails|I\n2|1|android.content.pm.SigningDetails|signaturesMatchExactly|boolean|android.content.pm.SigningDetails|I\n1|11|com.android.server.pm.KeySetManagerService|shouldCheckUpgradeKeySetLocked|boolean|com.android.server.pm.pkg.PackageStateInternal,com.android.server.pm.pkg.SharedUserApi,int|I\n1|12|com.android.server.pm.KeySetManagerService|checkUpgradeKeySetLocked|boolean|com.android.server.pm.pkg.PackageStateInternal,com.android.server.pm.pkg.AndroidPackage|I\n5|13|com.android.server.pm.InstallPackageHelper|doesSignatureMatchForPermissions|boolean|java.lang.String,com.android.internal.pm.parsing.pkg.ParsedPackage,int|I\n3|15|com.android.server.pm.SharedUserSetting|addPackage|void|com.android.server.pm.PackageSetting|I\n3|16|com.android.server.pm.SharedUserSetting|removePackage|boolean|com.android.server.pm.PackageSetting|I\n6|8|com.android.server.pm.VerifyingSession|handleStartVerify|void||I\n6|2|com.android.server.pm.VerifyingSession|isAdbVerificationEnabled|boolean|android.content.pm.PackageInfoLite,int,boolean|I\n7|2|com.nothing.server.ex.NtConfigListServiceImpl|isInstallingAppForbidden|boolean|*|A\n7|2|com.nothing.server.ex.NtConfigListServiceImpl|isStartingAppForbidden|boolean|*|A\n8|7|android.content.pm.ApplicationInfo|isPackageWhitelistedForHiddenApis|boolean||I\n9|2|android.content.res.AssetManager|containsAllocatedTable|boolean||I".split("\n")) {
            target(str);
        }
        verifyLinkedClasses();
        deopt("com.android.server.pm.ReconcilePackageUtils", "reconcilePackages");
        deopt("com.android.server.pm.PackageManagerServiceUtils", "canJoinSharedUserId");
        Audit.note("INSTALL registration finished; inspect HOOK / FAIL / N/A separately, not a success ratio");
    }

    public static String parameters(Executable executable) {
        Class<?>[] parameterTypes = executable.getParameterTypes();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parameterTypes.length; i++) {
            if (i != 0) {
                sb.append(",");
            }
            sb.append(parameterTypes[i].getName());
        }
        return sb.toString();
    }

    private static void target(String str) {
        String str2;
        try {
            String[] strArrSplit = str.split("\\|", -1);
            int i = Integer.parseInt(strArrSplit[0]);
            int i2 = Integer.parseInt(strArrSplit[1]);
            Class<?> clsLoadClass = loader.loadClass(strArrSplit[2]);
            String str3 = strArrSplit[3];
            boolean zEquals = "<init>".equals(str3);
            int i3 = 0;
            for (Executable executable : zEquals ? clsLoadClass.getDeclaredConstructors() : clsLoadClass.getDeclaredMethods()) {
                if (zEquals) {
                    str2 = strArrSplit[5];
                    if ("*".equals(str2)) {
                        attach(executable, i, i2, executable.toString());
                        i3++;
                    } else if (str2.equals(parameters(executable))) {
                        attach(executable, i, i2, executable.toString());
                        i3++;
                    }
                } else {
                    Method method = (Method) executable;
                    if (str3.equals(method.getName())) {
                        if (strArrSplit[4].equals(method.getReturnType().getName())) {
                            boolean zIsStatic = Modifier.isStatic(method.getModifiers());
                            String str4 = strArrSplit[6];
                            if ("A".equals(str4) || zIsStatic == "S".equals(str4)) {
                                str2 = strArrSplit[5];
                                if ("*".equals(str2) || str2.equals(parameters(executable))) {
                                    attach(executable, i, i2, executable.toString());
                                    i3++;
                                }
                            }
                        }
                    }
                }
            }
            if (i3 == 0) {
                throw new NoSuchMethodException(str);
            }
        } catch (Throwable th) {
            if (str.contains("com.nothing.")) {
                Audit.note("N/A vendor blocklist target not available on this firmware: ".concat(str));
            } else {
                Audit.error(str, th);
            }
        }
    }

    public static void verifyLinkedClasses() {
        for (String str : "R|Config|Audit|Boot|NHook|Shared|Recovery".split("\\|")) {
            String strConcat = "dev.local.hypercontrol.core.".concat(str);
            try {
                Class.forName(strConcat, true, Engine.class.getClassLoader()).getDeclaredMethods();
                Audit.note("VERIFY linked (not a functional test): ".concat(strConcat));
            } catch (Throwable th) {
                Audit.error(strConcat, th);
            }
        }
    }
}
