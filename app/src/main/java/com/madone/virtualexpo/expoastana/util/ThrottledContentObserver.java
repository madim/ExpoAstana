package com.madone.virtualexpo.expoastana.util;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;

public class ThrottledContentObserver extends ContentObserver {

    Handler mMyHandler;
    Runnable mScheduleRun = null;
    private static final int THROTTLE_DELAY = 1000;
    Callbacks mCallback = null;

    public interface Callbacks {
        public void onThrottledContentObserverFired();
    }

    public ThrottledContentObserver(Callbacks callback) {
        super(null);
        mMyHandler = new Handler();
        mCallback = callback;
    }

    @Override
    public void onChange(boolean selfChange) {
        if (mScheduleRun != null) {
            mMyHandler.removeCallbacks(mScheduleRun);
        } else {
            mScheduleRun = new Runnable() {
                @Override
                public void run() {
                    if (mCallback != null) {
                        mCallback.onThrottledContentObserverFired();
                    }
                }
            };
        }
        mMyHandler.postDelayed(mScheduleRun, THROTTLE_DELAY);
    }

    public void cancelPendingCallback() {
        if (mScheduleRun != null) {
            mMyHandler.removeCallbacks(mScheduleRun);
        }
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        onChange(selfChange);
    }
}