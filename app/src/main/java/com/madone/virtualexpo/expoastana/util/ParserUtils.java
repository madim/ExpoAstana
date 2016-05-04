package com.madone.virtualexpo.expoastana.util;

import android.content.ContentProvider;
import android.net.Uri;
import android.text.format.Time;

import java.util.ArrayList;
import java.util.regex.Pattern;

public class ParserUtils {
    private static final Pattern sSanitizePattern = Pattern.compile("[^a-z0-9-_]");

    public static String sanitizeId(String input) {
        if (input == null) {
            return null;
        }
        return sSanitizePattern.matcher(input.replace("+", "plus").toLowerCase()).replaceAll("");
    }

    /**
     * Parse the given string as a RFC 3339 timestamp, returning the value as
     * milliseconds since the epoch.
     */
    public static long parseTime(String timestamp) {
        final Time time = new Time();
        time.parse3339(timestamp);
        return time.toMillis(false);
    }

    public static String joinStrings(String connector, ArrayList<String> strings, StringBuilder recycle) {
        if (strings.size() <= 0) {
            return "";
        }
        if (recycle == null) {
            recycle = new StringBuilder();
        } else {
            recycle.setLength(0);
        }
        recycle.append(strings.get(0));
        for (int i = 1; i < strings.size(); i++) {
            recycle.append(connector);
            recycle.append(strings.get(i));
        }
        return recycle.toString();
    }
}