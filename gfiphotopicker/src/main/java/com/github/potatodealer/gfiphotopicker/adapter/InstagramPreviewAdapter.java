package com.github.potatodealer.gfiphotopicker.adapter;


import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.ImageView;

import com.bumptech.glide.DrawableRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.github.potatodealer.gfiphotopicker.R;
import com.github.potatodealer.gfiphotopicker.data.InstagramDBHelper;
import com.github.potatodealer.gfiphotopicker.util.transition.MediaSharedElementCallback;

import java.util.LinkedList;
import java.util.List;

import static android.view.View.NO_ID;

public class InstagramPreviewAdapter extends PagerAdapter {

    public interface Callbacks {

        void onCheckedUpdated(boolean checked);

        void onMaxSelectionReached();
    }

    private final FragmentActivity mActivity;
    private final LayoutInflater mInflater;
    private final CheckedTextView mCheckbox;
    private final MediaSharedElementCallback mSharedElementCallback;
    private final List<Uri> mSelection;
    @Nullable
    private InstagramPreviewAdapter.Callbacks mCallbacks;
    private int mMaxSelection;
    private int mInitialPosition;
    @Nullable
    private Cursor mData;
    private boolean mDontAnimate;
    private int mCurrentPosition = RecyclerView.NO_POSITION;

    public InstagramPreviewAdapter(@NonNull FragmentActivity activity, @NonNull CheckedTextView checkbox, @NonNull MediaSharedElementCallback sharedElementCallback, @NonNull List<Uri> selection) {
        mActivity = activity;
        mInflater = LayoutInflater.from(activity);
        mCheckbox = checkbox;
        mSharedElementCallback = sharedElementCallback;
        mSelection = selection;
        mDontAnimate = true;
    }

    public void setCallbacks(@Nullable InstagramPreviewAdapter.Callbacks callbacks) {
        mCallbacks = callbacks;
    }

    public void setMaxSelection(@IntRange(from = 0) int maxSelection) {
        mMaxSelection = maxSelection;
    }

    public void setInitialPosition(int position) {
        mInitialPosition = position;
    }

    public void swapData(Cursor data) {
        if (data != mData) {
            mData = data;
            notifyDataSetChanged();
        }
    }

    public void setDontAnimate(boolean dontAnimate) {
        mDontAnimate = dontAnimate;
    }

    @Override
    public int getCount() {
        if (mData != null && !mData.isClosed()) {
            return mData.getCount();
        }
        return 0;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View view = mInflater.inflate(R.layout.page_item_preview, container, false);
        InstagramPreviewAdapter.ViewHolder holder = new InstagramPreviewAdapter.ViewHolder(view);
        Uri data = getData(position);
        onViewBound(holder, position, data);
        container.addView(holder.itemView);
        return holder;
    }

    @Nullable
    public Uri getData(int position) {
        if (mData != null && !mData.isClosed()) {
            mData.moveToPosition(position);
            return Uri.parse(mData.getString(mData.getColumnIndex(InstagramDBHelper.DATA)));
        }
        return null;
    }

    private long getItemId(int position) {
        if (mData != null && !mData.isClosed()) {
            mData.moveToPosition(position);
            return mData.getLong(mData.getColumnIndex(InstagramDBHelper._ID));
        }
        return NO_ID;
    }

    private void onViewBound(InstagramPreviewAdapter.ViewHolder holder, int position, Uri data) {
        String imageTransitionName = holder.imageView.getContext().getString(R.string.activity_gallery_image_transition, data.toString());
        ViewCompat.setTransitionName(holder.imageView, imageTransitionName);
        Log.d("Preview", "Loaded Image Data " + data);
        DrawableRequestBuilder<Uri> request = Glide.with(mActivity)
                .load(data)
                .skipMemoryCache(true)
                .fitCenter()
                .listener(new InstagramPreviewAdapter.ImageLoadingCallback(position));
        if (mDontAnimate) {
            request.dontAnimate();
        }
        request.into(holder.imageView);
    }

    private boolean isSelected(int position) {
        Uri data = getData(position);
        return mSelection.contains(data);
    }

    private void startPostponedEnterTransition(int position) {
        if (position == mInitialPosition) {
            mActivity.supportStartPostponedEnterTransition();
        }
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        if (object instanceof InstagramPreviewAdapter.ViewHolder) {
            mCurrentPosition = position;
            mSharedElementCallback.setSharedElementViews(((ViewHolder) object).imageView, mCheckbox);
            if (mCallbacks != null) {
                mCallbacks.onCheckedUpdated(isSelected(position));
            }
        }
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return object instanceof InstagramPreviewAdapter.ViewHolder
                && view.equals(((InstagramPreviewAdapter.ViewHolder) object).itemView);
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        View view = ((InstagramPreviewAdapter.ViewHolder) object).itemView;
        container.removeView(view);
    }

    public void selectCurrentItem() {
        boolean selectionChanged = handleChangeSelection(mCurrentPosition);
        if (selectionChanged) {
            notifyDataSetChanged();
        }
        if (mCallbacks != null) {
            if (selectionChanged) {
                mCallbacks.onCheckedUpdated(isSelected(mCurrentPosition));
            } else {
                mCallbacks.onMaxSelectionReached();
            }
        }
    }

    public List<Uri> getSelection() {
        return new LinkedList<>(mSelection);
    }

    private boolean handleChangeSelection(int position) {
        Uri data = getData(position);
        if (!isSelected(position)) {
            if (mSelection.size() == mMaxSelection) {
                return false;
            }
            mSelection.add(data);
        } else {
            mSelection.remove(data);
        }
        return true;
    }

    private static class ViewHolder {

        final View itemView;
        final ImageView imageView;

        ViewHolder(View view) {
            itemView = view;
            imageView = view.findViewById(R.id.image);
        }

    }

    private class ImageLoadingCallback implements RequestListener<Uri, GlideDrawable> {

        final int mPosition;

        ImageLoadingCallback(int position) {
            mPosition = position;
        }

        @Override
        public boolean onResourceReady(GlideDrawable resource, Uri model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
            startPostponedEnterTransition(mPosition);
            return false;
        }

        @Override
        public boolean onException(Exception e, Uri model, Target<GlideDrawable> target, boolean isFirstResource) {
            startPostponedEnterTransition(mPosition);
            return false;
        }
    }
}
