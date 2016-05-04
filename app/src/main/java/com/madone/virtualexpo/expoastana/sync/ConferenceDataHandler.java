package com.madone.virtualexpo.expoastana.sync;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.madone.virtualexpo.expoastana.io.BlocksHandler;
import com.madone.virtualexpo.expoastana.io.JSONHandler;
import com.madone.virtualexpo.expoastana.io.RoomsHandler;
// import com.madone.virtualexpo.expoastana.io.SessionsHandler;
import com.madone.virtualexpo.expoastana.io.SessionsHandler;
import com.madone.virtualexpo.expoastana.io.TagsHandler;
import com.madone.virtualexpo.expoastana.provider.ScheduleContract;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;

public class ConferenceDataHandler {
    private static final String TAG = "ConferenceDataHandler";

    private static final String SP_KEY_DATA_TIMESTAMP = "data_timestamp";

    private static final String DEFAULT_TIMESTAMP = "Sat, 1 Jan 2000 00:00:00 GMT";

    private static final String DATA_KEY_ROOMS = "rooms";
    private static final String DATA_KEY_BLOCKS = "blocks";
    private static final String DATA_KEY_TAGS = "tags";
    private static final String DATA_KEY_SESSIONS = "sessions";

    private static final String[] DATA_KEYS_IN_ORDER = {
            DATA_KEY_ROOMS,
            DATA_KEY_BLOCKS,
            DATA_KEY_TAGS,
            DATA_KEY_SESSIONS,
    };

    Context mContext;

    RoomsHandler mRoomsHandler = null;
    BlocksHandler mBlocksHandler = null;
    TagsHandler mTagsHandler = null;
    SessionsHandler mSessionsHandler = null;

    HashMap<String, JSONHandler> mHandlerForKey = new HashMap<String, JSONHandler>();

    private int mContentProviderOperationsDone = 0;

    public ConferenceDataHandler(Context context) {
        mContext = context;
    }

    public void applyConferenceData(String[] dataBodies, String dataTimestamp,
                                    boolean downloadsAllowed) throws IOException {
        Log.d(TAG, "Applying data from " + dataBodies.length + " files, timestamp " + dataTimestamp);

        mHandlerForKey.put(DATA_KEY_ROOMS, mRoomsHandler = new RoomsHandler(mContext));
        mHandlerForKey.put(DATA_KEY_BLOCKS, mBlocksHandler = new BlocksHandler(mContext));
        mHandlerForKey.put(DATA_KEY_TAGS, mTagsHandler = new TagsHandler(mContext));
        mHandlerForKey.put(DATA_KEY_SESSIONS, mSessionsHandler = new SessionsHandler(mContext));
        // mHandlerForKey.put(DATA_KEY_MAP, mMapPropertyHandler = new MapPropertyHandler(mContext));

        Log.d(TAG, "Processing " + dataBodies.length + " JSON objects.");
        for (int i = 0; i < dataBodies.length; i++) {
            Log.d(TAG, "Processing json object #" + (i + 1) + " of " + dataBodies.length);
            processDataBody(dataBodies[i]);
        }


        mSessionsHandler.setTagMap(mTagsHandler.getTagMap());

        ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();
        for (String key : DATA_KEYS_IN_ORDER) {
            Log.d(TAG, "Building content provider operations for: " + key);
            mHandlerForKey.get(key).makeContentProviderOperations(batch);
            Log.d(TAG, "Content provider operations so far: " + batch.size());
        }
        Log.d(TAG, "Total content provider operations: " + batch.size());

        Log.d(TAG, "Applying " + batch.size() + " content provider operations.");
        try {
            int operations = batch.size();
            if (operations > 0) {
                mContext.getContentResolver().applyBatch(ScheduleContract.CONTENT_AUTHORITY, batch);
            }
            Log.d(TAG, "Successfully applied " + operations + " content provider operations.");
            mContentProviderOperationsDone += operations;
        } catch (RemoteException ex) {
            Log.e(TAG, "RemoteException while applying content provider operations.");
            throw new RuntimeException("Error executing content provider batch operation", ex);
        } catch (OperationApplicationException ex) {
            Log.e(TAG, "OperationApplicationException while applying content provider operations.");
            throw new RuntimeException("Error executing content provider batch operation", ex);
        }

        Log.d(TAG, "Notifying changes on all top-level paths on Content Resolver.");
        ContentResolver resolver = mContext.getContentResolver();
        for (String path : ScheduleContract.TOP_LEVEL_PATHS) {
            Uri uri = ScheduleContract.BASE_CONTENT_URI.buildUpon().appendPath(path).build();
            resolver.notifyChange(uri, null);
        }


        setDataTimestamp(dataTimestamp);
        Log.d(TAG, "Done applying conference data.");
    }

    public int getContentProviderOperationsDone() {
        return mContentProviderOperationsDone;
    }

    public String getDataTimestamp() {
        return PreferenceManager.getDefaultSharedPreferences(mContext).getString(
                SP_KEY_DATA_TIMESTAMP, DEFAULT_TIMESTAMP);
    }

    public void setDataTimestamp(String timestamp) {
        Log.d(TAG, "Setting data timestamp to: " + timestamp);
        PreferenceManager.getDefaultSharedPreferences(mContext).edit().putString(
                SP_KEY_DATA_TIMESTAMP, timestamp).commit();
    }

    public static void resetDataTimestamp(final Context context) {
        Log.d(TAG, "Resetting data timestamp to default (to invalidate our synced data)");
        PreferenceManager.getDefaultSharedPreferences(context).edit().remove(
                SP_KEY_DATA_TIMESTAMP).commit();
    }

    private void processDataBody(String dataBody) throws IOException {
        JsonReader reader = new JsonReader(new StringReader(dataBody));
        JsonParser parser = new JsonParser();
        try {
            reader.setLenient(true);

            reader.beginObject();

            while (reader.hasNext()) {
                String key = reader.nextName();
                if (mHandlerForKey.containsKey(key)) {
                    mHandlerForKey.get(key).process(parser.parse(reader));
                } else {
                    Log.w(TAG, "Skipping unknown key in conference data json: " + key);
                    reader.skipValue();
                }
            }
            reader.endObject();
        } finally {
            reader.close();
        }
    }
}
