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
import de.robv.android.xposed.XposedHelpers;
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

                hookStoryLists();

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
     * hasOnlySelfStories() and hasSelfStories(), in Telegram 12.10.5 and 12.10.6; hasStories() itself is inlined by R8). More than three would mean the
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


    // ---------------------------------------------------------------------------------------------
    // The row of stories above the chat list.
    //
    // In Telegram's release builds StoriesController.hasStories() no longer exists: R8 copied its body
    // ("the list of stories is not empty, or I have own stories") straight into the chat list screen, so
    // a hook on it cannot hide anything. What the screen reads is the list itself, and it re-reads it
    // whenever Telegram posts the "storiesUpdated" notification. So, with "disable stories" on, we empty
    // the lists of PeerStories right before that notification goes out. Only readable things are used:
    // NotificationCenter, MessagesController.getStoriesController() and the TL class name "PeerStories".
    // ---------------------------------------------------------------------------------------------

    private static final java.util.Map<Class<?>, java.lang.reflect.Field[]> LIST_FIELDS = new java.util.concurrent.ConcurrentHashMap<>();

    private static void hookStoryLists() {
        try {
            final Class<?> nc = ClassLoad.getClass(ClassNames.NOTIFICATION_CENTER);
            final Class<?> mc = ClassLoad.getClass(ClassNames.MESSAGES_CONTROLLER);
            if (nc == null || mc == null) return;

            final int storiesUpdated = staticInt(nc, "storiesUpdated");
            final int storiesListUpdated = staticInt(nc, "storiesListUpdated");
            if (storiesUpdated == Integer.MIN_VALUE && storiesListUpdated == Integer.MIN_VALUE) {
                HookStatus.failed("NotificationCenter#storiesUpdated");
                return;
            }

            HMethod.hookMethod(nc, Obfuscate.getMethodName("NotificationCenter", "postNotificationName"),
                    int.class, Object[].class, new BaseMethodHook() {
                        @Override
                        protected void beforeMethod(MethodHookParam param) {
                            if (!ConfigManager.disableStories.isEnable()) return;
                            int id = (Integer) param.args[0];
                            if (id != storiesUpdated && id != storiesListUpdated) return;
                            clearStoryLists(param.thisObject, mc);
                        }
                    });
        } catch (Throwable t) {
            Logger.e(t);
        }
    }

    private static int staticInt(Class<?> cls, String field) {
        try {
            java.lang.reflect.Field f = cls.getDeclaredField(Obfuscate.getFieldName("NotificationCenter", field));
            f.setAccessible(true);
            return f.getInt(null);
        } catch (Throwable t) {
            return Integer.MIN_VALUE;
        }
    }

    private static void clearStoryLists(Object notificationCenter, Class<?> messagesController) {
        try {
            int account = XposedHelpers.getIntField(notificationCenter, Obfuscate.getFieldName("NotificationCenter", "currentAccount"));
            Object controllerOwner = XposedHelpers.callStaticMethod(messagesController, Obfuscate.getMethodName("MessagesController", "getInstance"), account);
            if (controllerOwner == null) return;
            Object stories = XposedHelpers.callMethod(controllerOwner, Obfuscate.getMethodName("MessagesController", "getStoriesController"));
            if (stories == null) return;

            java.lang.reflect.Field[] fields = LIST_FIELDS.get(stories.getClass());
            if (fields == null) {
                java.util.List<java.lang.reflect.Field> l = new java.util.ArrayList<>();
                for (java.lang.reflect.Field f : stories.getClass().getDeclaredFields()) {
                    if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                    if (f.getType() != java.util.ArrayList.class) continue;
                    f.setAccessible(true);
                    l.add(f);
                }
                fields = l.toArray(new java.lang.reflect.Field[0]);
                LIST_FIELDS.put(stories.getClass(), fields);
            }
            for (java.lang.reflect.Field f : fields) {
                java.util.ArrayList<?> list = (java.util.ArrayList<?>) f.get(stories);
                if (list == null || list.isEmpty()) continue;
                Object first = list.get(0);
                // only the lists shown in the chat list (dialog and hidden stories), nothing else
                if (first != null && first.getClass().getName().contains("PeerStories")) list.clear();
            }
        } catch (Throwable t) {
            Logger.e(t);
        }
    }

}
