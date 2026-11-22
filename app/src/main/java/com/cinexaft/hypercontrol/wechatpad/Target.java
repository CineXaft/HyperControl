package com.cinexaft.hypercontrol.wechatpad;

/* JADX INFO: loaded from: classes.dex */
final class Target {
    static final int KIND_CGI_CHECK = 1;
    static final int KIND_COUNT = 2;
    static final int KIND_LOCAL_DEVICE = 0;
    final String className;
    final int kind;
    final String methodName;

    Target(int i, String str, String str2) {
        this.kind = i;
        this.className = str;
        this.methodName = str2;
    }

    public String toString() {
        return this.kind + "|" + this.className + "|" + this.methodName;
    }

    static Target parse(String str) {
        if (str == null) {
            return null;
        }
        String[] strArrSplit = str.split("\\|");
        if (strArrSplit.length != 3 || strArrSplit[KIND_CGI_CHECK].length() == 0 || strArrSplit[KIND_COUNT].length() == 0) {
            return null;
        }
        try {
            int i = Integer.parseInt(strArrSplit[0]);
            if (i != 0 && i != KIND_CGI_CHECK) {
                return null;
            }
            return new Target(i, strArrSplit[KIND_CGI_CHECK], strArrSplit[KIND_COUNT]);
        } catch (Throwable th) {
            return null;
        }
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Target)) {
            return false;
        }
        Target target = (Target) obj;
        return this.kind == target.kind && this.className.equals(target.className) && this.methodName.equals(target.methodName);
    }

    public int hashCode() {
        return (((this.kind * 31) + this.className.hashCode()) * 31) + this.methodName.hashCode();
    }
}
