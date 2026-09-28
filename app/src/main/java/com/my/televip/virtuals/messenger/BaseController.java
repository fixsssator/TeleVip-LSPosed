package com.my.televip.virtuals.messenger;

import com.my.televip.obfuscate.Obfuscate;

import de.robv.android.xposed.XposedHelpers;

public class BaseController {

    Object baseController;

    public BaseController(Object obj){baseController = obj;}

    public UserConfig getUserConfig(){
        return new UserConfig(XposedHelpers.callMethod(baseController, Obfuscate.getMethodName("BaseController", "getUserConfig")));
    }

}
