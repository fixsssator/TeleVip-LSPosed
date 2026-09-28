package com.my.televip.Clients;

import android.view.View;

import com.my.televip.Class.ClassLoad;
import com.my.televip.Class.ClassNames;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Nicegram {

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

        ParameterResolver.register("fillMessageMenu",new Class[]{ClassLoad.getClass(ClassNames.MESSAGE_OBJECT), ArrayList.class, ArrayList.class, ArrayList.class, ArrayList.class, ArrayList.class, ArrayList.class});

        ParameterResolver.register("processSelectedOption",new Class[]{int.class, View.class});


    }
}
