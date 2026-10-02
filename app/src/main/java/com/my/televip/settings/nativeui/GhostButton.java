package com.my.televip.settings.nativeui;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.my.televip.Drawable.GhostDrawable;
import com.my.televip.logging.Logger;

/**
 * A small draggable ghost on top of Telegram's main window that opens the TeleVip settings.
 *
 * The old entry (a "Ghost mode" row inside Telegram's Settings screen) cannot be added any more:
 * Telegram's release builds rename that screen's classes on every build. A view added to the activity's
 * decor view does not depend on any of them.
 *
 * Tap = settings, drag = move, long press = hide until Telegram is restarted.
 */
public final class GhostButton {

    private static final String TAG = "televip_ghost_button";

    private static boolean hiddenThisRun = false;

    private GhostButton() {}

    private static final String PREFS = "televip_ui";

    /** Off by default: the settings open from the TeleVip icon or the Quick Settings tile. */
    public static boolean isEnabled(Activity activity) {
        try {
            return activity.getSharedPreferences(PREFS, 0).getBoolean("ghost_enabled", false);
        } catch (Throwable t) {
            return false;
        }
    }

    public static void setEnabled(Activity activity, boolean on) {
        try {
            activity.getSharedPreferences(PREFS, 0).edit().putBoolean("ghost_enabled", on).apply();
            if (on) {
                hiddenThisRun = false;
                attach(activity);
            } else {
                View v = activity.getWindow().getDecorView().findViewWithTag(TAG);
                if (v != null) ((ViewGroup) v.getParent()).removeView(v);
            }
        } catch (Throwable t) {
            Logger.e(t);
        }
    }

    public static void attach(final Activity activity) {
        try {
            if (hiddenThisRun || activity == null || activity.isFinishing() || !isEnabled(activity)) return;
            final ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
            if (decor.findViewWithTag(TAG) != null) return;

            final int size = dp(activity, 40);
            ImageView ghost = new ImageView(activity);
            ghost.setTag(TAG);
            ghost.setImageDrawable(new GhostDrawable());
            int pad = dp(activity, 8);
            ghost.setPadding(pad, pad, pad, pad);
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            bg.setColor(0xE63390EC);
            ghost.setBackground(bg);
            ghost.setAlpha(0.8f);
            ghost.setElevation(dp(activity, 6));
            ghost.setContentDescription("TeleVip");

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(size, size, Gravity.TOP | Gravity.START);
            int screenW = activity.getResources().getDisplayMetrics().widthPixels;
            int screenH = activity.getResources().getDisplayMetrics().heightPixels;
            android.content.SharedPreferences prefs = activity.getSharedPreferences("televip_ui", 0);
            lp.leftMargin = Math.min(Math.max(0, prefs.getInt("ghost_x", screenW - size - dp(activity, 6))), Math.max(0, screenW - size));
            lp.topMargin = Math.min(Math.max(0, prefs.getInt("ghost_y", screenH / 3)), Math.max(0, screenH - size));

            ghost.setOnTouchListener(new View.OnTouchListener() {
                float downX, downY;
                int startLeft, startTop;
                long downTime;
                boolean moved, longFired;

                @Override
                public boolean onTouch(View v, MotionEvent e) {
                    FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) v.getLayoutParams();
                    switch (e.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            downX = e.getRawX();
                            downY = e.getRawY();
                            startLeft = p.leftMargin;
                            startTop = p.topMargin;
                            downTime = SystemClock.uptimeMillis();
                            moved = false;
                            longFired = false;
                            return true;
                        case MotionEvent.ACTION_MOVE: {
                            float dx = e.getRawX() - downX, dy = e.getRawY() - downY;
                            if (!moved && Math.hypot(dx, dy) > dp(activity, 8)) moved = true;
                            if (moved) {
                                p.leftMargin = Math.max(0, startLeft + Math.round(dx));
                                p.topMargin = Math.max(0, startTop + Math.round(dy));
                                v.setLayoutParams(p);
                            } else if (!longFired && SystemClock.uptimeMillis() - downTime > 700) {
                                longFired = true;
                                hiddenThisRun = true;
                                decor.removeView(v);
                            }
                            return true;
                        }
                        case MotionEvent.ACTION_UP:
                            if (moved) {
                                activity.getSharedPreferences("televip_ui", 0).edit()
                                        .putInt("ghost_x", p.leftMargin).putInt("ghost_y", p.topMargin).apply();
                            } else if (!longFired) {
                                NativeSettings.show(activity);
                            }
                            return true;
                        default:
                            return true;
                    }
                }
            });

            decor.addView(ghost, lp);
        } catch (Throwable t) {
            Logger.e(t);
        }
    }

    private static int dp(Activity a, int v) {
        return Math.round(v * a.getResources().getDisplayMetrics().density);
    }
}
