package com.madone.virtualexpo.expoastana.explore;

import com.madone.virtualexpo.expoastana.R;
import com.madone.virtualexpo.expoastana.explore.ExploreModel.ExploreQueryEnum;
import com.madone.virtualexpo.expoastana.explore.ExploreModel.ExploreUserActionEnum;
import com.madone.virtualexpo.expoastana.explore.data.ItemGroup;
import com.madone.virtualexpo.expoastana.explore.data.SessionData;
import com.madone.virtualexpo.expoastana.framework.QueryEnum;
import com.madone.virtualexpo.expoastana.provider.ScheduleContract;
// import com.madone.virtualexpo.expoastana.session.SessionDetailActivity;
import com.madone.virtualexpo.expoastana.BaseActivity;
import com.madone.virtualexpo.expoastana.session.SessionDetailActivity;
import com.madone.virtualexpo.expoastana.ui.widget.CollectionView;
import com.madone.virtualexpo.expoastana.ui.widget.DrawShadowFrameLayout;

import com.madone.virtualexpo.expoastana.util.UIUtils;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

public class ExploreExpoActivity extends BaseActivity {

    private static final String TAG = "ExploreExpoActivity";

    private static final String SCREEN_LABEL = "Explore Expo";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.explore_expo_act);
        addPresenterFragment(
                R.id.explore_library_frag,
                new ExploreModel(
                        getApplicationContext()),
                new QueryEnum[]{
                        ExploreQueryEnum.SESSIONS,
                        ExploreQueryEnum.TAGS},
                new ExploreUserActionEnum[]{
                        ExploreUserActionEnum.RELOAD});

        registerHideableHeaderView(findViewById(R.id.headerbar));
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        enableActionBarAutoHide((CollectionView) findViewById(R.id.explore_collection_view));
    }

    @Override
    protected int getSelfNavDrawerItem() {
        return NAVDRAWER_ITEM_EXPLORE;
    }

    @Override
    protected void onActionBarAutoShowOrHide(boolean shown) {
        super.onActionBarAutoShowOrHide(shown);
        DrawShadowFrameLayout frame = (DrawShadowFrameLayout) findViewById(R.id.main_content);
        frame.setShadowVisible(shown, shown);
    }

    public void sessionDetailItemClicked(View viewClicked) {
        Log.d(TAG, "clicked: " + viewClicked + " " +
                ((viewClicked != null) ? viewClicked.getTag() : ""));
        Object tag = null;
        if (viewClicked != null) {
            tag = viewClicked.getTag();
        }
        if (tag instanceof SessionData) {
            SessionData sessionData = (SessionData)viewClicked.getTag();
            if (!TextUtils.isEmpty(sessionData.getSessionId())) {
                Intent intent = new Intent(getApplicationContext(), SessionDetailActivity.class);
                Uri sessionUri = ScheduleContract.Sessions.buildSessionUri(sessionData.getSessionId());
                intent.setData(sessionUri);
                startActivity(intent);
            } else {
                Log.e(TAG, "Theme item clicked but session data was null:" + sessionData);
            }
        }
    }

    public void cardHeaderClicked(View viewClicked) {
        Log.d(TAG, "clicked: " + viewClicked + " " +
                ((viewClicked != null) ? viewClicked.getTag() : ""));

        View moreButton = viewClicked.findViewById(android.R.id.button1);
        Object tag = moreButton != null ? moreButton.getTag() : null;
        Intent intent = new Intent(getApplicationContext(), ExploreSessionsActivity.class);
        if (tag instanceof ItemGroup) {
            intent.putExtra(ExploreSessionsActivity.EXTRA_FILTER_TAG, ((ItemGroup)tag).getId());
        }
        startActivity(intent);
    }

}