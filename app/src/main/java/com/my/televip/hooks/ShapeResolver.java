package com.my.televip.hooks;

import com.my.televip.logging.Logger;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Finds a method by its shape (parameter types + return type) instead of its name.
 *
 * Telegram's release builds are shrunk with R8, so small androidx helper classes such as
 * androidx.collection.LongSparseArray end up with one-letter method names that change on every build
 * ("get(long)" became "f(long)" in 12.10.5 and 12.10.6 and will be something else later).
 * Their shape does not change, so it is what we look up.
 */
public final class ShapeResolver {

    private static final Map<String, Method> CACHE = new ConcurrentHashMap<>();
    private static final Set<String> FAILED = ConcurrentHashMap.newKeySet();

    private ShapeResolver() {}

    /**
     * Class returned by a no-argument method of a class whose own names are kept (e.g. MessagesController):
     * lets us find a minified class (StoriesController) through a stable, readable getter.
     */
    public static Class<?> returnTypeOf(Class<?> owner, String getterName) {
        if (owner == null || getterName == null) return null;
        for (Class<?> c = owner; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getName().equals(getterName) && m.getParameterTypes().length == 0
                        && !m.isBridge() && !m.isSynthetic()) {
                    return m.getReturnType();
                }
            }
        }
        return null;
    }

    /** Instance methods "() -> boolean" declared by cls itself. */
    public static java.util.List<Method> noArgBooleans(Class<?> cls) {
        java.util.List<Method> out = new java.util.ArrayList<>();
        if (cls == null) return out;
        for (Method m : cls.getDeclaredMethods()) {
            if (Modifier.isStatic(m.getModifiers()) || m.isBridge() || m.isSynthetic()) continue;
            if (m.getParameterTypes().length == 0 && m.getReturnType() == boolean.class) {
                m.setAccessible(true);
                out.add(m);
            }
        }
        return out;
    }

    /**
     * @param preferredName original (readable) method name, tried first; may be null
     * @return the method if exactly one public instance method has this shape (or the preferred name
     *         matches one of several), otherwise null. Cached per class+shape.
     */
    public static Method unique(Class<?> cls, String preferredName, Class<?> ret, Class<?>... params) {
        if (cls == null) return null;
        String key = cls.getName() + "#" + preferredName + "#" + ret.getName() + "#" + java.util.Arrays.toString(params);
        Method cached = CACHE.get(key);
        if (cached != null) return cached;
        if (FAILED.contains(key)) return null;

        Method byName = null;
        Method only = null;
        int count = 0;
        for (Class<?> c = cls; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (Modifier.isStatic(m.getModifiers()) || m.isBridge() || m.isSynthetic()) continue;
                if (m.getReturnType() != ret) continue;
                if (!java.util.Arrays.equals(m.getParameterTypes(), params)) continue;
                count++;
                only = m;
                if (preferredName != null && m.getName().equals(preferredName)) byName = m;
            }
        }

        Method result = byName != null ? byName : (count == 1 ? only : null);
        if (result == null) {
            FAILED.add(key);
            Logger.w("No unique method with shape " + java.util.Arrays.toString(params) + "->" + ret.getSimpleName()
                    + " in " + cls.getName() + " (candidates: " + count + ")");
            return null;
        }
        result.setAccessible(true);
        if (!result.getName().equals(preferredName)) {
            Logger.l("Resolved " + cls.getName() + "#" + preferredName + " by shape -> " + result.getName());
        }
        CACHE.put(key, result);
        return result;
    }
}
