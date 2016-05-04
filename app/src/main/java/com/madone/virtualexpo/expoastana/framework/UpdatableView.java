package com.madone.virtualexpo.expoastana.framework;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;

public interface UpdatableView<M> {

    public void displayData(M model, QueryEnum query);

    public void displayErrorMessage(QueryEnum query);

    public Uri getDataUri(QueryEnum query);

    public Context getContext();

    public void addListener(UserActionListener listener);

    interface UserActionListener {
        public void onUserAction(UserActionEnum action, @Nullable Bundle args);
    }
}