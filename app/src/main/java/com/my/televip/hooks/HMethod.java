package com.my.televip.hooks;

import com.my.televip.Clients.ClientManager;
import com.my.televip.base.BaseMethodHook;
import com.my.televip.logging.Logger;
import com.my.televip.obfuscate.Obfuscate;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class HMethod {

    /**
     * args = parameter types (Class, class-name String, or null for "unknown") followed by the hook.
     * Tries the exact signature first, then a tolerant match (see {@link MethodResolver}), so a
     * Telegram update that only appends a parameter no longer switches the feature off.
     */
    public static void hookMethod(Class<?> cls, String name, Object... args) {
        try {
            if (cls == null) return;

            if (args == null || args.length == 0 || !(args[args.length - 1] instanceof XC_MethodHook)) {
                Logger.w("Bad hook arguments for " + cls.getName() + "#" + name);
                return;
            }

            XC_MethodHook callback = (XC_MethodHook) args[args.length - 1];
            Class<?>[] expected = new Class<?>[args.length - 1];
            for (int i = 0; i < expected.length; i++) {
                Object a = args[i];
                if (a instanceof Class) {
                    expected[i] = (Class<?>) a;
                } else if (a instanceof String) {
                    expected[i] = XposedHelpers.findClassIfExists((String) a, cls.getClassLoader());
                } else {
                    expected[i] = null;
                }
            }

            MethodResolver.Resolution r = MethodResolver.resolve(cls, name, expected);
            if (r == null) {
                Logger.w("Method not found: " + cls.getName() + "#" + name + MethodResolver.describe(expected));
                return;
            }
            if (!r.exact) {
                Logger.w("Signature differs, hooked closest match: " + r.method + " (wanted "
                        + cls.getName() + "#" + name + MethodResolver.describe(expected) + ")");
            }
            XposedBridge.hookMethod(r.method, callback);
        } catch (Throwable t) {
            Logger.e(t);
        }
    }

    public static void hookMethod(Class<?> cls, String className, String[] names, Object... args) {
        try {
            if (cls != null) {
                for (String name : names) {
                    if (ClientManager.is(ClientManager.Client.Nagram) && name.equals("formatPmEditedDate")) continue;
                    hookMethod(cls, Obfuscate.getMethodName(className, name), args);
                }
            }
        } catch (Throwable t) {
            Logger.e(t);
        }
    }

    public static void hookMethod(Method method, BaseMethodHook callback) {
        try {
            if (method != null) {
                XposedBridge.hookMethod(method, callback);
            }
        } catch (Throwable t) {
            Logger.e(t);
        }
    }

}
