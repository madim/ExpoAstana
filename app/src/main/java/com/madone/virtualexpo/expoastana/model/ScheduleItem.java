package com.madone.virtualexpo.expoastana.model;

import com.madone.virtualexpo.expoastana.provider.ScheduleContract;

public class ScheduleItem implements Cloneable, Comparable<ScheduleItem> {

    public static final int FREE = 0;
    public static final int SESSION = 1;
    public static final int BREAK = 2;

    public static final int SESSION_TYPE_SESSION = 1;
    public static final int SESSION_TYPE_CODELAB = 2;
    public static final int SESSION_TYPE_BOXTALK = 3;
    public static final int SESSION_TYPE_MISC = 4;

    public int type = FREE;

    public int sessionType = SESSION_TYPE_MISC;

    public String mainTag;

    public long startTime = 0;
    public long endTime = 0;

    public int numOfSessions = 0;

    public String sessionId = "";

    public String title = "";
    public String subtitle = "";
    public String room;

    public boolean hasGivenFeedback;

    public String backgroundImageUrl = "";
    public int backgroundColor = 0;

    public int flags = 0;
    public static final int FLAG_HAS_LIVESTREAM = 0x01;
    public static final int FLAG_NOT_REMOVABLE = 0x02;
    public static final int FLAG_CONFLICTS_WITH_PREVIOUS = 0x04;
    public static final int FLAG_CONFLICTS_WITH_NEXT = 0x08;

    public void setTypeFromBlockType(String blockType) {
        if (!ScheduleContract.Blocks.isValidBlockType(blockType) ||
                ScheduleContract.Blocks.BLOCK_TYPE_FREE.equals(blockType)) {
            type = FREE;
        } else {
            type = BREAK;
        }
    }

    @Override
    public Object clone()  {
        try {
            return super.clone();
        } catch (CloneNotSupportedException unused) {
            return new ScheduleItem();
        }
    }

    @Override
    public int compareTo(ScheduleItem another) {
        return this.startTime < another.startTime ? -1 :
                ( this.startTime > another.startTime ? 1 : 0 );
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof ScheduleItem)) {
            return false;
        }
        ScheduleItem i = (ScheduleItem) o;
        return type == i.type &&
                sessionId.equals(i.sessionId) &&
                startTime == i.startTime &&
                endTime == i.endTime;
    }

    @Override
    public String toString() {
        return String.format("[item type=%d, startTime=%d, endTime=%d, title=%s, subtitle=%s, flags=%d]",
                type, startTime, endTime, title, subtitle, flags);
    }
}