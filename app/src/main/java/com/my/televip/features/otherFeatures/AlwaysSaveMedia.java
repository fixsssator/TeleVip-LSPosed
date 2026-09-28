package com.my.televip.features.otherFeatures;

import android.view.View;

import com.my.televip.Class.ClassLoad;
import com.my.televip.Class.ClassNames;
import com.my.televip.base.BaseMethodHook;
import com.my.televip.hooks.HMethod;
import com.my.televip.logging.Logger;
import com.my.televip.obfuscate.ArgsResolver;
import com.my.televip.obfuscate.Obfuscate;
import com.my.televip.virtuals.ui.PhotoViewer;

public class AlwaysSaveMedia {
    public static void init() {
        try {
            if (ClassLoad.getClass(ClassNames.PHOTO_VIEWER) != null) {

                HMethod.hookMethod(ClassLoad.getClass(ClassNames.PHOTO_VIEWER), Obfuscate.getMethodName("PhotoViewer", "setIsAboutToSwitchToIndex"), ArgsResolver.merge("setIsAboutToSwitchToIndex", new Class[]{int.class, boolean.class, boolean.class, boolean.class}, new BaseMethodHook() {
                    @Override
                    protected void afterMethod(MethodHookParam param) {
                        final PhotoViewer photoViewer = new PhotoViewer(param.thisObject);
                        if (photoViewer.getGalleryButton()!= null) photoViewer.getGalleryButton().setVisibility(View.VISIBLE);

                    }
                }));
            }
        } catch (Throwable t) {
            Logger.e(t);
        }
    }
}