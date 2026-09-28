package com.my.televip.features.ui;

import com.my.televip.Class.ClassLoad;
import com.my.televip.Class.ClassNames;
import com.my.televip.Configs.ConfigManager;
import com.my.televip.application.AndroidUtilities;
import com.my.televip.base.BaseMethodHook;
import com.my.televip.hooks.HMethod;
import com.my.televip.logging.Logger;
import com.my.televip.obfuscate.ArgsResolver;
import com.my.televip.obfuscate.Obfuscate;
import com.my.televip.virtuals.messenger.MessagesController;

public class HideProxySponsor {

    public static boolean isEnable = false;

    public static void init() {
        try {
            if (!isEnable) {
                isEnable = true;
                if (ClassLoad.getClass(ClassNames.MESSAGES_CONTROLLER) != null) {
                    HMethod.hookMethod(ClassLoad.getClass(ClassNames.MESSAGES_CONTROLLER),
                            Obfuscate.getMethodName("MessagesController", "checkPromoInfoInternal"),ArgsResolver.merge("checkPromoInfoInternal", new Class[]{boolean.class}, new BaseMethodHook() {
                                @Override
                                protected void afterMethod(MethodHookParam param) {
                                    if (ConfigManager.hideProxySponsor.isEnable()) {
                                        MessagesController messagesController = new MessagesController(param.thisObject);
                                        AndroidUtilities.runOnUIThread(messagesController::removePromoDialog);
                                        removePromoDialog();
                                    }
                                }
                            }));
                }
            }
        } catch (Throwable e) {
            Logger.e(e);
        }
    }

    public static void removePromoDialog() {
        if (MessagesController.getGlobalMainSettings() == null) return;
        MessagesController.getGlobalMainSettings().edit().remove("proxy_dialog").remove("proxyDialogAddress").remove("nextPromoInfoCheckTime").apply();
    }

}
