package com.cinexaft.hypercontrol.hook.systemframework.core;

import android.content.Context; import android.content.pm.PackageInfo; import android.content.pm.PackageManager; import android.content.pm.Signature; import android.content.pm.SigningInfo; import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Constructor; import java.lang.reflect.InvocationTargetException; import java.lang.reflect.Method; import java.security.cert.Certificate; import java.util.List; import java.util.Map;
import java.util.zip.ZipEntry;

public final class Recovery {
    public static void bytes(XposedInterface.Chain chain, Object[] objArr) throws IllegalAccessException, NoSuchMethodException, InstantiationException, ClassNotFoundException, InvocationTargetException {
        List list;
        if (Config.enabled(5)) {
            return;
        }
        Constructor<?> declaredConstructor = Class.forName("sun.security.pkcs.PKCS7").getDeclaredConstructor(byte[].class);
        declaredConstructor.setAccessible(true);
        Object objNewInstance = declaredConstructor.newInstance(chain.getArg(0));
        Object[] objArr2 = (Object[]) R.noArgs(objNewInstance, "getSignerInfos");
        if (objArr2 == null || objArr2.length == 0 || (list = (List) R.call(objArr2[0], "getCertificateChain", new Object[]{objNewInstance})) == null || list.isEmpty()) {
            return;
        }
        objArr[0] = (Certificate[]) list.toArray(new Certificate[list.size()]);
        objArr[1] = null;
        Audit.hit(1, "PKCS7 certificate chain recovered (typed Certificate[])");
    }

    private static Signature[] collect(Object obj, String str) throws IllegalAccessException, NoSuchMethodException, InstantiationException, ClassNotFoundException, InvocationTargetException {
        Class<?> cls = Class.forName("android.util.jar.StrictJarFile");
        Class cls2 = Boolean.TYPE;
        Constructor<?> declaredConstructor = cls.getDeclaredConstructor(String.class, cls2, cls2);
        declaredConstructor.setAccessible(true);
        Object objNewInstance = declaredConstructor.newInstance(str, Boolean.TRUE, Boolean.FALSE);
        try {
            Object objCall = R.call(objNewInstance, "findEntry", new Object[]{"AndroidManifest.xml"});
            Class<?> cls3 = Class.forName("android.util.apk.ApkSignatureVerifier");
            Method declaredMethod = cls3.getDeclaredMethod("loadCertificates", Class.forName("android.content.pm.parsing.result.ParseInput"), cls, ZipEntry.class);
            declaredMethod.setAccessible(true);
            Object objNoArgs = R.noArgs(declaredMethod.invoke(null, obj, objNewInstance, objCall), "getResult");
            Method declaredMethod2 = cls3.getDeclaredMethod("convertToSignatures", Certificate[][].class);
            declaredMethod2.setAccessible(true);
            return (Signature[]) declaredMethod2.invoke(null, objNoArgs);
        } finally {
            try {
                R.noArgs(objNewInstance, "close");
            } catch (Throwable th2) {
            }
        }
    }

    private static boolean exception103(Throwable th) {
        for (int i = 0; th != null && i < 4; i++) {
            if ("android.content.pm.PackageParser$PackageParserException".equals(th.getClass().getName()) && ((Integer) R.get(th, "error")).intValue() == -103) {
                return true;
            }
            th = th.getCause();
        }
        return false;
    }
    private static Signature[] fallback() {
        return new Signature[]{new Signature("308203c6308202aea003020102021426d148b7c65944abcf3a683b4c3dd3b139c4ec85300d06092a864886f70d01010b05003074310b3009060355040613025553311330110603550408130a43616c69666f726e6961311630140603550407130d4d6f756e7461696e205669657731143012060355040a130b476f6f676c6520496e632e3110300e060355040b1307416e64726f69643110300e06035504031307416e64726f6964301e170d3139303130323138353233385a170d3439303130323138353233385a3074310b3009060355040613025553311330110603550408130a43616c69666f726e6961311630140603550407130d4d6f756e7461696e205669657731143012060355040a130b476f6f676c6520496e632e3110300e060355040b1307416e64726f69643110300e06035504031307416e64726f696430820122300d06092a864886f70d01010105000382010f003082010a028201010087fcde48d9beaeba37b733a397ae586fb42b6c3f4ce758dc3ef1327754a049b58f738664ece587994f1c6362f98c9be5fe82c72177260c390781f74a10a8a6f05a6b5ca0c7c5826e15526d8d7f0e74f2170064896b0cf32634a388e1a975ed6bab10744d9b371cba85069834bf098f1de0205cdee8e715759d302a64d248067a15b9beea11b61305e367ac71b1a898bf2eec7342109c9c5813a579d8a1b3e6a3fe290ea82e27fdba748a663f73cca5807cff1e4ad6f3ccca7c02945926a47279d1159599d4ecf01c9d0b62e385c6320a7a1e4ddc9833f237e814b34024b9ad108a5b00786ea15593a50ca7987cbbdc203c096eed5ff4bf8a63d27d33ecc963990203010001a350304e300c0603551d13040530030101ff301d0603551d0e04160414a361efb002034d596c3a60ad7b0332012a16aee3301f0603551d23041830168014a361efb002034d596c3a60ad7b0332012a16aee3300d06092a864886f70d01010b0500038201010022ccb684a7a8706f3ee7c81d6750fd662bf39f84805862040b625ddf378eeefae5a4f1f283deea61a3c7f8e7963fd745415153a531912b82b596e7409287ba26fb80cedba18f22ae3d987466e1fdd88e440402b2ea2819db5392cadee501350e81b8791675ea1a2ed7ef7696dff273f13fb742bb9625fa12ce9c2cb0b7b3d94b21792f1252b1d9e4f7012cb341b62ff556e6864b40927e942065d8f0f51273fcda979b8832dd5562c79acf719de6be5aee2a85f89265b071bf38339e2d31041bc501d5e0c034ab1cd9c64353b10ee70b49274093d13f733eb9d3543140814c72f8e003f301c7a00b1872cc008ad55e26df2e8f07441002c4bcb7dc746745f0db")};
    }
    private static Signature[] previous(String str) throws PackageManager.NameNotFoundException {
        PackageManager packageManager;
        PackageInfo packageArchiveInfo;
        SigningInfo signingInfo;
        Context context = Config.context;
        if (context == null || (packageArchiveInfo = (packageManager = context.getPackageManager()).getPackageArchiveInfo(str, 0)) == null || (signingInfo = packageManager.getPackageInfo(packageArchiveInfo.packageName, 134217728).signingInfo) == null) {
            return null;
        }
        return signingInfo.getSigningCertificateHistory();
    }
    public static void v1(XposedInterface.Chain chain, Object[] objArr) throws IllegalAccessException, NoSuchMethodException, InstantiationException, ClassNotFoundException, InvocationTargetException {
        Object obj = objArr[0];
        Object obj2 = objArr[1];
        boolean z = false;
        if (obj != null) {
            if (Boolean.TRUE.equals(R.noArgs(obj, "isError")) && ((Integer) R.noArgs(obj, "getErrorCode")).intValue() == -103) {
                z = true;
            }
        }
        if (z || (obj2 != null && exception103((Throwable) obj2))) {
            Object arg = chain.getArg(0);
            String str = (String) chain.getArg(1);
            Signature[] signatureArrPrevious = null;
            boolean z2 = false;
            if (Config.enabled(5)) {
                try {
                    signatureArrPrevious = previous(str);
                    if (signatureArrPrevious != null) {
                        z2 = true;
                    }
                } catch (Throwable th) {
                    Audit.error("previous-signature lookup failed; following original fallback", th);
                }
            }
            if (signatureArrPrevious == null) {
                if (Config.enabled(1)) {
                    try {
                        signatureArrPrevious = collect(arg, str);
                    } catch (Throwable th2) {
                        Audit.error("manifest certificate recovery failed; following original fallback", th2);
                    }
                }
                if (signatureArrPrevious == null) {
                    signatureArrPrevious = fallback();
                    Audit.note("RECOVERY used the Core Patch N built-in fallback certificate (not the platform signing key)");
                }
            }
            Object objWrapper = wrapper(signatureArrPrevious);
            R.noArgs(arg, "reset");
            Object objCall = R.call(arg, "success", new Object[]{objWrapper});
            if (objCall != null) {
                objArr[0] = objCall;
                objArr[1] = null;
                Audit.hit(4, "verifyV1Signature recovered error -103");
                if (z2) {
                    Audit.hit(5, "installed package certificate history reused during V1 recovery");
                }
            }
        }
    }
    private static Object wrapper(Signature[] signatureArr) throws IllegalAccessException, NoSuchMethodException, InstantiationException, ClassNotFoundException, InvocationTargetException {
        Class<?> cls = Class.forName("android.content.pm.SigningDetails");
        Constructor<?> declaredConstructor = cls.getDeclaredConstructor(Signature[].class, Integer.TYPE);
        declaredConstructor.setAccessible(true);
        Object objNewInstance = declaredConstructor.newInstance(signatureArr, 1);
        Constructor<?> declaredConstructor2 = Class.forName("android.util.apk.ApkSignatureVerifier$SigningDetailsWithDigests").getDeclaredConstructor(cls, Map.class);
        declaredConstructor2.setAccessible(true);
        return declaredConstructor2.newInstance(objNewInstance, null);
    }
}
