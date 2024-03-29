package com.madone.virtualexpo.expoastana.myschedule;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.madone.virtualexpo.expoastana.util.LUtils;
import com.madone.virtualexpo.expoastana.R;
import com.madone.virtualexpo.expoastana.provider.ScheduleContract;
import com.madone.virtualexpo.expoastana.util.UIUtils;
import com.madone.virtualexpo.expoastana.model.ScheduleItem;
import com.madone.virtualexpo.expoastana.util.ImageLoader;
import com.madone.virtualexpo.expoastana.util.TimeUtils;
import com.madone.virtualexpo.expoastana.Config;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MyScheduleAdapter implements ListAdapter, AbsListView.RecyclerListener {
    private static final String TAG = "MyScheduleAdapter";

    private final Context mContext;
    private final LUtils mLUtils;

    ArrayList<ScheduleItem> mItems = new ArrayList<>();

    ArrayList<DataSetObserver> mObservers = new ArrayList<>();

    ImageLoader mImageLoader;

    private final int mHourColorDefault;
    private final int mHourColorPast;
    private final int mTitleColorDefault;
    private final int mTitleColorPast;
    private final int mIconColorDefault;
    private final int mIconColorPast;
    private final int mColorConflict;
    private final int mColorBackgroundDefault;
    private final int mColorBackgroundPast;
    private final int mListSpacing;
    private final int mSelectableItemBackground;
    private final boolean mIsRtl;

    public MyScheduleAdapter(Context context, LUtils lUtils) {
        mContext = context;
        mLUtils = lUtils;
        Resources resources = context.getResources();
        mHourColorDefault = resources.getColor(R.color.my_schedule_hour_header_default);
        mHourColorPast = resources.getColor(R.color.my_schedule_hour_header_finished);
        mTitleColorDefault = resources.getColor(R.color.my_schedule_session_title_default);
        mTitleColorPast = resources.getColor(R.color.my_schedule_session_title_finished);
        mIconColorDefault = resources.getColor(R.color.my_schedule_icon_default);
        mIconColorPast = resources.getColor(R.color.my_schedule_icon_finished);
        mColorConflict = resources.getColor(R.color.my_schedule_conflict);
        mColorBackgroundDefault = resources.getColor(android.R.color.white);
        mColorBackgroundPast = resources.getColor(R.color.my_schedule_past_background);
        mListSpacing = resources.getDimensionPixelOffset(R.dimen.element_spacing_normal);
        TypedArray a = context.obtainStyledAttributes(new int[]{R.attr.selectableItemBackground});
        mSelectableItemBackground = a.getResourceId(0, 0);
        a.recycle();
        mIsRtl = UIUtils.isRtl(context);
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        if (!mObservers.contains(observer)) {
            mObservers.add(observer);
        }
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        if (mObservers.contains(observer)) {
            mObservers.remove(observer);
        }
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public Object getItem(int position) {
        return position >= 0 && position < mItems.size() ? mItems.get(position) : null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    private String formatDescription(ScheduleItem item) {
        StringBuilder description = new StringBuilder();
        description.append(TimeUtils.formatShortTime(mContext, new Date(item.startTime)));
        if (!Config.Tags.SPECIAL_KEYNOTE.equals(item.mainTag)) {
            description.append(" - ");
            description.append(TimeUtils.formatShortTime(mContext, new Date(item.endTime)));
        }
        if (!TextUtils.isEmpty(item.room)) {
            description.append(" / ");
            description.append(item.room);
        }
        return description.toString();
    }

    private View.OnClickListener mUriOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Object tag = v.getTag(R.id.myschedule_uri_tagkey);
            if (tag != null && tag instanceof Uri) {
                Uri uri = (Uri) tag;
                mContext.startActivity(new Intent(Intent.ACTION_VIEW, uri));
            }
        }
    };

    private void setUriClickable(View view, Uri uri) {
        view.setTag(R.id.myschedule_uri_tagkey, uri);
        view.setOnClickListener(mUriOnClickListener);
        view.setBackgroundResource(mSelectableItemBackground);
    }

    private static void clearClickable(View view) {
        view.setOnClickListener(null);
        view.setBackgroundResource(0);
        view.setClickable(false);
    }

    @SuppressLint("RtlHardcoded")
    private void adjustForRtl(ViewHolder holder) {
        if (mIsRtl) {
            holder.startTime.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
            holder.title.setGravity(Gravity.RIGHT);
            holder.description.setGravity(Gravity.RIGHT);
            holder.browse.setGravity(Gravity.RIGHT);
            android.util.Log.d(TAG, "Gravity right");
        }
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        if (mImageLoader == null) {
            mImageLoader = new ImageLoader(mContext);
        }

        ViewHolder holder;
        if (view == null) {
            view = LayoutInflater.from(mContext).inflate(R.layout.my_schedule_item, parent, false);
            holder = new ViewHolder();
            holder.startTime = (TextView) view.findViewById(R.id.start_time);
            holder.icon = (ImageView) view.findViewById(R.id.icon);
            holder.title = (TextView) view.findViewById(R.id.slot_title);
            holder.description = (TextView) view.findViewById(R.id.slot_description);
            holder.browse = (TextView) view.findViewById(R.id.browse_sessions);
            holder.feedback = (Button) view.findViewById(R.id.give_feedback_button);
            holder.separator = view.findViewById(R.id.separator);
            holder.touchArea = view.findViewById(R.id.touch_area);
            view.setTag(holder);
            // Typeface
            mLUtils.setMediumTypeface(holder.startTime);
            mLUtils.setMediumTypeface(holder.browse);
            mLUtils.setMediumTypeface(holder.title);
            adjustForRtl(holder);
        } else {
            holder = (ViewHolder) view.getTag();
            clearClickable(view);
            clearClickable(holder.startTime);
            clearClickable(holder.touchArea);
            holder.description.setTextColor(mHourColorDefault);
        }

        if (position < 0 || position >= mItems.size()) {
            Log.e(TAG, "Invalid view position passed to MyScheduleAdapter: " + position);
            return view;
        }
        final ScheduleItem item = mItems.get(position);
        ScheduleItem nextItem = position < mItems.size() - 1 ? mItems.get(position + 1) : null;

        long now = UIUtils.getCurrentTime();
        boolean isPastDuringConference = item.endTime <= now && now < Config.CONFERENCE_END_MILLIS;

        if (isPastDuringConference) {
            view.setBackgroundColor(mColorBackgroundPast);
            holder.startTime.setTextColor(mHourColorPast);
            holder.title.setTextColor(mTitleColorPast);
            holder.description.setVisibility(View.GONE);
            holder.icon.setColorFilter(mIconColorPast);
        } else {
            view.setBackgroundColor(mColorBackgroundDefault);
            holder.startTime.setTextColor(mHourColorDefault);
            holder.title.setTextColor(mTitleColorDefault);
            holder.description.setVisibility(View.VISIBLE);
            holder.icon.setColorFilter(mIconColorDefault);
        }

        holder.startTime.setText(TimeUtils.formatShortTime(mContext, new Date(item.startTime)));


        holder.touchArea.setTag(R.id.myschedule_uri_tagkey, null);
        if (item.type == ScheduleItem.FREE) {
            holder.startTime.setVisibility(View.VISIBLE);
            holder.feedback.setVisibility(View.GONE);
            holder.title.setVisibility(View.VISIBLE);
            holder.title.setText(item.title);
            holder.icon.setImageResource(UIUtils.getBreakIcon(item.title));
            holder.browse.setVisibility(View.GONE);
            holder.description.setText(formatDescription(item));

        } else if (item.type == ScheduleItem.BREAK) {
            holder.startTime.setVisibility(View.VISIBLE);
            holder.icon.setImageResource(R.drawable.ic_browse);
            holder.feedback.setVisibility(View.GONE);
            holder.title.setVisibility(View.GONE);
            holder.browse.setVisibility(View.VISIBLE);
            setUriClickable(view, ScheduleContract.Sessions.buildUnscheduledSessionsInInterval(
                    item.startTime, item.endTime));
            holder.description.setVisibility(View.GONE);
        } else if (item.type == ScheduleItem.SESSION) {
            if (holder.feedback != null) {
                boolean showFeedbackButton = !item.hasGivenFeedback;
                // Can't use isPastDuringConference because we want to show feedback after the
                // conference too.
                if (showFeedbackButton) {
                    if (item.endTime > now) {
                        // Session hasn't finished yet, don't show button.
                        showFeedbackButton = false;
                    }
                }
                holder.feedback.setVisibility(showFeedbackButton ? View.VISIBLE : View.GONE);
            }
            holder.title.setVisibility(View.VISIBLE);
            holder.title.setText(item.title);
            holder.browse.setVisibility(View.GONE);
            holder.icon.setImageResource(UIUtils.getSessionIcon(item.sessionType));

            Uri sessionUri = ScheduleContract.Sessions.buildSessionUri(item.sessionId);
            if (0 != (item.flags & ScheduleItem.FLAG_CONFLICTS_WITH_PREVIOUS)) {
                holder.startTime.setVisibility(View.GONE);
                holder.description.setTextColor(mColorConflict);
                setUriClickable(holder.touchArea, sessionUri);
            } else {
                holder.startTime.setVisibility(View.VISIBLE);
                setUriClickable(holder.startTime,
                        ScheduleContract.Sessions.buildUnscheduledSessionsInInterval(
                                item.startTime, item.endTime));
                holder.startTime.setPadding(
                        (int) mContext.getResources().getDimension(R.dimen.keyline_2), 0,
                        (int) mContext.getResources().getDimension(R.dimen.keyline_2), 0);
                setUriClickable(holder.touchArea, sessionUri);
                if (0 != (item.flags & ScheduleItem.FLAG_CONFLICTS_WITH_NEXT)) {
                    holder.description.setTextColor(mColorConflict);
                }
            }
            holder.description.setText(formatDescription(item));
        } else {
            Log.e(TAG, "Invalid item type in MyScheduleAdapter: " + item.type);
        }

        holder.separator.setVisibility(nextItem == null ||
                0 != (item.flags & ScheduleItem.FLAG_CONFLICTS_WITH_NEXT) ? View.GONE : View.VISIBLE);

        if (position == 0) { // First item
            view.setPadding(0, mListSpacing, 0, 0);
        } else if (nextItem == null) { // Last item
            view.setPadding(0, 0, 0, mListSpacing);
        } else {
            view.setPadding(0, 0, 0, 0);
        }

        return view;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }


    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return mItems.isEmpty();
    }

    public void clear() {
        updateItems(null);
    }

    private void notifyObservers() {
        for (DataSetObserver observer : mObservers) {
            observer.onChanged();
        }
    }

    public void forceUpdate() {
        notifyObservers();
    }

    public void updateItems(List<ScheduleItem> items) {
        mItems.clear();
        if (items != null) {
            for (ScheduleItem item : items) {
                Log.d(TAG, "Adding schedule item: " + item + " start=" + new Date(item.startTime));
                mItems.add((ScheduleItem) item.clone());
            }
        }
        notifyObservers();
    }

    @Override
    public void onMovedToScrapHeap(View view) {
    }

    private static class ViewHolder {
        public TextView startTime;
        public ImageView icon;
        public TextView title;
        public TextView description;
        public Button feedback;
        public TextView browse;
        public View separator;
        public View touchArea;
    }

}