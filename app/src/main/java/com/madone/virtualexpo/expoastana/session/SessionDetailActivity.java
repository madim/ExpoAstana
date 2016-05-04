package com.madone.virtualexpo.expoastana.session;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import com.madone.virtualexpo.expoastana.BaseActivity;
import com.madone.virtualexpo.expoastana.R;
import com.madone.virtualexpo.expoastana.myschedule.MyScheduleActivity;
import com.madone.virtualexpo.expoastana.util.BeamUtils;
import com.madone.virtualexpo.expoastana.util.SessionsHelper;

public class SessionDetailActivity extends BaseActivity {
    private static final String TAG = "SessionDetailActivity";

    private Handler mHandler = new Handler();

    private Uri mSessionUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        BeamUtils.tryUpdateIntentFromBeam(this);

        boolean shouldBeFloatingWindow = shouldBeFloatingWindow();
        if (shouldBeFloatingWindow) {
            setupFloatingWindow(R.dimen.session_details_floating_width,
                    R.dimen.session_details_floating_height, 1, 0.4f);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.session_detail_act);

        final Toolbar toolbar = getActionBarToolbar();
        toolbar.setNavigationIcon(shouldBeFloatingWindow ? R.drawable.ic_ab_close : R.drawable.ic_up);
        toolbar.setNavigationContentDescription(R.string.close_and_go_back);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                toolbar.setTitle("");
            }
        });

        if (savedInstanceState == null) {
            Uri sessionUri = getIntent().getData();
            BeamUtils.setBeamSessionUri(this, sessionUri);
        }

        mSessionUri = getIntent().getData();

        if (mSessionUri == null) {
            Log.e(TAG, "SessionDetailActivity started with null session Uri!");
            finish();
            return;
        }

        addPresenterFragment(R.id.session_detail_frag, new SessionDetailModel(mSessionUri, getApplicationContext()),
                SessionDetailModel.SessionDetailQueryEnum.values(),
                SessionDetailModel.SessionDetailUserActionEnum.values());
    }

    public Uri getSessionUri() {
        return mSessionUri;
    }

        public Intent getParentActivityIntent() {
            return new Intent(this, MyScheduleActivity.class);
        }
}