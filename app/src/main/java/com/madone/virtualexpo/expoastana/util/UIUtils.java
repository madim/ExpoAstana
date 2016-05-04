package com.madone.virtualexpo.expoastana.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.text.Html;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

import com.madone.virtualexpo.expoastana.BuildConfigs;
import com.madone.virtualexpo.expoastana.Config;
import com.madone.virtualexpo.expoastana.R;
import com.madone.virtualexpo.expoastana.model.ScheduleItem;
import com.madone.virtualexpo.expoastana.settings.SettingsUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;
import java.util.TimeZone;
import java.util.regex.Pattern;

public class UIUtils {

    public static final float SESSION_BG_COLOR_SCALE_FACTOR = 0.75f;

    private static final Pattern REGEX_HTML_ESCAPE = Pattern.compile(".*&\\S;.*");

    private static final int TIME_FLAGS = DateUtils.FORMAT_SHOW_TIME
            | DateUtils.FORMAT_SHOW_DATE;

    public static void setAccessibilityIgnore(View view) {
        view.setClickable(false);
        view.setFocusable(false);
        view.setContentDescription("");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
    }

    public static int scaleSessionColorToDefaultBG(int color) {
        return scaleColor(color, SESSION_BG_COLOR_SCALE_FACTOR, false);
    }

    public static int scaleColor(int color, float factor, boolean scaleAlpha) {
        return Color.argb(scaleAlpha ? (Math.round(Color.alpha(color) * factor)) : Color.alpha(color),
                Math.round(Color.red(color) * factor), Math.round(Color.green(color) * factor),
                Math.round(Color.blue(color) * factor));
    }

    public static int setColorOpaque(int color){
        return Color.argb(255, Color.red(color), Color.green(color), Color.blue(color));
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static boolean isRtl(final Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return false;
        } else {
            return context.getResources().getConfiguration().getLayoutDirection()
                    == View.LAYOUT_DIRECTION_RTL;
        }
    }

    /**
     * Retrieve the current time. If the current build is a debug build the mock time is returned
     * when set.
     */
    public static long getCurrentTime() {

        return System.currentTimeMillis();

    }

    public static @DrawableRes
    int getBreakIcon(String breakTitle) {
        if (!TextUtils.isEmpty(breakTitle)) {
            if (breakTitle.contains("After")) {
                return R.drawable.ic_after_hours;
            } else if (breakTitle.contains("Badge")) {
                return R.drawable.ic_badge_pickup;
            } else if (breakTitle.contains("Pre-Keynote")) {
                return R.drawable.ic_session;
            }
        }
        return R.drawable.ic_food;
    }

    public static @DrawableRes int getSessionIcon(int sessionType) {
        switch (sessionType) {
            case ScheduleItem.SESSION_TYPE_SESSION:
                return R.drawable.ic_session;
            case ScheduleItem.SESSION_TYPE_CODELAB:
                return R.drawable.ic_codelab;
            case ScheduleItem.SESSION_TYPE_BOXTALK:
                return R.drawable.ic_sandbox;
            case ScheduleItem.SESSION_TYPE_MISC:
            default:
                return R.drawable.ic_misc;
        }
    }

    public static String formatSessionSubtitle(long intervalStart, long intervalEnd, String roomName, StringBuilder recycle,
                                               Context context) {
        return formatSessionSubtitle(intervalStart, intervalEnd, roomName, recycle, context, false);
    }

    public static String formatSessionSubtitle(long intervalStart, long intervalEnd, String roomName, StringBuilder recycle,
                                               Context context, boolean shortFormat) {

        // Determine if the session is in the past
        long currentTimeMillis = UIUtils.getCurrentTime(context);
        boolean conferenceEnded = currentTimeMillis > Config.CONFERENCE_END_MILLIS;
        boolean sessionEnded = currentTimeMillis > intervalEnd;

        if (roomName == null) {
            roomName = context.getString(R.string.unknown_room);
        }

        if (shortFormat) {
            TimeZone timeZone = SettingsUtils.getDisplayTimeZone(context);
            Date intervalStartDate = new Date(intervalStart);
            SimpleDateFormat shortDateFormat = new SimpleDateFormat("MMM dd");
            DateFormat shortTimeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
            shortDateFormat.setTimeZone(timeZone);
            shortTimeFormat.setTimeZone(timeZone);
            return shortDateFormat.format(intervalStartDate) + " "
                    + shortTimeFormat.format(intervalStartDate);
        } else {
            String timeInterval = formatIntervalTimeString(intervalStart, intervalEnd, recycle,
                    context);
            return context.getString(R.string.session_subtitle, timeInterval, roomName);
        }
    }

    public static String formatIntervalTimeString(long intervalStart, long intervalEnd,
                                                  StringBuilder recycle, Context context) {
        if (recycle == null) {
            recycle = new StringBuilder();
        } else {
            recycle.setLength(0);
        }
        Formatter formatter = new Formatter(recycle);
        return DateUtils.formatDateRange(context, formatter, intervalStart, intervalEnd, TIME_FLAGS,
                SettingsUtils.getDisplayTimeZone(context).getID()).toString();
    }

    /**
     * Format and return the given session speakers and {@link Rooms} values.
     */
    public static String formatSessionSubtitle(String roomName,
                                               Context context) {

        // Determine if the session is in the past
        if (roomName == null) {
            roomName = context.getString(R.string.unknown_room);
        }


        return roomName;

    }

    public static long getCurrentTime(final Context context) {

        return System.currentTimeMillis();

    }

    private static final int[] RES_IDS_ACTION_BAR_SIZE = { R.attr.actionBarSize };

    /** Calculates the Action Bar height in pixels. */
    public static int calculateActionBarSize(Context context) {
        if (context == null) {
            return 0;
        }

        Resources.Theme curTheme = context.getTheme();
        if (curTheme == null) {
            return 0;
        }

        TypedArray att = curTheme.obtainStyledAttributes(RES_IDS_ACTION_BAR_SIZE);
        if (att == null) {
            return 0;
        }

        float size = att.getDimension(0, 0);
        att.recycle();
        return (int) size;
    }

    public static int startTimeToDayIndex(long startTime) {
        if (startTime <= Config.CONFERENCE_DAYS[0][1] &&
                startTime >= Config.CONFERENCE_DAYS[0][0]) {
            return 1;
        } else if (startTime <= Config.CONFERENCE_DAYS[1][1] &&
                startTime >= Config.CONFERENCE_DAYS[1][0]) {
            return 2;
        }
        return 0;
    }

    /**
     * @param startTime The start time of a session in millis.
     * @param context The context to be used for getting the display timezone.
     * @return Formats a given startTime to the specific short time.
     *         example: 12:00 AM
     */
    public static String formatTime(long startTime, Context context) {
        StringBuilder sb = new StringBuilder();
        DateUtils.formatDateRange(context, new Formatter(sb), startTime, startTime,
                DateUtils.FORMAT_SHOW_TIME,
                SettingsUtils.getDisplayTimeZone(context).getID());
        return sb.toString();
    }

    public static float getProgress(int value, int min, int max) {
        if (min == max) {
            throw new IllegalArgumentException("Max (" + max + ") cannot equal min (" + min + ")");
        }

        return (value - min) / (float) (max - min);
    }

    public static void setTextMaybeHtml(TextView view, String text) {
        if (TextUtils.isEmpty(text)) {
            view.setText("");
            return;
        }
        if ((text.contains("<") && text.contains(">")) || REGEX_HTML_ESCAPE.matcher(text).find()) {
            view.setText(Html.fromHtml(text));
            view.setMovementMethod(LinkMovementMethod.getInstance());
        } else {
            view.setText(text);
        }
    }

}