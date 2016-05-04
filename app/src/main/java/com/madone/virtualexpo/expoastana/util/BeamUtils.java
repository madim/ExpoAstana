package com.madone.virtualexpo.expoastana.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Parcelable;

import com.madone.virtualexpo.expoastana.provider.ScheduleContract;

public class BeamUtils {

    public static void setBeamSessionUri(Activity activity, Uri sessionUri) {
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(activity);
        if (nfcAdapter == null) {
            return;
        }

        nfcAdapter.setNdefPushMessage(new NdefMessage(
                new NdefRecord[]{
                        new NdefRecord(NdefRecord.TNF_MIME_MEDIA,
                                ScheduleContract.makeContentItemType(
                                        ScheduleContract.Sessions.CONTENT_TYPE_ID).getBytes(),
                                new byte[0],
                                sessionUri.toString().getBytes())
                }), activity);
    }

    public static void tryUpdateIntentFromBeam(Activity activity) {
        Intent originalIntent = activity.getIntent();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(originalIntent.getAction())) {
            Parcelable[] rawMsgs = originalIntent.getParcelableArrayExtra(
                    NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage msg = (NdefMessage) rawMsgs[0];
            NdefRecord mimeRecord = msg.getRecords()[0];
            if (ScheduleContract.makeContentItemType(
                    ScheduleContract.Sessions.CONTENT_TYPE_ID).equals(
                    new String(mimeRecord.getType()))) {
                Intent sessionDetailIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse(new String(mimeRecord.getPayload())));
                activity.setIntent(sessionDetailIntent);
            }
        }
    }
}