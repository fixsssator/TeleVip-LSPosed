package com.my.televip.utils;

import android.app.Activity;

import com.google.gson.Gson;
import com.my.televip.logging.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;

public class Utils {
    public static String pkgName = null;
    public static String modulePath = null;
    public static ClassLoader classLoader = null;
    public static final String MODULE_VERSION = "3.7.5";
    public static final String issue = "A hook could not be resolved: this Telegram build changed something TeleVip relies on (or a fork obfuscation map is outdated). Other features keep working; please report it with the log.";

    private static WeakReference<Activity> currentActivity;

    public static void setCurrentActivity(Activity activity) {
        currentActivity = new WeakReference<>(activity);
    }

    public static Activity getCurrentActivity() {
        return currentActivity != null ? currentActivity.get() : null;
    }

    public static String getFieldAsString(Object value) {

        if (value == null) {
            return null;
        }

        if (value instanceof CharSequence) {
            return value.toString();
        }

        return String.valueOf(value);
    }

    public static void objectToJson(Object object) {
        try {
            Gson gson = new Gson();

            String json = gson.toJson(object);

            File dir = new File(Utils.getCurrentActivity().getFilesDir(), "backup");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File file = new File(
                    dir,
                    object.getClass().getSimpleName() + "_" + System.currentTimeMillis() + ".json"
            );

            FileOutputStream fos = new FileOutputStream(file);
            fos.write(json.getBytes(StandardCharsets.UTF_8));
            fos.close();
        } catch (Throwable throwable) {
            Logger.e(throwable);
        }
    }

}
