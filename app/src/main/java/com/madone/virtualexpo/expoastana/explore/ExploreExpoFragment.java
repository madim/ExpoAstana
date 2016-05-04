package com.madone.virtualexpo.expoastana.explore;

import com.bumptech.glide.Glide;
import com.madone.virtualexpo.expoastana.R;
import com.madone.virtualexpo.expoastana.explore.data.ItemGroup;
import com.madone.virtualexpo.expoastana.explore.data.MessageData;
import com.madone.virtualexpo.expoastana.explore.data.SessionData;
import com.madone.virtualexpo.expoastana.explore.data.ThemeGroup;
import com.madone.virtualexpo.expoastana.explore.data.TopicGroup;
import com.madone.virtualexpo.expoastana.framework.PresenterFragmentImpl;
import com.madone.virtualexpo.expoastana.framework.QueryEnum;
import com.madone.virtualexpo.expoastana.framework.UpdatableView;
import com.madone.virtualexpo.expoastana.provider.ScheduleContract;
import com.madone.virtualexpo.expoastana.settings.ConfMessageCardUtils;
import com.madone.virtualexpo.expoastana.settings.SettingsUtils;
import com.madone.virtualexpo.expoastana.ui.widget.CollectionView;
import com.madone.virtualexpo.expoastana.ui.widget.CollectionViewCallbacks;
import com.madone.virtualexpo.expoastana.ui.widget.DrawShadowFrameLayout;
import com.madone.virtualexpo.expoastana.util.ImageLoader;
import com.madone.virtualexpo.expoastana.util.ThrottledContentObserver;
import com.madone.virtualexpo.expoastana.util.UIUtils;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static android.content.SharedPreferences.OnSharedPreferenceChangeListener;
public class ExploreExpoFragment extends Fragment implements UpdatableView<ExploreModel>,
        CollectionViewCallbacks {

    private static final String TAG = "ExploreExpoFragment";

    private static final int GROUP_ID_KEYNOTE_STREAM_CARD = 10;

    private static final int GROUP_ID_LIVE_STREAM_CARD = 15;

    private static final int GROUP_ID_MESSAGE_CARDS = 20;

    private static final int GROUP_ID_TOPIC_CARDS = 15;

    private static final int GROUP_ID_THEME_CARDS = 20;

    /**
     * Used to load images asynchronously on a background thread.
     */
    private ImageLoader mImageLoader;

    /**
     * CollectionView representing the cards displayed to the user.
     */
    private CollectionView mCollectionView = null;

    /**
     * Empty view displayed when {@code mCollectionView} is empty.
     */
    private View mEmptyView;

    private List<UserActionListener> mListeners = new ArrayList<>();

    private ThrottledContentObserver mSessionsObserver, mTagsObserver;

    @Override
    public void displayData(ExploreModel model, QueryEnum query) {
        if (model.getTagTitles() != null) {
            updateCollectionView(model);
        }
    }

    @Override
    public void displayErrorMessage(QueryEnum query) {
    }

    @Override
    public Context getContext() {
        return getActivity();
    }

    @Override
    public void addListener(UserActionListener toAdd) {
        mListeners.add(toAdd);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.explore_expo_frag, container, false);
        mCollectionView = (CollectionView) root.findViewById(R.id.explore_collection_view);
        mEmptyView = root.findViewById(android.R.id.empty);
        getActivity().overridePendingTransition(0, 0);

        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mImageLoader = new ImageLoader(getActivity(), R.drawable.io_logo);
    }

    private void setContentTopClearance(int clearance) {
        if (mCollectionView != null) {
            mCollectionView.setContentTopClearance(clearance);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().invalidateOptionsMenu();

        int actionBarSize = UIUtils.calculateActionBarSize(getActivity());
        DrawShadowFrameLayout drawShadowFrameLayout =
                (DrawShadowFrameLayout) getActivity().findViewById(R.id.main_content);
        if (drawShadowFrameLayout != null) {
            drawShadowFrameLayout.setShadowTopOffset(actionBarSize);
        }
        setContentTopClearance(actionBarSize);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mSessionsObserver = new ThrottledContentObserver(new ThrottledContentObserver.Callbacks() {
            @Override
            public void onThrottledContentObserverFired() {
                fireReloadEvent();
                fireReloadTagsEvent();
            }
        });
        mTagsObserver = new ThrottledContentObserver(new ThrottledContentObserver.Callbacks() {
            @Override
            public void onThrottledContentObserverFired() {
                fireReloadTagsEvent();
            }
        });

    }

    @Override
    public void onDetach() {
        super.onDetach();

        getActivity().getContentResolver().unregisterContentObserver(mSessionsObserver);
        getActivity().getContentResolver().unregisterContentObserver(mTagsObserver);
    }

    private void updateCollectionView(ExploreModel model) {
        Log.d(TAG, "Updating collection view.");

        CollectionView.Inventory inventory = new CollectionView.Inventory();
        CollectionView.InventoryGroup inventoryGroup;

        SessionData keynoteData = model.getKeynoteData();
        if (keynoteData != null) {
            Log.d(TAG, "Keynote Live stream data found: " + model.getKeynoteData());
            inventoryGroup = new CollectionView.InventoryGroup
                    (GROUP_ID_KEYNOTE_STREAM_CARD);
            inventoryGroup.addItemWithTag(keynoteData);
            inventory.addGroup(inventoryGroup);
        }

        Log.d(TAG, "Inventory item count:" + inventory.getGroupCount() + " " + inventory
                .getTotalItemCount());

        ArrayList<CollectionView.InventoryGroup> themeGroups = new ArrayList<>();
        ArrayList<CollectionView.InventoryGroup> topicGroups = new ArrayList<>();

        for (TopicGroup topic : model.getTopics()) {
            Log.d(TAG, topic.getTitle() + ": " + topic.getSessions().size());

            if (topic.getSessions().size() > 0) {
                inventoryGroup = new CollectionView.InventoryGroup(GROUP_ID_TOPIC_CARDS);
                inventoryGroup.addItemWithTag(topic);
                topic.setTitle(getTranslatedTitle(topic.getTitle(), model));
                topicGroups.add(inventoryGroup);
            }
        }

        for (ThemeGroup theme : model.getThemes()) {
            Log.d(TAG, theme.getTitle() + ": " + theme.getSessions().size());

            if (theme.getSessions().size() > 0) {
                inventoryGroup = new CollectionView.InventoryGroup(GROUP_ID_THEME_CARDS);
                inventoryGroup.addItemWithTag(theme);
                theme.setTitle(getTranslatedTitle(theme.getTitle(), model));
                themeGroups.add(inventoryGroup);
            }
        }

        int topicsPerTheme = topicGroups.size();
        if (themeGroups.size() > 0) {
            topicsPerTheme = topicGroups.size() / themeGroups.size();
        }
        Iterator<CollectionView.InventoryGroup> themeIterator = themeGroups.iterator();
        int currentTopicNum = 0;
        for (CollectionView.InventoryGroup topicGroup : topicGroups) {
            inventory.addGroup(topicGroup);
            currentTopicNum++;
            if (currentTopicNum == topicsPerTheme) {
                if (themeIterator.hasNext()) {
                    inventory.addGroup(themeIterator.next());
                }
                currentTopicNum = 0;
            }
        }

        while (themeIterator.hasNext()) {
            inventory.addGroup(themeIterator.next());
        }

        Parcelable state = mCollectionView.onSaveInstanceState();
        mCollectionView.setCollectionAdapter(this);
        mCollectionView.updateInventory(inventory, false);
        if (state != null) {
            mCollectionView.onRestoreInstanceState(state);
        }

        // Show empty view if there were no Group cards.
        mEmptyView.setVisibility(inventory.getGroupCount() < 1 ? View.VISIBLE : View.GONE);
    }

    private String getTranslatedTitle(String title, ExploreModel model) {
        if (model.getTagTitles().get(title) != null) {
            return model.getTagTitles().get(title);
        } else {
            return title;
        }
    }

    @Override
    public View newCollectionHeaderView(Context context, int groupId, ViewGroup parent) {
        return LayoutInflater.from(context)
                .inflate(R.layout.explore_expo_card_header_with_button, parent, false);
    }

    @Override
    public void bindCollectionHeaderView(Context context, View view, final int groupId,
                                         final String headerLabel, Object headerTag) {
    }

    @Override
    public View newCollectionItemView(Context context, int groupId, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);

        // First inflate the card container.
        int containerLayoutId;
        switch (groupId) {
            case GROUP_ID_TOPIC_CARDS:
            case GROUP_ID_THEME_CARDS:

            default:
                containerLayoutId = R.layout.explore_expo_card_container;
                break;
        }
        ViewGroup containerView = (ViewGroup)inflater.inflate(containerLayoutId, parent, false);

        UIUtils.setAccessibilityIgnore(containerView);

        ViewGroup containerContents = (ViewGroup)containerView.findViewById(
                R.id.explore_expo_card_container_contents);

        int headerLayoutId = -1;
        switch (groupId) {
            case GROUP_ID_THEME_CARDS:
            case GROUP_ID_TOPIC_CARDS:
                headerLayoutId = R.layout.explore_expo_card_header_with_button;
                break;

        }
        // Inflate the specified number of items.
        if (headerLayoutId > -1) {
            inflater.inflate(headerLayoutId, containerContents, true);
        }

        // Now inflate the items within the container cards.
        int itemLayoutId = -1;
        int numItems = 1;
        switch (groupId) {
            case GROUP_ID_KEYNOTE_STREAM_CARD:
                itemLayoutId = R.layout.explore_expo_keynote_stream_item;
                numItems = 1;
                break;
            case GROUP_ID_THEME_CARDS:
                itemLayoutId = R.layout.explore_expo_topic_theme_livestream_item;
                numItems = ExploreModel.getThemeSessionLimit(getContext());
                break;
            case GROUP_ID_TOPIC_CARDS:
                itemLayoutId = R.layout.explore_expo_topic_theme_livestream_item;
                numItems = ExploreModel.getTopicSessionLimit(getContext());
                break;

        }

        if (itemLayoutId > -1) {
            for (int itemIndex = 0; itemIndex < numItems; itemIndex++) {
                inflater.inflate(itemLayoutId, containerContents, true);
            }
        }
        return containerView;
    }

    @Override
    public void bindCollectionItemView(Context context, View view, int groupId,
                                       int indexInGroup, int dataIndex, Object tag) {
        if (GROUP_ID_KEYNOTE_STREAM_CARD == groupId) {

            populateSubItemInfo(context, view, groupId, tag);

            View clickableView = view.findViewById(R.id.explore_io_clickable_item);
            if (clickableView != null) {
                clickableView.setTag(tag);
            }
        } else {

            ViewGroup viewWithChildrenSubItems = (ViewGroup)(view.findViewById(
                    R.id.explore_expo_card_container_contents));
            ItemGroup itemGroup = (ItemGroup) tag;

            // Set Header tag and title.
            viewWithChildrenSubItems.getChildAt(0).setTag(tag);
            TextView titleTextView = ((TextView) view.findViewById(android.R.id.title));
            View headerView = view.findViewById(R.id.explore_io_card_header_layout);
            if (headerView != null) {
                headerView.setContentDescription(
                        getString(R.string.more_items_button_desc_with_label_a11y,
                                itemGroup.getTitle()));
            }

            // Set the tag on the moreButton so it can be accessed by the click listener.
            View moreButton = view.findViewById(android.R.id.button1);
            if (moreButton != null) {
                moreButton.setTag(tag);
            }
            if (titleTextView != null) {
                titleTextView.setText(itemGroup.getTitle());
            }

            // Skipping first child b/c it is a header view.
            for (int viewChildIndex = 1; viewChildIndex < viewWithChildrenSubItems.getChildCount(); viewChildIndex++) {
                View childView = viewWithChildrenSubItems.getChildAt(viewChildIndex);

                int sessionIndex = viewChildIndex - 1;
                int sessionSize = itemGroup.getSessions().size();
                if (childView != null && sessionIndex < sessionSize) {
                    childView.setVisibility(View.VISIBLE);
                    SessionData sessionData = itemGroup.getSessions().get(sessionIndex);
                    childView.setTag(sessionData);
                    populateSubItemInfo(context, childView, groupId, sessionData);
                } else if (childView != null) {
                    childView.setVisibility(View.GONE);
                }
            }
        }

    }

    private void populateSubItemInfo(Context context, View view, int groupId, Object tag) {

        TextView titleView = (TextView) view.findViewById(R.id.title);
        TextView descriptionView = (TextView) view.findViewById(R.id.description);

        // Load item elements common to THEME and TOPIC group cards.
        if (tag instanceof SessionData) {
            SessionData sessionData = (SessionData)tag;
            titleView.setText(sessionData.getSessionName());
            if (!TextUtils.isEmpty(sessionData.getImageUrl())) {
                ImageView imageView = (ImageView) view.findViewById(R.id.thumbnail);
                mImageLoader.loadImage(sessionData.getImageUrl(), imageView);
            }

            if (!TextUtils.isEmpty(sessionData.getDetails())) {
                descriptionView.setText(sessionData.getDetails());
            }
        }
    }

    private void fireReloadEvent() {
        if (!isAdded()) {
            return;
        }
        for (UserActionListener h1 : mListeners) {
            Bundle args = new Bundle();
            args.putInt(PresenterFragmentImpl.KEY_RUN_QUERY_ID,
                    ExploreModel.ExploreQueryEnum.SESSIONS.getId());
            h1.onUserAction(ExploreModel.ExploreUserActionEnum.RELOAD, args);
        }
    }

    private void fireReloadTagsEvent() {
        if (!isAdded()) {
            return;
        }
        for (UserActionListener h1 : mListeners) {
            Bundle args = new Bundle();
            args.putInt(PresenterFragmentImpl.KEY_RUN_QUERY_ID,
                    ExploreModel.ExploreQueryEnum.TAGS.getId());
            h1.onUserAction(ExploreModel.ExploreUserActionEnum.RELOAD, args);
        }
    }

    @Override
    public Uri getDataUri(QueryEnum query) {
        if (query == ExploreModel.ExploreQueryEnum.SESSIONS) {
            return ScheduleContract.Sessions.CONTENT_URI;
        }
        return Uri.EMPTY;
    }

    private boolean shouldShowCard(ConfMessageCardUtils.ConfMessageCard card) {

        boolean shouldShow = ConfMessageCardUtils.shouldShowConfMessageCard(getContext(), card);
        boolean hasDismissed = ConfMessageCardUtils.hasDismissedConfMessageCard(getContext(),
                card);
        return  (shouldShow && !hasDismissed);
    }
}