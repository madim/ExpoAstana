package com.madone.virtualexpo.expoastana.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.madone.virtualexpo.expoastana.Config;
import com.madone.virtualexpo.expoastana.util.UIUtils;

import java.util.TimeZone;

public class SettingsUtils {

    public static final String PREF_DATA_BOOTSTRAP_DONE = "pref_data_bootstrap_done";

    private static final String CONFERENCE_YEAR_PREF_POSTFIX = "_2015";

    public static final String PREF_LAST_SYNC_SUCCEEDED = "pref_last_sync_succeeded";

    public static final String PREF_ATTENDEE_AT_VENUE = "pref_attendee_at_venue" +
            CONFERENCE_YEAR_PREF_POSTFIX;

    public static boolean isDataBootstrapDone(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_DATA_BOOTSTRAP_DONE, false);
    }

    public static void markDataBootstrapDone(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(PREF_DATA_BOOTSTRAP_DONE, true).apply();
    }

    public static void markSyncSucceededNow(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putLong(PREF_LAST_SYNC_SUCCEEDED, UIUtils.getCurrentTime(context)).apply();
    }

    public static boolean isAttendeeAtVenue(final Context context) {
        return true;
    }

    public static TimeZone getDisplayTimeZone(Context context) {
        TimeZone defaultTz = TimeZone.getDefault();
        return defaultTz;
    }

}
