package com.madone.virtualexpo.expoastana.myschedule;

import com.madone.virtualexpo.expoastana.model.ScheduleHelper;
import com.madone.virtualexpo.expoastana.Config;
import com.madone.virtualexpo.expoastana.BaseActivity;
import com.madone.virtualexpo.expoastana.settings.SettingsUtils;
import com.madone.virtualexpo.expoastana.util.ThrottledContentObserver;
import com.madone.virtualexpo.expoastana.R;
import com.madone.virtualexpo.expoastana.provider.ScheduleContract;
import com.madone.virtualexpo.expoastana.model.ScheduleItem;
import com.madone.virtualexpo.expoastana.util.UIUtils;
import com.madone.virtualexpo.expoastana.util.TimeUtils;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ListFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class MyScheduleActivity extends BaseActivity implements MyScheduleFragment.Listener {

    private static final long INTERVAL_TO_REDRAW_UI = 60 * 1000L;

    public static final String PREF_LOCAL_TIMES = "pref_local_times";

    private static final String SCREEN_LABEL = "My Schedule";
    private static final String TAG = "MyScheduleActivity";

    private MyScheduleAdapter[] mScheduleAdapters = new MyScheduleAdapter[
            Config.CONFERENCE_DAYS.length];

    private MyScheduleAdapter mDayZeroAdapter;

    private ScheduleHelper mDataHelper;

    ViewPager mViewPager = null;
    OurViewPagerAdapter mViewPagerAdapter = null;
    TabLayout mTabLayout = null;
    ScrollView mScrollViewWide;

    boolean mDestroyed = false;

    private static final String ARG_CONFERENCE_DAY_INDEX
            = "com.google.samples.apps.iosched.ARG_CONFERENCE_DAY_INDEX";

    private Set<MyScheduleFragment> mMyScheduleFragments = new HashSet<MyScheduleFragment>();

    public static final String EXTRA_DIALOG_TITLE
            = "com.google.samples.apps.iosched.EXTRA_DIALOG_TITLE";
    public static final String EXTRA_DIALOG_MESSAGE
            = "com.google.samples.apps.iosched.EXTRA_DIALOG_MESSAGE";
    public static final String EXTRA_DIALOG_YES
            = "com.google.samples.apps.iosched.EXTRA_DIALOG_YES";
    public static final String EXTRA_DIALOG_NO
            = "com.google.samples.apps.iosched.EXTRA_DIALOG_NO";
    public static final String EXTRA_DIALOG_URL
            = "com.google.samples.apps.iosched.EXTRA_DIALOG_URL";

    private boolean mShowedAnnouncementDialog = false;

    private int baseTabViewId = 12345;

    private int mViewPagerScrollState = ViewPager.SCROLL_STATE_IDLE;

    public MyScheduleActivity() {
        mDataHelper = new ScheduleHelper(this);
    }

    @Override
    protected int getSelfNavDrawerItem() {
        return NAVDRAWER_ITEM_MY_SCHEDULE;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_schedule);

        mViewPager = (ViewPager) findViewById(R.id.view_pager);

        mDayZeroAdapter = new MyScheduleAdapter(this, getLUtils());
        prepareDayZeroAdapter();

        for (int i = 0; i < Config.CONFERENCE_DAYS.length; i++) {
            mScheduleAdapters[i] = new MyScheduleAdapter(this, getLUtils());
        }

        mViewPagerAdapter = new OurViewPagerAdapter(getFragmentManager());
        mViewPager.setAdapter(mViewPagerAdapter);

        mTabLayout = (TabLayout) findViewById(R.id.sliding_tabs);

        mTabLayout.setTabsFromPagerAdapter(mViewPagerAdapter);

        mTabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mViewPager.setCurrentItem(tab.getPosition(), true);
                TextView view = (TextView) findViewById(baseTabViewId + tab.getPosition());
                view.setContentDescription(
                        getString(R.string.talkback_selected,
                                getString(R.string.a11y_button, tab.getText())));
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                TextView view = (TextView) findViewById(baseTabViewId + tab.getPosition());
                view.setContentDescription(
                        getString(R.string.a11y_button, tab.getText()));
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Do nothing
            }
        });
        mViewPager.setPageMargin(getResources()
                    .getDimensionPixelSize(R.dimen.my_schedule_page_margin));
        mViewPager.setPageMarginDrawable(R.drawable.page_margin);

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                TabLayout.Tab tab = mTabLayout.getTabAt(position);
                tab.select();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        setTabLayoutContentDescriptions();

        overridePendingTransition(0, 0);
        addDataObservers();
    }

    private void prepareDayZeroAdapter() {
        ScheduleItem item = new ScheduleItem();
        item.title = "Badge Pick-Up";
        item.startTime = 1432742400000l; // 2015/05/27 9:00 AM PST
        item.endTime = 1432782000000l; // 2015/05/27 8:00 PM PST
        item.type = ScheduleItem.FREE;
        item.room = item.subtitle = "Registration Desk";
        item.sessionType = ScheduleItem.SESSION_TYPE_MISC;
        mDayZeroAdapter.updateItems(Arrays.asList(item));
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (mViewPager != null) {
            long now = UIUtils.getCurrentTime();
            selectDay(0);
            for (int i = 0; i < Config.CONFERENCE_DAYS.length; i++) {
                if (now >= Config.CONFERENCE_DAYS[i][0] && now <= Config.CONFERENCE_DAYS[i][1]) {
                    selectDay(i);
                    break;
                }
            }
        }
        setProgressBarTopWhenActionBarShown((int)
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2,
                        getResources().getDisplayMetrics()));
    }

    private void selectDay(int day) {
        int gap = mDayZeroAdapter != null ? 1 : 0;
        mViewPager.setCurrentItem(day + gap);
        setTimerToUpdateUI(day + gap);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent, extras " + intent.getExtras());
        if (intent.hasExtra(EXTRA_DIALOG_MESSAGE)) {
            mShowedAnnouncementDialog = false;
            showAnnouncementDialogIfNeeded(intent);
        }
    }

    private String getDayName(int position) {
        long day1Start = Config.CONFERENCE_DAYS[0][0];
        long day = 1000 * 60 * 60 * 24;
        return TimeUtils.formatShortDate(this, new Date(day1Start + day * position));
    }

    private void setTabLayoutContentDescriptions() {
        LayoutInflater inflater = getLayoutInflater();
        int gap = mDayZeroAdapter == null ? 0 : 1;
        for (int i = 0, count = mTabLayout.getTabCount(); i < count; i++) {
            TabLayout.Tab tab = mTabLayout.getTabAt(i);
            TextView view = (TextView) inflater.inflate(R.layout.tab_my_schedule, mTabLayout, false);
            view.setId(baseTabViewId + i);
            view.setText(tab.getText());
            if (i == 0) {
                view.setContentDescription(
                        getString(R.string.talkback_selected,
                                getString(R.string.a11y_button, tab.getText())));
            } else {
                view.setContentDescription(
                        getString(R.string.a11y_button, tab.getText()));
            }
            tab.setCustomView(view);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateData();
        showAnnouncementDialogIfNeeded(getIntent());
    }

    private void showAnnouncementDialogIfNeeded(Intent intent) {
        final String title = intent.getStringExtra(EXTRA_DIALOG_TITLE);
        final String message = intent.getStringExtra(EXTRA_DIALOG_MESSAGE);

        if (!mShowedAnnouncementDialog && !TextUtils.isEmpty(title) && !TextUtils
                .isEmpty(message)) {
            Log.d(TAG, "showAnnouncementDialogIfNeeded, title: " + title);
            Log.d(TAG, "showAnnouncementDialogIfNeeded, message: " + message);
            final String yes = intent.getStringExtra(EXTRA_DIALOG_YES);
            Log.d(TAG, "showAnnouncementDialogIfNeeded, yes: " + yes);
            final String no = intent.getStringExtra(EXTRA_DIALOG_NO);
            Log.d(TAG, "showAnnouncementDialogIfNeeded, no: " + no);
            final String url = intent.getStringExtra(EXTRA_DIALOG_URL);
            Log.d(TAG, "showAnnouncementDialogIfNeeded, url: " + url);
            final SpannableString spannable = new SpannableString(message == null ? "" : message);
            Linkify.addLinks(spannable, Linkify.WEB_URLS);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            if (!TextUtils.isEmpty(title)) {
                builder.setTitle(title);
            }
            builder.setMessage(spannable);
            if (!TextUtils.isEmpty(no)) {
                builder.setNegativeButton(no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
            }
            if (!TextUtils.isEmpty(yes)) {
                builder.setPositiveButton(yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                    }
                });
            }
            final AlertDialog dialog = builder.create();
            dialog.show();
            final TextView messageView = (TextView) dialog.findViewById(android.R.id.message);
            if (messageView != null) {
                messageView.setMovementMethod(LinkMovementMethod.getInstance());
            }
            mShowedAnnouncementDialog = true;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDestroyed = true;
        removeDataObservers();
    }

    protected void updateData() {
        for (int i = 0; i < Config.CONFERENCE_DAYS.length; i++) {
            mDataHelper.getScheduleDataAsync(mScheduleAdapters[i],
                    Config.CONFERENCE_DAYS[i][0], Config.CONFERENCE_DAYS[i][1]);
        }
    }

    @Override
    public void onFragmentViewCreated(ListFragment fragment) {
        fragment.getListView().addHeaderView(
                getLayoutInflater().inflate(R.layout.reserve_action_bar_space_header_view, null));
        int dayIndex = fragment.getArguments().getInt(ARG_CONFERENCE_DAY_INDEX, 0);
        if (dayIndex < 0) {
            fragment.setListAdapter(mDayZeroAdapter);
            fragment.getListView().setRecyclerListener(mDayZeroAdapter);
        } else {
            fragment.setListAdapter(mScheduleAdapters[dayIndex]);
            fragment.getListView().setRecyclerListener(mScheduleAdapters[dayIndex]);
        }
    }

    @Override
    public void onFragmentAttached(MyScheduleFragment fragment) {
        mMyScheduleFragments.add(fragment);
    }

    @Override
    public void onFragmentDetached(MyScheduleFragment fragment) {
        mMyScheduleFragments.remove(fragment);
    }

    private class OurViewPagerAdapter extends FragmentPagerAdapter {

        public OurViewPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Log.d(TAG, "Creating fragment #" + position);
            if (mDayZeroAdapter != null) {
                position--;
            }
            MyScheduleFragment frag = new MyScheduleFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_CONFERENCE_DAY_INDEX, position);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public int getCount() {
            return Config.CONFERENCE_DAYS.length + (mDayZeroAdapter == null ? 0 : 1);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return getDayName(position - (mDayZeroAdapter == null ? 0 : 1));
        }
    }

    protected void addDataObservers() {
        getContentResolver().registerContentObserver(
                ScheduleContract.BASE_CONTENT_URI, true, mObserver);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.registerOnSharedPreferenceChangeListener(mPrefChangeListener);
    }

    public void removeDataObservers() {
        getContentResolver().unregisterContentObserver(mObserver);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.unregisterOnSharedPreferenceChangeListener(mPrefChangeListener);
    }

    private final SharedPreferences.OnSharedPreferenceChangeListener mPrefChangeListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
                    Log.d(TAG, "sharedpreferences key " + key + " changed, maybe reloading data.");
                    for (MyScheduleAdapter adapter : mScheduleAdapters) {
                        if (PREF_LOCAL_TIMES.equals(key)) {
                            adapter.forceUpdate();
                        }
                    }
                }
            };

    private final ContentObserver mObserver = new ThrottledContentObserver(
            new ThrottledContentObserver.Callbacks() {
                @Override
                public void onThrottledContentObserverFired() {
                    Log.d(TAG, "content may be changed, reloading data");
                    Toast.makeText(MyScheduleActivity.this, "content may be changed, reloading data", Toast.LENGTH_LONG).show();
                    updateData();
                }
            });

    private void setTimerToUpdateUI(final int today) {
        new UpdateUIRunnable(this, today, new Handler()).scheduleNextRun();
    }

    boolean hasBeenDestroyed() {
        return mDestroyed;
    }

    static final class UpdateUIRunnable implements Runnable {

        final WeakReference<MyScheduleActivity> weakRefToParent;
        final Handler handler;
        final int today;

        public UpdateUIRunnable(MyScheduleActivity activity, int today, Handler handler) {
            weakRefToParent = new WeakReference<MyScheduleActivity>(activity);
            this.handler = handler;
            this.today = today;
        }

        public void scheduleNextRun() {
            handler.postDelayed(this, INTERVAL_TO_REDRAW_UI);
        }

        @Override
        public void run() {
            MyScheduleActivity activity = weakRefToParent.get();
            if (activity == null || activity.hasBeenDestroyed()) {
                Log.d(TAG, "Ativity is not valid anymore. Stopping UI Updater");
                return;
            }
            Log.d(TAG, "Running MySchedule UI updater (now=" +
                    new Date(UIUtils.getCurrentTime()) + ")");
            if (activity.mScheduleAdapters != null
                    && activity.mScheduleAdapters.length > today
                    && activity.mScheduleAdapters[today] != null) {
                try {
                    activity.mScheduleAdapters[today].forceUpdate();
                } finally {
                    this.scheduleNextRun();
                }
            }
        }
    }

}