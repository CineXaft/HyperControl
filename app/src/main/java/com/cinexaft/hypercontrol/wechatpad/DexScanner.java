package com.cinexaft.hypercontrol.wechatpad;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/* JADX INFO: loaded from: classes.dex */
final class DexScanner {
    private static final int MAX_SITES = 64;
    private static final int MAX_TARGETS = 16;
    private static final String TAG = "scan: ";
    private static volatile Logger sLogger;
    private static final String[] ANCHORS = {"Lenovo TB-9707F", "lenovo", "/cgi-bin/micromsg-bin/checkloginaspad", "MicroMsg.CgiCheckLoginAsPad", "doCgi callback loginAsPad:", "doCgi deviceBrand:", "checkloginaspad", "CgiCheckLoginAsPad"};
    private static final int[] ORDER_LOCAL = {0, 1};
    private static final int[] ORDER_CGI = {2, 3, 4, 5, 6, 7};
    private static final Comparator<Candidate> CANDIDATE_ORDER = new Comparator<Candidate>() { // from class: cc.liujiale.wxpad.DexScanner.1
        @Override // java.util.Comparator
        public int compare(Candidate candidate, Candidate candidate2) {
            return candidate.anchor - candidate2.anchor;
        }
    };
    private static final Comparator<MethodRef> METHOD_ORDER = new Comparator<MethodRef>() { // from class: cc.liujiale.wxpad.DexScanner.2
        @Override // java.util.Comparator
        public int compare(MethodRef methodRef, MethodRef methodRef2) {
            if (methodRef.start < methodRef2.start) {
                return -1;
            }
            return methodRef.start == methodRef2.start ? 0 : 1;
        }
    };

    interface Logger {
        void log(String str);
    }

    static void setLogger(Logger logger) {
        sLogger = logger;
    }

    private static void log(String str) {
        Logger logger = sLogger;
        if (logger != null) {
            logger.log(TAG + str);
        }
    }

    private DexScanner() {
    }

    static List<Target> scan(String[] strArr) {
        ZipFile zipFile;
        Object th;
        byte[] entry;
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        for (String str : strArr) {
            try {
                zipFile = new ZipFile(str);
                try {
                    Enumeration<? extends ZipEntry> enumerationEntries = zipFile.entries();
                    while (enumerationEntries.hasMoreElements()) {
                        ZipEntry zipEntryNextElement = enumerationEntries.nextElement();
                        if (isDexEntry(zipEntryNextElement.getName()) && (entry = readEntry(zipFile, zipEntryNextElement)) != null && entry.length >= 112) {
                            int[] iArrFindStringIndices = findStringIndices(entry, ANCHORS);
                            List<MethodRef> listBuildMethodMap = buildMethodMap(entry);
                            if (!listBuildMethodMap.isEmpty()) {
                                Collections.sort(listBuildMethodMap, METHOD_ORDER);
                                probe(entry, listBuildMethodMap, iArrFindStringIndices, ORDER_LOCAL, arrayList);
                                probe(entry, listBuildMethodMap, iArrFindStringIndices, ORDER_CGI, arrayList2);
                            }
                        }
                    }
                    try {
                        zipFile.close();
                    } catch (Throwable th2) {
                    }
                } catch (Throwable th3) {
                    th = th3;
                    try {
                        log("open " + str + " failed: " + th);
                        if (zipFile != null) {
                            zipFile.close();
                        }
                    } catch (Throwable th4) {
                        if (zipFile != null) {
                            try {
                                zipFile.close();
                            } catch (Throwable th5) {
                            }
                        }
                        throw th4;
                    }
                }
            } catch (Throwable th6) {
                zipFile = null;
                th = th6;
            }
        }
        ArrayList arrayList3 = new ArrayList();
        collect(arrayList, 0, arrayList3);
        collect(arrayList2, 1, arrayList3);
        return arrayList3;
    }

    private static void probe(byte[] bArr, List<MethodRef> list, int[] iArr, int[] iArr2, List<Candidate> list2) {
        for (int i : iArr2) {
            int i2 = iArr[i];
            if (i2 >= 0) {
                list2.addAll(sitesOf(bArr, list, i2, i));
            }
        }
    }

    private static void collect(List<Candidate> list, int i, List<Target> list2) {
        if (list.isEmpty()) {
            return;
        }
        Collections.sort(list, CANDIDATE_ORDER);
        int size = list2.size();
        for (int i2 = 0; i2 < list.size() && list2.size() - size < MAX_TARGETS; i2++) {
            Candidate candidate = list.get(i2);
            Target target = new Target(i, candidate.className, candidate.methodName);
            if (!list2.contains(target)) {
                list2.add(target);
            }
        }
    }

    private static List<Candidate> sitesOf(byte[] bArr, List<MethodRef> list, int i, int i2) {
        ArrayList arrayList = new ArrayList();
        try {
            List<int[]> listFindConstStringSites = findConstStringSites(bArr, i, list);
            for (int i3 = 0; i3 < listFindConstStringSites.size() && arrayList.size() < MAX_SITES; i3++) {
                int[] iArr = listFindConstStringSites.get(i3);
                String strClassName = className(bArr, iArr[0]);
                String strMethodName = methodName(bArr, iArr[1]);
                if (strClassName != null && strMethodName != null) {
                    Candidate candidate = new Candidate();
                    candidate.anchor = i2;
                    candidate.className = strClassName;
                    candidate.methodName = strMethodName;
                    if (arrayList.contains(candidate)) {
                        continue;
                    } else {
                        arrayList.add(candidate);
                    }
                }
            }
        } catch (Throwable th) {
            log("scan sites failed: " + th);
        }
        return arrayList;
    }

    private static int[] findStringIndices(byte[] bArr, String[] strArr) {
        int i;
        int i2;
        int length = strArr.length;
        int[] iArr = new int[length];
        int i3 = 0;
        for (int i4 = 0; i4 < length; i4++) {
            iArr[i4] = -1;
        }
        int iU32 = u32(bArr, 56);
        int iU33 = u32(bArr, 60);
        if (iU32 <= 0 || iU33 <= 0 || (iU32 * 4) + iU33 > bArr.length) {
            return iArr;
        }
        boolean[] zArr = new boolean[128];
        for (String str : strArr) {
            zArr[str.charAt(0) & 127] = true;
        }
        byte[][] bArr2 = new byte[length][];
        for (int i5 = 0; i5 < length; i5++) {
            bArr2[i5] = asciiBytes(strArr[i5]);
        }
        int i6 = length;
        int i7 = 0;
        while (i7 < iU32 && i6 > 0) {
            int iU34 = u32(bArr, (i7 << 2) + iU33);
            if (iU34 > 0 && iU34 < bArr.length) {
                int[] iArr2 = {iU34};
                int iUleb = uleb(bArr, iArr2);
                int i8 = iArr2[i3];
                if (i8 < bArr.length && (i = bArr[i8] & 255) < 128 && zArr[i]) {
                    int i9 = i3;
                    while (i9 < length) {
                        if (iArr[i9] < 0) {
                            i2 = i3;
                            if (iUleb == strArr[i9].length() && regionEquals(bArr, i8, bArr2[i9]) && bArr[bArr2[i9].length + i8] == 0) {
                                iArr[i9] = i7;
                                i6--;
                            }
                        } else {
                            i2 = i3;
                        }
                        i9++;
                        i3 = i2;
                    }
                }
            }
            i7++;
            i3 = i3;
        }
        boolean z = false;
        if (i6 == 0) {
            return iArr;
        }
        boolean[] zArr2 = new boolean[length];
        int i10 = z ? 1 : 0;
        int i11 = i10;
        while (i10 < length) {
            if (iArr[i10] < 0) {
                zArr2[i10] = true;
                i11++;
            }
            i10++;
        }
        for (int i12 = z ? 1 : 0; i12 < iU32 && i11 > 0; i12++) {
            int iU35 = u32(bArr, (i12 << 2) + iU33);
            if (iU35 > 0 && iU35 < bArr.length) {
                int[] iArr3 = {iU35};
                uleb(bArr, iArr3);
                int i13 = iArr3[z ? 1 : 0];
                int iIndexOfNul = indexOfNul(bArr, i13);
                if (iIndexOfNul >= 0) {
                    for (int i14 = z ? 1 : 0; i14 < length; i14++) {
                        if (zArr2[i14] && iIndexOfNul - i13 >= bArr2[i14].length && contains(bArr, i13, iIndexOfNul, bArr2[i14])) {
                            iArr[i14] = i12;
                            zArr2[i14] = z;
                            i11--;
                        }
                    }
                }
            }
        }
        return iArr;
    }

    private static boolean regionEquals(byte[] bArr, int i, byte[] bArr2) {
        for (int i2 = 0; i2 < bArr2.length; i2++) {
            int i3 = i + i2;
            if (i3 >= bArr.length || bArr[i3] != bArr2[i2]) {
                return false;
            }
        }
        return true;
    }

    private static int indexOfNul(byte[] bArr, int i) {
        while (i < bArr.length) {
            if (bArr[i] != 0) {
                i++;
            } else {
                return i;
            }
        }
        return -1;
    }

    private static byte[] asciiBytes(String str) {
        byte[] bArr = new byte[str.length()];
        for (int i = 0; i < str.length(); i++) {
            bArr[i] = (byte) (str.charAt(i) & 127);
        }
        return bArr;
    }

    private static boolean contains(byte[] bArr, int i, int i2, byte[] bArr2) {
        int length = i2 - bArr2.length;
        while (i <= length) {
            if (bArr[i] == bArr2[0]) {
                int i3 = 1;
                while (i3 < bArr2.length && bArr[i + i3] == bArr2[i3]) {
                    i3++;
                }
                if (i3 == bArr2.length) {
                    return true;
                }
            }
            i++;
        }
        return false;
    }

    private static List<MethodRef> buildMethodMap(byte[] bArr) {
        int i;
        ArrayList arrayList = new ArrayList();
        int iU32 = u32(bArr, 96);
        int iU33 = u32(bArr, 100);
        if (iU32 <= 0 || iU33 <= 0 || (iU32 * 32) + iU33 > bArr.length) {
            return arrayList;
        }
        for (int i2 = 0; i2 < iU32; i2++) {
            int i3 = (i2 * 32) + iU33;
            int iU34 = u32(bArr, i3);
            int iU35 = u32(bArr, i3 + 24);
            if (iU35 > 0 && iU35 < bArr.length) {
                int[] iArr = {iU35};
                int iUleb = uleb(bArr, iArr);
                int iUleb2 = uleb(bArr, iArr);
                int iUleb3 = uleb(bArr, iArr);
                int iUleb4 = uleb(bArr, iArr);
                if (iUleb >= 0 && iUleb2 >= 0 && iUleb3 >= 0 && iUleb4 >= 0 && (i = iUleb + iUleb2) <= 200000 && iUleb3 + iUleb4 <= 200000) {
                    for (int i4 = 0; i4 < i; i4++) {
                        uleb(bArr, iArr);
                        uleb(bArr, iArr);
                    }
                    addMethods(bArr, arrayList, iU34, iArr, iUleb3);
                    addMethods(bArr, arrayList, iU34, iArr, iUleb4);
                }
            }
        }
        return arrayList;
    }

    private static void addMethods(byte[] bArr, List<MethodRef> list, int i, int[] iArr, int i2) {
        int iU32;
        int iUleb = 0;
        for (int i3 = 0; i3 < i2; i3++) {
            iUleb += uleb(bArr, iArr);
            uleb(bArr, iArr);
            int iUleb2 = uleb(bArr, iArr);
            if (iUleb2 > 0 && iUleb2 + MAX_TARGETS <= bArr.length && (iU32 = u32(bArr, iUleb2 + 12)) > 0) {
                long j = ((long) iUleb2) + 16;
                long length = (((long) iU32) * 2) + j;
                if (length > bArr.length) {
                    length = bArr.length;
                }
                MethodRef methodRef = new MethodRef();
                methodRef.start = (int) j;
                methodRef.end = (int) length;
                methodRef.classIdx = i;
                methodRef.methodIdx = iUleb;
                list.add(methodRef);
            }
        }
    }

    private static List<int[]> findConstStringSites(byte[] bArr, int i, List<MethodRef> list) {
        ArrayList arrayList = new ArrayList();
        byte b = (byte) (i & 255);
        byte b2 = (byte) ((i >>> 8) & 255);
        byte b3 = (byte) ((i >>> MAX_TARGETS) & 255);
        byte b4 = (byte) ((i >>> 24) & 255);
        boolean z = i <= 65535;
        int length = bArr.length;
        for (int i2 = 0; i2 + 6 <= length; i2++) {
            int i3 = bArr[i2] & 255;
            if (i3 == 26) {
                if (z && bArr[i2 + 2] == b && bArr[i2 + 3] == b2) {
                    record(list, i2, arrayList);
                }
            } else if (i3 == 27 && bArr[i2 + 2] == b && bArr[i2 + 3] == b2 && bArr[i2 + 4] == b3 && bArr[i2 + 5] == b4) {
                record(list, i2, arrayList);
            }
            if (arrayList.size() >= MAX_SITES) {
                break;
            }
        }
        return arrayList;
    }

    private static void record(List<MethodRef> list, int i, List<int[]> list2) {
        int size = list.size() - 1;
        int i2 = 0;
        int i3 = -1;
        while (i2 <= size) {
            int i4 = (i2 + size) >>> 1;
            if (list.get(i4).start <= i) {
                i2 = i4 + 1;
                i3 = i4;
            } else {
                size = i4 - 1;
            }
        }
        if (i3 >= 0) {
            MethodRef methodRef = list.get(i3);
            if (i < methodRef.end) {
                list2.add(new int[]{methodRef.classIdx, methodRef.methodIdx});
            }
        }
    }

    private static String className(byte[] bArr, int i) {
        String strStringById;
        int iU32 = u32(bArr, MAX_SITES);
        int iU33 = u32(bArr, 68);
        if (i < 0 || i >= iU32 || (strStringById = stringById(bArr, u32(bArr, iU33 + (i << 2)))) == null || strStringById.length() < 2 || strStringById.charAt(0) != 'L' || strStringById.charAt(strStringById.length() - 1) != ';') {
            return null;
        }
        return strStringById.substring(1, strStringById.length() - 1).replace('/', '.');
    }

    private static String methodName(byte[] bArr, int i) {
        int iU32 = u32(bArr, 88);
        int iU33 = u32(bArr, 92);
        if (i < 0 || i >= iU32) {
            return null;
        }
        return stringById(bArr, u32(bArr, iU33 + (i * 8) + 4));
    }

    private static String stringById(byte[] bArr, int i) {
        int iU32;
        int iU33 = u32(bArr, 56);
        int iU34 = u32(bArr, 60);
        if (i < 0 || i >= iU33 || (iU32 = u32(bArr, iU34 + (i << 2))) <= 0 || iU32 >= bArr.length) {
            return null;
        }
        return decodeMutf8(bArr, iU32 + ulebSize(bArr, iU32));
    }

    private static String decodeMutf8(byte[] bArr, int i) {
        int i2;
        StringBuilder sb = new StringBuilder(32);
        while (i < bArr.length) {
            int i3 = i + 1;
            int i4 = bArr[i] & 255;
            if (i4 == 0) {
                break;
            }
            if (i4 < 128) {
                sb.append((char) i4);
                i = i3;
            } else if ((i4 & 224) == 192) {
                if (i3 >= bArr.length) {
                    break;
                }
                sb.append((char) (((i4 & 31) << 6) | (bArr[i3] & 255 & 63)));
                i = i3 + 1;
            } else {
                if ((i4 & 240) != 224 || (i2 = i3 + 1) >= bArr.length) {
                    break;
                }
                sb.append((char) (((i4 & 15) << 12) | (((bArr[i3] & 255) & 63) << 6) | (bArr[i2] & 255 & 63)));
                i = i2 + 1;
            }
        }
        return sb.toString();
    }

    private static int u32(byte[] bArr, int i) {
        return ((bArr[i + 3] & 255) << 24) | (bArr[i] & 255) | ((bArr[i + 1] & 255) << 8) | ((bArr[i + 2] & 255) << MAX_TARGETS);
    }

    private static int uleb(byte[] bArr, int[] iArr) {
        int i = iArr[0];
        int i2 = 0;
        int i3 = 0;
        while (i < bArr.length) {
            int i4 = i + 1;
            int i5 = bArr[i] & 255;
            i2 |= (i5 & 127) << i3;
            i3 += 7;
            if ((i5 & 128) == 0 || i3 >= 32) {
                iArr[0] = i4;
                return i2;
            }
            i = i4;
        }
        iArr[0] = i;
        return i2;
    }

    private static int ulebSize(byte[] bArr, int i) {
        int i2 = 1;
        while (true) {
            int i3 = i + i2;
            if (i3 >= bArr.length || (bArr[i3 - 1] & 128) == 0 || i2 >= 5) {
                break;
            }
            i2++;
        }
        return i2;
    }

    private static boolean isDexEntry(String str) {
        if (str == null || !str.startsWith("classes") || !str.endsWith(".dex") || str.indexOf(47) >= 0) {
            return false;
        }
        for (int i = 7; i < str.length() - 4; i++) {
            char cCharAt = str.charAt(i);
            if (cCharAt < '0' || cCharAt > '9') {
                return false;
            }
        }
        return true;
    }

    private static byte[] readEntry(ZipFile zipFile, ZipEntry zipEntry) {
        InputStream inputStream;
        Throwable failure;
        try {
            inputStream = zipFile.getInputStream(zipEntry);
            try {
                long size = zipEntry.getSize();
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(Math.max((size <= 0 || size >= 2147483647L) ? 65536 : (int) size, 1024));
                byte[] bArr = new byte[65536];
                while (true) {
                    int i = inputStream.read(bArr);
                    if (i <= 0) {
                        break;
                    }
                    byteArrayOutputStream.write(bArr, 0, i);
                }
                byte[] byteArray = byteArrayOutputStream.toByteArray();
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Throwable th) {
                    }
                }
                return byteArray;
            } catch (Throwable th2) {
                failure = th2;
                try {
                    log("read " + zipEntry.getName() + " failed: " + failure);
                    return null;
                } finally {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (Throwable th3) {
                        }
                    }
                }
            }
        } catch (Throwable th4) {
            failure = th4;
            inputStream = null;
        }
        return null;
    }

    private static final class Candidate {
        int anchor;
        String className;
        String methodName;

        private Candidate() {
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof Candidate)) {
                return false;
            }
            Candidate candidate = (Candidate) obj;
            return this.className.equals(candidate.className) && this.methodName.equals(candidate.methodName);
        }

        public int hashCode() {
            return (this.className.hashCode() * 31) + this.methodName.hashCode();
        }
    }

    private static final class MethodRef {
        int classIdx;
        int end;
        int methodIdx;
        int start;

        private MethodRef() {
        }
    }
}
