package com.madone.virtualexpo.expoastana.framework;

import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;

public interface Model {

    public QueryEnum[] getQueries();

    public boolean readDataFromCursor(Cursor cursor, QueryEnum query);

    public Loader<Cursor> createCursorLoader(int loaderId, Uri uri, Bundle args);

    public boolean requestModelUpdate(UserActionEnum action, @Nullable Bundle args);
}