package com.madone.virtualexpo.expoastana.provider;

import android.net.Uri;
import android.text.TextUtils;

public class ScheduleContractHelper {

    public static final String QUERY_PARAMETER_DISTINCT = "distinct";

    private static final String QUERY_PARAMETER_CALLER_IS_SYNC_ADAPTER = "callerIsSyncAdapter";

    private static final String QUERY_PARAMETER_OVERRIDE_ACCOUNT_NAME = "overrideAccountName";

    public static Uri setUriAsCalledFromSyncAdapter(Uri uri) {
        return uri.buildUpon().appendQueryParameter(QUERY_PARAMETER_CALLER_IS_SYNC_ADAPTER, "true")
                .build();
    }

    public static boolean isUriCalledFromSyncAdapter(Uri uri) {
        return uri.getBooleanQueryParameter(QUERY_PARAMETER_CALLER_IS_SYNC_ADAPTER, false);
    }

    public static boolean isQueryDistinct(Uri uri){
        return !TextUtils.isEmpty(uri.getQueryParameter(QUERY_PARAMETER_DISTINCT));
    }

    public static String getOverrideAccountName(Uri uri) {
        return uri.getQueryParameter(QUERY_PARAMETER_OVERRIDE_ACCOUNT_NAME);
    }

    public static Uri addOverrideAccountName(Uri uri, String accountName) {
        return uri.buildUpon().appendQueryParameter(
                QUERY_PARAMETER_OVERRIDE_ACCOUNT_NAME, accountName).build();
    }
}
