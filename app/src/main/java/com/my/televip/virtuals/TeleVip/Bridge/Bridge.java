package com.my.televip.virtuals.TeleVip.Bridge;

import android.content.Context;
import android.util.TypedValue;
import android.view.View;

import com.my.televip.Class.ClassLoad;
import com.my.televip.Class.ClassNames;
import com.my.televip.base.BaseMethodHook;
import com.my.televip.dex.DexInjector;
import com.my.televip.hooks.HMethod;
import com.my.televip.logging.Logger;
import com.my.televip.obfuscate.Obfuscate;
import com.my.televip.settings.controller.SettingsController;
import com.my.televip.settings.ui.SettingsAdapter;
import com.my.televip.ui.ThemeColors;
import com.my.televip.utils.Utils;
import com.my.televip.ui.Cells.ExpandableTextCheckCell;
import com.my.televip.virtuals.ui.Cells.HeaderCell;
import com.my.televip.virtuals.ui.Cells.ShadowSectionCell;
import com.my.televip.virtuals.ui.Cells.TextCheckCell;
import com.my.televip.ui.Cells.TextInfoCell;
import com.my.televip.virtuals.ui.Cells.TextSettingsCell;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class Bridge {

    public static final TypedValue outValue = new TypedValue();

    public static Object getLayoutManager(Context context){
        return XposedHelpers.callStaticMethod(ClassLoad.getClass(ClassNames.SETTINGS_ADAPTER, DexInjector.classLoader), "getLayoutManager", context);
    }


    private static Class<?> loadInjected(String name) {
        try {
            Class<?> c = XposedHelpers.findClassIfExists(name, DexInjector.classLoader);
            if (c == null) Logger.w("Injected class not found: " + name);
            return c;
        } catch (Throwable t) {
            StringBuilder why = new StringBuilder(String.valueOf(t));
            for (Throwable c = t.getCause(); c != null; c = c.getCause()) why.append(" <= ").append(c);
            Logger.w("Injected class cannot be loaded: " + name + " -> " + why);
            return null;
        }
    }

    private static void hookCtor(Class<?> cls, String label, Object... typesAndCallback) {
        try {
            if (cls == null) {
                Logger.w("Skip constructor hook, class missing: " + label);
                return;
            }
            XposedHelpers.findAndHookConstructor(cls, typesAndCallback);
        } catch (Throwable t) {
            Logger.e(t);
        }
    }

    /** Logs what Telegram actually has where the settings adapter expects androidx.recyclerview. */
    private static void diagnoseRecyclerView() {
        try {
            ClassLoader cl = Utils.classLoader;
            StringBuilder sb = new StringBuilder("settings-adapter diagnostics:");
            String[] names = {
                    "androidx.recyclerview.widget.RecyclerView",
                    "androidx.recyclerview.widget.RecyclerView$Adapter",
                    "androidx.recyclerview.widget.RecyclerView$ViewHolder",
                    "androidx.recyclerview.widget.LinearLayoutManager",
                    "androidx.annotation.NonNull"
            };
            for (String n : names) {
                boolean ok;
                try { ok = XposedHelpers.findClassIfExists(n, cl) != null; } catch (Throwable t) { ok = false; }
                sb.append(" ").append(n.substring(n.lastIndexOf('.') + 1)).append("=").append(ok);
            }
            try {
                Class<?> rlv = XposedHelpers.findClassIfExists(
                        Obfuscate.getClassName("org.telegram.ui.Components.RecyclerListView"), cl);
                sb.append(" | Telegram RecyclerListView extends ")
                        .append(rlv == null ? "(class not found)" : String.valueOf(rlv.getSuperclass()));
            } catch (Throwable t) {
                sb.append(" | RecyclerListView lookup failed: ").append(t);
            }
            Logger.w(sb.toString());
        } catch (Throwable t) {
            Logger.e(t);
        }
    }

    public static void init(SettingsController settingsController){
        if (DexInjector.classLoader == null) return;
        try {
            Class<?> bridgeClass = loadInjected("com.televip.SettingsAdapter.Bridge");
            Class<?> textCheckCellClass = loadInjected("com.televip.SettingsAdapter.SettingsAdapter$TextCheckCellHolder");
            Class<?> expandableTextCheckCellClass = loadInjected("com.televip.SettingsAdapter.SettingsAdapter$ExpandableTextCheckCellHolder");
            Class<?> textSettingsCellClass = loadInjected("com.televip.SettingsAdapter.SettingsAdapter$TextSettingsCellHolder");
            Class<?> headerCellClass = loadInjected("com.televip.SettingsAdapter.SettingsAdapter$HeaderCellHolder");
            Class<?> shadowSectionCellClass = loadInjected("com.televip.SettingsAdapter.SettingsAdapter$ShadowSectionCellHolder");
            Class<?> textInfoCellClass = loadInjected("com.televip.SettingsAdapter.SettingsAdapter$TextInfoCellHolder");

            if (bridgeClass == null || textCheckCellClass == null || expandableTextCheckCellClass == null
                    || textSettingsCellClass == null || headerCellClass == null
                    || shadowSectionCellClass == null || textInfoCellClass == null) {
                Logger.w("TeleVip settings screen is unavailable: its helper classes could not be loaded in this Telegram build.");
                diagnoseRecyclerView();
                return;
            }

            HMethod.hookMethod(bridgeClass, "getRow", int.class, new BaseMethodHook() {
                @Override
                protected void beforeMethod(XC_MethodHook.MethodHookParam param) {
                    param.setResult(SettingsAdapter.getRow((int) param.args[0]));
                }
            });

            HMethod.hookMethod(bridgeClass, "log", String.class, new BaseMethodHook() {
                @Override
                protected void beforeMethod(XC_MethodHook.MethodHookParam param) {
                    Logger.l((String) param.args[0]);
                }
            });

            HMethod.hookMethod(bridgeClass, "getRowCount", new BaseMethodHook() {
                @Override
                protected void beforeMethod(XC_MethodHook.MethodHookParam param) {
                    param.setResult(SettingsAdapter.getRowCount());
                }
            });
            
            HMethod.hookMethod(bridgeClass, "onBindViewHolder", Object.class, int.class, int.class, new BaseMethodHook() {
                @Override
                protected void beforeMethod(XC_MethodHook.MethodHookParam param) {
                   SettingsAdapter.onBindViewHolder(param.args[0], settingsController, (int) param.args[1], (int) param.args[2]);
                }
            });

            settingsController.getContext().getTheme().resolveAttribute(
                    android.R.attr.selectableItemBackground,
                    outValue,
                    true
            );

            hookCtor(textCheckCellClass, "textCheckCellClass", View.class, Object.class, new BaseMethodHook() {
                @Override
                protected void beforeMethod(XC_MethodHook.MethodHookParam param) {
                    TextCheckCell textCheckCell = createTextCheckCell(settingsController.getContext());
                    param.args[0] = textCheckCell.getView();
                    param.args[1] = textCheckCell.textCell;
                }
            });

            hookCtor(expandableTextCheckCellClass, "expandableTextCheckCellClass", View.class, Object.class, new BaseMethodHook() {
                @Override
                protected void beforeMethod(XC_MethodHook.MethodHookParam param) {
                    ExpandableTextCheckCell expandableTextCheckCell = createExpandableTextCheckCell(settingsController.getContext());
                    param.args[0] = expandableTextCheckCell;
                    param.args[1] = expandableTextCheckCell;
                }
            });

            hookCtor(textSettingsCellClass, "textSettingsCellClass", View.class, Object.class, new BaseMethodHook() {
                @Override
                protected void beforeMethod(XC_MethodHook.MethodHookParam param) {
                    TextSettingsCell textSettingsCell = createTextSettingsCell(settingsController.getContext());
                    param.args[0] = textSettingsCell.getView();
                    param.args[1] = textSettingsCell.textSettingsCell;
                }
            });

            hookCtor(headerCellClass, "headerCellClass", View.class, Object.class, new BaseMethodHook() {
                @Override
                protected void beforeMethod(XC_MethodHook.MethodHookParam param) {
                    HeaderCell header = createHeaderCell(settingsController.getContext());
                    param.args[0] = header.getView();
                    param.args[1] = header.headerCell;
                }
            });

            hookCtor(shadowSectionCellClass, "shadowSectionCellClass", View.class, new BaseMethodHook() {
                @Override
                protected void beforeMethod(XC_MethodHook.MethodHookParam param) {
                    param.args[0] = new ShadowSectionCell(settingsController.getContext()).getView();
                }
            });

            hookCtor(textInfoCellClass, "textInfoCellClass", View.class, new BaseMethodHook() {
                @Override
                protected void beforeMethod(XC_MethodHook.MethodHookParam param) {
                    TextInfoCell textInfoCell = createTextInfoCell(settingsController.getContext());
                    param.args[0] = textInfoCell;
                }
            });

        } catch (Throwable e){
            Logger.e(e);
        }
    }

    public static TextCheckCell createTextCheckCell(Context context) {

        TextCheckCell textCheckCell = new TextCheckCell(context);

        textCheckCell.getView().setBackgroundColor(
                ThemeColors.getBackgroundWhiteOrBlueColor()
        );

        textCheckCell.getView().setBackgroundResource(outValue.resourceId);
        textCheckCell.getView().setClickable(true);
        textCheckCell.getView().setFocusable(true);

        return textCheckCell;
    }
    public static ExpandableTextCheckCell createExpandableTextCheckCell(Context context) {

        ExpandableTextCheckCell expandableTextCheckCell = new ExpandableTextCheckCell(context);

        expandableTextCheckCell.setBackgroundColor(
                ThemeColors.getBackgroundWhiteOrBlueColor()
        );

        expandableTextCheckCell.setBackgroundResource(outValue.resourceId);
        expandableTextCheckCell.setBChildResource(outValue.resourceId);
        expandableTextCheckCell.setClickable(true);
        expandableTextCheckCell.setFocusable(true);

        return expandableTextCheckCell;
    }

    public static TextSettingsCell createTextSettingsCell(Context context) {
        TextSettingsCell textSettingsCell = new TextSettingsCell(context);
        textSettingsCell.getView().setBackgroundColor(ThemeColors.getBackgroundWhiteOrBlueColor());
        textSettingsCell.getView().setBackgroundResource(outValue.resourceId);
        textSettingsCell.getView().setClickable(true);
        textSettingsCell.getView().setFocusable(true);

        return textSettingsCell;
    }
    public static HeaderCell createHeaderCell(Context context) {
        HeaderCell header = new HeaderCell(context);
        header.getView().setBackgroundColor(ThemeColors.getBackgroundWhiteOrBlueColor());
        return header;
    }

    public static TextInfoCell createTextInfoCell(Context context) {
        TextInfoCell textInfoCell = new TextInfoCell(context);
        textInfoCell.setBackgroundColor(ThemeColors.getBackgroundGrayColor());
        return textInfoCell;
    }

}
