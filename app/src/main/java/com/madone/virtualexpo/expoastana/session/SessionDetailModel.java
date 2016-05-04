package com.madone.virtualexpo.expoastana.session;

import com.google.common.annotations.VisibleForTesting;
import com.madone.virtualexpo.expoastana.Config;
import com.madone.virtualexpo.expoastana.R;
import com.madone.virtualexpo.expoastana.framework.Model;
import com.madone.virtualexpo.expoastana.framework.QueryEnum;
import com.madone.virtualexpo.expoastana.framework.UserActionEnum;
import com.madone.virtualexpo.expoastana.model.TagMetadata;
import com.madone.virtualexpo.expoastana.provider.ScheduleContract;
import com.madone.virtualexpo.expoastana.util.SessionsHelper;
import com.madone.virtualexpo.expoastana.util.UIUtils;

import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SessionDetailModel implements Model {

    protected final static String TAG = "SessionDetailModel";

    private final Context mContext;

    private String mSessionId;

    private Uri mSessionUri;

    private boolean mSessionLoaded = false;

    private String mTitle;

    private String mSubtitle;

    private int mSessionColor;

    private boolean mIsKeynote;

    private long mSessionStart;

    private long mSessionEnd;

    private String mSessionAbstract;

    private String mHashTag;

    private String mRoomId;

    private String mRoomName;

    private String mTagsString;

    private String mPhotoUrl;

    private TagMetadata mTagMetadata;

    private List<Pair<Integer, Intent>> mLinks = new ArrayList<Pair<Integer, Intent>>();

    private StringBuilder mBuffer = new StringBuilder();

    public SessionDetailModel(Uri sessionUri, Context context) {
        mContext = context;
        mSessionUri = sessionUri;
    }

    public String getSessionId() {
        return mSessionId;
    }

    public String getSessionTitle() {
        return mTitle;
    }

    public String getSessionSubtitle() {
        return mSubtitle;
    }

    public int getSessionColor() {
        return mSessionColor;
    }

    public String getSessionAbstract() {
        return mSessionAbstract;
    }

    public boolean isSessionOngoing() {
        long currentTimeMillis = UIUtils.getCurrentTime(mContext);
        return currentTimeMillis > mSessionStart && currentTimeMillis <= mSessionEnd;
    }

    public boolean hasSessionStarted() {
        long currentTimeMillis = UIUtils.getCurrentTime(mContext);
        return currentTimeMillis > mSessionStart;
    }

    public boolean hasSessionEnded() {
        long currentTimeMillis = UIUtils.getCurrentTime(mContext);
        return currentTimeMillis > mSessionEnd;
    }

    public long minutesSinceSessionStarted() {
        if (!hasSessionStarted()) {
            return 0l;
        } else {
            long currentTimeMillis = UIUtils.getCurrentTime(mContext);
            return (currentTimeMillis - mSessionStart) / 60000;
        }
    }

    public long minutesUntilSessionStarts() {
        if (hasSessionStarted()) {
            return 0l;
        } else {
            long currentTimeMillis = UIUtils.getCurrentTime(mContext);
            return (mSessionStart - currentTimeMillis) / 60000 + 1;
        }
    }

    public boolean isKeynote() {
        return mIsKeynote;
    }

    public boolean hasPhotoUrl() {
        return !TextUtils.isEmpty(mPhotoUrl);
    }

    public String getPhotoUrl() {
        return mPhotoUrl;
    }

    public String getHashTag() {
        return mHashTag;
    }

    public TagMetadata getTagMetadata() {
        return mTagMetadata;
    }

    public String getTagsString() {
        return mTagsString;
    }

    public List<Pair<Integer, Intent>> getLinks() {
        return mLinks;
    }

    public boolean hasSummaryContent() {
        return !TextUtils.isEmpty(mSessionAbstract);
    }

    @Override
    public QueryEnum[] getQueries() {
        return SessionDetailQueryEnum.values();
    }

    @Override
    public boolean readDataFromCursor(Cursor cursor, QueryEnum query) {
        boolean success = false;

        if (cursor != null && cursor.moveToFirst()) {
            if (SessionDetailQueryEnum.SESSIONS == query) {
                readDataFromSessionCursor(cursor);
                mSessionLoaded = true;
                success = true;
            } else if (SessionDetailQueryEnum.TAG_METADATA == query) {
                readDataFromTagMetadataCursor(cursor);
                success = true;
            }
        }

        return success;
    }

    private void readDataFromSessionCursor(Cursor cursor) {
        mTitle = cursor.getString(cursor.getColumnIndex(
                ScheduleContract.Sessions.SESSION_TITLE));

        mTagsString = cursor
                .getString(cursor.getColumnIndex(ScheduleContract.Sessions.SESSION_TAGS));
        mIsKeynote = mTagsString != null && mTagsString.contains(Config.Tags.SPECIAL_KEYNOTE);

        mSessionColor = cursor.getInt(
                cursor.getColumnIndex(ScheduleContract.Sessions.SESSION_COLOR));
        if (mSessionColor == 0) {
            mSessionColor = mContext.getResources().getColor(R.color.default_session_color);
        } else {
            mSessionColor = UIUtils.setColorOpaque(mSessionColor);
        }

        mSessionStart = cursor
                .getLong(cursor.getColumnIndex(ScheduleContract.Sessions.SESSION_START));
        mSessionEnd = cursor.getLong(cursor.getColumnIndex(ScheduleContract.Sessions.SESSION_END));

        mRoomName = cursor.getString(cursor.getColumnIndex(ScheduleContract.Sessions.ROOM_NAME));
        mRoomId = cursor.getString(cursor.getColumnIndex(ScheduleContract.Sessions.ROOM_ID));

        mHashTag = cursor
                .getString(cursor.getColumnIndex(ScheduleContract.Sessions.SESSION_HASHTAG));

        mPhotoUrl =
                cursor.getString(
                        cursor.getColumnIndex(ScheduleContract.Sessions.SESSION_PHOTO_URL));

        mSessionAbstract = cursor
                .getString(cursor.getColumnIndex(ScheduleContract.Sessions.SESSION_ABSTRACT));

        formatSubtitle();

    }

    @VisibleForTesting
    public void formatSubtitle() {
        mSubtitle = UIUtils.formatSessionSubtitle(
                mSessionStart, mSessionEnd, mRoomName, mBuffer, mContext);
    }

    private void readDataFromTagMetadataCursor(Cursor cursor) {
        mTagMetadata = new TagMetadata(cursor);
    }

    @Override
    public Loader<Cursor> createCursorLoader(int loaderId, Uri uri, Bundle args) {
        CursorLoader loader = null;

        if (loaderId == SessionDetailQueryEnum.SESSIONS.getId()) {
            mSessionUri = uri;
            mSessionId = getSessionId(uri);
            loader = getCursorLoaderInstance(mContext, uri,
                    SessionDetailQueryEnum.SESSIONS.getProjection(), null, null, null);
        } else if (loaderId == SessionDetailQueryEnum.TAG_METADATA.getId()) {
            loader = getTagMetadataLoader();
        }

        return loader;
    }

    @Override
    public boolean requestModelUpdate(UserActionEnum action, @Nullable Bundle args) {
        return false;
    }

    @VisibleForTesting
    public CursorLoader getCursorLoaderInstance(Context context, Uri uri, String[] projection,
                                                String selection, String[] selectionArgs, String sortOrder) {
        return new CursorLoader(context, uri, projection, selection, selectionArgs, sortOrder);
    }

    @VisibleForTesting
    public CursorLoader getTagMetadataLoader() {
        return TagMetadata.createCursorLoader(mContext);
    }


    @VisibleForTesting
    public String getSessionId(Uri uri) {
        return ScheduleContract.Sessions.getSessionId(uri);
    }

    public enum SessionDetailQueryEnum implements QueryEnum {
        SESSIONS(0, new String[]{ScheduleContract.Sessions.SESSION_START,
                ScheduleContract.Sessions.SESSION_END,
                ScheduleContract.Sessions.SESSION_LEVEL,
                ScheduleContract.Sessions.SESSION_TITLE,
                ScheduleContract.Sessions.SESSION_ABSTRACT,
                ScheduleContract.Sessions.SESSION_HASHTAG,
                ScheduleContract.Sessions.ROOM_ID,
                ScheduleContract.Rooms.ROOM_NAME,
                ScheduleContract.Sessions.SESSION_COLOR,
                ScheduleContract.Sessions.SESSION_PHOTO_URL,
                ScheduleContract.Sessions.SESSION_TAGS}),
        TAG_METADATA(3, null);

        private int id;

        private String[] projection;

        SessionDetailQueryEnum(int id, String[] projection) {
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

    public enum SessionDetailUserActionEnum implements UserActionEnum {
        STAR(1),
        UNSTAR(2),
        SHOW_MAP(3),
        SHOW_SHARE(4);

        private int id;

        SessionDetailUserActionEnum(int id) {
            this.id = id;
        }

        @Override
        public int getId() {
            return id;
        }

    }
}