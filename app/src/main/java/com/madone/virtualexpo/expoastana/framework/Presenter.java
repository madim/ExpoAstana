package com.madone.virtualexpo.expoastana.framework;

import android.content.Context;

public interface Presenter {

    void setModel(Model model);

    void setUpdatableView(UpdatableView view);

    void setInitialQueriesToLoad(QueryEnum[] queries);

    void setValidUserActions(UserActionEnum[] actions);

    public void cleanUp();

    public Context getContext();
}
