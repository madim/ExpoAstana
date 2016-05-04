package com.madone.virtualexpo.expoastana.provider;

import com.madone.virtualexpo.expoastana.Config;
import com.madone.virtualexpo.expoastana.provider.ScheduleContract.Blocks;
import com.madone.virtualexpo.expoastana.provider.ScheduleContract.Rooms;
import com.madone.virtualexpo.expoastana.provider.ScheduleContract.Sessions;
import com.madone.virtualexpo.expoastana.provider.ScheduleContract.Tags;
import com.madone.virtualexpo.expoastana.provider.ScheduleDatabase.Tables;
import com.madone.virtualexpo.expoastana.util.SelectionBuilder;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ScheduleProvider extends ContentProvider {

    private static final String TAG = "ScheduleProvider";

    private ScheduleDatabase mOpenHelper;

    private ScheduleProviderUriMatcher mUriMatcher;

    @Override
    public boolean onCreate() {
        mOpenHelper = new ScheduleDatabase(getContext());
        mUriMatcher = new ScheduleProviderUriMatcher();
        return true;
    }

    private void deleteDatabase() {
        mOpenHelper.close();
        Context context = getContext();
        ScheduleDatabase.deleteDatabase(context);
        mOpenHelper = new ScheduleDatabase(getContext());
    }

    /** {@inheritDoc} */
    @Override
    public String getType(Uri uri) {
        ScheduleUriEnum matchingUriEnum = mUriMatcher.matchUri(uri);
        return matchingUriEnum.contentType;
    }

    private String makeQuestionMarkTuple(int count) {
        if (count < 1) {
            return "()";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("(?");
        for (int i = 1; i < count; i++) {
            stringBuilder.append(",?");
        }
        stringBuilder.append(")");
        return stringBuilder.toString();
    }

    private void addTagsFilter(SelectionBuilder builder, String tagsFilter, String numCategories) {
        String[] requiredTags = tagsFilter.split(",");
        if (requiredTags.length == 0) {
            return;
        } else if (requiredTags.length == 1) {
            builder.where(Tags.TAG_ID + "=?", requiredTags[0]);
        } else {
            int categories = 1;
            if (numCategories != null && TextUtils.isDigitsOnly(numCategories)) {
                try {
                    categories = Integer.parseInt(numCategories);
                    Log.d(TAG, "Categories being used " + categories);
                } catch (Exception ex) {
                    Log.e(TAG, "exception parsing categories ", ex);
                }
            }
            String questionMarkTuple = makeQuestionMarkTuple(requiredTags.length);
            builder.where(Tags.TAG_ID + " IN " + questionMarkTuple, requiredTags);
            builder.having(
                    "COUNT(" + Qualified.SESSIONS_SESSION_ID + ") >= " + categories);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        final SQLiteDatabase db = mOpenHelper.getReadableDatabase();

        String tagsFilter = uri.getQueryParameter(Sessions.QUERY_PARAMETER_TAG_FILTER);
        String categories = uri.getQueryParameter(Sessions.QUERY_PARAMETER_CATEGORIES);

        ScheduleUriEnum matchingUriEnum = mUriMatcher.matchUri(uri);

        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "uri=" + uri + " code=" + matchingUriEnum.code + " proj=" +
                    Arrays.toString(projection) + " selection=" + selection + " args="
                    + Arrays.toString(selectionArgs) + ")");
        }

        switch (matchingUriEnum) {
            default: {
                 final SelectionBuilder builder = buildExpandedSelection(uri, matchingUriEnum.code);

                if (!TextUtils.isEmpty(tagsFilter) && !TextUtils.isEmpty(categories)) {
                    addTagsFilter(builder, tagsFilter, categories);
                }

                boolean distinct = ScheduleContractHelper.isQueryDistinct(uri);

                Cursor cursor = builder
                        .where(selection, selectionArgs)
                        .query(db, distinct, projection, sortOrder, null);

                Context context = getContext();
                if (null != context) {
                    cursor.setNotificationUri(context.getContentResolver(), uri);
                }
                return cursor;
            }
        }

    }


    /** {@inheritDoc} */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Log.d(TAG, "insert(uri=" + uri + ", values=" + values.toString()
                + ", account=" + "madone@example.com" + ")");

        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        ScheduleUriEnum matchingUriEnum = mUriMatcher.matchUri(uri);
        if (matchingUriEnum.table != null) {
            db.insertOrThrow(matchingUriEnum.table, null, values);
            notifyChange(uri);
        }

        switch (matchingUriEnum) {
            case BLOCKS: {
                return Blocks.buildBlockUri(values.getAsString(Blocks.BLOCK_ID));
            }
            case TAGS: {
                return Tags.buildTagUri(values.getAsString(Tags.TAG_ID));
            }
            case ROOMS: {
                return Rooms.buildRoomUri(values.getAsString(Rooms.ROOM_ID));
            }
            case SESSIONS: {
                return Sessions.buildSessionUri(values.getAsString(Sessions.SESSION_ID));
            }
            case SESSIONS_ID_TAGS: {
                return Tags.buildTagUri(values.getAsString(Tags.TAG_ID));
            }
            default: {
                throw new UnsupportedOperationException("Unknown insert uri: " + uri);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        String accountName = "madone@example.com";
        Log.d(TAG, "update(uri=" + uri + ", values=" + values.toString()
                + ", account=" + accountName + ")");

        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        String accountName = "madone@example.com";
        Log.d(TAG, "delete(uri=" + uri + ", account=" + accountName + ")");

        if (uri == ScheduleContract.BASE_CONTENT_URI) {
            deleteDatabase();
            notifyChange(uri);
            return 1;
        }

        return 0;
    }

    private void notifyChange(Uri uri) {
        if (!ScheduleContractHelper.isUriCalledFromSyncAdapter(uri)) {
            Context context = getContext();
            context.getContentResolver().notifyChange(uri, null);

        }
    }

    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            final int numOperations = operations.size();
            final ContentProviderResult[] results = new ContentProviderResult[numOperations];
            for (int i = 0; i < numOperations; i++) {
                results[i] = operations.get(i).apply(this, results, i);
            }
            db.setTransactionSuccessful();
            return results;
        } finally {
            db.endTransaction();
        }
    }

    private SelectionBuilder buildSimpleSelection(Uri uri) {
        final SelectionBuilder builder = new SelectionBuilder();
        ScheduleUriEnum matchingUriEnum = mUriMatcher.matchUri(uri);
        switch (matchingUriEnum) {
            case BLOCKS:
            case TAGS:
            case ROOMS:
            case SESSIONS:

                return builder.table(matchingUriEnum.table);
            case BLOCKS_ID: {
                final String blockId = Blocks.getBlockId(uri);
                return builder.table(Tables.BLOCKS)
                        .where(Blocks.BLOCK_ID + "=?", blockId);
            }
            case TAGS_ID: {
                final String tagId = Tags.getTagId(uri);
                return builder.table(Tables.TAGS)
                        .where(Tags.TAG_ID + "=?", tagId);
            }
            case ROOMS_ID: {
                final String roomId = Rooms.getRoomId(uri);
                return builder.table(Tables.ROOMS)
                        .where(Rooms.ROOM_ID + "=?", roomId);
            }
            case SESSIONS_ID: {
                final String sessionId = Sessions.getSessionId(uri);
                return builder.table(Tables.SESSIONS)
                        .where(Sessions.SESSION_ID + "=?", sessionId);
            }
            case SESSIONS_ID_TAGS: {
                final String sessionId = Sessions.getSessionId(uri);
                return builder.table(Tables.SESSIONS_TAGS)
                        .where(Sessions.SESSION_ID + "=?", sessionId);
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri for " + uri);
            }
        }
    }

    private SelectionBuilder buildExpandedSelection(Uri uri, int match) {
        final SelectionBuilder builder = new SelectionBuilder();
        ScheduleUriEnum matchingUriEnum = mUriMatcher.matchCode(match);
        if (matchingUriEnum == null) {
            throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        switch (matchingUriEnum) {
            case BLOCKS: {
                return builder.table(Tables.BLOCKS);
            }
            case BLOCKS_BETWEEN: {
                final List<String> segments = uri.getPathSegments();
                final String startTime = segments.get(2);
                final String endTime = segments.get(3);
                return builder.table(Tables.BLOCKS)
                        .where(Blocks.BLOCK_START + ">=?", startTime)
                        .where(Blocks.BLOCK_START + "<=?", endTime);
            }
            case BLOCKS_ID: {
                final String blockId = Blocks.getBlockId(uri);
                return builder.table(Tables.BLOCKS)
                        .where(Blocks.BLOCK_ID + "=?", blockId);
            }
            case TAGS: {
                return builder.table(Tables.TAGS);
            }
            case TAGS_ID: {
                final String tagId = Tags.getTagId(uri);
                return builder.table(Tables.TAGS)
                        .where(Tags.TAG_ID + "=?", tagId);
            }
            case ROOMS: {
                return builder.table(Tables.ROOMS);
            }
            case ROOMS_ID: {
                final String roomId = Rooms.getRoomId(uri);
                return builder.table(Tables.ROOMS)
                        .where(Rooms.ROOM_ID + "=?", roomId);
            }
            case ROOMS_ID_SESSIONS: {
                final String roomId = Rooms.getRoomId(uri);
                return builder.table(Tables.SESSIONS_JOIN_ROOMS)
                        .mapToTable(Sessions._ID, Tables.SESSIONS)
                        .mapToTable(Sessions.ROOM_ID, Tables.SESSIONS)
                        .where(Qualified.SESSIONS_ROOM_ID + "=?", roomId)
                        .groupBy(Qualified.SESSIONS_SESSION_ID);
            }
            case SESSIONS: {
                return builder
                        .table(Tables.SESSIONS_JOIN_ROOMS_TAGS)
                        .mapToTable(Sessions._ID, Tables.SESSIONS)
                        .mapToTable(Sessions.ROOM_ID, Tables.SESSIONS)
                        .mapToTable(Sessions.SESSION_ID, Tables.SESSIONS)
                        .groupBy(Qualified.SESSIONS_SESSION_ID);
            }
            case SESSIONS_AT: {
                final List<String> segments = uri.getPathSegments();
                final String time = segments.get(2);
                return builder.table(Tables.SESSIONS_JOIN_ROOMS)
                        .mapToTable(Sessions._ID, Tables.SESSIONS)
                        .mapToTable(Sessions.ROOM_ID, Tables.SESSIONS)
                        .where(Sessions.SESSION_START + "<=?", time)
                        .where(Sessions.SESSION_END + ">=?", time);
            }
            case SESSIONS_ID: {
                final String sessionId = Sessions.getSessionId(uri);
                return builder.table(Tables.SESSIONS_JOIN_ROOMS)
                        .mapToTable(Sessions._ID, Tables.SESSIONS)
                        .mapToTable(Sessions.ROOM_ID, Tables.SESSIONS)
                        .mapToTable(Sessions.SESSION_ID, Tables.SESSIONS)
                        .where(Qualified.SESSIONS_SESSION_ID + "=?", sessionId);
            }
            case SESSIONS_ROOM_AFTER: {
                final String room = Sessions.getRoom(uri);
                final String time = Sessions.getAfterForRoom(uri);
                return builder.table(Tables.SESSIONS_JOIN_ROOMS_TAGS)
                        .mapToTable(Sessions._ID, Tables.SESSIONS)
                        .mapToTable(Sessions.ROOM_ID, Tables.SESSIONS)
                        .mapToTable(Sessions.SESSION_ID, Tables.SESSIONS)
                        .where(Qualified.SESSIONS_ROOM_ID + "=?", room)
                        .where("(" + Sessions.SESSION_START + "<= ? AND " + Sessions.SESSION_END +
                                        " >= ?) OR (" + Sessions.SESSION_START + " >= ?)", time,
                                time,
                                time)
                        .groupBy(Qualified.SESSIONS_SESSION_ID);
            }
            case SESSIONS_ID_TAGS: {
                final String sessionId = Sessions.getSessionId(uri);
                return builder.table(Tables.SESSIONS_TAGS_JOIN_TAGS)
                        .mapToTable(Tags._ID, Tables.TAGS)
                        .mapToTable(Tags.TAG_ID, Tables.TAGS)
                        .where(Qualified.SESSIONS_TAGS_SESSION_ID + "=?", sessionId);
            }
            case SESSIONS_AFTER: {
                final String time = Sessions.getAfter(uri);
                return builder.table(Tables.SESSIONS_JOIN_ROOMS_TAGS)
                        .mapToTable(Sessions._ID, Tables.SESSIONS)
                        .mapToTable(Sessions.SESSION_ID, Tables.SESSIONS)
                        .mapToTable(Sessions.ROOM_ID, Tables.SESSIONS)
                        .where("(" + Sessions.SESSION_START + "<= ? AND " + Sessions.SESSION_END +
                                        " >= ?) OR (" + Sessions.SESSION_START + " >= ?)", time,
                                time, time)
                        .groupBy(Qualified.SESSIONS_SESSION_ID);
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        throw new UnsupportedOperationException("openFile is not supported for " + uri);
    }

    private interface Qualified {
        String SESSIONS_SESSION_ID = Tables.SESSIONS + "." + Sessions.SESSION_ID;
        String SESSIONS_ROOM_ID = Tables.SESSIONS + "." + Sessions.ROOM_ID;
        String SESSIONS_TAGS_SESSION_ID = Tables.SESSIONS_TAGS + "."
                + ScheduleDatabase.SessionsTags.SESSION_ID;
    }
}