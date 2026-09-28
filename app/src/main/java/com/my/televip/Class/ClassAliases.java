package com.my.televip.Class;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Telegram keeps moving TL classes out of TLRPC into org.telegram.tgnet.tl.TL_* holders
 * (TLRPC$TL_updateDeleteMessages -> tl.TL_update$TL_updateDeleteMessages, ...).
 * When a class is not found under the name the module was written with, these are the other
 * places it may live. Only meant for non-obfuscated clients.
 */
public final class ClassAliases {

    private static final String TGNET = "org.telegram.tgnet.";

    private static final String[] HOLDERS = {
            "TLRPC",
            "tl.TL_account", "tl.TL_aicompose", "tl.TL_bots", "tl.TL_chatlists", "tl.TL_communities",
            "tl.TL_ephemeral", "tl.TL_forum", "tl.TL_fragment", "tl.TL_iv", "tl.TL_keyboard",
            "tl.TL_payments", "tl.TL_phone", "tl.TL_stars", "tl.TL_stats", "tl.TL_stories",
            "tl.TL_update",
            // holders that upstream is likely to add as it keeps splitting TLRPC
            "tl.TL_messages", "tl.TL_channels", "tl.TL_contacts", "tl.TL_help", "tl.TL_auth",
            "tl.TL_users", "tl.TL_langpack", "tl.TL_photos", "tl.TL_upload", "tl.TL_premium",
            "tl.TL_folders", "tl.TL_updates", "tl.TL_secret", "tl.TL_encrypted", "tl.TL_business",
            "tl.TL_smsjobs", "tl.TL_star_gifts", "tl.TL_star_ref"
    };

    private ClassAliases() {}

    /** Original name first, then alternatives. Never contains duplicates. */
    public static List<String> candidates(String name) {
        Set<String> out = new LinkedHashSet<>();
        out.add(name);

        if (name == null || !name.startsWith(TGNET)) return new ArrayList<>(out);
        int dollar = name.indexOf('$');
        if (dollar < 0) return new ArrayList<>(out);   // top-level tgnet class, nowhere else to look

        String inner = name.substring(dollar + 1);
        if (inner.indexOf('$') >= 0) return new ArrayList<>(out);

        String[] variants;
        if (inner.startsWith("TL_")) {
            variants = new String[]{inner, inner.substring(3)};
        } else {
            variants = new String[]{inner, "TL_" + inner};
        }

        for (String holder : HOLDERS) {
            for (String v : variants) {
                out.add(TGNET + holder + "$" + v);
            }
        }
        return new ArrayList<>(out);
    }
}
