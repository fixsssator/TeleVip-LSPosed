package com.my.televip.settings.hook;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.view.View;

import com.my.televip.Class.ClassLoad;
import com.my.televip.Class.ClassNames;
import com.my.televip.application.ApplicationLoaderHook;
import com.my.televip.base.BaseMethodHook;
import com.my.televip.dex.DexNames;
import com.my.televip.hooks.HookStatus;
import com.my.televip.logging.Logger;
import com.my.televip.settings.nativeui.NativeSettings;
import com.my.televip.utils.Utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The "Ghost mode" row inside Telegram's own Settings screen, found by what it is instead of by name.
 *
 * In Telegram's release builds SettingsActivity and UItem have one-letter names, and R8 turned
 * fillItems(...) and onClick(...) into static methods of the fragment class:
 *     static void  x(SettingsFragment, ArrayList)          // builds the list (fillItems)
 *     static void  y(SettingsFragment, UItem)              // click handler   (onClick)
 *     static boolean z(SettingsFragment, UItem, View)      // long click
 * The fragment is the only direct subclass of BaseFragment (known through the readable ProfileActivity)
 * that has all three. Its list is filled with ready-made UItem objects, so the new row is a copy of an
 * existing text-and-subtitle row with another title; no Telegram UI class has to be created by name.
 */
public final class GhostSettingsRow {

    private static final String PREFS = "televip_ui";

    private static boolean installed = false;
    private static final Map<Object, Boolean> OURS = new IdentityHashMap<>();

    /** The three methods that make up the Settings screen's list logic. */
    static final class Located {
        final Class<?> fragment;
        final Method fill;
        final Method click;
        final Class<?> item;

        Located(Class<?> fragment, Method fill, Method click, Class<?> item) {
            this.fragment = fragment;
            this.fill = fill;
            this.click = click;
            this.item = item;
        }
    }

    private GhostSettingsRow() {}

    public static void install() {
        try {
            if (installed) return;
            installed = true;

            // Plain builds keep SettingsActivity readable and are handled by SettingsHook.
            if (ClassLoad.getClass("org.telegram.ui.SettingsActivity", Utils.classLoader, false) != null) return;

            Class<?> profile = ClassLoad.getClass(ClassNames.PROFILE_ACTIVITY, Utils.classLoader, false);
            if (profile == null || profile.getSuperclass() == null) {
                HookStatus.failed("Settings row");
                return;
            }
            Located loc = locate(profile.getSuperclass());
            if (loc == null) {
                Logger.w("Settings row: the Settings screen was not found in this Telegram build");
                HookStatus.failed("Settings row");
                return;
            }

            de.robv.android.xposed.XposedBridge.hookMethod(loc.fill, new BaseMethodHook() {
                @Override
                @SuppressWarnings("unchecked")
                protected void afterMethod(MethodHookParam param) {
                    try {
                        onFill((ArrayList<Object>) param.args[1], loc.item);
                    } catch (Throwable t) {
                        Logger.e(t);
                    }
                }
            });
            de.robv.android.xposed.XposedBridge.hookMethod(loc.click, new BaseMethodHook() {
                @Override
                protected void beforeMethod(MethodHookParam param) {
                    boolean ours;
                    synchronized (OURS) {
                        ours = OURS.containsKey(param.args[1]);
                    }
                    if (!ours) return;
                    param.setResult(null);                    // do not run the copied row's own action
                    Activity a = Utils.getCurrentActivity();
                    if (a != null) NativeSettings.show(a);
                }
            });
            HookStatus.ok();
            Logger.l("Settings row hooked in " + loc.fragment.getName() + " (" + loc.fill.getName() + "/" + loc.click.getName() + ")");
        } catch (Throwable t) {
            Logger.e(t);
        }
    }

    // ------------------------------------------------------------------------------------------------

    private static Located locate(Class<?> baseFragment) {
        String cached = cachedName();
        if (cached != null) {
            Located l = tryClass(cached);
            if (l != null) return l;
        }
        List<String> names = DexNames.subclassesInApks(apkPaths(), "L" + baseFragment.getName().replace('.', '/') + ";");
        Located found = null;
        for (String name : names) {
            Located l = tryClass(name);
            if (l == null) continue;
            if (found != null) return null;                  // ambiguous: better nothing than the wrong screen
            found = l;
        }
        if (found != null) saveName(found.fragment.getName());
        return found;
    }

    private static Located tryClass(String name) {
        try {
            return check(Class.forName(name, false, Utils.classLoader));
        } catch (Throwable t) {
            return null;
        }
    }

    /** The shape test; package-private for unit tests. */
    static Located check(Class<?> c) {
        if (c == null) return null;
        Method fill = null, click = null;
        Class<?> itemOfLong = null;
        int fills = 0, clicks = 0;
        List<Method> longs = new ArrayList<>();
        for (Method m : c.getDeclaredMethods()) {
            if (!Modifier.isStatic(m.getModifiers())) continue;
            Class<?>[] p = m.getParameterTypes();
            Class<?> r = m.getReturnType();
            if (p.length == 0 || p[0] != c) continue;
            if (p.length == 2 && p[1] == ArrayList.class && r == void.class) {
                fill = m;
                fills++;
            } else if (p.length == 2 && !p[1].isPrimitive() && p[1] != ArrayList.class && r == void.class) {
                click = m;
                clicks++;
            } else if (p.length == 3 && p[2] == View.class && r == boolean.class && !p[1].isPrimitive()) {
                longs.add(m);
            }
        }
        if (fills != 1 || clicks < 1 || longs.size() != 1) return null;
        itemOfLong = longs.get(0).getParameterTypes()[1];
        // the click handler is the (self, item) one whose item type matches the long-click's
        Method match = null;
        int matches = 0;
        for (Method m : c.getDeclaredMethods()) {
            Class<?>[] p = m.getParameterTypes();
            if (Modifier.isStatic(m.getModifiers()) && p.length == 2 && p[0] == c && p[1] == itemOfLong && m.getReturnType() == void.class) {
                match = m;
                matches++;
            }
        }
        if (matches != 1) return null;
        fill.setAccessible(true);
        match.setAccessible(true);
        return new Located(c, fill, match, itemOfLong);
    }

    // ------------------------------------------------------------------------------------------------

    static void onFill(ArrayList<Object> list, Class<?> itemClass) throws Throwable {
        if (list == null) return;
        synchronized (OURS) {
            if (OURS.size() > 64) OURS.clear();
        }
        for (int i = 0; i < list.size(); i++) {
            Object template = list.get(i);
            if (template == null || template.getClass() != itemClass) continue;
            if (charSequenceFields(itemClass).size() < 2) return;
            if (!hasText(template, 2)) continue;           // first row with a title AND a subtitle
            Object ghost = cloneAsRow(template, title(), subtitle());
            synchronized (OURS) {
                OURS.put(ghost, Boolean.TRUE);
            }
            list.add(i, ghost);
            return;
        }
    }

    private static boolean hasText(Object item, int atLeast) throws Throwable {
        int n = 0;
        for (Field f : charSequenceFields(item.getClass())) {
            if (f.get(item) != null) n++;
        }
        return n >= atLeast;
    }

    /** Shallow copy of the whole object, then title and subtitle replaced. */
    static Object cloneAsRow(Object template, String title, String subtitle) throws Throwable {
        Class<?> cls = template.getClass();
        Object copy = newBlank(cls);
        for (Class<?> c = cls; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                f.setAccessible(true);
                f.set(copy, f.get(template));
            }
        }
        // R8 names fields in declaration order, so the first two CharSequence fields are text and subtext.
        List<Field> cs = charSequenceFields(cls);
        cs.get(0).set(copy, title);
        cs.get(1).set(copy, subtitle);
        return copy;
    }

    /** Instance CharSequence fields declared by the item class itself, in declaration order. */
    static List<Field> charSequenceFields(Class<?> cls) {
        List<Field> out = new ArrayList<>();
        for (Field f : cls.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers()) || f.getType() != CharSequence.class) continue;
            f.setAccessible(true);
            out.add(f);
        }
        return out;
    }

    /** A fresh object of the item class: its (int) constructor (R8 kept it), else without any constructor. */
    private static Object newBlank(Class<?> cls) throws Throwable {
        try {
            java.lang.reflect.Constructor<?> c = cls.getDeclaredConstructor(int.class);
            c.setAccessible(true);
            return c.newInstance(0);
        } catch (Throwable ignored) {
            Field f = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
            f.setAccessible(true);
            Object unsafe = f.get(null);
            return unsafe.getClass().getMethod("allocateInstance", Class.class).invoke(unsafe, cls);
        }
    }

    private static String title() {
        return "ru".equals(Locale.getDefault().getLanguage()) ? "Режим призрака" : "Ghost mode";
    }

    private static String subtitle() {
        return "ru".equals(Locale.getDefault().getLanguage()) ? "Настройки TeleVip" : "TeleVip settings";
    }

    // ------------------------------------------------------------------------------------------------

    private static List<String> apkPaths() {
        List<String> out = new ArrayList<>();
        try {
            ApplicationInfo ai = ApplicationLoaderHook.getApplicationContext().getApplicationInfo();
            out.add(ai.sourceDir);
            if (ai.splitSourceDirs != null) for (String s : ai.splitSourceDirs) out.add(s);
        } catch (Throwable t) {
            Logger.e(t);
        }
        return out;
    }

    private static String cacheKey() {
        try {
            android.content.Context c = ApplicationLoaderHook.getApplicationContext();
            android.content.pm.PackageInfo pi = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
            return "settings_cls_" + pi.versionName + "_" + pi.getLongVersionCode() + "_" + pi.lastUpdateTime;
        } catch (Throwable t) {
            return null;
        }
    }

    private static String cachedName() {
        try {
            String key = cacheKey();
            if (key == null) return null;
            SharedPreferences sp = ApplicationLoaderHook.getApplicationContext().getSharedPreferences(PREFS, 0);
            return sp.getString(key, null);
        } catch (Throwable t) {
            return null;
        }
    }

    private static void saveName(String name) {
        try {
            String key = cacheKey();
            if (key == null) return;
            ApplicationLoaderHook.getApplicationContext().getSharedPreferences(PREFS, 0).edit().putString(key, name).apply();
        } catch (Throwable ignored) {
        }
    }
}
