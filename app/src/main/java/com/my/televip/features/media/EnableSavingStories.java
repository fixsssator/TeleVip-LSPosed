package com.my.televip.features.media;

import com.my.televip.Class.ClassNames;
import com.my.televip.Class.ClassLoad;
import com.my.televip.Configs.ConfigManager;
import com.my.televip.base.BaseMethodHook;
import com.my.televip.hooks.HMethod;
import com.my.televip.logging.Logger;
import com.my.televip.obfuscate.ArgsResolver;
import com.my.televip.obfuscate.Obfuscate;

public class EnableSavingStories {

    public static boolean isEnable = false;

    public static void init() {
        try {
            if (!isEnable) {
                isEnable = true;

                if (ClassLoad.getClass(ClassNames.STORY_ITEM_HOLDER) != null) {
                    HMethod.hookMethod(ClassLoad.getClass(ClassNames.STORY_ITEM_HOLDER), Obfuscate.getMethodName("PeerStoriesView$StoryItemHolder", "allowScreenshots"),
                            new BaseMethodHook() {
                                @Override
                                protected void beforeMethod(MethodHookParam param) {
                                    if (ConfigManager.enableSavingStories.isEnable())
                                        param.setResult(true);
                                }
                            });
                }
            }
        } catch (Throwable t){
            Logger.e(t);
        }
    }
}
