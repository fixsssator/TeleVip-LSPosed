package com.my.televip.Clients;

import com.my.televip.Class.ClassLoad;
import com.my.televip.Class.ClassNames;
import com.my.televip.language.Keys;
import com.my.televip.language.Translator;
import com.my.televip.settings.controller.SettingsController;
import com.my.televip.virtuals.ActionBar.AlertDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Turrit {

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
        ParameterResolver.register("fillMessageMenu", new Class[]{ClassLoad.getClass(ClassNames.MESSAGE_OBJECT), ArrayList.class, ArrayList.class, ArrayList.class, CharSequence.class});
    }

    public static void showGhostModeDialog(SettingsController settingsController){
        AlertDialog ghostMode = new AlertDialog(settingsController.getContext());
        ghostMode.setTitle(Translator.get(Keys.GhostMode));
        ghostMode.setMessage(Translator.get(Keys.OpenGhostMode));
        ghostMode.setPositiveButton(Translator.get(Keys.Open), AlertDialog.click(settingsController::openView));
        ghostMode.setNegativeButton(Translator.get(Keys.Cancel), null);
        ghostMode.show();
    }

}
