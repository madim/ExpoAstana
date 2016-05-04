package com.madone.virtualexpo.expoastana;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.madone.virtualexpo.expoastana.BuildConfigs;
import com.madone.virtualexpo.expoastana.R;
import com.madone.virtualexpo.expoastana.explore.ExploreExpoActivity;
import com.madone.virtualexpo.expoastana.framework.Model;
import com.madone.virtualexpo.expoastana.framework.PresenterFragmentImpl;
import com.madone.virtualexpo.expoastana.framework.QueryEnum;
import com.madone.virtualexpo.expoastana.framework.UpdatableView;
import com.madone.virtualexpo.expoastana.framework.UserActionEnum;
import com.madone.virtualexpo.expoastana.myschedule.MyScheduleActivity;
import com.madone.virtualexpo.expoastana.provider.ScheduleContract;
import com.madone.virtualexpo.expoastana.service.DataBootstrapService;
import com.madone.virtualexpo.expoastana.settings.SettingsUtils;
import com.madone.virtualexpo.expoastana.ui.widget.NavDrawerItemView;
import com.madone.virtualexpo.expoastana.ui.widget.ScrimInsetsScrollView;
import com.madone.virtualexpo.expoastana.util.ImageLoader;
import com.madone.virtualexpo.expoastana.util.LUtils;
import com.madone.virtualexpo.expoastana.util.TimeUtils;
import com.madone.virtualexpo.expoastana.util.UIUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public abstract class BaseActivity extends AppCompatActivity {

    public final static String PRESENTER_TAG = "Presenter";

    private static final String TAG = "BaseActivity";

    private DrawerLayout mDrawerLayout;

    private LUtils mLUtils;

    private ObjectAnimator mStatusBarColorAnimator;

    private LinearLayout mAccountListContainer;

    private ViewGroup mDrawerItemsListContainer;

    private Handler mHandler;

    private ImageView mExpandAccountBoxIndicator;

    private boolean mAccountBoxExpanded = false;

    private ArrayList<View> mHideableHeaderViews = new ArrayList<View>();

    private static final int HEADER_HIDE_ANIM_DURATION = 300;

    private static final int ACCOUNT_BOX_EXPAND_ANIM_DURATION = 200;

    protected static final int NAVDRAWER_ITEM_MY_SCHEDULE = 0;

    protected static final int NAVDRAWER_ITEM_EXPLORE = 1;

    protected static final int NAVDRAWER_ITEM_MAP = 2;

    protected static final int NAVDRAWER_ITEM_SETTINGS = 3;

    protected static final int NAVDRAWER_ITEM_ABOUT = 4;

    protected static final int NAVDRAWER_ITEM_INVALID = -1;

    protected static final int NAVDRAWER_ITEM_SEPARATOR_SPECIAL = -2;

    private static final int[] NAVDRAWER_TITLE_RES_ID = new int[]{
            R.string.navdrawer_item_my_schedule,
            R.string.navdrawer_item_explore,
            R.string.navdrawer_item_map,
            R.string.navdrawer_item_settings,
            R.string.description_about
    };

    private static final int[] NAVDRAWER_ICON_RES_ID = new int[]{
            R.drawable.ic_navview_my_schedule,  // My Schedule
            R.drawable.ic_navview_explore,  // Explore
            R.drawable.ic_navview_map, // Map
            R.drawable.ic_navview_settings, // Settings.
            R.drawable.ic_info_outline // About
    };

    private static final int NAVDRAWER_LAUNCH_DELAY = 250;

    private static final int MAIN_CONTENT_FADEOUT_DURATION = 150;

    private static final int MAIN_CONTENT_FADEIN_DURATION = 250;

    private ArrayList<Integer> mNavDrawerItems = new ArrayList<Integer>();

    private View[] mNavDrawerItemViews = null;

    private Toolbar mActionBarToolbar;

    private Object mSyncObserverHandle; // ?

    private boolean mActionBarAutoHideEnabled = false;

    private int mActionBarAutoHideSensivity = 0;

    private int mActionBarAutoHideMinY = 0;

    private int mActionBarAutoHideSignal = 0;

    private boolean mActionBarShown = true;

    private Runnable mDeferredOnDrawerClosedRunnable;

    private boolean mManualSyncRequest;

    private int mThemedStatusBarColor;

    private int mNormalStatusBarColor;

    private int mProgressBarTopWhenActionBarShown;

    private static final TypeEvaluator ARGB_EVALUATOR = new ArgbEvaluator();

    private ImageLoader mImageLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mImageLoader = new ImageLoader(this);
        mHandler = new Handler();

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        mLUtils = LUtils.getInstance(this); // ?
        mThemedStatusBarColor = getResources().getColor(R.color.theme_primary_dark);
        mNormalStatusBarColor = mThemedStatusBarColor;
    }

    protected void setProgressBarTopWhenActionBarShown(int progressBarTopWhenActionBarShown) {
        mProgressBarTopWhenActionBarShown = progressBarTopWhenActionBarShown;
    }

    protected int getSelfNavDrawerItem() {
        return NAVDRAWER_ITEM_INVALID;
    }

    private void setupNavDrawer() {
        int selfItem = getSelfNavDrawerItem();

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (mDrawerLayout == null) {
            return;
        }
        mDrawerLayout.setStatusBarBackgroundColor(
                getResources().getColor(R.color.theme_primary_dark));
        ScrimInsetsScrollView navDrawer = (ScrimInsetsScrollView)
                mDrawerLayout.findViewById(R.id.navdrawer);
        if (selfItem == NAVDRAWER_ITEM_INVALID) {
            if (navDrawer != null) {
                ((ViewGroup) navDrawer.getParent()).removeView(navDrawer);
            }
            mDrawerLayout = null;
            return;
        }

        if (navDrawer != null) {
            final View chosenAccountContentView = findViewById(R.id.chosen_account_content_view);
            final View chosenAccountView = findViewById(R.id.chosen_account_view);
            final int navDrawerChosenAccountHeight = getResources().getDimensionPixelSize(
                    R.dimen.navdrawer_chosen_account_height);
            navDrawer.setOnInsetsCallback(new ScrimInsetsScrollView.OnInsetsCallback() {
                @Override
                public void onInsetsChanged(Rect insets) {
                    ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams)
                            chosenAccountContentView.getLayoutParams();
                    lp.topMargin = insets.top;
                    chosenAccountContentView.setLayoutParams(lp);

                    ViewGroup.LayoutParams lp2 = chosenAccountView.getLayoutParams();
                    lp2.height = navDrawerChosenAccountHeight + insets.top;
                    chosenAccountView.setLayoutParams(lp2);
                }
            });
        }

        if (mActionBarToolbar != null) {
            mActionBarToolbar.setNavigationIcon(R.drawable.ic_ab_drawer);
            mActionBarToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mDrawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }

        mDrawerLayout.setDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerClosed(View drawerView) {
                // run deferred action, if we have one
                if (mDeferredOnDrawerClosedRunnable != null) {
                    mDeferredOnDrawerClosedRunnable.run();
                    mDeferredOnDrawerClosedRunnable = null;
                }
                if (mAccountBoxExpanded) {
                    mAccountBoxExpanded = false;
                    setupAccountBoxToggle();
                }
                onNavDrawerStateChanged(false, false);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                onNavDrawerStateChanged(true, false);
            }

            @Override
            public void onDrawerStateChanged(int newState) {
                onNavDrawerStateChanged(isNavDrawerOpen(), newState != DrawerLayout.STATE_IDLE);
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                onNavDrawerSlide(slideOffset);
            }
        });

        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        populateNavDrawer();
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        getActionBarToolbar();
    }

    protected void onNavDrawerStateChanged(boolean isOpen, boolean isAnimating) {
        if (mActionBarAutoHideEnabled && isOpen) {
            autoShowOrHideActionBar(true);
        }
    }

    protected void onNavDrawerSlide(float offset) {
    }

    protected boolean isNavDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.START);
    }

    protected void closeNavDrawer() {
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    private void populateNavDrawer() {
        mNavDrawerItems.clear();

        mNavDrawerItems.add(NAVDRAWER_ITEM_MY_SCHEDULE);

        mNavDrawerItems.add(NAVDRAWER_ITEM_EXPLORE);

        mNavDrawerItems.add(NAVDRAWER_ITEM_MAP);

        mNavDrawerItems.add(NAVDRAWER_ITEM_SEPARATOR_SPECIAL);
        mNavDrawerItems.add(NAVDRAWER_ITEM_SETTINGS);
        mNavDrawerItems.add(NAVDRAWER_ITEM_ABOUT);

        createNavDrawerItems();
    }

    @Override
    public void onBackPressed() {
        if (isNavDrawerOpen()) {
            closeNavDrawer();
        } else {
            super.onBackPressed();
        }
    }

    private void createNavDrawerItems() {
        mDrawerItemsListContainer = (ViewGroup) findViewById(R.id.navdrawer_items_list);
        if (mDrawerItemsListContainer == null) {
            return;
        }

        mNavDrawerItemViews = new View[mNavDrawerItems.size()];
        mDrawerItemsListContainer.removeAllViews();
        int i = 0;
        for (int itemId : mNavDrawerItems) {
            mNavDrawerItemViews[i] = makeNavDrawerItem(itemId, mDrawerItemsListContainer);
            mDrawerItemsListContainer.addView(mNavDrawerItemViews[i]);
            ++i;
        }
    }

    private void setSelectedNavDrawerItem(int itemId) {
        if (mNavDrawerItemViews != null) {
            for (int i = 0; i < mNavDrawerItemViews.length; i++) {
                if (i < mNavDrawerItems.size()) {
                    int thisItemId = mNavDrawerItems.get(i);
                    mNavDrawerItemViews[i].setActivated(itemId == thisItemId);
                }
            }
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setupNavDrawer();

        View mainContent = findViewById(R.id.main_content);
        if (mainContent != null) {
            mainContent.setAlpha(0);
            mainContent.animate().alpha(1).setDuration(MAIN_CONTENT_FADEIN_DURATION);
        } else {
            Log.i(TAG, "No view with ID main_content to fade in.");
        }
    }

    private void setupAccountBoxToggle() {
        int selfItem = getSelfNavDrawerItem();
        if (mDrawerLayout == null || selfItem == NAVDRAWER_ITEM_INVALID) {
            return;
        }
        mExpandAccountBoxIndicator.setImageResource(mAccountBoxExpanded
                ? R.drawable.ic_navview_accounts_collapse
                : R.drawable.ic_navview_accounts_expand);
        int hideTranslateY = -mAccountListContainer.getHeight() / 4; // last 25% of animation
        if (mAccountBoxExpanded && mAccountListContainer.getTranslationY() == 0) {
            mAccountListContainer.setAlpha(0);
            mAccountListContainer.setTranslationY(hideTranslateY);
        }

        AnimatorSet set = new AnimatorSet();
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mDrawerItemsListContainer.setVisibility(mAccountBoxExpanded
                        ? View.INVISIBLE : View.VISIBLE);
                mAccountListContainer.setVisibility(mAccountBoxExpanded
                        ? View.VISIBLE : View.INVISIBLE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                onAnimationEnd(animation);
            }
        });

        if (mAccountBoxExpanded) {
            mAccountListContainer.setVisibility(View.VISIBLE);
            AnimatorSet subSet = new AnimatorSet();
            subSet.playTogether(
                    ObjectAnimator.ofFloat(mAccountListContainer, View.ALPHA, 1)
                            .setDuration(ACCOUNT_BOX_EXPAND_ANIM_DURATION),
                    ObjectAnimator.ofFloat(mAccountListContainer, View.TRANSLATION_Y, 0)
                            .setDuration(ACCOUNT_BOX_EXPAND_ANIM_DURATION));
            set.playSequentially(
                    ObjectAnimator.ofFloat(mDrawerItemsListContainer, View.ALPHA, 0)
                            .setDuration(ACCOUNT_BOX_EXPAND_ANIM_DURATION),
                    subSet);
            set.start();
        } else {
            mDrawerItemsListContainer.setVisibility(View.VISIBLE);
            AnimatorSet subSet = new AnimatorSet();
            subSet.playTogether(
                    ObjectAnimator.ofFloat(mAccountListContainer, View.ALPHA, 0)
                            .setDuration(ACCOUNT_BOX_EXPAND_ANIM_DURATION),
                    ObjectAnimator.ofFloat(mAccountListContainer, View.TRANSLATION_Y,
                            hideTranslateY)
                            .setDuration(ACCOUNT_BOX_EXPAND_ANIM_DURATION));
            set.playSequentially(
                    subSet,
                    ObjectAnimator.ofFloat(mDrawerItemsListContainer, View.ALPHA, 1)
                            .setDuration(ACCOUNT_BOX_EXPAND_ANIM_DURATION));
            set.start();
        }

        set.start();
    }

    private void goToNavDrawerItem(int item) {
        switch (item) {
            case NAVDRAWER_ITEM_MY_SCHEDULE:
                createBackStack(new Intent(this, MyScheduleActivity.class));
                break;
            case NAVDRAWER_ITEM_EXPLORE:
                startActivity(new Intent(this, ExploreExpoActivity.class));
                finish();
                break;
            case NAVDRAWER_ITEM_MAP:
                break;
            case NAVDRAWER_ITEM_SETTINGS:
                break;
        }
    }

    private void createBackStack(Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            TaskStackBuilder builder = TaskStackBuilder.create(this);
            builder.addNextIntentWithParentStack(intent);
            builder.startActivities();
        } else {
            startActivity(intent);
            finish();
        }
    }

    public static void navigateUpOrBack(Activity currentActivity,
                                        Class<? extends Activity> syntheticParentActivity) {
        Intent intent = NavUtils.getParentActivityIntent(currentActivity);

        if (intent == null && syntheticParentActivity != null) {
            try {
                intent = NavUtils.getParentActivityIntent(currentActivity, syntheticParentActivity);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        if (intent == null) {
            currentActivity.onBackPressed();
        } else {
            if (NavUtils.shouldUpRecreateTask(currentActivity, intent)) {
                TaskStackBuilder builder = TaskStackBuilder.create(currentActivity);
                builder.addNextIntentWithParentStack(intent);
                builder.startActivities();
            } else {
                NavUtils.navigateUpTo(currentActivity, intent);
            }
        }
    }

    private void onNavDrawerItemClicked(final int itemId) {
        if (itemId == getSelfNavDrawerItem()) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
            return;
        }

        if (isSpecialItem(itemId)) {
            goToNavDrawerItem(itemId);
        } else {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    goToNavDrawerItem(itemId);
                }
            }, NAVDRAWER_LAUNCH_DELAY);

            setSelectedNavDrawerItem(itemId);
            View mainContent = findViewById(R.id.main_content);
            if (mainContent != null) {
                mainContent.animate().alpha(0).setDuration(MAIN_CONTENT_FADEOUT_DURATION);
            }
        }

        mDrawerLayout.closeDrawer(GravityCompat.START);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Perform one-time bootstrap setup, if needed
        DataBootstrapService.startDataBootstrapIfNecessary(this);

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mSyncObserverHandle != null) {
            ContentResolver.removeStatusChangeListener(mSyncObserverHandle);
            mSyncObserverHandle = null;
        }
    }

    public static Bundle intentToFragmentArguments(Intent intent) {
        Bundle arguments = new Bundle();
        if (intent == null) {
            return arguments;
        }

        final Uri data = intent.getData();
        if (data != null) {
            arguments.putParcelable("_uri", data);
        }

        final Bundle extras = intent.getExtras();
        if (extras != null) {
            arguments.putAll(intent.getExtras());
        }

        return arguments;
    }

    public static Bundle intentToFragmentArguments(Intent intent, Bundle extras) {
        Bundle arguments = new Bundle();
        if (intent == null) {
            return arguments;
        }

        final Uri data = intent.getData();
        if (data != null) {
            arguments.putParcelable("_uri", data);
        }

        if (extras != null) {
            arguments.putAll(intent.getExtras());
        }

        return arguments;
    }

    public static Intent fragmentArgumentsToIntent(Bundle arguments) {
        Intent intent = new Intent();
        if (arguments == null) {
            return intent;
        }

        final Uri data = arguments.getParcelable("_uri");
        if (data != null) {
            intent.setData(data);
        }

        intent.putExtras(arguments);
        intent.removeExtra("_uri");
        return intent;
    }

    private void promptAddAccount() {
        Intent intent = new Intent(Settings.ACTION_ADD_ACCOUNT);
        intent.putExtra(Settings.EXTRA_ACCOUNT_TYPES, new String[]{"com.google"});
        startActivity(intent);
        finish();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private void initActionBarAutoHide() {
        mActionBarAutoHideEnabled = true;
        mActionBarAutoHideMinY = getResources().getDimensionPixelSize(
                R.dimen.action_bar_auto_hide_min_y);
        mActionBarAutoHideSensivity = getResources().getDimensionPixelSize(
                R.dimen.action_bar_auto_hide_sensivity);
    }

    private void onMainContentScrolled(int currentY, int deltaY) {
        if (deltaY > mActionBarAutoHideSensivity) {
            deltaY = mActionBarAutoHideSensivity;
        } else if (deltaY < -mActionBarAutoHideSensivity) {
            deltaY = -mActionBarAutoHideSensivity;
        }

        if (Math.signum(deltaY) * Math.signum(mActionBarAutoHideSignal) < 0) {
            // deltaY is a motion opposite to the accumulated signal, so reset signal
            mActionBarAutoHideSignal = deltaY;
        } else {
            // add to accumulated signal
            mActionBarAutoHideSignal += deltaY;
        }

        boolean shouldShow = currentY < mActionBarAutoHideMinY ||
                (mActionBarAutoHideSignal <= -mActionBarAutoHideSensivity);
        autoShowOrHideActionBar(shouldShow);
    }

    protected Toolbar getActionBarToolbar() {
        if (mActionBarToolbar == null) {
            mActionBarToolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
            if (mActionBarToolbar != null) {
                mActionBarToolbar.setNavigationContentDescription(getResources().getString(R.string
                        .navdrawer_description_a11y));
                setSupportActionBar(mActionBarToolbar);
            }
        }
        return mActionBarToolbar;
    }

    protected void autoShowOrHideActionBar(boolean show) {
        if (show == mActionBarShown) {
            return;
        }

        mActionBarShown = show;
        onActionBarAutoShowOrHide(show);
    }

    protected void enableActionBarAutoHide(final ListView listView) {
        initActionBarAutoHide();
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {

            /** The heights of all items. */
            private Map<Integer, Integer> heights = new HashMap<>();
            private int lastCurrentScrollY = 0;

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                                 int totalItemCount) {

                View firstVisibleItemView = view.getChildAt(0);
                if (firstVisibleItemView == null) {
                    return;
                }

                heights.put(firstVisibleItem, firstVisibleItemView.getHeight());

                int previousItemsHeight = 0;
                for (int i = 0; i < firstVisibleItem; i++) {
                    previousItemsHeight += heights.get(i) != null ? heights.get(i) : 0;
                }

                int currentScrollY = previousItemsHeight - firstVisibleItemView.getTop()
                        + view.getPaddingTop();

                onMainContentScrolled(currentScrollY, currentScrollY - lastCurrentScrollY);

                lastCurrentScrollY = currentScrollY;
            }
        });
    }

    private View makeNavDrawerItem(final int itemId, ViewGroup container) {
        if (isSeparator(itemId)) {
            View separator =
                    getLayoutInflater().inflate(R.layout.navdrawer_separator, container, false);
            UIUtils.setAccessibilityIgnore(separator);
            return separator;
        }

        NavDrawerItemView item = (NavDrawerItemView) getLayoutInflater().inflate(
                R.layout.navdrawer_item, container, false);
        item.setContent(NAVDRAWER_ICON_RES_ID[itemId], NAVDRAWER_TITLE_RES_ID[itemId]);
        item.setActivated(getSelfNavDrawerItem() == itemId);
        if (item.isActivated()) {
            item.setContentDescription(getString(R.string.navdrawer_selected_menu_item_a11y_wrapper,
                    getString(NAVDRAWER_TITLE_RES_ID[itemId])));
        } else {
            item.setContentDescription(getString(R.string.navdrawer_menu_item_a11y_wrapper,
                    getString(NAVDRAWER_TITLE_RES_ID[itemId])));
        }

        item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onNavDrawerItemClicked(itemId);
            }
        });
        return item;
    }

    private boolean isSpecialItem(int itemId) {
        return itemId == NAVDRAWER_ITEM_SETTINGS;
    }

    private boolean isSeparator(int itemId) {
        return itemId == NAVDRAWER_ITEM_SEPARATOR_SPECIAL;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    protected void registerHideableHeaderView(View hideableHeaderView) {
        if (!mHideableHeaderViews.contains(hideableHeaderView)) {
            mHideableHeaderViews.add(hideableHeaderView);
        }
    }

    protected void deregisterHideableHeaderView(View hideableHeaderView) {
        if (mHideableHeaderViews.contains(hideableHeaderView)) {
            mHideableHeaderViews.remove(hideableHeaderView);
        }
    }

    public LUtils getLUtils() {
        return mLUtils;
    }

    public int getThemedStatusBarColor() {
        return mThemedStatusBarColor;
    }

    public void setNormalStatusBarColor(int color) {
        mNormalStatusBarColor = color;
        if (mDrawerLayout != null) {
            mDrawerLayout.setStatusBarBackgroundColor(mNormalStatusBarColor);
        }
    }

    protected void onActionBarAutoShowOrHide(boolean shown) {
        if (mStatusBarColorAnimator != null) {
            mStatusBarColorAnimator.cancel();
        }
        mStatusBarColorAnimator = ObjectAnimator.ofInt(
                (mDrawerLayout != null) ? mDrawerLayout : mLUtils,
                (mDrawerLayout != null) ? "statusBarBackgroundColor" : "statusBarColor",
                shown ? Color.BLACK : mNormalStatusBarColor,
                shown ? mNormalStatusBarColor : Color.BLACK)
                .setDuration(250);
        if (mDrawerLayout != null) {
            mStatusBarColorAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    ViewCompat.postInvalidateOnAnimation(mDrawerLayout);
                }
            });
        }
        mStatusBarColorAnimator.setEvaluator(ARGB_EVALUATOR);
        mStatusBarColorAnimator.start();

        for (final View view : mHideableHeaderViews) {
            if (shown) {
                ViewCompat.animate(view)
                        .translationY(0)
                        .alpha(1)
                        .setDuration(HEADER_HIDE_ANIM_DURATION)
                        .setInterpolator(new DecelerateInterpolator())
                        .withLayer();
            } else {
                ViewCompat.animate(view)
                        .translationY(-view.getBottom())
                        .alpha(0)
                        .setDuration(HEADER_HIDE_ANIM_DURATION)
                        .setInterpolator(new DecelerateInterpolator())
                        .withLayer();
            }
        }
    }

    public PresenterFragmentImpl addPresenterFragment(int updatableViewResId, Model model, QueryEnum[] queries,
                                                      UserActionEnum[] actions) {
        FragmentManager fragmentManager = getFragmentManager();

        PresenterFragmentImpl presenter = (PresenterFragmentImpl) fragmentManager.findFragmentByTag(
                PRESENTER_TAG);
        if (presenter == null) {
            presenter = new PresenterFragmentImpl();
            setUpPresenter(presenter, fragmentManager, updatableViewResId, model, queries, actions);
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.add(presenter, PRESENTER_TAG);
            fragmentTransaction.commit();
        } else {
            setUpPresenter(presenter, fragmentManager, updatableViewResId, model, queries, actions);
        }
        return presenter;
    }

    public void registerPresenterFragmentAsContentObserverForUri
    (PresenterFragmentImpl presenter, Uri uri, QueryEnum[] queries) {
        if (presenter != null) {
            presenter.registerContentObserverOnUri(uri, queries);
        } else {
            Log.e(TAG, "You must add the presenter using addPresenterFragment method before " +
                    "calling registerPresenterFragmentAsContentObserverForUri! Pass in the returned"
                    + " object from addPresenterFragment as first argument.");
        }
    }

    private void setUpPresenter(PresenterFragmentImpl presenter, FragmentManager fragmentManager,
                                int updatableViewResId, Model model, QueryEnum[] queries,
                                UserActionEnum[] actions) {
        UpdatableView ui = (UpdatableView) fragmentManager.findFragmentById(
                updatableViewResId);
        presenter.setModel(model);
        presenter.setUpdatableView(ui);
        presenter.setInitialQueriesToLoad(queries);
        presenter.setValidUserActions(actions);
    }

    protected void setupFloatingWindow(int width, int height, int alpha, float dim) {
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.width = getResources().getDimensionPixelSize(width);
        params.height = getResources().getDimensionPixelSize(height);
        params.alpha = alpha;
        params.dimAmount = dim;
        params.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        getWindow().setAttributes(params);
    }

    protected boolean shouldBeFloatingWindow() {
        Resources.Theme theme = getTheme();
        TypedValue floatingWindowFlag = new TypedValue();

        if (theme == null || !theme
                .resolveAttribute(R.attr.isFloatingWindow, floatingWindowFlag, true)) {
            return false;
        }

        return (floatingWindowFlag.data != 0);
    }
}