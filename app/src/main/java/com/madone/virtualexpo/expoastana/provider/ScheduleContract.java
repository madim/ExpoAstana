package com.madone.virtualexpo.expoastana.provider;

import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.text.format.DateUtils;

import com.madone.virtualexpo.expoastana.util.ParserUtils;

import java.util.List;

public class ScheduleContract {

    public static final String CONTENT_AUTHORITY = "com.madone.virtualexpo.expoastana";

    public static final String CONTENT_TYPE_APP_BASE = "iosched2015.";

    public static final String CONTENT_TYPE_BASE = "vnd.android.cursor.dir/vnd."
            + CONTENT_TYPE_APP_BASE;

    public static final String CONTENT_ITEM_TYPE_BASE = "vnd.android.cursor.item/vnd."
            + CONTENT_TYPE_APP_BASE;

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public interface SyncColumns {

        String UPDATED = "updated";
    }

    private static final String PATH_AFTER = "after";
    private static final String PATH_ROOM = "room";
    private static final String PATH_ROOMS = "rooms";
    private static final String PATH_SESSIONS = "sessions";
    private static final String PATH_BLOCKS = "blocks";
    private static final String PATH_TAGS = "tags";
    private static final String PATH_UNSCHEDULED = "unscheduled";
    private static final String PATH_MY_SCHEDULE = "my_schedule";

    public static final String[] TOP_LEVEL_PATHS = {
            PATH_BLOCKS,
            PATH_TAGS,
            PATH_ROOMS,
            PATH_SESSIONS,
            PATH_MY_SCHEDULE
    };

    public static final String[] USER_DATA_RELATED_PATHS = {
            PATH_SESSIONS,
            PATH_MY_SCHEDULE
    };

    interface RoomsColumns {

        String ROOM_ID = "room_id";
        String ROOM_NAME = "room_name";
        String ROOM_FLOOR = "room_floor";
    }

    interface BlocksColumns {

        String BLOCK_ID = "block_id";
        String BLOCK_TITLE = "block_title";
        String BLOCK_START = "block_start";
        String BLOCK_END = "block_end";
        String BLOCK_TYPE = "block_type";
        String BLOCK_SUBTITLE = "block_subtitle";
    }

    interface TagsColumns {

        String TAG_ID = "tag_id";
        String TAG_CATEGORY = "tag_category";
        String TAG_NAME = "tag_name";
        String TAG_ORDER_IN_CATEGORY = "tag_order_in_category";
        String TAG_COLOR = "tag_color";
        String TAG_ABSTRACT = "tag_abstract";
    }

    interface SessionsColumns {

        String SESSION_ID = "session_id";
        String SESSION_LEVEL = "session_level";
        String SESSION_START = "session_start";
        String SESSION_END = "session_end";
        String SESSION_TITLE = "session_title";
        String SESSION_ABSTRACT = "session_abstract";
        String SESSION_HASHTAG = "session_hashtag";
        String SESSION_CAL_EVENT_ID = "session_cal_event_id";
        String SESSION_TAGS = "session_tags";
        String SESSION_GROUPING_ORDER = "session_grouping_order";
        String SESSION_IMPORT_HASHCODE = "session_import_hashcode";
        String SESSION_MAIN_TAG = "session_main_tag";
        String SESSION_COLOR = "session_color";
        String SESSION_PHOTO_URL = "session_photo_url";
    }


    public static class Rooms implements RoomsColumns, BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_ROOMS).build();

        public static final String CONTENT_TYPE_ID = "room";

        public static Uri buildRoomUri(String roomId) {
            return CONTENT_URI.buildUpon().appendPath(roomId).build();
        }

        public static Uri buildSessionsDirUri(String roomId) {
            return CONTENT_URI.buildUpon().appendPath(roomId).appendPath(PATH_SESSIONS).build();
        }

        public static String getRoomId(Uri uri) {
            return uri.getPathSegments().get(1);
        }
    }

    public static String makeContentType(String id) {
        if (id != null) {
            return CONTENT_TYPE_BASE + id;
        } else {
            return null;
        }
    }

    public static String makeContentItemType(String id) {
        if (id != null) {
            return CONTENT_ITEM_TYPE_BASE + id;
        } else {
            return null;
        }
    }

    public static class Blocks implements BlocksColumns, BaseColumns {

        public static final String BLOCK_TYPE_FREE = "free";

        public static final String BLOCK_TYPE_BREAK = "break";

        public static final String BLOCK_TYPE_KEYNOTE = "keynote";

        public static final boolean isValidBlockType(String type) {
            return BLOCK_TYPE_FREE.equals(type) || BLOCK_TYPE_BREAK.equals(type)
                    || BLOCK_TYPE_KEYNOTE.equals(type);
        }

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_BLOCKS).build();

        public static final String CONTENT_TYPE_ID = "block";

        public static Uri buildBlockUri(String blockId) {
            return CONTENT_URI.buildUpon().appendPath(blockId).build();
        }

        public static String getBlockId(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        public static String generateBlockId(long startTime, long endTime) {
            startTime /= DateUtils.SECOND_IN_MILLIS;
            endTime /= DateUtils.SECOND_IN_MILLIS;
            return ParserUtils.sanitizeId(startTime + "-" + endTime);
        }
    }

    public static class Tags implements TagsColumns, BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_TAGS).build();

        public static final String CONTENT_TYPE_ID = "tag";

        public static final String TAG_ORDER_BY_CATEGORY = Tags.TAG_ORDER_IN_CATEGORY + " ASC";

        public static Uri buildTagsUri() {
            return CONTENT_URI;
        }

        public static Uri buildTagUri(String tagId) {
            return CONTENT_URI.buildUpon().appendPath(tagId).build();
        }

        public static String getTagId(Uri uri) {
            return uri.getPathSegments().get(1);
        }
    }

    public static class Sessions implements SessionsColumns, RoomsColumns, BaseColumns {

        public static final String QUERY_PARAMETER_TAG_FILTER = "filter";
        public static final String QUERY_PARAMETER_CATEGORIES = "categories";

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_SESSIONS).build();

        public static final Uri CONTENT_MY_SCHEDULE_URI =
                CONTENT_URI.buildUpon().appendPath(PATH_MY_SCHEDULE).build();

        public static final String CONTENT_TYPE_ID = "session";

        public static final String ROOM_ID = "room_id";

        public static final String HAS_GIVEN_FEEDBACK = "has_given_feedback";

        public static final String SORT_BY_TYPE_THEN_TIME = SESSION_GROUPING_ORDER + " ASC,"
                + SESSION_START + " ASC," + SESSION_TITLE + " COLLATE NOCASE ASC";

        public static final String STARTING_AT_TIME_INTERVAL_SELECTION =
                SESSION_START + " >= ? and " + SESSION_START + " <= ?";

        public static final String AT_TIME_SELECTION =
                SESSION_START + " <= ? and " + SESSION_END + " >= ?";

        public static String[] buildAtTimeIntervalArgs(long intervalStart, long intervalEnd) {
            return new String[]{String.valueOf(intervalStart), String.valueOf(intervalEnd)};
        }

        public static String[] buildAtTimeSelectionArgs(long time) {
            final String timeString = String.valueOf(time);
            return new String[]{timeString, timeString};
        }

        public static final String UPCOMING_LIVE_SELECTION = SESSION_START + " > ?";

        public static String[] buildUpcomingSelectionArgs(long minTime) {
            return new String[]{String.valueOf(minTime)};
        }

        public static Uri buildSessionUri(String sessionId) {
            return CONTENT_URI.buildUpon().appendPath(sessionId).build();
        }

        public static Uri buildTagsDirUri(String sessionId) {
            return CONTENT_URI.buildUpon().appendPath(sessionId).appendPath(PATH_TAGS).build();
        }

        public static Uri buildSessionsInRoomAfterUri(String room, long time) {
            return CONTENT_URI.buildUpon().appendPath(PATH_ROOM).appendPath(room)
                    .appendPath(PATH_AFTER)
                    .appendPath(String.valueOf(time)).build();
        }

        public static Uri buildSessionsAfterUri(long time) {
            return CONTENT_URI.buildUpon().appendPath(PATH_AFTER)
                    .appendPath(String.valueOf(time)).build();
        }

        public static Uri buildUnscheduledSessionsInInterval(long start, long end) {
            String interval = start + "-" + end;
            return CONTENT_URI.buildUpon().appendPath(PATH_UNSCHEDULED).appendPath(interval)
                    .build();
        }

        public static boolean isUnscheduledSessionsInInterval(Uri uri) {
            return uri != null && uri.toString().startsWith(
                    CONTENT_URI.buildUpon().appendPath(PATH_UNSCHEDULED).toString());
        }

        public static long[] getInterval(Uri uri) {
            if (uri == null) {
                return null;
            }
            List<String> segments = uri.getPathSegments();
            if (segments.size() == 3 && segments.get(2).indexOf('-') > 0) {
                String[] interval = segments.get(2).split("-");
                return new long[]{Long.parseLong(interval[0]), Long.parseLong(interval[1])};
            }
            return null;
        }

        public static String getRoom(Uri uri) {
            return uri.getPathSegments().get(2);
        }

        public static String getAfterForRoom(Uri uri) {
            return uri.getPathSegments().get(4);
        }

        public static String getAfter(Uri uri) {
            return uri.getPathSegments().get(2);
        }

        public static String getSessionId(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        public static String getSearchQuery(Uri uri) {
            List<String> segments = uri.getPathSegments();
            if (2 < segments.size()) {
                return segments.get(2);
            }
            return null;
        }

        public static boolean hasFilterParam(Uri uri) {
            return uri != null && uri.getQueryParameter(QUERY_PARAMETER_TAG_FILTER) != null;
        }

        @Deprecated
        public static Uri buildTagFilterUri(Uri contentUri, String[] requiredTags) {
            return buildCategoryTagFilterUri(contentUri, requiredTags,
                    requiredTags == null ? 0 : requiredTags.length);
        }

        @Deprecated
        public static Uri buildTagFilterUri(String[] requiredTags) {
            return buildTagFilterUri(CONTENT_URI, requiredTags);
        }

        public static Uri buildCategoryTagFilterUri(Uri contentUri, String[] tags, int categories) {
            StringBuilder sb = new StringBuilder();
            for (String tag : tags) {
                if (TextUtils.isEmpty(tag)) {
                    continue;
                }
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(tag.trim());
            }
            if (sb.length() == 0) {
                return contentUri;
            } else {
                return contentUri.buildUpon()
                        .appendQueryParameter(QUERY_PARAMETER_TAG_FILTER, sb.toString())
                        .appendQueryParameter(QUERY_PARAMETER_CATEGORIES,
                                String.valueOf(categories))
                        .build();
            }
        }

    }

}