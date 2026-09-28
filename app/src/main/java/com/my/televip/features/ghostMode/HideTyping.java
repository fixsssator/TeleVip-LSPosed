package com.my.televip.features.ghostMode;

import com.my.televip.Class.ClassLoad;
import com.my.televip.Class.ClassNames;
import com.my.televip.Clients.ClientManager;
import com.my.televip.obfuscate.ArgsResolver;
import com.my.televip.obfuscate.Obfuscate;

public class HideTyping {

    public static boolean isTypingRequest(Object object) {
        if (!ClientManager.isTgnetObfuscated()) {
            String className = object.getClass().getName();
            return className.contains("TL_messages_setTyping") ||
                    className.contains("TL_messages_setEncryptedTyping");
        } else {
            Class<?> objectClass = object.getClass();
            return objectClass.equals(ClassLoad.getClass(Obfuscate.getClassName(ClassNames.TL_MESSAGES_SET_TYPING))) ||
                    objectClass.equals(ClassLoad.getClass(Obfuscate.getClassName(ClassNames.TL_MESSAGES_SET_ENCRYPTED_TYPING)));
        }
    }
}
