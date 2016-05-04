package com.madone.virtualexpo.expoastana.explore;

import android.os.Parcel;
import android.os.Parcelable;

import com.madone.virtualexpo.expoastana.Config;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TagFilterHolder implements Parcelable {

    public static final int CATEGORY_THEME = 0;
    public static final int CATEGORY_TYPE  = 1;
    public static final int CATEGORY_TOPIC = 2;

    private final Set<String> mSelectedFilters;
    private final int[] mCategories;
    private boolean mShowLiveStreamedSessions;

    TagFilterHolder() {
        mSelectedFilters = new HashSet<>();
        mCategories = new int[3];
        mCategories[CATEGORY_THEME] = 0;
        mCategories[CATEGORY_TYPE] = 0;
        mCategories[CATEGORY_TOPIC] = 0;
    }

    public boolean contains(String tagId) {
        return mSelectedFilters.contains(tagId);
    }

    public boolean add(String tagId, String category) {
        boolean added = mSelectedFilters.add(tagId);
        if (added) {
            mCategories[categoryId(category)]++;
        }
        return added;
    }

    public boolean remove(String tagId, String category) {
        boolean removed = mSelectedFilters.remove(tagId);
        if (removed) {
            mCategories[categoryId(category)]--;
        }
        return removed;
    }

    public String[] toStringArray() {
        return mSelectedFilters.toArray(new String[mSelectedFilters.size()]);
    }

    public Set<String> getSelectedFilters() {
        return Collections.unmodifiableSet(mSelectedFilters);
    }

    public int getCategoryCount() {
        return Math.max(1,
                (mCategories[CATEGORY_THEME] > 0 ? 1 : 0) +
                        (mCategories[CATEGORY_TYPE] > 0 ? 1 : 0) +
                        (mCategories[CATEGORY_TOPIC] > 0 ? 1 : 0));
    }

    public boolean isEmpty() {
        return mSelectedFilters.isEmpty();
    }

    public int size() {
        return mSelectedFilters.size();
    }

    public void setShowLiveStreamedSessions(boolean show) {
        this.mShowLiveStreamedSessions = show;
    }

    public boolean isShowLiveStreamedSessions() {
        return mShowLiveStreamedSessions;
    }

    public int getCountByCategory(String category) {
        return mCategories[categoryId(category)];
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(mSelectedFilters.toArray(new String[mSelectedFilters.size()]));
        dest.writeIntArray(mCategories);
        dest.writeInt(mShowLiveStreamedSessions ? 1 : 0);
    }

    private static int categoryId(String category) {
        switch (category) {
            case Config.Tags.CATEGORY_THEME:
                return TagFilterHolder.CATEGORY_THEME;
            case Config.Tags.CATEGORY_TYPE:
                return TagFilterHolder.CATEGORY_TYPE;
            case Config.Tags.CATEGORY_TOPIC:
                return TagFilterHolder.CATEGORY_TOPIC;
            default:
                throw new IllegalArgumentException("Invalid category " + category);
        }
    }

    public static final Creator CREATOR = new Creator() {

        public TagFilterHolder createFromParcel(Parcel in) {
            TagFilterHolder holder = new TagFilterHolder();

            String[] filters = in.createStringArray();
            in.readStringArray(filters);
            Collections.addAll(holder.mSelectedFilters, filters);

            int[] categories = in.createIntArray();
            in.readIntArray(categories);
            System.arraycopy(categories, 0, holder.mCategories, 0, categories.length);

            holder.mShowLiveStreamedSessions = in.readInt() == 1;
            
            return holder;
        }

        public TagFilterHolder[] newArray(int size) {
            return new TagFilterHolder[size];
        }
    };
}