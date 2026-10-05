package com.my.televip.features.messages;

import android.graphics.Color;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import com.my.televip.Class.ClassLoad;
import com.my.televip.Class.ClassNames;
import com.my.televip.Configs.ConfigManager;
import com.my.televip.base.BaseMethodHook;
import com.my.televip.hooks.HMethod;
import com.my.televip.language.Keys;
import com.my.televip.language.Translator;
import com.my.televip.logging.Logger;
import com.my.televip.obfuscate.Obfuscate;
import com.my.televip.utils.Utils;
import com.my.televip.virtuals.tgnet.TLRPC;

import java.util.Locale;

import de.robv.android.xposed.XposedHelpers;

/**
 * Shows which messages were deleted by the other side, without touching Telegram's chat cell.
 *
 * The usual marker ("Deleted" in front of the time) is drawn by ChatMessageCell. In Telegram's release
 * builds that class and its fields are renamed, so this adds " 🗑 Deleted" in red to the end of the message
 * text (or caption) instead. MessageObject keeps its readable names in every build.
 *
 * The same two hooks draw the message ID ("Show message ID"). Only active when ChatMessageCell cannot be found by name; otherwise the old marker is used and this would
 * show it twice. Messages without any text or caption (a bare photo, a sticker) get no marker.
 */
public final class DeletedMarker {

    private static boolean installed = false;

    private DeletedMarker() {}

    public static void init() {
        try {
            if (installed) return;
            Class<?> messageObject = ClassLoad.getClass(ClassNames.MESSAGE_OBJECT);
            if (messageObject == null) return;
            if (ClassLoad.getClass(ClassNames.CHAT_MESSAGE_CELL, Utils.classLoader, false) != null) return;
            installed = true;

            // generateLayout(User) builds the text bubble from messageText
            HMethod.hookLoose(messageObject, Obfuscate.getMethodName("MessageObject", "generateLayout"), new BaseMethodHook() {
                @Override
                protected void beforeMethod(MethodHookParam param) {
                    markField(param.thisObject, "messageText");
                }
            });

            // generateCaption() fills `caption` from the message; the cell reads it afterwards
            HMethod.hookLoose(messageObject, Obfuscate.getMethodName("MessageObject", "generateCaption"), new BaseMethodHook() {
                @Override
                protected void afterMethod(MethodHookParam param) {
                    markField(param.thisObject, "caption");
                }
            });
            Logger.l("Deleted-message marker installed (text/caption)");
        } catch (Throwable t) {
            Logger.e(t);
        }
    }

    private static void markField(Object messageObject, String field) {
        try {
            boolean wantDeleted = ConfigManager.showDeletedMessages != null && ConfigManager.showDeletedMessages.isEnable();
            boolean wantId = ConfigManager.showMessageId != null && ConfigManager.showMessageId.isEnable();
            if (!wantDeleted && !wantId) return;

            String name = Obfuscate.getFieldName("MessageObject", field);
            Object current = XposedHelpers.getObjectField(messageObject, name);
            if (!(current instanceof CharSequence)) return;
            CharSequence text = (CharSequence) current;

            // ID first, "deleted" last, so the red marker is always the very end of the message
            if (wantId) {
                int id = messageId(messageObject);
                if (id > 0) text = decorate(text, " \u00B7 ID " + id, 0xFF8A8A8A, true);
            }
            if (wantDeleted && isDeleted(messageObject)) {
                text = decorate(text, suffix(), Color.rgb(0xFF, 0x3B, 0x30), false);
            }
            if (text != current) XposedHelpers.setObjectField(messageObject, name, text);
        } catch (Throwable t) {
            Logger.e(t);
        }
    }

    private static int messageId(Object messageObject) {
        Object owner = XposedHelpers.getObjectField(messageObject, Obfuscate.getFieldName("MessageObject", "messageOwner"));
        return owner == null ? 0 : new TLRPC.Message(owner).getId();
    }

    /** Adds the suffix once; an existing one (anywhere for the ID, at the end for "deleted") is left alone. */
    static CharSequence decorate(CharSequence text, String suffix, int color, boolean anywhere) {
        if (text == null || text.length() == 0) return text;
        if (anywhere ? text.toString().contains(suffix) : endsWith(text, suffix)) return text;
        SpannableStringBuilder b = new SpannableStringBuilder(text);   // keeps emoji / entity spans
        int start = b.length();
        b.append(suffix);
        b.setSpan(new ForegroundColorSpan(color), start, b.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return b;
    }

    private static boolean isDeleted(Object messageObject) {
        Object owner = XposedHelpers.getObjectField(messageObject, Obfuscate.getFieldName("MessageObject", "messageOwner"));
        if (owner == null) return false;
        return (new TLRPC.Message(owner).getFlags() & ShowDeletedMessages.FLAG_DELETED) != 0;
    }

    static String suffix() {
        String word = "ru".equals(Locale.getDefault().getLanguage()) ? "Удалено" : Translator.get(Keys.Deleted);
        return " \uD83D\uDDD1 " + word;
    }

    /** Same object when there is nothing to add or the marker is already there. */
    static CharSequence mark(CharSequence text, String suffix) {
        return decorate(text, suffix, Color.rgb(0xFF, 0x3B, 0x30), false);
    }

    static boolean endsWith(CharSequence text, String suffix) {
        int n = suffix.length();
        if (text.length() < n) return false;
        for (int i = 0; i < n; i++) {
            if (text.charAt(text.length() - n + i) != suffix.charAt(i)) return false;
        }
        return true;
    }
}
