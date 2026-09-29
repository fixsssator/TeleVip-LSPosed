package com.my.televip.launcher;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

/**
 * Icon of the TeleVip module. It has no Xposed or Telegram dependencies on purpose: it only starts the
 * installed Telegram client with an extra that the hooked LaunchActivity turns into the settings window.
 */
public class LauncherActivity extends Activity {

    /** Keep in sync with ClientManager.Client (that enum cannot be loaded outside the hooked process). */
    private static final String[][] CLIENTS = {
            {"org.telegram.messenger", "Telegram"},
            {"org.telegram.messenger.beta", "Telegram Beta"},
            {"org.telegram.messenger.web", "Telegram (web)"},
            {"org.telegram.plus", "Telegram Plus"},
            {"org.forkgram.messenger", "Forkgram"},
            {"org.forkclient.messenger.beta", "Forkgram Beta"},
            {"org.forkgram.classic", "Forkgram Classic"},
            {"com.iMe.android", "iMe"},
            {"com.iMe.android.web", "iMe (web)"},
            {"com.xplus.messenger", "XPlus"},
            {"ru.dahl.messenger", "Telega"},
            {"xyz.nextalone.nagram", "Nagram"},
            {"nu.gpu.nagram", "NagramX"},
            {"fork.risin42.nagramx", "NagramX (F)"},
            {"app.nicegram", "Nicegram"},
            {"ir.ilmili.telegraph", "Telegraph"},
            {"nekox.messenger.broken", "Momogram"},
            {"tw.nekomimi.nekogram", "Nekogram"},
            {"uz.unnarsx.cherrygram", "Cherrygram"},
            {"org.telegram.group", "Turrit"},
            {"com.tgconnect.android", "TGConnect"},
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final boolean ru = "ru".equals(Locale.getDefault().getLanguage());

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        root.setPadding(pad, pad * 2, pad, pad);

        TextView title = new TextView(this);
        title.setText("TeleVip");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);

        TextView hint = new TextView(this);
        hint.setText(ru
                ? "Откройте настройки TeleVip внутри Telegram. Модуль должен быть включён в LSPosed для этого приложения."
                : "Open the TeleVip settings inside Telegram. The module must be enabled for that app in LSPosed.");
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        hint.setPadding(0, dp(8), 0, dp(20));
        root.addView(hint);

        PackageManager pm = getPackageManager();
        int found = 0;
        for (final String[] c : CLIENTS) {
            final Intent launch = pm.getLaunchIntentForPackage(c[0]);
            if (launch == null) continue;
            found++;
            Button b = new Button(this);
            b.setAllCaps(false);
            b.setText((ru ? "Настройки в " : "Settings in ") + c[1] + "\n" + c[0]);
            b.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    try {
                        Intent i = new Intent(launch);
                        i.putExtra("televip_open_settings", true);
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(i);
                    } catch (Throwable t) {
                        Toast.makeText(LauncherActivity.this, String.valueOf(t), Toast.LENGTH_LONG).show();
                    }
                }
            });
            root.addView(b, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        if (found == 0) {
            TextView none = new TextView(this);
            none.setText(ru ? "Поддерживаемый клиент Telegram не найден." : "No supported Telegram client found.");
            none.setGravity(Gravity.CENTER);
            root.addView(none);
        }

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
