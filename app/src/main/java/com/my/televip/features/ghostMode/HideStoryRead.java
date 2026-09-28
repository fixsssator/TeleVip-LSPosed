package com.my.televip.features.ghostMode;

import com.my.televip.Class.ClassLoad;
import com.my.televip.Class.ClassNames;
import com.my.televip.Clients.ClientManager;
import com.my.televip.obfuscate.ArgsResolver;
import com.my.televip.obfuscate.Obfuscate;

public class HideStoryRead {

    public static boolean isReadStoriesRequest(Object object) {
        if (!ClientManager.isTgnetObfuscated()) {
            String className = object.getClass().getName();
            return className.contains("TL_stories_readStories") ||
                    className.contains("TL_stories_incrementStoryViews");
        } else {
            Class<?> objectClass = object.getClass();
            return objectClass.equals(ClassLoad.getClass(Obfuscate.getClassName(ClassNames.TL_STORIES_READ_STORIES))) ||
                    objectClass.equals(ClassLoad.getClass(Obfuscate.getClassName(ClassNames.TL_STORIES_INCREMENT_STORY_VIEWS)));
        }
    }
}
