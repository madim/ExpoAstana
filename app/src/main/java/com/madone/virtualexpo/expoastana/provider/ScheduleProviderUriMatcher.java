package com.madone.virtualexpo.expoastana.provider;

import android.content.UriMatcher;
import android.net.Uri;
import android.util.Log;
import android.util.SparseArray;

import java.util.HashMap;

/**
 * Provides methods to match a {@link android.net.Uri} to a {@link ScheduleUriEnum}.
 * <p />
 * All methods are thread safe, except {@link #buildUriMatcher()} and {@link #buildEnumsMap()},
 * which is why they are called only from the constructor.
 */
public class ScheduleProviderUriMatcher {

    /**
     * All methods on a {@link UriMatcher} are thread safe, except {@code addURI}.
     */
    private UriMatcher mUriMatcher;

    private SparseArray<ScheduleUriEnum> mEnumsMap = new SparseArray<>();

    /**
     * This constructor needs to be called from a thread-safe method as it isn't thread-safe itself.
     */
    public ScheduleProviderUriMatcher(){
        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        buildUriMatcher();
    }

    private void buildUriMatcher() {
        final String authority = ScheduleContract.CONTENT_AUTHORITY;

        ScheduleUriEnum[] uris = ScheduleUriEnum.values();
        for (int i = 0; i < uris.length; i++) {
            mUriMatcher.addURI(authority, uris[i].path, uris[i].code);
        }

        buildEnumsMap();
    }

    private void buildEnumsMap() {
        ScheduleUriEnum[] uris = ScheduleUriEnum.values();
        for (int i = 0; i < uris.length; i++) {
            mEnumsMap.put(uris[i].code, uris[i]);
        }
    }

    /**
     * Matches a {@code uri} to a {@link ScheduleUriEnum}.
     *
     * @return the {@link ScheduleUriEnum}, or throws new UnsupportedOperationException if no match.
     */
    public ScheduleUriEnum matchUri(Uri uri){
        final int code = mUriMatcher.match(uri);
        try {
            return matchCode(code);
        } catch (UnsupportedOperationException e){
            throw new UnsupportedOperationException("Unknown uri " + uri);
        }
    }

    /**
     * Matches a {@code code} to a {@link ScheduleUriEnum}.
     *
     * @return the {@link ScheduleUriEnum}, or throws new UnsupportedOperationException if no match.
     */
    public ScheduleUriEnum matchCode(int code){
        ScheduleUriEnum scheduleUriEnum = mEnumsMap.get(code);
        if (scheduleUriEnum != null){
            return scheduleUriEnum;
        } else {
            throw new UnsupportedOperationException("Unknown uri with code " + code);
        }
    }
}