package com.madone.virtualexpo.expoastana.provider;

import android.accounts.Account;
import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import com.madone.virtualexpo.expoastana.provider.ScheduleContract.*;
import com.madone.virtualexpo.expoastana.sync.ConferenceDataHandler;

public class ScheduleDatabase extends SQLiteOpenHelper {
    private static final String TAG = "ScheduleDatabase";

    private static final String DATABASE_NAME = "schedule.db";

    private static final int CUR_DATABASE_VERSION = 1;

    private final Context mContext;

    interface Tables {
        String BLOCKS = "blocks";
        String TAGS = "tags";
        String ROOMS = "rooms";
        String SESSIONS = "sessions";

        String SESSIONS_TAGS = "sessions_tags";
        String HASHTAGS = "hashtags";

        String SESSIONS_JOIN_ROOMS_TAGS = "sessions "
                + "LEFT OUTER JOIN rooms ON sessions.room_id=rooms.room_id "
                + "LEFT OUTER JOIN sessions_tags ON sessions.session_id=sessions_tags.session_id";

        String SESSIONS_JOIN_ROOMS = "sessions "
                + "LEFT OUTER JOIN rooms ON sessions.room_id=rooms.room_id";

        String SESSIONS_TAGS_JOIN_TAGS = "sessions_tags "
                + "LEFT OUTER JOIN tags ON sessions_tags.tag_id=tags.tag_id";

        enum DeprecatedTables {
            TRACKS("tracks"),
            SESSIONS_TRACKS("sessions_tracks"),
            SANDBOX("sandbox"),
            PEOPLE_IVE_MET("people_ive_met"),
            EXPERTS("experts"),
            PARTNERS("partners");

            String tableName;

            DeprecatedTables(String tableName) {
                this.tableName = tableName;
            }
        };
    }

    private interface Triggers {
        String SESSIONS_TAGS_DELETE = "sessions_tags_delete";
        String SESSIONS_SPEAKERS_DELETE = "sessions_speakers_delete";
        String SESSIONS_MY_SCHEDULE_DELETE = "sessions_myschedule_delete";
        String SESSIONS_FEEDBACK_DELETE = "sessions_feedback_delete";

        interface DeprecatedTriggers {
            String SESSIONS_TRACKS_DELETE = "sessions_tracks_delete";
        };
    }

    public interface SessionsTags {
        String SESSION_ID = "session_id";
        String TAG_ID = "tag_id";
    }

    private interface References {
        String BLOCK_ID = "REFERENCES " + Tables.BLOCKS + "(" + Blocks.BLOCK_ID + ")";
        String TAG_ID = "REFERENCES " + Tables.TAGS + "(" + Tags.TAG_ID + ")";
        String ROOM_ID = "REFERENCES " + Tables.ROOMS + "(" + Rooms.ROOM_ID + ")";
        String SESSION_ID = "REFERENCES " + Tables.SESSIONS + "(" + Sessions.SESSION_ID + ")";
    }

    public ScheduleDatabase(Context context) {
        super(context, DATABASE_NAME, null, CUR_DATABASE_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Tables.BLOCKS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + BlocksColumns.BLOCK_ID + " TEXT NOT NULL,"
                + BlocksColumns.BLOCK_TITLE + " TEXT NOT NULL,"
                + BlocksColumns.BLOCK_START + " INTEGER NOT NULL,"
                + BlocksColumns.BLOCK_END + " INTEGER NOT NULL,"
                + BlocksColumns.BLOCK_TYPE + " TEXT,"
                + BlocksColumns.BLOCK_SUBTITLE + " TEXT,"
                + "UNIQUE (" + BlocksColumns.BLOCK_ID + ") ON CONFLICT REPLACE)");

        db.execSQL("CREATE TABLE " + Tables.TAGS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + TagsColumns.TAG_ID + " TEXT NOT NULL,"
                + TagsColumns.TAG_CATEGORY + " TEXT NOT NULL,"
                + TagsColumns.TAG_NAME + " TEXT NOT NULL,"
                + TagsColumns.TAG_ORDER_IN_CATEGORY + " INTEGER,"
                + TagsColumns.TAG_COLOR + " TEXT NOT NULL,"
                + TagsColumns.TAG_ABSTRACT + " TEXT NOT NULL,"
                + "UNIQUE (" + TagsColumns.TAG_ID + ") ON CONFLICT REPLACE)");

        db.execSQL("CREATE TABLE " + Tables.ROOMS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + RoomsColumns.ROOM_ID + " TEXT NOT NULL,"
                + RoomsColumns.ROOM_NAME + " TEXT,"
                + RoomsColumns.ROOM_FLOOR + " TEXT,"
                + "UNIQUE (" + RoomsColumns.ROOM_ID + ") ON CONFLICT REPLACE)");

        db.execSQL("CREATE TABLE " + Tables.SESSIONS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + SyncColumns.UPDATED + " INTEGER NOT NULL,"
                + SessionsColumns.SESSION_ID + " TEXT NOT NULL,"
                + Sessions.ROOM_ID + " TEXT " + References.ROOM_ID + ","
                + SessionsColumns.SESSION_START + " INTEGER NOT NULL,"
                + SessionsColumns.SESSION_END + " INTEGER NOT NULL,"
                + SessionsColumns.SESSION_LEVEL + " TEXT,"
                + SessionsColumns.SESSION_TITLE + " TEXT,"
                + SessionsColumns.SESSION_ABSTRACT + " TEXT,"
                + SessionsColumns.SESSION_HASHTAG + " TEXT,"
                + SessionsColumns.SESSION_TAGS + " TEXT,"
                + SessionsColumns.SESSION_GROUPING_ORDER + " INTEGER,"
                + SessionsColumns.SESSION_IMPORT_HASHCODE + " TEXT NOT NULL DEFAULT '',"
                + SessionsColumns.SESSION_MAIN_TAG + " TEXT,"
                + SessionsColumns.SESSION_PHOTO_URL + " TEXT,"
                + SessionsColumns.SESSION_COLOR + " INTEGER,"
                + "UNIQUE (" + SessionsColumns.SESSION_ID + ") ON CONFLICT REPLACE)");

        db.execSQL("CREATE TABLE " + Tables.SESSIONS_TAGS + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + SessionsTags.SESSION_ID + " TEXT NOT NULL " + References.SESSION_ID + ","
                + SessionsTags.TAG_ID + " TEXT NOT NULL " + References.TAG_ID + ","
                + "UNIQUE (" + SessionsTags.SESSION_ID + ","
                + SessionsTags.TAG_ID + ") ON CONFLICT REPLACE)");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "onUpgrade() from " + oldVersion + " to " + newVersion);

        Account account = null;
        if (account != null) {
            Log.i(TAG, "Cancelling any pending syncs for account");
            ContentResolver.cancelSync(account, ScheduleContract.CONTENT_AUTHORITY);
        }

        int version = oldVersion;

        boolean dataInvalidated = true;

        for (Tables.DeprecatedTables deprecatedTable : Tables.DeprecatedTables.values()) {
            db.execSQL(("DROP TABLE IF EXISTS " + deprecatedTable.tableName));
        }

        if (version != CUR_DATABASE_VERSION) {
            Log.w(TAG, "Upgrade unsuccessful -- destroying old data during upgrade");

            db.execSQL("DROP TRIGGER IF EXISTS " + Triggers.SESSIONS_TAGS_DELETE);
            db.execSQL("DROP TRIGGER IF EXISTS " + Triggers.SESSIONS_SPEAKERS_DELETE);
            db.execSQL("DROP TRIGGER IF EXISTS " + Triggers.SESSIONS_FEEDBACK_DELETE);
            db.execSQL("DROP TRIGGER IF EXISTS " + Triggers.SESSIONS_MY_SCHEDULE_DELETE);
            db.execSQL("DROP TRIGGER IF EXISTS " + Triggers.DeprecatedTriggers.SESSIONS_TRACKS_DELETE);

            db.execSQL("DROP TABLE IF EXISTS " + Tables.BLOCKS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.ROOMS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.TAGS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.SESSIONS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.SESSIONS_TAGS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.HASHTAGS);

            onCreate(db);
            version = CUR_DATABASE_VERSION;
        }

        if (dataInvalidated) {
            Log.d(TAG, "Data invalidated; resetting our data timestamp.");
            ConferenceDataHandler.resetDataTimestamp(mContext);
        }
    }

    public static void deleteDatabase(Context context) {
        context.deleteDatabase(DATABASE_NAME);
    }
}