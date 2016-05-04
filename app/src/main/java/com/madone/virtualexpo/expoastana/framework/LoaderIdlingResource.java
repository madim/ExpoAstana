package com.madone.virtualexpo.expoastana.framework;
import com.bumptech.glide.request.ResourceCallback;

import android.app.LoaderManager;
import android.content.Loader;
import android.support.test.espresso.IdlingResource;

import java.util.HashSet;
import java.util.Set;

public class LoaderIdlingResource implements IdlingResource {

    private ResourceCallback mResourceCallback;

    private Set<Integer> mLoadersLoading = new HashSet<>();

    private final String mName;

    private final LoaderManager mLoaderManager;

    public LoaderIdlingResource(String name, LoaderManager loaderManager) {
        mName = name;
        mLoaderManager = loaderManager;
    }

    @Override
    public String getName() {
        return mName;
    }

    @Override
    public boolean isIdleNow() {
        cleanupLoaders();
        return mLoadersLoading.isEmpty();
    }

    private void cleanupLoaders() {
        for (int loaderId : mLoadersLoading) {
            Loader loader = mLoaderManager.getLoader(loaderId);
            if (loader == null) {
                mLoadersLoading.remove(loaderId);
            }
        }
    }

    public void onLoaderStarted(Loader loader) {
        mLoadersLoading.add(loader.getId());
    }

    public void onLoaderFinished(Loader loader) {
        mLoadersLoading.remove(loader.getId());
        if (isIdleNow() && mResourceCallback != null) {
            mResourceCallback.onTransitionToIdle();
        }
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback resourceCallback) {
        mResourceCallback = resourceCallback;
    }
}