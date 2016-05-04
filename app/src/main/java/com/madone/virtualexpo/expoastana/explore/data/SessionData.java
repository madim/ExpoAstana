package com.madone.virtualexpo.expoastana.explore.data;

import java.util.Date;

public class SessionData {
    private String mSessionName;
    private String mDetails;
    private String mSessionId;
    private String mImageUrl;
    private String mMainTag;
    private Date mStartDate;
    private Date mEndDate;
    private String mTags;

    public SessionData() { }

    public SessionData(String sessionName, String details, String sessionId, String imageUrl,
                       String mainTag, long startTime, long endTime, String tags) {
        updateData(sessionName, details, sessionId, imageUrl, mainTag, startTime, endTime,
                tags);
    }

    public void updateData(String sessionName, String details, String sessionId, String imageUrl,
                           String mainTag, long startTime, long endTime, String tags) {
        mSessionName = sessionName;
        mDetails = details;
        mSessionId = sessionId;
        mImageUrl = imageUrl;
        mMainTag = mainTag;
        try { mStartDate = new java.util.Date(startTime); } catch (Exception ignored) { }
        try { mEndDate = new java.util.Date(endTime); } catch (Exception ignored) { }
        mTags = tags;
    }


    public String getSessionName() {
        return mSessionName;
    }

    public String getDetails() {
        return mDetails;
    }

    public String getSessionId() {
        return mSessionId;
    }

    public String getImageUrl() {
        return mImageUrl;
    }

    public String getMainTag() {
        return mMainTag;
    }

    public void setDetails(String details) { mDetails = details; }

    public Date getStartDate() { return mStartDate; }

    public Date getEndDate() { return mEndDate; }

    public String getTags() { return mTags; }
}