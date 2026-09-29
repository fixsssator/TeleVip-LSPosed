package com.my.televip;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.my.televip.Class.ClassLoad;
import com.my.televip.Class.ClassNames;
import com.my.televip.Clients.ClientManager;
import com.my.televip.base.BaseMethodHook;
import com.my.televip.hooks.HMethod;
import com.my.televip.settings.nativeui.NativeSettings;
import com.my.televip.utils.Utils;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    private boolean isStart;

    @Override
    public void initZygote(StartupParam startupParam){ Utils.modulePath = startupParam.modulePath; }

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {
        if (!ClientManager.containsPackage(lpparam.packageName, lpparam.classLoader)) return;

        Utils.classLoader = lpparam.classLoader;
        Utils.pkgName = lpparam.packageName;

        HMethod.hookMethod(ClassLoad.getClass(ClassNames.LAUNCH_ACTIVITY), "onCreate", Bundle.class, new BaseMethodHook() {
            @Override
            protected void beforeMethod(MethodHookParam param) {
                Utils.setCurrentActivity((Activity) param.thisObject);
                if (!isStart) {
                    TeleVip.startHook();
                    isStart = true;
                }
            }

            @Override
            protected void afterMethod(MethodHookParam param) {
                // Opened from the TeleVip icon: show the settings window (plain Android views).
                Activity activity = (Activity) param.thisObject;
                NativeSettings.showWhenReady(activity, activity.getIntent());
            }
        });

        // Telegram is usually already running when the icon is tapped, then Android calls onNewIntent.
        HMethod.hookMethod(ClassLoad.getClass(ClassNames.LAUNCH_ACTIVITY), "onNewIntent", Intent.class, new BaseMethodHook() {
            @Override
            protected void afterMethod(MethodHookParam param) {
                Activity activity = (Activity) param.thisObject;
                Intent intent = (Intent) param.args[0];
                if (NativeSettings.wantsSettings(intent)) {
                    Utils.setCurrentActivity(activity);
                    NativeSettings.showWhenReady(activity, intent);
                }
            }
        });
    }


}

