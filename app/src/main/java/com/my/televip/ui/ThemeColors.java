package com.my.televip.ui;

import static com.my.televip.virtuals.ActionBar.Theme.isLight;

import android.graphics.Color;

public class ThemeColors {


    public static int getBackgroundGrayColor(){
        return isLight() ? Color.rgb(241, 241, 243) : Color.rgb(21, 30, 39);
    }

    public static int getBackgroundWhiteOrBlueColor(){
        return isLight() ? Color.WHITE : Color.rgb(29, 39, 51);
    }

    public static int getToolBarColor(){
        return isLight() ? Color.WHITE : Color.rgb(36, 45, 57);
    }

    public static int getToolBarRippleColor(){
        return isLight() ? 0x20000000 : 0x20FFFFFF;
    }

    public static int getTextToolBarColor(){
        return isLight() ? Color.BLACK : Color.WHITE;
    }
    public static int getTextColor(){
        return isLight() ? Color.BLACK : Color.WHITE;
    }

    public static int getTextBlueColor(){
        return isLight() ? Color.rgb(100, 164, 221) : Color.rgb(112, 184, 221);
    }
    public static int getTextGrayColor(){
        return isLight() ? Color.rgb(128, 128, 128) : Color.rgb(103,115,128);
    }

    public static int getArrowDrawableColor(){
        return isLight() ? Color.BLACK : Color.WHITE;
    }

}
