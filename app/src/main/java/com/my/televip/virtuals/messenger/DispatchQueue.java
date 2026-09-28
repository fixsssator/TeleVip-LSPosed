package com.my.televip.virtuals.messenger;

import com.my.televip.obfuscate.ArgsResolver;
import com.my.televip.obfuscate.Obfuscate;

import de.robv.android.xposed.XposedHelpers;

public class DispatchQueue {

    Object dispatchQueue;

    public DispatchQueue(Object obj){
        dispatchQueue = obj;
    }

    public void postRunnable(Runnable runnable) {
        XposedHelpers.callMethod(dispatchQueue, Obfuscate.getMethodName("DispatchQueue", "postRunnable"), runnable);
    }

}
