package com.madone.virtualexpo.expoastana.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.madone.virtualexpo.expoastana.BuildConfigs;
import com.madone.virtualexpo.expoastana.R;
import com.madone.virtualexpo.expoastana.io.JSONHandler;
import com.madone.virtualexpo.expoastana.provider.ScheduleContract;
import com.madone.virtualexpo.expoastana.settings.SettingsUtils;
import com.madone.virtualexpo.expoastana.sync.ConferenceDataHandler;

import java.io.IOException;

public class DataBootstrapService extends IntentService {

    private static final String TAG = "DataBootstrapService";

    public static void startDataBootstrapIfNecessary(Context context) {
        if (!SettingsUtils.isDataBootstrapDone(context)) {
            Log.w(TAG, "One-time data bootstrap not done yet. Doing now.");
            context.startService(new Intent(context, DataBootstrapService.class));
        }
    }

    public DataBootstrapService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Context appContext = getApplicationContext();

        if (SettingsUtils.isDataBootstrapDone(appContext)) {
            Log.d(TAG, "Data bootstrap already done.");
            return;
        }
        try {
            Log.d(TAG, "Starting data bootstrap process.");
            String bootstrapJson = JSONHandler
                    .parseResource(appContext, R.raw.bootstrap_data);

            ConferenceDataHandler dataHandler = new ConferenceDataHandler(appContext);
            dataHandler.applyConferenceData(new String[]{bootstrapJson},
                    BuildConfigs.BOOTSTRAP_DATA_TIMESTAMP, false);

            Log.i(TAG, "End of bootstrap -- successful. Marking bootstrap as done.");

            SettingsUtils.markSyncSucceededNow(appContext);
            SettingsUtils.markDataBootstrapDone(appContext);

            getContentResolver().notifyChange(Uri.parse(ScheduleContract.CONTENT_AUTHORITY),
                    null, false);

        } catch (IOException ex) {
            Log.e(TAG, "*** ERROR DURING BOOTSTRAP! Problem in bootstrap data?", ex);
            Log.e(TAG, "Applying fallback -- marking boostrap as done; sync might fix problem.");

            SettingsUtils.markDataBootstrapDone(appContext);
        } finally {
        }
    }
}