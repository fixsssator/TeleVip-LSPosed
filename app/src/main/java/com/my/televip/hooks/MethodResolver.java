package com.my.televip.hooks;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

/**
 * Finds the method to hook without requiring the exact signature the module was built against.
 *
 * Order of attempts:
 *  1. exact signature (old behaviour, nothing changes for builds that still match);
 *  2. same name, where the parameters the hook code expects are a *prefix* of the real parameters
 *     (Telegram appended a parameter). Indexes used by the hook code (param.args[i]) stay valid;
 *  3. unresolved entries in the expected list (null) act as wildcards;
 *  an expected type also matches a narrower real type (real type is a subtype of the expected one).
 *
 * Anything else is NOT hooked: hooking a method whose parameters moved around would feed the hook
 * code the wrong arguments, which is worse than a feature that is simply off.
 * An empty expected list is exact-only (otherwise "hasStories()" could bind to "hasStories(long)").
 */
public final class MethodResolver {

    public static final class Resolution {
        public final Method method;
        public final boolean exact;

        Resolution(Method method, boolean exact) {
            this.method = method;
            this.exact = exact;
        }
    }

    private MethodResolver() {}

    public static Resolution resolve(Class<?> cls, String name, Class<?>[] expected) {
        if (cls == null || name == null) return null;
        if (expected == null) expected = new Class<?>[0];

        // 1. exact
        if (!hasNull(expected)) {
            for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
                try {
                    Method m = c.getDeclaredMethod(name, expected);
                    m.setAccessible(true);
                    return new Resolution(m, true);
                } catch (NoSuchMethodException ignored) {
                }
            }
        }

        if (expected.length == 0) return null;

        // 2. tolerant
        Method best = null;
        int bestExtra = Integer.MAX_VALUE;
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (!m.getName().equals(name)) continue;
                if (m.isBridge() || m.isSynthetic() || Modifier.isAbstract(m.getModifiers())) continue;
                Class<?>[] real = m.getParameterTypes();
                if (!isPrefixCompatible(expected, real)) continue;
                int extra = real.length - expected.length;
                if (extra < bestExtra) {
                    best = m;
                    bestExtra = extra;
                }
            }
        }
        if (best == null) return null;
        best.setAccessible(true);
        return new Resolution(best, false);
    }

    static boolean isPrefixCompatible(Class<?>[] expected, Class<?>[] real) {
        if (real.length < expected.length) return false;
        for (int i = 0; i < expected.length; i++) {
            Class<?> e = expected[i];
            Class<?> r = real[i];
            if (e == null) continue;                       // wildcard
            if (e == r) continue;
            if (e.isPrimitive() || r.isPrimitive()) return false;
            if (!e.isAssignableFrom(r)) return false;      // real must be same or narrower
        }
        return true;
    }

    private static boolean hasNull(Class<?>[] types) {
        for (Class<?> t : types) if (t == null) return true;
        return false;
    }

    public static String describe(Class<?>[] types) {
        String[] s = new String[types.length];
        for (int i = 0; i < types.length; i++) s[i] = types[i] == null ? "?" : types[i].getName();
        return Arrays.toString(s);
    }
}
