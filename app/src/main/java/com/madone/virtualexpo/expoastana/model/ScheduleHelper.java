package com.madone.virtualexpo.expoastana.model;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.madone.virtualexpo.expoastana.BuildConfigs;
import com.madone.virtualexpo.expoastana.R;
import com.madone.virtualexpo.expoastana.provider.ScheduleContract;
import com.madone.virtualexpo.expoastana.provider.ScheduleContract.Blocks;
import com.madone.virtualexpo.expoastana.provider.ScheduleContract.Sessions;
import com.madone.virtualexpo.expoastana.myschedule.MyScheduleAdapter;
import com.madone.virtualexpo.expoastana.util.UIUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

public class ScheduleHelper {

    private static final String TAG = "ScheduleHelper";

    private Context mContext;

    public ScheduleHelper(Context context) {
        this.mContext = context;
    }

    public ArrayList<ScheduleItem> getScheduleData(long start, long end) {
        ArrayList<ScheduleItem> mutableItems = new ArrayList<ScheduleItem>();
        ArrayList<ScheduleItem> immutableItems = new ArrayList<ScheduleItem>();
        addBlocks(start, end, mutableItems, immutableItems);
        addSessions(start, end, mutableItems, immutableItems);

        ArrayList<ScheduleItem> result = ScheduleItemHelper.processItems(mutableItems, immutableItems);
        if (BuildConfigs.DEBUG || Log.isLoggable(TAG, Log.DEBUG)) {
            ScheduleItem previous = null;
            for (ScheduleItem item: result) {
                if ((item.flags & ScheduleItem.FLAG_CONFLICTS_WITH_PREVIOUS) != 0) {
                    Log.d(TAG, "Schedule Item conflicts with previous. item="+item+" previous="+previous);
                }
                previous = item;
            }
        }

        return result;
    }

    public void getScheduleDataAsync(final MyScheduleAdapter adapter,
                                     long start, long end) {
        AsyncTask<Long, Void, ArrayList<ScheduleItem>> task
                = new AsyncTask<Long, Void, ArrayList<ScheduleItem>>() {
            @Override
            protected ArrayList<ScheduleItem> doInBackground(Long... params) {
                Long start = params[0];
                Long end = params[1];
                return getScheduleData(start, end);
            }

            @Override
            protected void onPostExecute(ArrayList<ScheduleItem> scheduleItems) {
                adapter.updateItems(scheduleItems);
            }
        };
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, start, end);
    }

    protected void addSessions(long start, long end,
                               ArrayList<ScheduleItem> mutableItems, ArrayList<ScheduleItem> immutableItems) {

        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(
                    Sessions.CONTENT_MY_SCHEDULE_URI,
                    SessionsQuery.PROJECTION,
                    Sessions.STARTING_AT_TIME_INTERVAL_SELECTION,
                    new String[]{String.valueOf(start), String.valueOf(end)},
                    Sessions.SESSION_START);

            if (cursor.moveToFirst()) {
                do {
                    ScheduleItem item = new ScheduleItem();
                    item.type = ScheduleItem.SESSION;
                    item.sessionId = cursor.getString(SessionsQuery.SESSION_ID);
                    item.title = cursor.getString(SessionsQuery.SESSION_TITLE);
                    item.startTime = cursor.getLong(SessionsQuery.SESSION_START);
                    item.endTime = cursor.getLong(SessionsQuery.SESSION_END);

                    item.subtitle = UIUtils.formatSessionSubtitle(
                            cursor.getString(SessionsQuery.ROOM_ROOM_NAME), mContext);
                    item.room = cursor.getString(SessionsQuery.ROOM_ROOM_NAME);
                    item.backgroundImageUrl = cursor.getString(SessionsQuery.SESSION_PHOTO_URL);
                    item.backgroundColor = cursor.getInt(SessionsQuery.SESSION_COLOR);
                    item.sessionType = detectSessionType(cursor.getString(SessionsQuery.SESSION_TAGS));
                    item.mainTag = cursor.getString(SessionsQuery.SESSION_MAIN_TAG);
                    immutableItems.add(item);
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    protected void addBlocks(long start, long end,
                             ArrayList<ScheduleItem> mutableItems, ArrayList<ScheduleItem> immutableItems) {
        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(
                    Blocks.CONTENT_URI,
                    BlocksQuery.PROJECTION,

                    Blocks.BLOCK_START + " >= ? and " + Blocks.BLOCK_START + " <= ?",
                    new String[]{String.valueOf(start), String.valueOf(end)},

                    Blocks.BLOCK_START);

            if (cursor.moveToFirst()) {
                do {
                    ScheduleItem item = new ScheduleItem();
                    item.setTypeFromBlockType(cursor.getString(BlocksQuery.BLOCK_TYPE));
                    item.title = cursor.getString(BlocksQuery.BLOCK_TITLE);
                    item.room = item.subtitle = cursor.getString(BlocksQuery.BLOCK_SUBTITLE);
                    item.startTime = cursor.getLong(BlocksQuery.BLOCK_START);
                    item.endTime = cursor.getLong(BlocksQuery.BLOCK_END);

                    if (item.type == ScheduleItem.BREAK) {
                        continue;
                    }
                    if (item.type == ScheduleItem.FREE) {
                        mutableItems.add(item);
                    } else {
                        immutableItems.add(item);
                        item.flags |= ScheduleItem.FLAG_NOT_REMOVABLE;
                    }
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static int detectSessionType(String tagsText) {
        if (TextUtils.isEmpty(tagsText)) {
            return ScheduleItem.SESSION_TYPE_MISC;
        }
        String tags = tagsText.toUpperCase(Locale.US);
        if (tags.contains("TYPE_SESSIONS") || tags.contains("KEYNOTE")) {
            return ScheduleItem.SESSION_TYPE_SESSION;
        } else if (tags.contains("TYPE_CODELAB")) {
            return ScheduleItem.SESSION_TYPE_CODELAB;
        } else if (tags.contains("TYPE_SANDBOXTALKS")) {
            return ScheduleItem.SESSION_TYPE_BOXTALK;
        } else if (tags.contains("TYPE_APPREVIEWS") || tags.contains("TYPE_OFFICEHOURS") ||
                tags.contains("TYPE_WORKSHOPS")) {
            return ScheduleItem.SESSION_TYPE_MISC;
        }
        return ScheduleItem.SESSION_TYPE_MISC; // default
    }

    private interface SessionsQuery {
        String[] PROJECTION = {
                Sessions.SESSION_ID,
                Sessions.SESSION_TITLE,
                Sessions.SESSION_START,
                Sessions.SESSION_END,
                ScheduleContract.Rooms.ROOM_NAME,
                Sessions.SESSION_PHOTO_URL,
                Sessions.SESSION_COLOR,
                Sessions.SESSION_TAGS,
                Sessions.SESSION_MAIN_TAG,
        };

        int SESSION_ID = 0;
        int SESSION_TITLE = 1;
        int SESSION_START = 2;
        int SESSION_END = 3;
        int ROOM_ROOM_NAME = 4;
        int SESSION_PHOTO_URL = 5;
        int SESSION_COLOR = 6;
        int SESSION_TAGS = 7;
        int SESSION_MAIN_TAG = 8;
    }

    private interface BlocksQuery {
        String[] PROJECTION = {
                Blocks.BLOCK_TITLE,
                Blocks.BLOCK_TYPE,
                Blocks.BLOCK_START,
                Blocks.BLOCK_END,
                Blocks.BLOCK_SUBTITLE
        };

        int BLOCK_TITLE = 0;
        int BLOCK_TYPE= 1;
        int BLOCK_START = 2;
        int BLOCK_END = 3;
        int BLOCK_SUBTITLE = 4;
    }


    private interface SessionsCounterQuery {
        String[] PROJECTION = {
                Sessions.SESSION_START,
                Sessions.SESSION_END,
        };

        int SESSION_INTERVAL_START = 0;
        int SESSION_INTERVAL_END = 1;
    }

}