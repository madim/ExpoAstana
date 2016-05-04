package com.madone.virtualexpo.expoastana.provider;

public enum ScheduleUriEnum {
    BLOCKS(100, "blocks", ScheduleContract.Blocks.CONTENT_TYPE_ID, false, ScheduleDatabase.Tables.BLOCKS),
    BLOCKS_BETWEEN(101, "blocks/between/*/*", ScheduleContract.Blocks.CONTENT_TYPE_ID, false, null),
    BLOCKS_ID(102, "blocks/*", ScheduleContract.Blocks.CONTENT_TYPE_ID, true, null),
    TAGS(200, "tags", ScheduleContract.Tags.CONTENT_TYPE_ID, false, ScheduleDatabase.Tables.TAGS),
    TAGS_ID(201, "tags/*", ScheduleContract.Tags.CONTENT_TYPE_ID, false, null),
    ROOMS(300, "rooms", ScheduleContract.Rooms.CONTENT_TYPE_ID, false, ScheduleDatabase.Tables.ROOMS),
    ROOMS_ID(301, "rooms/*", ScheduleContract.Rooms.CONTENT_TYPE_ID, true, null),
    ROOMS_ID_SESSIONS(302, "rooms/*/sessions", ScheduleContract.Sessions.CONTENT_TYPE_ID, false, null),
    SESSIONS(400, "sessions", ScheduleContract.Sessions.CONTENT_TYPE_ID, false, ScheduleDatabase.Tables.SESSIONS),
    SESSIONS_SEARCH(403, "sessions/search/*", ScheduleContract.Sessions.CONTENT_TYPE_ID, false, null),
    SESSIONS_AT(404, "sessions/at/*", ScheduleContract.Sessions.CONTENT_TYPE_ID, false, null),
    SESSIONS_AFTER(411, "sessions/after/*", ScheduleContract.Sessions.CONTENT_TYPE_ID, false, null),
    SESSIONS_ROOM_AFTER(408, "sessions/room/*/after/*", ScheduleContract.Sessions.CONTENT_TYPE_ID, false, null),
    SESSIONS_UNSCHEDULED(409, "sessions/unscheduled/*", ScheduleContract.Sessions.CONTENT_TYPE_ID, false, null),
    SESSIONS_ID(405, "sessions/*", ScheduleContract.Sessions.CONTENT_TYPE_ID, true, null),
    SESSIONS_ID_TAGS(407, "sessions/*/tags", ScheduleContract.Tags.CONTENT_TYPE_ID, false, ScheduleDatabase.Tables.SESSIONS_TAGS);

    public int code;

    public String path;

    public String contentType;

    public String table;

    ScheduleUriEnum(int code, String path, String contentTypeId, boolean item, String table) {
        this.code = code;
        this.path = path;
        this.contentType = item ? ScheduleContract.makeContentItemType(contentTypeId)
                : ScheduleContract.makeContentType(contentTypeId);
        this.table = table;
    }

}