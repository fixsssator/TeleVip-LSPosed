package com.my.televip.settings.nativeui;

import com.my.televip.Class.ClassLoad;
import com.my.televip.Class.ClassNames;
import com.my.televip.hooks.MethodResolver;
import com.my.televip.language.Keys;
import com.my.televip.logging.Logger;
import com.my.televip.obfuscate.Obfuscate;
import com.my.televip.utils.Utils;

import java.util.Locale;

/**
 * Tells the settings window which switches cannot do anything in this Telegram build, so a switch that
 * would silently do nothing is shown as unavailable instead.
 *
 * Telegram's release builds rename most of their own UI classes (ChatActivity, ChatMessageCell, ...), and
 * R8 also inlines many small UI methods. Features that need those exact classes cannot be hooked there.
 */
final class FeatureSupport {

    private FeatureSupport() {}

    /** null = the feature can work; otherwise a one-line reason to show under the switch. */
    static String problem(String key) {
        try {
            String missing = null;
            if (Keys.DisableChannelSwipeBack.equals(key) || Keys.HidePinnedMessages.equals(key)
                    || Keys.PreventMedia.equals(key) || Keys.SaveEditsHistory.equals(key)) {
                missing = missingClass(ClassNames.CHAT_ACTIVITY, "ChatActivity");
            } else if (Keys.SecretMediaSave.equals(key)) {
                missing = missingClass(ClassNames.CHAT_MESSAGE_CELL_DELEGATE, "ChatMessageCellDelegate");
            } else if (Keys.EnableSavingStories.equals(key)) {
                missing = missingClass(ClassNames.STORY_ITEM_HOLDER, "StoryItemHolder");
            } else if (Keys.ShowUserID.equals(key)) {
                Class<?> profile = ClassLoad.getClass(ClassNames.PROFILE_ACTIVITY, Utils.classLoader, false);
                if (profile == null) missing = "ProfileActivity";
                else if (MethodResolver.byName(profile, Obfuscate.getMethodName("ProfileActivity", "updateProfileData")).isEmpty())
                    missing = "ProfileActivity.updateProfileData";
            }
            if (missing == null) return null;
            boolean ru = "ru".equals(Locale.getDefault().getLanguage());
            return ru ? "Не работает в этой версии Telegram: " + missing + " переименован или встроен"
                    : "Not available in this Telegram build: " + missing + " is renamed or inlined";
        } catch (Throwable t) {
            Logger.e(t);
            return null;
        }
    }

    private static String missingClass(String name, String label) {
        return ClassLoad.getClass(name, Utils.classLoader, false) == null ? label : null;
    }
}
