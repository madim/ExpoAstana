package com.madone.virtualexpo.expoastana.io;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.madone.virtualexpo.expoastana.Config;
import com.madone.virtualexpo.expoastana.R;
import com.madone.virtualexpo.expoastana.io.model.Session;
import com.madone.virtualexpo.expoastana.io.model.Tag;
import com.madone.virtualexpo.expoastana.provider.ScheduleContract;
import com.madone.virtualexpo.expoastana.provider.ScheduleContractHelper;
import com.madone.virtualexpo.expoastana.provider.ScheduleDatabase;
import com.madone.virtualexpo.expoastana.util.TimeUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class SessionsHandler extends JSONHandler {
    private static final String TAG = "SessionHandler";
    private HashMap<String, Session> mSessions = new HashMap<String, Session>();
    private HashMap<String, Tag> mTagMap = null;
    private int mDefaultSessionColor;

    public SessionsHandler(Context context) {
        super(context);
        mDefaultSessionColor = mContext.getResources().getColor(R.color.default_session_color);
    }

    @Override
    public void process(JsonElement element) {
        for (Session session : new Gson().fromJson(element, Session[].class)) {
            mSessions.put(session.id, session);
        }
    }

    @Override
    public void makeContentProviderOperations(ArrayList<ContentProviderOperation> list) {
        Uri uri = ScheduleContractHelper.setUriAsCalledFromSyncAdapter(
                ScheduleContract.Sessions.CONTENT_URI);

        HashMap<String, String> sessionHashCodes = loadSessionHashCodes();
        boolean incrementalUpdate = (sessionHashCodes != null) && (sessionHashCodes.size() > 0);

        HashSet<String> sessionsToKeep = new HashSet<String>();

        if (incrementalUpdate) {
            Log.d(TAG, "Doing incremental update for sessions.");
        } else {
            Log.d(TAG, "Doing full (non-incremental) update for sessions.");
            list.add(ContentProviderOperation.newDelete(uri).build());
        }

        int updatedSessions = 0;
        for (Session session : mSessions.values()) {
            session.groupingOrder = computeTypeOrder(session);

            String hashCode = session.getImportHashCode();
            sessionsToKeep.add(session.id);

            if (!incrementalUpdate || !sessionHashCodes.containsKey(session.id) ||
                    !sessionHashCodes.get(session.id).equals(hashCode)) {
                ++updatedSessions;
                boolean isNew = !incrementalUpdate || !sessionHashCodes.containsKey(session.id);
                buildSession(isNew, session, list);

                buildTagsMapping(session, list);
            }
        }

        int deletedSessions = 0;
        if (incrementalUpdate) {
            for (String sessionId : sessionHashCodes.keySet()) {
                if (!sessionsToKeep.contains(sessionId)) {
                    buildDeleteOperation(sessionId, list);
                    ++deletedSessions;
                }
            }
        }

        Log.d(TAG, "Sessions: " + (incrementalUpdate ? "INCREMENTAL" : "FULL") + " update. " +
                updatedSessions + " to update, " + deletedSessions + " to delete. New total: " +
                mSessions.size());
    }

    private void buildDeleteOperation(String sessionId, List<ContentProviderOperation> list) {
        Uri sessionUri = ScheduleContractHelper.setUriAsCalledFromSyncAdapter(
                ScheduleContract.Sessions.buildSessionUri(sessionId));
        list.add(ContentProviderOperation.newDelete(sessionUri).build());
    }

    private HashMap<String, String> loadSessionHashCodes() {
        Uri uri = ScheduleContractHelper.setUriAsCalledFromSyncAdapter(
                ScheduleContract.Sessions.CONTENT_URI);
        Log.d(TAG, "Loading session hashcodes for session import optimization.");
        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(uri, SessionHashcodeQuery.PROJECTION,
                    null, null, null);
            if (cursor == null || cursor.getCount() < 1) {
                Log.w(TAG, "Warning: failed to load session hashcodes. Not optimizing session import.");
                return null;
            }
            HashMap<String, String> hashcodeMap = new HashMap<String, String>();
            if (cursor.moveToFirst()) {
                do {
                    String sessionId = cursor.getString(SessionHashcodeQuery.SESSION_ID);
                    String hashcode = cursor.getString(SessionHashcodeQuery.SESSION_IMPORT_HASHCODE);
                    hashcodeMap.put(sessionId, hashcode == null ? "" : hashcode);
                } while (cursor.moveToNext());
            }
            Log.d(TAG, "Session hashcodes loaded for " + hashcodeMap.size() + " sessions.");
            return hashcodeMap;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    StringBuilder mStringBuilder = new StringBuilder();

    private void buildSession(boolean isInsert,
                              Session session, ArrayList<ContentProviderOperation> list) {
        ContentProviderOperation.Builder builder;
        Uri allSessionsUri = ScheduleContractHelper
                .setUriAsCalledFromSyncAdapter(ScheduleContract.Sessions.CONTENT_URI);
        Uri thisSessionUri = ScheduleContractHelper
                .setUriAsCalledFromSyncAdapter(ScheduleContract.Sessions.buildSessionUri(
                        session.id));

        if (isInsert) {
            builder = ContentProviderOperation.newInsert(allSessionsUri);
        } else {
            builder = ContentProviderOperation.newUpdate(thisSessionUri);
        }

        int color = mDefaultSessionColor;
        try {
            if (!TextUtils.isEmpty(session.color)) {
                color = Color.parseColor(session.color);
            }
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "Ignoring invalid formatted session color: "+session.color);
        }

        builder.withValue(ScheduleContract.SyncColumns.UPDATED, System.currentTimeMillis())
                .withValue(ScheduleContract.Sessions.SESSION_ID, session.id)
                .withValue(ScheduleContract.Sessions.SESSION_LEVEL, null)            // Not available
                .withValue(ScheduleContract.Sessions.SESSION_TITLE, session.title)
                .withValue(ScheduleContract.Sessions.SESSION_ABSTRACT, session.description)
                .withValue(ScheduleContract.Sessions.SESSION_HASHTAG, session.hashtag)
                .withValue(ScheduleContract.Sessions.SESSION_START, TimeUtils.timestampToMillis(session.startTimestamp, 0))
                .withValue(ScheduleContract.Sessions.SESSION_END, TimeUtils.timestampToMillis(session.endTimestamp, 0))
                .withValue(ScheduleContract.Sessions.SESSION_TAGS, session.makeTagsList())
                .withValue(ScheduleContract.Sessions.ROOM_ID, session.room)
                .withValue(ScheduleContract.Sessions.SESSION_GROUPING_ORDER, session.groupingOrder)
                .withValue(ScheduleContract.Sessions.SESSION_IMPORT_HASHCODE,
                        session.getImportHashCode())
                .withValue(ScheduleContract.Sessions.SESSION_MAIN_TAG, session.mainTag)
                .withValue(ScheduleContract.Sessions.SESSION_PHOTO_URL, session.photoUrl)
                .withValue(ScheduleContract.Sessions.SESSION_COLOR, color);
        list.add(builder.build());
    }

    private int computeTypeOrder(Session session) {
        int order = Integer.MAX_VALUE;
        int keynoteOrder = -1;
        if (mTagMap == null) {
            throw new IllegalStateException("Attempt to compute type order without tag map.");
        }
        for (String tagId : session.tags) {
            if (Config.Tags.SPECIAL_KEYNOTE.equals(tagId)) {
                return keynoteOrder;
            }
            Tag tag = mTagMap.get(tagId);
            if (tag != null && Config.Tags.SESSION_GROUPING_TAG_CATEGORY.equals(tag.category)) {
                if (tag.order_in_category < order) {
                    order = tag.order_in_category;
                }
            }
        }
        return order;
    }

    private void buildTagsMapping(Session session, ArrayList<ContentProviderOperation> list) {
        final Uri uri = ScheduleContractHelper.setUriAsCalledFromSyncAdapter(
                ScheduleContract.Sessions.buildTagsDirUri(session.id));

        list.add(ContentProviderOperation.newDelete(uri).build());

        for (String tag : session.tags) {
            list.add(ContentProviderOperation.newInsert(uri)
                    .withValue(ScheduleDatabase.SessionsTags.SESSION_ID, session.id)
                    .withValue(ScheduleDatabase.SessionsTags.TAG_ID, tag).build());
        }
    }

    public void setTagMap(HashMap<String, Tag> tagMap) {
        mTagMap = tagMap;
    }

    private interface SessionHashcodeQuery {
        String[] PROJECTION = {
                BaseColumns._ID,
                ScheduleContract.Sessions.SESSION_ID,
                ScheduleContract.Sessions.SESSION_IMPORT_HASHCODE
        };
        int _ID = 0;
        int SESSION_ID = 1;
        int SESSION_IMPORT_HASHCODE = 2;
    };
}