package com.my.televip.features.stories;

import com.my.televip.Class.ClassLoad;
import com.my.televip.Class.ClassNames;
import com.my.televip.Clients.ClientManager;
import com.my.televip.Configs.ConfigManager;
import com.my.televip.base.BaseMethodHook;
import com.my.televip.hooks.HMethod;
import com.my.televip.hooks.HookStatus;
import com.my.televip.hooks.ShapeResolver;
import com.my.televip.logging.Logger;
import com.my.televip.utils.Utils;
import com.my.televip.obfuscate.ArgsResolver;
import com.my.televip.obfuscate.Obfuscate;

public class DisableStories {

    public static boolean isEnable = false;

    public static void init() {
        try {
            if (!isEnable) {
                isEnable = true;

                if (ClassLoad.getClass(ClassNames.MESSAGES_CONTROLLER) != null) {
                    HMethod.hookMethod(ClassLoad.getClass(ClassNames.MESSAGES_CONTROLLER), "MessagesController", new String[]{"storiesEnabled", "storyEntitiesAllowed",}, new BaseMethodHook() {
                        @Override
                        protected void beforeMethod(MethodHookParam param) {
                            if (ConfigManager.disableStories.isEnable()) param.setResult(false);
                        }
                    });

                    HMethod.hookMethod(ClassLoad.getClass(ClassNames.MESSAGES_CONTROLLER), fixOverloadKey(Obfuscate.getMethodName("MessagesController", "storyEntitiesAllowed2")), ArgsResolver.merge("storyEntitiesAllowed", new Class[]{ClassLoad.getClass(ClassNames.TLRPC_USER)}, new BaseMethodHook() {
                        @Override
                        protected void beforeMethod(MethodHookParam param) {
                            if (ConfigManager.disableStories.isEnable()) param.setResult(false);
                        }
                    }));
                }

                Class<?> storiesController = findStoriesController();
                if (storiesController != null) {
                    if (ClientManager.is(ClientManager.Client.NagramX)) {
                        HMethod.hookMethod(ClassLoad.getClass(ClassNames.STORIES_CONTROLLER),"hasStories", long.class, new BaseMethodHook() {
                            @Override
                            protected void beforeMethod(MethodHookParam param) {
                                if (ConfigManager.disableStories.isEnable())
                                    param.setResult(false);
                            }
                        });
                    } else {
                        hookHasStories(storiesController);
                    }
                }
            }
        } catch (Throwable t){
            Logger.e(t);
        }
    }


    /** "storyEntitiesAllowed2" only exists as a key in obfuscation maps; plain builds call it "storyEntitiesAllowed". */
    private static String fixOverloadKey(String name) {
        return "storyEntitiesAllowed2".equals(name) ? "storyEntitiesAllowed" : name;
    }

    /**
     * StoriesController by its readable name (older builds / forks) or, when Telegram's build renamed it,
     * as the return type of MessagesController.getStoriesController(), a getter whose name is kept.
     */
    private static Class<?> findStoriesController() {
        Class<?> byName = ClassLoad.getClass(ClassNames.STORIES_CONTROLLER, Utils.classLoader, false);
        if (byName != null) return byName;
        Class<?> mc = ClassLoad.getClass(ClassNames.MESSAGES_CONTROLLER);
        Class<?> found = ShapeResolver.returnTypeOf(mc, Obfuscate.getMethodName("MessagesController", "getStoriesController"));
        if (found != null) Logger.l("StoriesController located via MessagesController.getStoriesController(): " + found.getName());
        else HookStatus.failed("class StoriesController");
        return found;
    }

    /**
     * StoriesController.hasStories() decides whether the row of stories is shown above the chat list.
     * By name when it is readable; otherwise the class's "() -> boolean" methods (there are exactly two,
     * hasStories() and hasSelfStories(), in Telegram 12.10.5 and 12.10.6). More than three would mean the
     * class changed shape, and then nothing is hooked rather than guessing.
     */
    private static void hookHasStories(Class<?> controller) {
        BaseMethodHook hook = new BaseMethodHook() {
            @Override
            protected void beforeMethod(MethodHookParam param) {
                if (ConfigManager.disableStories.isEnable()) param.setResult(false);
            }
        };

        String readable = Obfuscate.getMethodName("StoriesController", "hasStories");
        java.util.List<java.lang.reflect.Method> candidates = ShapeResolver.noArgBooleans(controller);
        java.util.List<java.lang.reflect.Method> chosen = new java.util.ArrayList<>();
        for (java.lang.reflect.Method m : candidates) if (m.getName().equals(readable)) chosen.add(m);

        if (chosen.isEmpty()) {
            if (candidates.isEmpty() || candidates.size() > 3) {
                Logger.w("StoriesController: cannot tell which method is hasStories() (" + candidates.size() + " candidates)");
                HookStatus.failed("StoriesController#hasStories");
                return;
            }
            chosen.addAll(candidates);
            Logger.w("StoriesController#hasStories is minified, hooking by shape: " + candidates);
        }
        for (java.lang.reflect.Method m : chosen) {
            de.robv.android.xposed.XposedBridge.hookMethod(m, hook);
            HookStatus.ok();
        }
    }

}
