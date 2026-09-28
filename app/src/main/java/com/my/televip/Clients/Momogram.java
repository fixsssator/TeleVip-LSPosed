package com.my.televip.Clients;

import com.my.televip.Class.ClassLoad;
import com.my.televip.Class.ClassNames;

import java.util.HashMap;
import java.util.Map;

public class Momogram {

    public static class ParameterResolver
    {
        static Map<String,Class<?>[]> objectList = new HashMap<>();

        public static void register(String name,  Class<?>[] classes){
            objectList.put(name, classes);
        }

        public static Class<?>[] resolve(String name) {
            return objectList.get(name);
        }

        public static boolean has(String name)
        {
            boolean has = false;
            Class<?>[] classes = objectList.get(name);
            if (classes != null){
                has = true;
            }
            return has;
        }
    }

    public static void loadParameter() {
        ParameterResolver.register("sendRequestInternal",new Class[]{ClassLoad.getClass(ClassNames.TL_OBJECT), ClassLoad.getClass(ClassNames.REQUEST_DELEGATE), ClassLoad.getClass(ClassNames.REQUEST_DELEGATE_TIMESTAMP), ClassLoad.getClass(ClassNames.QUICK_ACK_DELEGATE), ClassLoad.getClass(ClassNames.WRITE_TO_SOCKET_DELEGATE), int.class, int.class, int.class, boolean.class, int.class, Runnable.class});
    }
}
