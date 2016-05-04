package com.madone.virtualexpo.expoastana.explore;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import com.madone.virtualexpo.expoastana.Config;
import com.madone.virtualexpo.expoastana.R;
import com.madone.virtualexpo.expoastana.model.TagMetadata;
import com.madone.virtualexpo.expoastana.BaseActivity;
import com.madone.virtualexpo.expoastana.provider.ScheduleContract;
import com.madone.virtualexpo.expoastana.settings.SettingsUtils;
import com.madone.virtualexpo.expoastana.ui.widget.CollectionView;
import com.madone.virtualexpo.expoastana.ui.widget.CollectionView.Inventory;
import com.madone.virtualexpo.expoastana.ui.widget.CollectionView.InventoryGroup;
import com.madone.virtualexpo.expoastana.ui.widget.CollectionViewCallbacks;
import com.madone.virtualexpo.expoastana.ui.widget.DrawShadowFrameLayout;
import com.madone.virtualexpo.expoastana.util.UIUtils;

import java.util.List;

public class ExploreSessionsActivity extends BaseActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String EXTRA_FILTER_TAG =
            "com.google.samples.apps.iosched.explore.EXTRA_FILTER_TAG";
    public static final String EXTRA_SHOW_LIVE_STREAM_SESSIONS =
            "com.google.samples.apps.iosched.explore.EXTRA_SHOW_LIVE_STREAM_SESSIONS";

    // The saved instance state filters
    private static final String STATE_FILTER_TAGS =
            "com.google.samples.apps.iosched.explore.STATE_FILTER_TAGS";
    private static final String STATE_CURRENT_URI =
            "com.google.samples.apps.iosched.explore.STATE_CURRENT_URI";

    private static final String SCREEN_LABEL = "ExploreSessions";

    private static final String TAG = "ExploreSessionsActivity";
    private static final int TAG_METADATA_TOKEN = 0x8;

    private static final int GROUP_TOPIC_TYPE_OR_THEME = 0;
    private static final int GROUP_LIVE_STREAM = 1;

    private static final int MODE_TIME_FIT = 1;
    private static final int MODE_EXPLORE = 2;

    private CollectionView mDrawerCollectionView;
    private DrawerLayout mDrawerLayout;

    private TagMetadata mTagMetadata;
    private TagFilterHolder mTagFilterHolder;
    private Uri mCurrentUri;

    private ExploreSessionsFragment mFragment;
    private int mMode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.explore_sessions_act);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        registerHideableHeaderView(findViewById(R.id.headerbar));

        mFragment = (ExploreSessionsFragment) getFragmentManager()
                .findFragmentById(R.id.explore_sessions_frag);

        if (savedInstanceState != null) {

            mTagFilterHolder = savedInstanceState.getParcelable(STATE_FILTER_TAGS);
            mCurrentUri = savedInstanceState.getParcelable(STATE_CURRENT_URI);

        } else if (getIntent() != null) {
            mCurrentUri = getIntent().getData();
        }

        // Build the tag URI
        long[] interval = ScheduleContract.Sessions.getInterval(mCurrentUri);
        if (interval != null) {
            mMode = MODE_TIME_FIT;

            String title = getString(R.string.explore_sessions_time_slot_title,
                    getString(R.string.explore_sessions_show_day_n,
                            UIUtils.startTimeToDayIndex(interval[0])),
                    UIUtils.formatTime(interval[0], this));
            setTitle(title);

        } else {
            mMode = MODE_EXPLORE;
        }

        // Add the back button to the toolbar.
        Toolbar toolbar = getActionBarToolbar();
        toolbar.setNavigationIcon(R.drawable.ic_up);
        toolbar.setNavigationContentDescription(R.string.close_and_go_back);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateUpOrBack(ExploreSessionsActivity.this, null);
            }
        });

        getLoaderManager().initLoader(TAG_METADATA_TOKEN, null, this);

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(STATE_FILTER_TAGS, mTagFilterHolder);
        outState.putParcelable(STATE_CURRENT_URI, mCurrentUri);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        enableActionBarAutoHide((CollectionView) findViewById(R.id.collection_view));
    }

    @Override
    protected void onActionBarAutoShowOrHide(boolean shown) {
        super.onActionBarAutoShowOrHide(shown);
        DrawShadowFrameLayout frame = (DrawShadowFrameLayout) findViewById(R.id.main_content);
        frame.setShadowVisible(shown, shown);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id == TAG_METADATA_TOKEN) {
            return TagMetadata.createCursorLoader(this);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case TAG_METADATA_TOKEN:
                mTagMetadata = new TagMetadata(cursor);
                onTagMetadataLoaded();
                break;
            default:
                cursor.close();
        }
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.END)) {
            mDrawerLayout.closeDrawer(GravityCompat.END);
        } else {
            super.onBackPressed();
        }
    }

    private void onTagMetadataLoaded() {
        if (mTagFilterHolder == null) {
            mTagFilterHolder = new TagFilterHolder();

            String tag = getIntent().getStringExtra(EXTRA_FILTER_TAG);
            TagMetadata.Tag userTag = mTagMetadata.getTag(tag);
            String userTagCategory = userTag == null ? null : userTag.getCategory();
            if (tag != null && userTagCategory != null) {
                mTagFilterHolder.add(tag, userTagCategory);
            }

            mTagFilterHolder.setShowLiveStreamedSessions(
                    getIntent().getBooleanExtra(EXTRA_SHOW_LIVE_STREAM_SESSIONS, false));

            if (SettingsUtils.isAttendeeAtVenue(this)) {
                List<TagMetadata.Tag> tags =
                        mTagMetadata.getTagsInCategory(Config.Tags.CATEGORY_TYPE);
                if (tags != null && !TextUtils.equals(userTagCategory, Config.Tags.CATEGORY_TYPE)) {
                    for (TagMetadata.Tag theTag : tags) {
                        mTagFilterHolder.add(theTag.getId(), theTag.getCategory());
                    }
                }
            } else {
                TagMetadata.Tag theTag = mTagMetadata.getTag(Config.Tags.SESSIONS);
                if (!TextUtils.equals(theTag.getCategory(), userTagCategory)) {
                    mTagFilterHolder.add(theTag.getId(), theTag.getCategory());
                }
                mTagFilterHolder.setShowLiveStreamedSessions(true);
            }
        }
        reloadFragment();
    }

    private void setActivityTitle() {
        if (mMode == MODE_EXPLORE && mTagMetadata != null) {
            String tag = getIntent().getStringExtra(EXTRA_FILTER_TAG);
            TagMetadata.Tag titleTag = tag == null ? null : mTagMetadata.getTag(tag);
            String title = null;
            if (titleTag != null &&
                    mTagFilterHolder.getCountByCategory(titleTag.getCategory()) == 1) {
                for (String tagId : mTagFilterHolder.getSelectedFilters()) {
                    TagMetadata.Tag theTag = mTagMetadata.getTag(tagId);
                    if (TextUtils.equals(titleTag.getCategory(), theTag.getCategory())) {
                        title = theTag.getName();
                    }
                }
            }
            setTitle(title == null ? getString(R.string.title_explore) : title);
        }
    }

    private void reloadFragment() {
        Uri uri = mCurrentUri;

        if (uri == null) {
            uri = ScheduleContract.Sessions.buildCategoryTagFilterUri(
                    ScheduleContract.Sessions.CONTENT_URI,
                    mTagFilterHolder.toStringArray(),
                    mTagFilterHolder.getCategoryCount());
        } else {
            uri = ScheduleContract.Sessions.buildCategoryTagFilterUri(uri,
                    mTagFilterHolder.toStringArray(),
                    mTagFilterHolder.getCategoryCount());
        }
        setActivityTitle();
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.putExtra(ExploreSessionsFragment.EXTRA_SHOW_LIVESTREAMED_SESSIONS,
                mTagFilterHolder.isShowLiveStreamedSessions());

        Log.d(TAG, "Reloading fragment with categories " + mTagFilterHolder.getCategoryCount() +
                " uri: " + uri +
                " showLiveStreamedEvents: " + mTagFilterHolder.isShowLiveStreamedSessions());

        mFragment.reloadFromArguments(intentToFragmentArguments(intent));
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    private class TagAdapter implements CollectionViewCallbacks {

        public Inventory getInventory() {
            List<TagMetadata.Tag> themes =
                    mTagMetadata.getTagsInCategory(Config.Tags.CATEGORY_THEME);
            Inventory inventory = new Inventory();

            InventoryGroup themeGroup = new InventoryGroup(GROUP_TOPIC_TYPE_OR_THEME)
                    .setDisplayCols(1)
                    .setDataIndexStart(0)
                    .setShowHeader(false);

            if (themes != null && themes.size() > 0) {
                for (TagMetadata.Tag type : themes) {
                    themeGroup.addItemWithTag(type);
                }
                inventory.addGroup(themeGroup);
            }

            InventoryGroup typesGroup = new InventoryGroup(GROUP_TOPIC_TYPE_OR_THEME)
                    .setDataIndexStart(0)
                    .setShowHeader(true);
            List<TagMetadata.Tag> data = mTagMetadata.getTagsInCategory(Config.Tags.CATEGORY_TYPE);

            if (data != null && data.size() > 0) {
                for (TagMetadata.Tag tag : data) {
                    typesGroup.addItemWithTag(tag);
                }
                inventory.addGroup(typesGroup);
            }

            InventoryGroup liveStreamGroup = new InventoryGroup(GROUP_LIVE_STREAM)
                    .setDataIndexStart(0)
                    .setShowHeader(true)
                    .addItemWithTag("Livestreamed");
            inventory.addGroup(liveStreamGroup);

            InventoryGroup topicsGroup = new InventoryGroup(GROUP_TOPIC_TYPE_OR_THEME)
                    .setDataIndexStart(0)
                    .setShowHeader(true);

            List<TagMetadata.Tag> topics =
                    mTagMetadata.getTagsInCategory(Config.Tags.CATEGORY_TOPIC);
            if (topics != null && topics.size() > 0) {
                for (TagMetadata.Tag topic : topics) {
                    topicsGroup.addItemWithTag(topic);
                }
                inventory.addGroup(topicsGroup);
            }

            return inventory;
        }

        @Override
        public View newCollectionHeaderView(Context context, int groupId, ViewGroup parent) {
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.explore_sessions_list_item_alt_header, parent, false);
            UIUtils.setAccessibilityIgnore(view);
            return view;
        }

        @Override
        public void bindCollectionHeaderView(Context context, View view, int groupId,
                                             String headerLabel, Object headerTag) {
        }

        @Override
        public View newCollectionItemView(Context context, int groupId, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(groupId == GROUP_LIVE_STREAM ?
                    R.layout.explore_sessions_list_item_livestream_alt_drawer :
                    R.layout.explore_sessions_list_item_alt_drawer, parent, false);
        }

        @Override
        public void bindCollectionItemView(Context context, View view, int groupId,
                                           int indexInGroup, int dataIndex, Object tag) {
            final CheckBox checkBox = (CheckBox) view.findViewById(R.id.filter_checkbox);
            if (groupId == GROUP_LIVE_STREAM) {
                checkBox.setChecked(mTagFilterHolder.isShowLiveStreamedSessions());
                checkBox.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mTagFilterHolder.setShowLiveStreamedSessions(checkBox.isChecked());
                    }
                });

            } else {
                TagMetadata.Tag theTag = (TagMetadata.Tag) tag;
                if (theTag != null) {
                    ((TextView) view.findViewById(R.id.text_view)).setText(theTag.getName());
                    checkBox.setChecked(mTagFilterHolder.contains(theTag.getId()));
                    checkBox.setTag(theTag);
                }
            }
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    checkBox.performClick();
                }
            });
        }
    }
}
