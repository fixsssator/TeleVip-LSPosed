package com.my.televip.features.ui;

import android.view.MotionEvent;

import com.my.televip.Class.ClassLoad;
import com.my.televip.Class.ClassNames;
import com.my.televip.Configs.ConfigManager;
import com.my.televip.base.BaseMethodHook;
import com.my.televip.hooks.HMethod;
import com.my.televip.logging.Logger;
import com.my.televip.obfuscate.ArgsResolver;
import com.my.televip.obfuscate.Obfuscate;

public class DisableProfileSwipeBack {
    public static boolean isEnable = false;

    public static void init() {
        try {
            if (!isEnable) {
                isEnable = true;

                if (ClassLoad.getClass(ClassNames.PROFILE_ACTIVITY) != null) {

                    HMethod.hookMethod(ClassLoad.getClass(ClassNames.PROFILE_ACTIVITY), Obfuscate.getMethodName("ProfileActivity", "isSwipeBackEnabled"), ArgsResolver.merge("isSwipeBackEnabled", new Class[]{MotionEvent.class}, new BaseMethodHook() {
                        @Override
                        protected void beforeMethod(MethodHookParam param) {
                            if (ConfigManager.disableProfileSwipeBack.isEnable()) {
                                param.setResult(false);
                            }
                        }
                    }));
                }
            }
        } catch (Throwable e){
            Logger.e(e);
        }
    }

}
