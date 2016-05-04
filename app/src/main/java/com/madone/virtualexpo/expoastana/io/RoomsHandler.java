package com.madone.virtualexpo.expoastana.io;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.net.Uri;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.madone.virtualexpo.expoastana.io.model.Room;
import com.madone.virtualexpo.expoastana.provider.ScheduleContract;
import com.madone.virtualexpo.expoastana.provider.ScheduleContractHelper;

import java.util.ArrayList;
import java.util.HashMap;

public class RoomsHandler extends JSONHandler {
    private static final String TAG = "RoomsHandler";

    private HashMap<String, Room> mRooms = new HashMap<>();

    public RoomsHandler(Context context) {
        super(context);
    }

    @Override
    public void process(JsonElement element) {
        for (Room room : new Gson().fromJson(element, Room[].class)) {
            mRooms.put(room.id, room);
        }
    }

    @Override
    public void makeContentProviderOperations(ArrayList<ContentProviderOperation> list) {
        Uri uri = ScheduleContractHelper.setUriAsCalledFromSyncAdapter(ScheduleContract.Rooms.CONTENT_URI);

        list.add(ContentProviderOperation.newDelete(uri).build());
        for (Room room : mRooms.values()) {
            ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(uri);
            builder.withValue(ScheduleContract.Rooms.ROOM_ID, room.id);
            builder.withValue(ScheduleContract.Rooms.ROOM_NAME, room.name);
            builder.withValue(ScheduleContract.Rooms.ROOM_FLOOR, room.floor);
            list.add(builder.build());
        }
    }
}
