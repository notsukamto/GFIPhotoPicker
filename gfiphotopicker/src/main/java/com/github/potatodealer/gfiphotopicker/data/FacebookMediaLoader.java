package com.github.potatodealer.gfiphotopicker.data;


import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.os.Bundle;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;

import com.github.potatodealer.gfiphotopicker.R;

import static com.github.potatodealer.gfiphotopicker.data.FacebookDBHelper.ALL_IMAGE_PROJECTION;
import static com.github.potatodealer.gfiphotopicker.data.FacebookDBHelper.BUCKET_PROJECTION;
import static com.github.potatodealer.gfiphotopicker.data.FacebookDBHelper.BUCKET_SELECTION;
import static com.github.potatodealer.gfiphotopicker.data.FacebookDBHelper.IMAGE_PROJECTION;
import static com.github.potatodealer.gfiphotopicker.data.FacebookProvider.FACEBOOK_ALL_IMAGE_URI;
import static com.github.potatodealer.gfiphotopicker.data.FacebookProvider.FACEBOOK_BUCKET_URI;
import static com.github.potatodealer.gfiphotopicker.data.FacebookProvider.FACEBOOK_IMAGE_URI;

public class FacebookMediaLoader implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int TIME_LOADER = 3;
    private static final int BUCKET_LOADER = 4;
    private static final int MEDIA_LOADER = 5;

    static final long ALL_MEDIA_BUCKET_ID = 0;
    private static final String BUCKET_ID = FacebookDBHelper.BUCKET_ID;

    public static long bucketId;

    public interface Callbacks {

        void onBucketLoadFinished(@Nullable Cursor data);

        void onMediaLoadFinished(@Nullable Cursor data);
    }

    private FragmentActivity mActivity;
    private Callbacks mCallbacks;

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.d("ID", "" + id);
        if (id == TIME_LOADER) {
            return new CursorLoader(mActivity,
                    FACEBOOK_ALL_IMAGE_URI,
                    ALL_IMAGE_PROJECTION,
                    null,
                    null,
                    null);
        }
        if (id == BUCKET_LOADER) {
            return new CursorLoader(mActivity,
                    FACEBOOK_BUCKET_URI,
                    BUCKET_PROJECTION,
                    String.format("%s", BUCKET_SELECTION),
                    null,
                    null);
        }
        // id == MEDIA_LOADER
        bucketId = args.getLong(BUCKET_ID);
        return new CursorLoader(mActivity,
                FACEBOOK_IMAGE_URI,
                IMAGE_PROJECTION,
                String.format("%s=%s", BUCKET_ID, bucketId),
                null,
                null);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, @Nullable Cursor data) {
        if (mCallbacks != null) {
            if (loader.getId() == BUCKET_LOADER) {
                mCallbacks.onBucketLoadFinished(addAllMediaBucketItem(data));
            } else {
                mCallbacks.onMediaLoadFinished(data);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    public void onAttach(@NonNull FragmentActivity activity, @NonNull FacebookMediaLoader.Callbacks callbacks) {
        mActivity = activity;
        mCallbacks = callbacks;
    }

    public void onDetach() {
        mActivity = null;
        mCallbacks = null;
    }

    public void loadBuckets() {
        ensureActivityAttached();
        mActivity.getSupportLoaderManager().restartLoader(BUCKET_LOADER, null, this);
    }

    public void loadByBucket(@IntRange(from = 0) long bucketId) {
        ensureActivityAttached();
        if (ALL_MEDIA_BUCKET_ID == bucketId) {
            mActivity.getSupportLoaderManager().restartLoader(TIME_LOADER, null, this);
        } else {
            Bundle args = new Bundle();
            args.putLong(BUCKET_ID, bucketId);
            mActivity.getSupportLoaderManager().restartLoader(MEDIA_LOADER, args, this);
        }
    }

    /**
     * Ensure that a FragmentActivity is attached to this loader.
     */
    private void ensureActivityAttached() {
        if (mActivity == null) {
            throw new IllegalStateException("The FragmentActivity was not attached!");
        }
    }

    /**
     * Add "All Media" item as the first row of bucket items.
     *
     * @param cursor The original data of all bucket items
     * @return The data with "All Media" item added
     */
    private Cursor addAllMediaBucketItem(@Nullable Cursor cursor) {
        if (cursor == null || !cursor.moveToPosition(0)) {
            return null;
        }
        ensureActivityAttached();
        long id = ALL_MEDIA_BUCKET_ID;
        String label = mActivity.getString(R.string.activity_gallery_bucket_all_media);
        String data = cursor.getString(cursor.getColumnIndex(FacebookDBHelper.DATA));
        MatrixCursor allMediaRow = new MatrixCursor(BUCKET_PROJECTION);
        allMediaRow.newRow()
                .add(id)
                .add(label)
                .add(data);
        return new MergeCursor(new Cursor[]{allMediaRow, cursor});
    }
}