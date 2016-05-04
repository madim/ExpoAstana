package com.madone.virtualexpo.expoastana.explore;

import com.google.common.annotations.VisibleForTesting;
import com.madone.virtualexpo.expoastana.Config;
import com.madone.virtualexpo.expoastana.R;
import com.madone.virtualexpo.expoastana.explore.data.ItemGroup;
import com.madone.virtualexpo.expoastana.explore.data.SessionData;
import com.madone.virtualexpo.expoastana.explore.data.ThemeGroup;
import com.madone.virtualexpo.expoastana.explore.data.TopicGroup;
import com.madone.virtualexpo.expoastana.framework.Model;
import com.madone.virtualexpo.expoastana.framework.QueryEnum;
import com.madone.virtualexpo.expoastana.framework.UserActionEnum;
import com.madone.virtualexpo.expoastana.provider.ScheduleContract;
import com.madone.virtualexpo.expoastana.settings.SettingsUtils;
import com.madone.virtualexpo.expoastana.util.TimeUtils;
import com.madone.virtualexpo.expoastana.util.UIUtils;

import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

public class ExploreModel implements Model {

    private static final String TAG = "ExploreModel";

    private final Context mContext;

    /**
     * Topic groups loaded from the database pre-randomly filtered and stored by topic name.
     */
    private Map<String, TopicGroup> mTopics = new HashMap<>();

    /**
     * Theme groups loaded from the database pre-randomly filtered and stored by topic name.
     */
    private Map<String, ThemeGroup> mThemes = new HashMap<>();

    private Map<String, String> mTagTitles;

    private SessionData mKeynoteData;

    public ExploreModel(Context context) {
        mContext = context;
    }

    public Collection<TopicGroup> getTopics() {
        return mTopics.values();
    }

    public Collection<ThemeGroup> getThemes() {
        return mThemes.values();
    }

    public Map<String, String> getTagTitles() { return mTagTitles; }

    public SessionData getKeynoteData() { return mKeynoteData; }

    @Override
    public QueryEnum[] getQueries() {
        return ExploreQueryEnum.values();
    }

    @Override
    public boolean readDataFromCursor(Cursor cursor, QueryEnum query) {
        Log.d(TAG, "readDataFromCursor");
        if (query == ExploreQueryEnum.SESSIONS) {
            Log.d(TAG, "Reading session data from cursor.");

            boolean atVenue = true;
            int themeSessionLimit = getThemeSessionLimit(mContext);

            int topicSessionLimit = getTopicSessionLimit(mContext);

            Map<String, TopicGroup> topicGroups = new HashMap<>();
            Map<String, ThemeGroup> themeGroups = new HashMap<>();

            // Iterating through rows in Sessions query.
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    SessionData session = new SessionData();
                    populateSessionFromCursorRow(session, cursor);

                    if (TextUtils.isEmpty(session.getSessionName()) ||
                            TextUtils.isEmpty(session.getDetails()) ||
                            TextUtils.isEmpty(session.getSessionId()) ||
                            TextUtils.isEmpty(session.getImageUrl())) {
                        continue;
                    }

                    String tags = session.getTags();

                    if (Config.Tags.SPECIAL_KEYNOTE.equals(session.getMainTag())) {
                        SessionData keynoteData = new SessionData();
                        populateSessionFromCursorRow(keynoteData, cursor);
                        rewriteKeynoteDetails(keynoteData);
                        mKeynoteData = keynoteData;
                    }

                    if (!TextUtils.isEmpty(tags)) {
                        StringTokenizer tagsTokenizer = new StringTokenizer(tags, ",");
                        while (tagsTokenizer.hasMoreTokens()) {
                            String rawTag = tagsTokenizer.nextToken();
                            if (rawTag.startsWith("TOPIC_")) {
                                TopicGroup topicGroup = topicGroups.get(rawTag);
                                if (topicGroup == null) {
                                    topicGroup = new TopicGroup();
                                    topicGroup.setTitle(rawTag);
                                    topicGroup.setId(rawTag);
                                    topicGroups.put(rawTag, topicGroup);
                                }
                                topicGroup.addSessionData(session);

                            } else if (rawTag.startsWith("THEME_")) {
                                ThemeGroup themeGroup = themeGroups.get(rawTag);
                                if (themeGroup == null) {
                                    themeGroup = new ThemeGroup();
                                    themeGroup.setTitle(rawTag);
                                    themeGroup.setId(rawTag);
                                    themeGroups.put(rawTag, themeGroup);
                                }
                                themeGroup.addSessionData(session);
                            }
                        }
                    }
                } while (cursor.moveToNext());
            }

            for (ItemGroup group : themeGroups.values()) {
                group.trimSessionData(themeSessionLimit);
            }
            for (ItemGroup group : topicGroups.values()) {
                group.trimSessionData(topicSessionLimit);
            }

            mThemes = themeGroups;
            mTopics = topicGroups;
            return true;
        } else if (query == ExploreQueryEnum.TAGS) {
            Log.w(TAG, "TAGS query loaded");
            Map<String, String> newTagTitles = new HashMap<>();
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String tagId = cursor.getString(cursor.getColumnIndex(
                            ScheduleContract.Tags.TAG_ID));
                    String tagName = cursor.getString(cursor.getColumnIndex(
                            ScheduleContract.Tags.TAG_NAME));
                    newTagTitles.put(tagId, tagName);
                } while (cursor.moveToNext());
                mTagTitles = newTagTitles;
            }
            return true;
        }
        return false;
    }

    public static int getTopicSessionLimit(Context context) {
        boolean atVenue = true;
        int topicSessionLimit;
        if (atVenue) {
            topicSessionLimit = context.getResources().getInteger(R.integer
                    .explore_topic_theme_onsite_max_item_count);
        } else {
            topicSessionLimit = 0;
        }
        return topicSessionLimit;
    }

    public static int getThemeSessionLimit(Context context) {
        boolean atVenue = true;
        int themeSessionLimit;
        if (atVenue) {
            themeSessionLimit = context.getResources().getInteger(R.integer
                    .explore_topic_theme_onsite_max_item_count);
        } else {
            themeSessionLimit = context.getResources().getInteger(R.integer
                    .explore_theme_max_item_count_offsite);
        }
        return themeSessionLimit;
    }

    private void rewriteKeynoteDetails(SessionData keynoteData) {
        long startTime, endTime, currentTime;
        currentTime = UIUtils.getCurrentTime(mContext);
        if (keynoteData.getStartDate() != null) {
            startTime = keynoteData.getStartDate().getTime();
        } else {
            Log.d(TAG, "Keynote start time wasn't set");
            startTime = 0;
        }
        if (keynoteData.getEndDate() != null) {
            endTime = keynoteData.getEndDate().getTime();
        } else {
            Log.d(TAG, "Keynote end time wasn't set");
            endTime = Long.MAX_VALUE;
        }

        StringBuilder stringBuilder = new StringBuilder();
        if (currentTime >= startTime && currentTime < endTime) {
            stringBuilder.append("Live");
        } else {
            String shortDate = TimeUtils.formatShortDate(mContext, keynoteData.getStartDate());
            stringBuilder.append(shortDate);

            if (startTime > 0) {
                stringBuilder.append(" / " );
                stringBuilder.append(TimeUtils.formatShortTime(mContext,
                        new java.util.Date(startTime)));
            }
        }
        keynoteData.setDetails(stringBuilder.toString());
    }

    private void populateSessionFromCursorRow(SessionData session, Cursor cursor) {
        session.updateData(
                cursor.getString(cursor.getColumnIndex(
                        ScheduleContract.Sessions.SESSION_TITLE)),
                cursor.getString(cursor.getColumnIndex(
                        ScheduleContract.Sessions.SESSION_ABSTRACT)),
                cursor.getString(cursor.getColumnIndex(
                        ScheduleContract.Sessions.SESSION_ID)),
                cursor.getString(cursor.getColumnIndex(
                        ScheduleContract.Sessions.SESSION_PHOTO_URL)),
                cursor.getString(cursor.getColumnIndex(
                        ScheduleContract.Sessions.SESSION_MAIN_TAG)),
                cursor.getLong(cursor.getColumnIndex(
                        ScheduleContract.Sessions.SESSION_START)),
                cursor.getLong(cursor.getColumnIndex(
                        ScheduleContract.Sessions.SESSION_END)),
                cursor.getString(cursor.getColumnIndex(
                        ScheduleContract.Sessions.SESSION_TAGS)));
    }

    @Override
    public Loader<Cursor> createCursorLoader(int loaderId, Uri uri, @Nullable Bundle args) {
        CursorLoader loader = null;

        if (loaderId == ExploreQueryEnum.SESSIONS.getId()) {

            // Create and return the Loader.
            loader = getCursorLoaderInstance(mContext, uri,
                    ExploreQueryEnum.SESSIONS.getProjection(), null, null,
                    ScheduleContract.Sessions.SORT_BY_TYPE_THEN_TIME);
        } else if (loaderId == ExploreQueryEnum.TAGS.getId()) {
            Log.w(TAG, "Starting sessions tag query");
            loader =  new CursorLoader(mContext, ScheduleContract.Tags.CONTENT_URI,
                    ExploreQueryEnum.TAGS.getProjection(), null, null, null);
        } else {
            Log.e(TAG, "Invalid query loaderId: " + loaderId);
        }
        return loader;
    }

    @VisibleForTesting
    public CursorLoader getCursorLoaderInstance(Context context, Uri uri, String[] projection,
                                                String selection, String[] selectionArgs, String sortOrder) {
        return new CursorLoader(context, uri, projection, selection, selectionArgs, sortOrder);
    }

    @Override
    public boolean requestModelUpdate(UserActionEnum action, @Nullable Bundle args) {
        return true;
    }

    public static enum ExploreQueryEnum implements QueryEnum {

        SESSIONS(0x1, new String[]{
                ScheduleContract.Sessions.SESSION_ID,
                ScheduleContract.Sessions.SESSION_TITLE,
                ScheduleContract.Sessions.SESSION_ABSTRACT,
                ScheduleContract.Sessions.SESSION_TAGS,
                ScheduleContract.Sessions.SESSION_MAIN_TAG,
                ScheduleContract.Sessions.SESSION_PHOTO_URL,
                ScheduleContract.Sessions.SESSION_START,
                ScheduleContract.Sessions.SESSION_END,
                ScheduleContract.Sessions.SESSION_START,
                ScheduleContract.Sessions.SESSION_END,
        }),

        TAGS(0x2, new String[] {
                ScheduleContract.Tags.TAG_ID,
                ScheduleContract.Tags.TAG_NAME,
        });


        private int id;

        private String[] projection;

        ExploreQueryEnum(int id, String[] projection) {
            this.id = id;
            this.projection = projection;
        }

        @Override
        public int getId() {
            return id;
        }

        @Override
        public String[] getProjection() {
            return projection;
        }
    }

    public static enum ExploreUserActionEnum implements UserActionEnum {

        RELOAD(2);

        private int id;

        ExploreUserActionEnum(int id) {
            this.id = id;
        }

        @Override
        public int getId() {
            return id;
        }

    }
}