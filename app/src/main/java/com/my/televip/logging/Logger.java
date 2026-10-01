package com.my.televip.logging;

import static com.my.televip.utils.Utils.pkgName;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.my.televip.application.ApplicationLoaderHook;
import com.my.televip.obfuscate.struct.ResolverRegistry;

import de.robv.android.xposed.XposedBridge;

public class Logger {

    private static final Map<String, AtomicInteger> SEEN = new ConcurrentHashMap<>();

    /** true for the first 3 occurrences of a message, then only at 10, 100, 1000 ... (stops log floods). */
    private static boolean shouldLog(String key) {
        if (SEEN.size() > 2000) SEEN.clear();
        int n = SEEN.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();
        if (n <= 3) return true;
        for (int p = 10; p <= 1000000; p *= 10) if (n == p) return true;
        return false;
    }

    public static void w(String text)
    {
        if (!shouldLog("w:" + text)) return;
        XposedBridge.log("[TeleVip] [Warning] pkgName: "+ pkgName + " " + text);
    }

    public static void l(String text)
    {
        XposedBridge.log("[TeleVip] pkgName: "+ pkgName +" " + text);
    }

    public static void e(String text)
    {
        XposedBridge.log("[TeleVip] [Error] Ai: " + text);
    }

    /** "12.10.6 (69700)" of the host app, or "?" if it is not available yet. */
    public static String hostVersion() {
        try {
            PackageManager pm = ApplicationLoaderHook.getApplicationContext().getPackageManager();
            PackageInfo info = pm.getPackageInfo(ApplicationLoaderHook.getApplicationContext().getPackageName(), 0);
            return info.versionName + " (" + info.versionCode + ")";
        } catch (Throwable t) {
            return "?";
        }
    }

    public static void e(Throwable throwable) {
        try {
            // One line on purpose: the LSPosed log viewer folds multi-line entries ("N more lines"),
            // which used to hide exactly the part needed to diagnose a problem.
            StringBuilder log = new StringBuilder();
            log.append("[TeleVip] [Error] pkgName: ").append(pkgName).append(" ");

            Throwable t = throwable;
            for (int depth = 0; t != null && depth < 4; depth++) {
                if (depth > 0) log.append(" <= caused by ");
                log.append(t);
                t = t.getCause();
            }

            StackTraceElement[] st = throwable.getStackTrace();
            if (st.length == 0) {
                log.append(" @ (no stack trace)");
            } else {
                log.append(" @ ");
                for (int i = 0; i < Math.min(10, st.length); i++) {
                    if (i > 0) log.append(" < ");
                    log.append(st[i]);
                }
            }

            String dedupeKey = "e:" + throwable + (throwable.getStackTrace().length > 0 ? throwable.getStackTrace()[0] : "");
            if (!shouldLog(dedupeKey)) return;
            log.append(" | Telegram ").append(hostVersion());
            log.append(" | TeleVip ").append(com.my.televip.utils.Utils.MODULE_VERSION);
            log.append(" | Android ").append(Build.VERSION.RELEASE).append("/").append(Build.VERSION.SDK_INT);
            log.append(" ").append(Build.MODEL);

            XposedBridge.log(log.toString());
        } catch (Throwable g) {}
    }

}
