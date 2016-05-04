package com.madone.virtualexpo.expoastana.ui.widget;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

public interface CollectionViewCallbacks {

    View newCollectionHeaderView(Context context, int groupId, ViewGroup parent);

    void bindCollectionHeaderView(Context context, View view, int groupId, String headerLabel,
                                  Object headerTag);

    View newCollectionItemView(Context context, int groupId, ViewGroup parent);

    void bindCollectionItemView(Context context, View view, int groupId, int indexInGroup,
                                int dataIndex, Object tag);

    public static interface GroupCollectionViewCallbacks extends CollectionViewCallbacks {

        ViewGroup newCollectionGroupView(Context context, int groupId, CollectionView.InventoryGroup group, ViewGroup parent);
    }
}