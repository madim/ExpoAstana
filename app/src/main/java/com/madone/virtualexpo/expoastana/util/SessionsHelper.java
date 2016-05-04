package com.madone.virtualexpo.expoastana.util;

import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.ShareCompat;
import com.madone.virtualexpo.expoastana.BuildConfigs;
import com.madone.virtualexpo.expoastana.R;
import com.madone.virtualexpo.expoastana.provider.ScheduleContract;

public class SessionsHelper {

    private static final String TAG = "SessionHelper";

    private final Activity mActivity;

    public SessionsHelper(Activity activity) {
        mActivity = activity;
    }

    public Intent createShareIntent(int messageTemplateResId, String title, String hashtags,
                                    String url) {
        ShareCompat.IntentBuilder builder = ShareCompat.IntentBuilder.from(mActivity)
                .setType("text/plain")
                .setText(mActivity.getString(messageTemplateResId,
                        title, BuildConfigs.CONFERENCE_HASHTAG, " " + url));
        return builder.getIntent();
    }
}