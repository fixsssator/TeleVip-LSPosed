package com.my.televip.hooks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** What could and could not be hooked in this Telegram build; shown in the settings window. */
public final class HookStatus {

    private static final Set<String> FAILED = ConcurrentHashMap.newKeySet();
    private static final AtomicInteger OK = new AtomicInteger();

    private HookStatus() {}

    public static void ok() {
        OK.incrementAndGet();
    }

    public static void failed(String what) {
        if (what != null) FAILED.add(what);
    }

    public static int okCount() {
        return OK.get();
    }

    public static List<String> failures() {
        List<String> l = new ArrayList<>(FAILED);
        Collections.sort(l);
        return l;
    }

    /** "org.telegram.ui.ChatActivity" -> "ChatActivity" */
    public static String shortName(String className) {
        if (className == null) return "?";
        int i = className.lastIndexOf('.');
        return i >= 0 ? className.substring(i + 1) : className;
    }
}
