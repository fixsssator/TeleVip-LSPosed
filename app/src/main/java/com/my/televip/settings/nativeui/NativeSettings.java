package com.my.televip.settings.nativeui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Process;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.my.televip.Configs.ConfigItem;
import com.my.televip.Configs.ConfigManager;
import com.my.televip.hooks.HookStatus;
import com.my.televip.language.Keys;
import com.my.televip.language.Translator;
import com.my.televip.logging.Logger;
import com.my.televip.utils.Utils;

import java.util.List;
import java.util.Locale;

/**
 * TeleVip settings screen made only of plain Android views.
 *
 * Telegram's release builds rename their own UI classes (and change the names on every build), so the
 * old screen that was glued into Telegram's Settings could not be found any more. This one does not use
 * a single Telegram UI class: it is opened by an Intent extra sent from the module's own launcher icon
 * and hooked on LaunchActivity.onCreate / onNewIntent, which are Android lifecycle methods whose names
 * can never be obfuscated.
 */
public final class NativeSettings {

    /** Boolean extra the launcher puts on the intent that starts Telegram. */
    public static final String EXTRA_OPEN = "televip_open_settings";

    private static Dialog current;

    private NativeSettings() {}

    public static boolean wantsSettings(Intent intent) {
        try {
            return intent != null && intent.getBooleanExtra(EXTRA_OPEN, false);
        } catch (Throwable t) {
            return false;
        }
    }

    /** Call from LaunchActivity.onCreate/onNewIntent; waits until the activity is on screen. */
    public static void showWhenReady(final Activity activity, final Intent intent) {
        try {
            if (activity == null || !wantsSettings(intent)) return;
            intent.removeExtra(EXTRA_OPEN);                 // do not re-open on rotation / task restore
            Logger.l("Settings requested from the TeleVip icon");
            final View decor = activity.getWindow().getDecorView();
            decor.postDelayed(new Runnable() {
                @Override public void run() { show(activity); }
            }, 700);
        } catch (Throwable t) {
            Logger.e(t);
        }
    }

    public static void show(final Activity activity) {
        try {
            if (activity == null || activity.isFinishing()) return;
            if (current != null && current.isShowing()) return;
            if (ConfigManager.getItems().isEmpty()) {
                Toast.makeText(activity, "TeleVip is not loaded yet, try again in a few seconds", Toast.LENGTH_LONG).show();
                return;
            }

            final Palette p = new Palette(activity);
            final Dialog dialog = new Dialog(activity, android.R.style.Theme_Material_NoActionBar);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

            LinearLayout root = new LinearLayout(activity);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setBackgroundColor(p.bg);

            // top bar
            LinearLayout bar = new LinearLayout(activity);
            bar.setOrientation(LinearLayout.HORIZONTAL);
            bar.setGravity(Gravity.CENTER_VERTICAL);
            bar.setBackgroundColor(p.bar);
            bar.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 8), dp(activity, 12));
            TextView title = text(activity, "TeleVip", 20, p.barText, true);
            bar.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            TextView close = text(activity, "\u2715", 22, p.barText, false);
            close.setPadding(dp(activity, 16), dp(activity, 8), dp(activity, 16), dp(activity, 8));
            close.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { dialog.dismiss(); }
            });
            bar.addView(close);
            root.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            // list
            ScrollView scroll = new ScrollView(activity);
            LinearLayout list = new LinearLayout(activity);
            list.setOrientation(LinearLayout.VERTICAL);
            list.setPadding(0, 0, 0, dp(activity, 32));
            scroll.addView(list, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

            fill(activity, dialog, list, p);

            dialog.setContentView(root);
            Window w = dialog.getWindow();
            if (w != null) {
                w.setBackgroundDrawable(new ColorDrawable(p.bg));
                w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            }
            current = dialog;
            dialog.show();
        } catch (Throwable t) {
            Logger.e(t);
        }
    }

    // ---------------------------------------------------------------------------------------------

    private static void fill(final Activity a, final Dialog dialog, LinearLayout list, Palette p) {
        List<ConfigItem> items = ConfigManager.getItems();
        boolean lastWasSpacer = true;
        for (final ConfigItem item : items) {
            if (item == null) continue;
            try {
                switch (item.getType()) {
                    case ConfigItem.HEADER:
                        list.addView(header(a, Translator.get(item.getKey()), p));
                        lastWasSpacer = false;
                        break;

                    case ConfigItem.SWITCH:
                        list.addView(switchRow(a, item, null, p));
                        lastWasSpacer = false;
                        break;

                    case ConfigItem.EXPANDABLE_SWITCH:
                        List<ConfigItem> children = item.getChildren();
                        if (children == null) break;
                        list.addView(subHeader(a, Translator.get(item.getKey()), p));
                        for (ConfigItem child : children) list.addView(switchRow(a, child, item, p));
                        lastWasSpacer = false;
                        break;

                    case ConfigItem.INFO:
                        list.addView(info(a, Translator.get(item.getKey()), p));
                        break;

                    case ConfigItem.DIVIDER:
                        if (!lastWasSpacer) list.addView(spacer(a, p));
                        lastWasSpacer = true;
                        break;

                    case ConfigItem.TEXT:
                        list.addView(textRow(a, dialog, item, p));
                        lastWasSpacer = false;
                        break;

                    default:
                        break;
                }
            } catch (Throwable t) {
                Logger.e(t);
            }
        }
        try {
            list.addView(spacer(a, p));
            list.addView(ghostRow(a, p));
            list.addView(statusBlock(a, p));
        } catch (Throwable t) {
            Logger.e(t);
        }
    }

    /** Optional floating ghost over Telegram; a view-only setting, kept outside the feature config. */
    private static View ghostRow(final Activity a, Palette p) {
        boolean ru = "ru".equals(Locale.getDefault().getLanguage());
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(a, 16), dp(a, 12), dp(a, 16), dp(a, 12));
        row.setBackground(ripple(a, p));

        LinearLayout labels = new LinearLayout(a);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(text(a, ru ? "Плавающий призрак в Telegram" : "Floating ghost in Telegram", 16, p.text, false));
        labels.addView(text(a, ru ? "Без него: иконка TeleVip или плитка в шторке" : "Without it: the TeleVip icon or the Quick Settings tile", 13, p.sub, false));
        row.addView(labels, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        final Switch sw = new Switch(a);
        sw.setChecked(GhostButton.isEnabled(a));
        row.addView(sw);
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton b, boolean checked) {
                GhostButton.setEnabled(a, checked);
            }
        });
        row.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { sw.toggle(); }
        });
        return row;
    }

    /** What was hooked in this Telegram build; explains why a switch may do nothing. */
    private static View statusBlock(Context c, Palette p) {
        boolean ru = "ru".equals(Locale.getDefault().getLanguage());
        List<String> bad = HookStatus.failures();
        StringBuilder sb = new StringBuilder();
        sb.append("TeleVip ").append(Utils.MODULE_VERSION).append(" / Telegram ").append(Logger.hostVersion()).append("\n");
        sb.append(ru ? "Подключено хуков: " : "Hooks installed: ").append(HookStatus.okCount()).append("\n");
        if (bad.isEmpty()) {
            sb.append(ru ? "Все найденные хуки подключены." : "Every hook was found.");
        } else {
            sb.append(ru ? "Не найдено в этой версии Telegram (связанные функции не сработают): "
                    : "Not found in this Telegram build (related features will not work): ");
            for (int i = 0; i < bad.size(); i++) sb.append(i > 0 ? ", " : "").append(bad.get(i));
        }
        TextView t = text(c, sb.toString(), 12, p.sub, false);
        t.setPadding(dp(c, 16), dp(c, 12), dp(c, 16), dp(c, 12));
        t.setTextIsSelectable(true);
        return t;
    }

    private static View switchRow(final Activity a, final ConfigItem item, final ConfigItem parent, Palette p) {
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(a, parent != null ? 32 : 16), dp(a, 12), dp(a, 16), dp(a, 12));
        row.setBackground(ripple(a, p));

        LinearLayout labels = new LinearLayout(a);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(text(a, Translator.get(item.getKey()), 16, p.text, false));
        String sub = item.getValue() != null ? item.getValue()
                : item.isRestartRequired() ? Translator.get(Keys.RestartRequired) : null;
        if (sub != null) labels.addView(text(a, sub, 13, p.sub, false));
        row.addView(labels, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        final Switch sw = new Switch(a);
        sw.setChecked(item.isEnable());
        row.addView(sw);

        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton b, boolean checked) {
                apply(a, item, parent, checked);
            }
        });
        row.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { sw.toggle(); }
        });
        return row;
    }

    /** Same behaviour as the old screen: persist, run the feature init, refresh the group switch. */
    private static void apply(Activity a, ConfigItem item, ConfigItem parent, boolean checked) {
        try {
            item.setEnable(checked);
            item.run();
            if (parent != null && parent.getChildren() != null) {
                boolean any = false;
                for (ConfigItem c : parent.getChildren()) if (c.isEnable()) { any = true; break; }
                parent.setEnable(any);
            }
            if (item.isRestartRequired()) {
                Toast.makeText(a, Translator.get(Keys.RestartRequired), Toast.LENGTH_SHORT).show();
            }
        } catch (Throwable t) {
            Logger.e(t);
        }
    }

    private static View textRow(final Activity a, final Dialog dialog, final ConfigItem item, Palette p) {
        final String key = item.getKey();
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(a, 16), dp(a, 14), dp(a, 16), dp(a, 14));
        row.setBackground(ripple(a, p));

        final boolean isCalendar = Keys.Calendar.equals(key);
        row.addView(text(a, Translator.get(key), 16, isCalendar ? p.text : p.accent, false));
        if (isCalendar) {
            String v = calendarName(item.getCustomCalendar());
            row.addView(text(a, v, 13, p.sub, false));
        }

        row.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    if (Keys.DeveloperChannel.equals(key)) {
                        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/t_l0_e"));
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        a.startActivity(i);
                    } else if (Keys.RestartApp.equals(key)) {
                        restart(a);
                    } else if (isCalendar) {
                        final String[] names = {calendarName(0), calendarName(1), calendarName(2)};
                        new AlertDialog.Builder(a)
                                .setTitle(Translator.get(Keys.Calendar))
                                .setSingleChoiceItems(names, item.getCustomCalendar(), new android.content.DialogInterface.OnClickListener() {
                                    @Override public void onClick(android.content.DialogInterface d, int which) {
                                        try {
                                            item.setCustomCalendar(which);
                                            item.run();
                                        } catch (Throwable t) {
                                            Logger.e(t);
                                        }
                                        d.dismiss();
                                        dialog.dismiss();
                                        show(a);      // rebuild to show the new value
                                    }
                                })
                                .show();
                    }
                } catch (Throwable t) {
                    Logger.e(t);
                }
            }
        });
        return row;
    }

    private static String calendarName(int i) {
        switch (i) {
            case 1: return Translator.get(Keys.Hijri);
            case 2: return Translator.get(Keys.Persian);
            default: return Translator.get(Keys.Gregorian);
        }
    }

    private static void restart(Activity a) {
        Intent intent = a.getPackageManager().getLaunchIntentForPackage(Utils.pkgName);
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            a.startActivity(intent);
        }
        a.finishAffinity();
        Process.killProcess(Process.myPid());
    }

    // ---- small view helpers ------------------------------------------------------------------------

    private static TextView header(Context c, String s, Palette p) {
        TextView t = text(c, s, 14, p.accent, true);
        t.setPadding(dp(c, 16), dp(c, 20), dp(c, 16), dp(c, 6));
        return t;
    }

    private static TextView subHeader(Context c, String s, Palette p) {
        TextView t = text(c, s, 15, p.text, true);
        t.setPadding(dp(c, 16), dp(c, 12), dp(c, 16), dp(c, 2));
        return t;
    }

    private static TextView info(Context c, String s, Palette p) {
        TextView t = text(c, s, 13, p.sub, false);
        t.setPadding(dp(c, 16), dp(c, 4), dp(c, 16), dp(c, 8));
        return t;
    }

    private static View spacer(Context c, Palette p) {
        View v = new View(c);
        v.setBackgroundColor(p.divider);
        v.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(c, 10)));
        return v;
    }

    private static TextView text(Context c, String s, int sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private static android.graphics.drawable.Drawable ripple(Context c, Palette p) {
        TypedValue tv = new TypedValue();
        if (c.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true)) {
            return c.getResources().getDrawable(tv.resourceId, c.getTheme());
        }
        return new ColorDrawable(Color.TRANSPARENT);
    }

    private static int dp(Context c, int v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    private static final class Palette {
        final int bg, bar, barText, text, sub, accent, divider;

        Palette(Context c) {
            boolean night = (c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                    == Configuration.UI_MODE_NIGHT_YES;
            bg = night ? 0xFF17212B : 0xFFFFFFFF;
            bar = night ? 0xFF242F3D : 0xFF527DA3;
            barText = 0xFFFFFFFF;
            text = night ? 0xFFF5F5F5 : 0xFF111111;
            sub = night ? 0xFF7F91A4 : 0xFF6D7883;
            accent = night ? 0xFF6AB3F3 : 0xFF3390EC;
            divider = night ? 0xFF0E1621 : 0xFFEFEFF3;
        }
    }
}
