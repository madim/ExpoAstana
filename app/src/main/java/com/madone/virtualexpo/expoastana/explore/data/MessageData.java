package com.madone.virtualexpo.expoastana.explore.data;

import android.content.Context;
import android.text.Html;
import android.text.Spanned;
import android.view.View;

public class MessageData {
    private int mMessageStringResourceId = -1;

    private int mStartButtonStringResourceId = -1;

    private int mEndButtonStringResourceId = -1;

    private int mIconDrawableId = -1;

    private View.OnClickListener mStartButtonClickListener;

    private View.OnClickListener mEndButtonClickListener;

    public void setMessageStringResourceId(int messageStringResourceId) {
        mMessageStringResourceId = messageStringResourceId;
    }

    public Spanned getMessageString(Context context) {
        return Html.fromHtml(context.getResources().getString(mMessageStringResourceId));
    }

    public int getEndButtonStringResourceId() {
        return mEndButtonStringResourceId;
    }

    public void setEndButtonStringResourceId(int resourceId) {
        mEndButtonStringResourceId = resourceId;
    }

    public int getStartButtonStringResourceId() {
        return mStartButtonStringResourceId;
    }

    public void setStartButtonStringResourceId(int resourceId) {
        mStartButtonStringResourceId = resourceId;
    }

    public int getIconDrawableId() {
        return mIconDrawableId;
    }

    public void setIconDrawableId(int iconDrawableId) {
        mIconDrawableId = iconDrawableId;
    }

    public View.OnClickListener getEndButtonClickListener() {
        return mEndButtonClickListener;
    }

    public void setEndButtonClickListener(View.OnClickListener clickListener) {
        this.mEndButtonClickListener = clickListener;
    }

    public View.OnClickListener getStartButtonClickListener() {
        return mStartButtonClickListener;
    }

    public void setStartButtonClickListener(View.OnClickListener clickListener) {
        this.mStartButtonClickListener = clickListener;
    }
}