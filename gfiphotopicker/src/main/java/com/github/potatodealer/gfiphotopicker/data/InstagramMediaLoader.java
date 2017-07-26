package com.github.potatodealer.gfiphotopicker.data;


import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

public class InstagramMediaLoader implements LoaderManager.LoaderCallbacks<Cursor> {

    public interface Callbacks {

        void onMediaLoadFinished(@Nullable Cursor data);
    }

    private FragmentActivity mActivity;
    private InstagramMediaLoader.Callbacks mCallbacks;

    public InstagramMediaLoader() {
    }

    @Override
    public final Loader<Cursor> onCreateLoader(int id, Bundle args) {

        return new CursorLoader(mActivity,
                InstagramProvider.CONTENT_URI,
                InstagramDBHelper.ALL_COLUMNS,
                null,
                null,
                null);

    }

    @Override
    public final void onLoadFinished(@NonNull Loader<Cursor> loader, @Nullable Cursor data) {
        if (mCallbacks != null) {
            mCallbacks.onMediaLoadFinished(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // no-op
    }

    public void onAttach(@NonNull FragmentActivity activity, @NonNull InstagramMediaLoader.Callbacks callbacks) {
        mActivity = activity;
        mCallbacks = callbacks;
    }

    public void onDetach() {
        mActivity = null;
        mCallbacks = null;
    }

    public void loadMedias() {
        ensureActivityAttached();
        mActivity.getSupportLoaderManager().restartLoader(0, null, this);
    }

    /**
     * Ensure that a FragmentActivity is attached to this loader.
     */
    private void ensureActivityAttached() {
        if (mActivity == null) {
            throw new IllegalStateException("The FragmentActivity was not attached!");
        }
    }

}
