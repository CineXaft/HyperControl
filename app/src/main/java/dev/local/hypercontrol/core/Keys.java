package dev.local.hypercontrol.core;

/* JADX INFO: loaded from: classes.dex */
public final class Keys {
    private static final String[] names = "downgrade|bypass_digest|bypass_exact_sig_match|bypass_shared_user|bypass_verification|use_previous_signatures|disable_verification_agent|bypass_block|allow_hidden_apis_for_system_apps|bypass_resource_arsc_restrictions".split("\\|");

    public static String key(int i) {
        return "system_framework_n_".concat(name(i));
    }

    public static String name(int i) {
        return names[i];
    }
}
