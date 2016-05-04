package com.madone.virtualexpo.expoastana.settings;

import com.madone.virtualexpo.expoastana.util.TimeUtils;
import com.madone.virtualexpo.expoastana.util.UIUtils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import java.util.HashMap;
import java.util.Random;

public class ConfMessageCardUtils {

    public static final String PREF_CONF_MESSAGE_CARDS_ENABLED = "pref_conf_message_cards_enabled";

    public static final String PREF_ANSWERED_CONF_MESSAGE_CARDS_PROMPT
            = "pref_answered_conf_message_cards_prompt";

    private static Random random = new Random();

    private static final int WIFI_FEEDBACK_RANDOM_INT_UPPER_RANGE = 30;

    public enum ConfMessageCard {
        CONFERENCE_CREDENTIALS("2015-05-27T09:00:00-07:00", "2015-05-27T18:00:00-07:00"),

        KEYNOTE_ACCESS("2015-05-27T09:00:00-07:00", "2015-05-28T09:30:00-07:00"),

        AFTER_HOURS("2015-05-28T11:00:00-07:00", "2015-05-28T17:00:00-07:00"),

        WIFI_FEEDBACK("2015-05-28T09:30:00-07:00", "2015-05-29T17:30:00-07:00");

        long mStartTime;
        long mEndTime;
        ConfMessageCard(String startTime, String endTime) {
            mStartTime = TimeUtils.parseTimestamp(startTime).getTime();
            mEndTime = TimeUtils.parseTimestamp(endTime).getTime();
        }


        public boolean isActive(long millisSinceEpoch) {
            boolean returnVal = mStartTime <= millisSinceEpoch && mEndTime >= millisSinceEpoch;

            if (WIFI_FEEDBACK.equals(this)) {
                return (random.nextInt(WIFI_FEEDBACK_RANDOM_INT_UPPER_RANGE) == 1);
            }
            return returnVal;
        }
    }

    private static final HashMap<ConfMessageCard, String> ConfMessageCardsDismissedMap
            = new HashMap<>();
    private static final String dismiss_prefix = "pref_conf_message_cards_dismissed_";

    private static final HashMap<ConfMessageCard, String> ConfMessageCardsShouldShowMap
            = new HashMap<>();

    private static final String should_show_prefix = "pref_conf_message_cards_should_show_";

    static {
        ConfMessageCardsDismissedMap.put(ConfMessageCard.CONFERENCE_CREDENTIALS, dismiss_prefix
                + "conference_credentials");
        ConfMessageCardsDismissedMap.put(ConfMessageCard.KEYNOTE_ACCESS, dismiss_prefix
                + "keynote_access");
        ConfMessageCardsDismissedMap.put(ConfMessageCard.AFTER_HOURS, dismiss_prefix
                + "after_hours");
        ConfMessageCardsDismissedMap.put(ConfMessageCard.WIFI_FEEDBACK, dismiss_prefix
                + "wifi_feedback");

        ConfMessageCardsShouldShowMap.put(ConfMessageCard.CONFERENCE_CREDENTIALS, should_show_prefix
                + "conference_credentials");
        ConfMessageCardsShouldShowMap.put(ConfMessageCard.KEYNOTE_ACCESS, should_show_prefix
                + "keynote_access");
        ConfMessageCardsShouldShowMap.put(ConfMessageCard.AFTER_HOURS, should_show_prefix
                + "after_hours");
        ConfMessageCardsShouldShowMap.put(ConfMessageCard.WIFI_FEEDBACK, should_show_prefix
                + "wifi_feedback");
    }


    public static boolean isConfMessageCardsEnabled(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return true;
    }

    public static void setConfMessageCardsEnabled(final Context context,
                                                  @Nullable Boolean newValue) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        if (newValue == null) {
            sp.edit().remove(PREF_CONF_MESSAGE_CARDS_ENABLED).apply();
        } else {
            sp.edit().putBoolean(PREF_CONF_MESSAGE_CARDS_ENABLED, newValue).apply();
        }
    }

    public static boolean hasAnsweredConfMessageCardsPrompt(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return true;
    }

    public static void markAnsweredConfMessageCardsPrompt(final Context context,
                                                          @Nullable Boolean newValue) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        if (newValue == null) {
            sp.edit().remove(PREF_ANSWERED_CONF_MESSAGE_CARDS_PROMPT).apply();
        } else {
            sp.edit().putBoolean(PREF_ANSWERED_CONF_MESSAGE_CARDS_PROMPT, newValue).apply();
        }
    }

    public static boolean hasDismissedConfMessageCard(final Context context, ConfMessageCard card) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(ConfMessageCardsDismissedMap.get(card), false);
    }

    public static void markDismissedConfMessageCard(final Context context, ConfMessageCard card) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(ConfMessageCardsDismissedMap.get(card), true).apply();
    }

    public static void setDismissedConfMessageCard(final Context context, ConfMessageCard card,
                                                   Boolean newValue) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        if (newValue == null) {
            sp.edit().remove(ConfMessageCardsDismissedMap.get(card)).apply();
        } else {
            sp.edit().putBoolean(ConfMessageCardsDismissedMap.get(card), newValue).apply();
        }
    }

    public static boolean shouldShowConfMessageCard(final Context context, ConfMessageCard card) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(ConfMessageCardsShouldShowMap.get(card), false);
    }

    public static void markShouldShowConfMessageCard(final Context context, ConfMessageCard card,
                                                     Boolean newValue) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        if (newValue == null) {
            sp.edit().remove(ConfMessageCardsShouldShowMap.get(card)).apply();
        } else {
            sp.edit().putBoolean(ConfMessageCardsShouldShowMap.get(card), newValue).apply();
        }
    }

    public static void registerPreferencesChangeListener(final Context context,
                                                         ConferencePrefChangeListener listener) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.registerOnSharedPreferenceChangeListener(listener);
    }

    public static void unregisterPreferencesChangeListener(final Context context,
                                                           ConferencePrefChangeListener listener) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.unregisterOnSharedPreferenceChangeListener(listener);
    }

    public static void unsetStateForAllCards(final Context context) {
        for (ConfMessageCard card : ConfMessageCard.values()) {
            setDismissedConfMessageCard(context, card, null /* new state */);
        }
    }

    public static void enableActiveCards(final Context context) {
        long currentTime = UIUtils.getCurrentTime(context);
        for (ConfMessageCard card : ConfMessageCard.values()) {
            if (card.isActive(currentTime)) {
                markShouldShowConfMessageCard(context, card, true);
            }
        }
    }

    public static class ConferencePrefChangeListener implements SharedPreferences.OnSharedPreferenceChangeListener {

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
            if (PREF_ANSWERED_CONF_MESSAGE_CARDS_PROMPT.equals(key)) {
                onPrefChanged(PREF_ANSWERED_CONF_MESSAGE_CARDS_PROMPT,
                        sp.getBoolean(PREF_ANSWERED_CONF_MESSAGE_CARDS_PROMPT, true));
            } else if (PREF_CONF_MESSAGE_CARDS_ENABLED.equals(key)) {
                onPrefChanged(PREF_CONF_MESSAGE_CARDS_ENABLED,
                        sp.getBoolean(PREF_CONF_MESSAGE_CARDS_ENABLED, false));
            } else if (key != null && key.startsWith(dismiss_prefix)) {
                onPrefChanged(key, sp.getBoolean(key, false));
            } else if (key != null && key.startsWith(should_show_prefix)) {
                onPrefChanged(key, sp.getBoolean(key, false));
            }
        }

        protected void onPrefChanged(String key, boolean value) {
        }
    }
}