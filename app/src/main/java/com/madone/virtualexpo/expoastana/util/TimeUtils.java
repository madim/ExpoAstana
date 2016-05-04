package com.madone.virtualexpo.expoastana.util;

import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;
import java.util.TimeZone;

public class TimeUtils {

    private static final SimpleDateFormat[] ACCEPTED_TIMESTAMP_FORMATS = {
            new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US),
            new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy", Locale.US),
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US),
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss Z", Locale.US)
    };

    /**
     * Format a {@code date} honoring the app preference for using Conference or device timezone.
     * {@code Context} is used to lookup the shared preference settings.
     */
    public static String formatShortDate(Context context, Date date) {
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);
        TimeZone defaultTz = TimeZone.getDefault();
        return DateUtils.formatDateRange(context, formatter, date.getTime(), date.getTime(),
                DateUtils.FORMAT_ABBREV_ALL | DateUtils.FORMAT_NO_YEAR,
                defaultTz.getID()).toString();
    }

    public static String formatShortTime(Context context, Date time) {
        // Android DateFormatter will honor the user's current settings.
        DateFormat format = android.text.format.DateFormat.getTimeFormat(context);
        // Override with Timezone based on settings since users can override their phone's timezone
        // with Pacific time zones.
        TimeZone tz = TimeZone.getDefault();
        if (tz != null) {
            format.setTimeZone(tz);
        }
        return format.format(time);
    }

    public static long timestampToMillis(String timestamp, long defaultValue) {
        if (TextUtils.isEmpty(timestamp)) {
            return defaultValue;
        }
        Date d = parseTimestamp(timestamp);
        return d == null ? defaultValue : d.getTime();
    }

    public static Date parseTimestamp(String timestamp) {
        for (SimpleDateFormat format : ACCEPTED_TIMESTAMP_FORMATS) {
            // TODO: We shouldn't be forcing the time zone when parsing dates.
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            try {
                return format.parse(timestamp);
            } catch (ParseException ex) {
                continue;
            }
        }

        // All attempts to parse have failed
        return null;
    }

}
