package com.madone.virtualexpo.expoastana.framework;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.common.annotations.VisibleForTesting;
import com.madone.virtualexpo.expoastana.util.ThrottledContentObserver;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static com.google.common.base.Preconditions.checkState;

public class PresenterFragmentImpl extends Fragment
        implements Presenter, UpdatableView.UserActionListener,
        LoaderManager.LoaderCallbacks<Cursor> {

    public static final String KEY_RUN_QUERY_ID = "RUN_QUERY_ID";

    private static final String TAG = "PresenterFragmentImpl";

    private UpdatableView mUpdatableView;

    private Model mModel;

    private QueryEnum[] mInitialQueriesToLoad;

    private UserActionEnum[] mValidUserActions;

    private LoaderIdlingResource mLoaderIdlingResource;

    private HashMap<Uri, ThrottledContentObserver> mContentObservers;

    public LoaderIdlingResource getLoaderIdlingResource() {
        return mLoaderIdlingResource;
    }

    @Override
    public void setModel(Model  model) {
        mModel = model;
    }

    @Override
    public void setUpdatableView(UpdatableView view) {
        mUpdatableView = view;
        mUpdatableView.addListener(this);
    }

    @Override
    public void setInitialQueriesToLoad(QueryEnum[] queries) {
        mInitialQueriesToLoad = queries;
    }

    @Override
    public void setValidUserActions(UserActionEnum[] actions) {
        mValidUserActions = actions;
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (mContentObservers != null) {
            Iterator<Map.Entry<Uri, ThrottledContentObserver>> observers =
                    mContentObservers.entrySet().iterator();
            while (observers.hasNext()) {
                Map.Entry<Uri, ThrottledContentObserver> entry = observers.next();
                activity.getContentResolver().registerContentObserver(
                        entry.getKey(), true, entry.getValue());
            }
        }
    }


    @Override
    public void onDetach() {
        super.onDetach();
        cleanUp();
    }

    @Override
    public void cleanUp() {
        mUpdatableView = null;
        mModel = null;
        if (mContentObservers != null) {
            Iterator<ThrottledContentObserver> observers = mContentObservers.values().iterator();
            while (observers.hasNext()) {
                getActivity().getContentResolver().unregisterContentObserver(observers.next());
            }
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mLoaderIdlingResource =
                new LoaderIdlingResource(getClass().getName() + "/" + getId(), getLoaderManager());

        if (mInitialQueriesToLoad != null && mInitialQueriesToLoad.length > 0) {
            LoaderManager manager = getLoaderManager();
            for (int i = 0; i < mInitialQueriesToLoad.length; i++) {
                manager.initLoader(mInitialQueriesToLoad[i].getId(), null, this);
            }
        } else {
            mUpdatableView.displayData(mModel, null);
        }
    }

    @Override
    public Context getContext() {
        return mUpdatableView.getContext();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Loader<Cursor> cursorLoader = createLoader(id, args);
        mLoaderIdlingResource.onLoaderStarted(cursorLoader);
        return cursorLoader;
    }

    @VisibleForTesting
    protected Loader<Cursor> createLoader(int id, Bundle args) {
        Uri uri = mUpdatableView.getDataUri(QueryEnumHelper.getQueryForId(id, mModel.getQueries()));
        return mModel.createCursorLoader(id, uri, args);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        processData(loader, data);
        mLoaderIdlingResource.onLoaderFinished(loader);
    }

    @VisibleForTesting
    protected void processData(Loader<Cursor> loader, Cursor data) {
        QueryEnum query = QueryEnumHelper.getQueryForId(loader.getId(), mModel.getQueries());
        boolean successfulDataRead = mModel.readDataFromCursor(data, query);
        if (successfulDataRead) {
            mUpdatableView.displayData(mModel, query);
        } else {
            mUpdatableView.displayErrorMessage(query);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mLoaderIdlingResource.onLoaderFinished(loader);
    }

    @Override
    public void onUserAction(UserActionEnum action, @Nullable Bundle args) {
        boolean isValid = false;
        if (mValidUserActions != null && mValidUserActions.length > 0 && action != null) {
            for (int i = 0; i < mValidUserActions.length; i++) {
                if (mValidUserActions[i].getId() == action.getId()) {
                    isValid = true;
                }
            }
        }
        if (isValid) {
            if (args != null && args.containsKey(KEY_RUN_QUERY_ID)) {
                Object queryId = args.get(KEY_RUN_QUERY_ID);
                if (queryId instanceof Integer) {
                    LoaderManager manager = getLoaderManager();
                    manager.restartLoader((Integer) queryId, args, this);
                } else {
                    Log.e(TAG, "onUserAction called with a bundle containing KEY_RUN_QUERY_ID but"
                            + "the value is not an Integer so it's not a valid query id!");
                }
            }
            boolean success = mModel.requestModelUpdate(action, args);
            if (!success) {
                Log.e(TAG, "Model doesn't implement user action " + action.getId() + ". Have you "
                        + "forgotten to implement this UserActionEnum in your model, or have you "
                        + "called setValidUserActions on your presenter with a UserActionEnum that "
                        + "it shouldn't support?");
            }
        } else {
            Log.e(TAG, "Invalid user action " + action.getId() + ". Have you called "
                    + "setValidUserActions on your presenter, with all the UserActionEnum you want "
                    + "to support?");
        }
    }

    public void registerContentObserverOnUri(Uri uri, final QueryEnum[] queriesToRun) {
        checkState(queriesToRun != null && queriesToRun.length > 0, "Error registering content " +
                "observer on uri " + uri + ", you must specify at least one query to run");

        if (mContentObservers == null) {
            mContentObservers = new HashMap<Uri, ThrottledContentObserver>();
        }
        if (!mContentObservers.containsKey(uri)) {

            ThrottledContentObserver observer =
                    new ThrottledContentObserver(new ThrottledContentObserver.Callbacks() {
                        @Override
                        public void onThrottledContentObserverFired() {
                            onObservedContentChanged(queriesToRun);
                        }
                    });
            mContentObservers.put(uri, observer);

        } else {
            Log.e(TAG, "This presenter is already registered as a content observer for uri " + uri
                    + ", ignoring this call to registerContentObserverOnUri");
        }
    }

    private void onObservedContentChanged(QueryEnum[] queriesToRun) {
        for (int i = 0; i < queriesToRun.length; i++) {
            getLoaderManager().initLoader(queriesToRun[i].getId(), null, this);
        }
    }

    @VisibleForTesting
    public Model getModel() {
        return mModel;
    }

    @VisibleForTesting
    public QueryEnum[] getInitialQueriesToLoad() {
        return mInitialQueriesToLoad;
    }

    @VisibleForTesting
    public UserActionEnum[] getValidUserActions() {
        return mValidUserActions;
    }
}