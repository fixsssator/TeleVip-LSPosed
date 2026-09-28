package com.my.televip.features.other;


import com.my.televip.Class.ClassNames;
import com.my.televip.Clients.ClientManager;
import com.my.televip.Configs.ConfigManager;
import com.my.televip.base.BaseMethodHook;
import com.my.televip.hooks.HMethod;
import com.my.televip.Class.ClassLoad;
import com.my.televip.obfuscate.ArgsResolver;
import com.my.televip.obfuscate.Obfuscate;
import com.my.televip.logging.Logger;

public class RemovesContentSaving {

    public static boolean isEnable = false;

    public static void init() {
        try {
            if (!isEnable) {
                isEnable = true;

                if (ClassLoad.getClass(ClassNames.MESSAGES_CONTROLLER) != null) {
                    HMethod.hookMethod(ClassLoad.getClass(ClassNames.MESSAGES_CONTROLLER), Obfuscate.getMethodName("MessagesController", "isChatNoForwards"), ArgsResolver.merge("isChatNoForwards", new Class[]{ClassLoad.getClass(ClassNames.TLRPC_CHAT)}, new BaseMethodHook() {
                        @Override
                        protected void beforeMethod(MethodHookParam param) {
                            if (ConfigManager.removesContentSaving.isEnable())
                                param.setResult(false);
                        }
                    }));
                }

                if (ClassLoad.getClass(ClassNames.CHAT_ACTIVITY) != null && !ClientManager.is(ClientManager.Client.NagramXF)) {
                    HMethod.hookMethod(ClassLoad.getClass(ClassNames.CHAT_ACTIVITY), Obfuscate.getMethodName("ChatActivity", "hasSelectedNoforwardsMessage"), new BaseMethodHook() {
                        @Override
                        protected void beforeMethod(MethodHookParam param) {
                            if (ConfigManager.removesContentSaving.isEnable())
                                param.setResult(false);
                        }
                    });
                }

                if (ClassLoad.getClass(ClassNames.MESSAGE_OBJECT) != null) {
                    HMethod.hookMethod(ClassLoad.getClass(ClassNames.MESSAGE_OBJECT), Obfuscate.getMethodName("MessageObject", "canForwardMessage"), new BaseMethodHook() {
                        @Override
                        protected void beforeMethod(MethodHookParam param) {
                            if (ConfigManager.removesContentSaving.isEnable())
                                param.setResult(true);
                        }
                    });
                }
            }
        } catch (Throwable t) {
            Logger.e(t);
        }
    }

}
