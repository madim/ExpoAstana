package com.madone.virtualexpo.expoastana.session;

import com.madone.virtualexpo.expoastana.Config;
import com.madone.virtualexpo.expoastana.R;
import com.madone.virtualexpo.expoastana.explore.ExploreSessionsActivity;
import com.madone.virtualexpo.expoastana.framework.QueryEnum;
import com.madone.virtualexpo.expoastana.framework.UpdatableView;
import com.madone.virtualexpo.expoastana.framework.UserActionEnum;
import com.madone.virtualexpo.expoastana.model.TagMetadata;
import com.madone.virtualexpo.expoastana.session.SessionDetailModel.SessionDetailQueryEnum;
import com.madone.virtualexpo.expoastana.ui.widget.ObservableScrollView;
import com.madone.virtualexpo.expoastana.util.ImageLoader;
import com.madone.virtualexpo.expoastana.util.LUtils;
import com.madone.virtualexpo.expoastana.util.TimeUtils;
import com.madone.virtualexpo.expoastana.util.UIUtils;

import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class SessionDetailFragment extends Fragment
        implements ObservableScrollView.Callbacks, UpdatableView<SessionDetailModel> {

    private static final String TAG = "SessionDetailFragment";

    public static final String TRANSITION_NAME_PHOTO = "photo";


    private static HashSet<String> sDismissedFeedbackCard = new HashSet<>();

    private static final float PHOTO_ASPECT_RATIO = 1.7777777f;

    private View mScrollViewChild;

    private TextView mTitle;

    private TextView mSubtitle;

    private ObservableScrollView mScrollView;

    private TextView mAbstract;

    private LinearLayout mTags;

    private ViewGroup mTagsContainer;

    private ImageLoader mNoPlaceholderImageLoader;

    private View mHeaderBox;

    private View mDetailsContainer;

    private int mPhotoHeightPixels;

    private int mHeaderHeightPixels;

    private boolean mHasPhoto;

    private View mPhotoViewContainer;

    private ImageView mPhotoView;

    private float mMaxHeaderElevation;

    private List<Runnable> mDeferredUiOperations = new ArrayList<>();

    private Handler mHandler;

    List<UserActionListener> mListeners = new ArrayList<>();

    @Override
    public void addListener(UserActionListener toAdd) {
        mListeners.add(toAdd);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.session_detail_frag, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mHandler = new Handler();
        initViews();
    }

    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mScrollView == null) {
            return;
        }
    }

    private void sendUserAction(UserActionEnum action, Bundle args) {
        for (UserActionListener l : mListeners) {
            l.onUserAction(action, args);
        }
    }

    private void initViews() {
        mMaxHeaderElevation = getResources().getDimensionPixelSize(
                R.dimen.session_detail_max_header_elevation);

        mScrollView = (ObservableScrollView) getActivity().findViewById(R.id.scroll_view);
        mScrollView.addCallbacks(this);


        mScrollViewChild = getActivity().findViewById(R.id.scroll_view_child);
        mScrollViewChild.setVisibility(View.INVISIBLE);

        mDetailsContainer = getActivity().findViewById(R.id.details_container);
        mHeaderBox = getActivity().findViewById(R.id.header_session);
        mTitle = (TextView) getActivity().findViewById(R.id.session_title);
        mSubtitle = (TextView) getActivity().findViewById(R.id.session_subtitle);
        mPhotoViewContainer = getActivity().findViewById(R.id.session_photo_container);
        mPhotoView = (ImageView) getActivity().findViewById(R.id.session_photo);

        mAbstract = (TextView) getActivity().findViewById(R.id.session_abstract);

        mTags = (LinearLayout) getActivity().findViewById(R.id.session_tags);
        mTagsContainer = (ViewGroup) getActivity().findViewById(R.id.session_tags_container);

        ViewCompat.setTransitionName(mPhotoView, TRANSITION_NAME_PHOTO);

        mNoPlaceholderImageLoader = new ImageLoader(getContext());
    }

    private void recomputePhotoAndScrollingMetrics() {
        mHeaderHeightPixels = mHeaderBox.getHeight();

        mPhotoHeightPixels = 0;
        if (mHasPhoto) {
            mPhotoHeightPixels = (int) (mPhotoView.getWidth() / PHOTO_ASPECT_RATIO);
            mPhotoHeightPixels = Math.min(mPhotoHeightPixels, mScrollView.getHeight() * 2 / 3);
        }

        ViewGroup.LayoutParams lp;
        lp = mPhotoViewContainer.getLayoutParams();
        if (lp.height != mPhotoHeightPixels) {
            lp.height = mPhotoHeightPixels;
            mPhotoViewContainer.setLayoutParams(lp);
        }

        ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams)
                mDetailsContainer.getLayoutParams();
        if (mlp.topMargin != mHeaderHeightPixels + mPhotoHeightPixels) {
            mlp.topMargin = mHeaderHeightPixels + mPhotoHeightPixels;
            mDetailsContainer.setLayoutParams(mlp);
        }

        onScrollChanged(0, 0); // trigger scroll handling
    }

    @Override
    public void displayData(SessionDetailModel data, QueryEnum query) {
        if (SessionDetailQueryEnum.SESSIONS == query) {
            displaySessionData(data);
        } else if (SessionDetailQueryEnum.TAG_METADATA == query) {
            displayTags(data);
        }
    }


    @Override
    public void displayErrorMessage(QueryEnum query) {
    }

    @Override
    public Uri getDataUri(QueryEnum query) {
        if (SessionDetailQueryEnum.SESSIONS == query) {
            return ((SessionDetailActivity) getActivity()).getSessionUri();
        } else {
            return null;
        }
    }


    @Override
    public Context getContext() {
        return getActivity();
    }

    @Override
    public void onScrollChanged(int deltaX, int deltaY) {
        int scrollY = mScrollView.getScrollY();

        float newTop = Math.max(mPhotoHeightPixels, scrollY);
        mHeaderBox.setTranslationY(newTop);

        float gapFillProgress = 1;
        if (mPhotoHeightPixels != 0) {
            gapFillProgress = Math.min(Math.max(UIUtils.getProgress(scrollY,
                    0,
                    mPhotoHeightPixels), 0), 1);
        }

        ViewCompat.setElevation(mHeaderBox, gapFillProgress * mMaxHeaderElevation);

        mPhotoViewContainer.setTranslationY(scrollY * 0.5f);
    }

    private void displaySessionData(final SessionDetailModel data) {
        mTitle.setText(data.getSessionTitle());
        mSubtitle.setText(data.getSessionSubtitle());

        mPhotoViewContainer
                .setBackgroundColor(UIUtils.scaleSessionColorToDefaultBG(data.getSessionColor()));

        if (data.hasPhotoUrl()) {
            mHasPhoto = true;
            mNoPlaceholderImageLoader.loadImage(data.getPhotoUrl(), mPhotoView, new RequestListener<String, Bitmap>() {
                @Override
                public boolean onException(Exception e, String model, Target<Bitmap> target,
                                           boolean isFirstResource) {
                    mHasPhoto = false;
                    recomputePhotoAndScrollingMetrics();
                    return false;
                }

                @Override
                public boolean onResourceReady(Bitmap resource, String model, Target<Bitmap> target,
                                               boolean isFromMemoryCache, boolean isFirstResource) {

                    recomputePhotoAndScrollingMetrics();
                    return false;
                }
            });
            recomputePhotoAndScrollingMetrics();
        } else {
            mHasPhoto = false;
            recomputePhotoAndScrollingMetrics();
        }

        tryExecuteDeferredUiOperations();

        displayTags(data);

        if (!TextUtils.isEmpty(data.getSessionAbstract())) {
            UIUtils.setTextMaybeHtml(mAbstract, data.getSessionAbstract());
            mAbstract.setVisibility(View.VISIBLE);
        } else {
            mAbstract.setVisibility(View.GONE);
        }

        updateEmptyView(data);

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                onScrollChanged(0, 0); // trigger scroll handling
                mScrollViewChild.setVisibility(View.VISIBLE);
            }
        });

    }

    private void updateEmptyView(SessionDetailModel data) {
        getActivity().findViewById(android.R.id.empty).setVisibility(
                (data.getSessionTitle() != null && !data.hasSummaryContent())
                        ? View.VISIBLE
                        : View.GONE);
    }

    private void displayTags(SessionDetailModel data) {
        if (data.getTagMetadata() == null || data.getTagsString() == null) {
            if (data.getTagsString() == null) {
                Log.e(TAG, "displayTags: getTagsString is null");
            } else if (data.getTagMetadata() == null ) {
                Log.e(TAG, "displayTags: tagmetadata is null");
            }

                mTagsContainer.setVisibility(View.GONE);
            return;
        }

        if (TextUtils.isEmpty(data.getTagsString())) {
            mTagsContainer.setVisibility(View.GONE);
        } else {
            mTagsContainer.setVisibility(View.VISIBLE);
            mTags.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            String[] tagIds = data.getTagsString().split(",");

            List<TagMetadata.Tag> tags = new ArrayList<TagMetadata.Tag>();
            for (String tagId : tagIds) {
                if (Config.Tags.SESSIONS.equals(tagId) ||
                        Config.Tags.SPECIAL_KEYNOTE.equals(tagId)) {
                    continue;
                }

                TagMetadata.Tag tag = data.getTagMetadata().getTag(tagId);
                if (tag == null) {
                    continue;
                }

                tags.add(tag);
            }

            if (tags.size() == 0) {
                mTagsContainer.setVisibility(View.GONE);
                return;
            }

            Collections.sort(tags, TagMetadata.TAG_DISPLAY_ORDER_COMPARATOR);

            for (final TagMetadata.Tag tag : tags) {
                TextView chipView = (TextView) inflater.inflate(
                        R.layout.include_session_tag_chip, mTags, false);
                chipView.setText(tag.getName());
                chipView.setContentDescription(
                        getString(R.string.talkback_button, tag.getName()));
                chipView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(getContext(), ExploreSessionsActivity.class)
                                .putExtra(ExploreSessionsActivity.EXTRA_FILTER_TAG, tag.getId())
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }
                });

                mTags.addView(chipView);
            }
        }
    }


    private void tryExecuteDeferredUiOperations() {
        for (Runnable r : mDeferredUiOperations) {
            r.run();
            mDeferredUiOperations.clear();
        }
    }
}