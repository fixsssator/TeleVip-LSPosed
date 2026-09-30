package com.my.televip.Class;

import com.my.televip.Clients.ClientManager;
import com.my.televip.logging.Logger;
import com.my.televip.obfuscate.Obfuscate;
import com.my.televip.utils.Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import de.robv.android.xposed.XposedHelpers;

public class ClassLoad {

    private static final Map<ClassLoader, Map<String, Class<?>>> cache = new WeakHashMap<>();

    public static Class<?> getClass(String name) {
        return getClass(name, Utils.classLoader, true);
    }

    public static Class<?> getClass(String name, ClassLoader classLoader) {
        return getClass(name, classLoader, true);
    }

    public static Class<?> getClass(String name, ClassLoader classLoader, boolean log) {

        String resolved = Obfuscate.getClassName(name);

        Map<String, Class<?>> loaderCache =
                cache.computeIfAbsent(classLoader, k -> new HashMap<>());

        if (loaderCache.containsKey(resolved)) {
            return loaderCache.get(resolved);
        }

        try {
            Class<?> cls = XposedHelpers.findClassIfExists(
                    resolved,
                    classLoader
            );

            // Not where we expected it: Telegram moves TL classes between TLRPC and tl.TL_* holders.
            // Only for clients without an obfuscation map (resolved == name).
            if (cls == null && resolved.equals(name)) {
                for (String alt : ClassAliases.candidates(resolved)) {
                    if (alt.equals(resolved)) continue;
                    cls = XposedHelpers.findClassIfExists(alt, classLoader);
                    if (cls != null) {
                        Logger.l("Class " + resolved + " found as " + alt);
                        break;
                    }
                }
            }

            if (cls != null) {
                loaderCache.put(resolved, cls);
            } else {
                if (log) {
                    if ((ClientManager.is(ClientManager.Client.Nagram) || ClientManager.is(ClientManager.Client.Momogram) || ClientManager.is(ClientManager.Client.Nekogram)) && name.equals(ClassNames.DRAWABLE))
                        return null;

                    if (name.equals(resolved)) {
                        Logger.w("Not found " + name + " " + Utils.issue);
                        com.my.televip.hooks.HookStatus.failed("class " + com.my.televip.hooks.HookStatus.shortName(name));
                    } else {
                        Logger.w("Not found " + name + ", " + resolved + " " + Utils.issue);
                        com.my.televip.hooks.HookStatus.failed("class " + com.my.televip.hooks.HookStatus.shortName(name));
                    }
                }
            }

            return cls;

        } catch (Throwable e) {
            Logger.e(e);
            return null;
        }
    }

}
