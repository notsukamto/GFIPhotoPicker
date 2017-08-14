package com.github.potatodealer.gfiphotopicker.adapter;

import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.IntDef;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.github.potatodealer.gfiphotopicker.R;
import com.github.potatodealer.gfiphotopicker.util.AnimationHelper;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.LinkedList;
import java.util.List;

/**
 * {@link RecyclerView.Adapter} subclass used to bind {@link Cursor} items from {@link MediaStore} into {@link RecyclerView}
 * <p>
 * We can have two types of {@link View} items: {@link #VIEW_TYPE_BUCKET} or {@link #VIEW_TYPE_MEDIA}
 */
public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.ViewHolder> {

    public static final int VIEW_TYPE_BUCKET = 0;
    public static final int VIEW_TYPE_MEDIA = 1;

    private static final String SELECTION_PAYLOAD = "selection";
    private static final float SELECTED_SCALE = .8f;
    private static final float UNSELECTED_SCALE = 1f;

    @IntDef({VIEW_TYPE_BUCKET, VIEW_TYPE_MEDIA})
    @Retention(RetentionPolicy.SOURCE)
    @interface ViewType {
    }

    public interface Callbacks {

        void onGalleryBucketClick(long bucketId, String label, int position);

        void onGalleryMediaClick(View imageView, View checkView, long bucketId, int position);

        void onSelectionUpdated(int count);

        void onMaxSelectionReached();

        void onWillExceedMaxSelection();

        void onLowResImageSelected();
    }

    private final List<Uri> mSelection;
    private int mSelectionCount;

    @Nullable
    private Callbacks mCallbacks;
    private int mMaxSelection;
    private int mMinWidth;
    private int mMinHeight;
    @Nullable
    private LinearLayoutManager mLayoutManager;
    private int mViewType = VIEW_TYPE_BUCKET;
    @Nullable
    private Cursor mData;

    public GalleryAdapter() {
        mSelection = new LinkedList<>();
        setHasStableIds(true);
    }

    public void setCallbacks(@Nullable Callbacks callbacks) {
        mCallbacks = callbacks;
    }

    public void setMaxSelection(@IntRange(from = 0) int maxSelection) {
        mMaxSelection = maxSelection;
    }

    public void setLayoutManager(@NonNull LinearLayoutManager layoutManager) {
        mLayoutManager = layoutManager;
    }

    public void swapData(@ViewType int viewType, @Nullable Cursor data) {
        if (viewType != mViewType) {
            mViewType = viewType;
        }
        if (data != mData) {
            mData = data;
            notifyDataSetChanged();
        }
    }

    public void updateAllSelectionCount(int selectionCount) {
        mSelectionCount = selectionCount;
    }

    @Override
    public long getItemId(int position) {
        if (mData != null && !mData.isClosed()) {
            mData.moveToPosition(position);
            if (VIEW_TYPE_MEDIA == mViewType) {
                return mData.getLong(mData.getColumnIndex(MediaStore.Images.ImageColumns._ID));
            } else {
                return mData.getLong(mData.getColumnIndex(MediaStore.Images.ImageColumns.BUCKET_ID));
            }
        }
        return super.getItemId(position);
    }

    @Override
    public int getItemCount() {
        if (mData != null && !mData.isClosed()) {
            return mData.getCount();
        }
        return 0;
    }

    @Override
    public int getItemViewType(int position) {
        return mViewType;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, @ViewType int viewType) {
        if (VIEW_TYPE_MEDIA == viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_gallery_media, parent, false);
            return new MediaViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_gallery_bucket, parent, false);
            return new BucketViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull GalleryAdapter.ViewHolder holder, int position) {
        Uri data = getData(position);
        String imageTransitionName = holder.itemView.getContext().getString(R.string.activity_gallery_image_transition, data.toString());
        String checkboxTransitionName = holder.itemView.getContext().getString(R.string.activity_gallery_checkbox_transition, data.toString());
        ViewCompat.setTransitionName(holder.mImageView, imageTransitionName);
        Glide.with(holder.mImageView.getContext())
                .load(data)
                .skipMemoryCache(true)
                .centerCrop()
                .placeholder(R.color.gallery_item_background)
                .into(holder.mImageView);

        boolean selected = isSelected(position);
        if (selected) {
            holder.mImageView.setScaleX(SELECTED_SCALE);
            holder.mImageView.setScaleY(SELECTED_SCALE);
        } else {
            holder.mImageView.setScaleX(UNSELECTED_SCALE);
            holder.mImageView.setScaleY(UNSELECTED_SCALE);
        }

        if (VIEW_TYPE_MEDIA == getItemViewType(position)) {
            MediaViewHolder viewHolder = (MediaViewHolder) holder;
            viewHolder.mTextView.setVisibility(View.INVISIBLE);
            if (checkMinImageResolution(data)) {
                ViewCompat.setTransitionName(viewHolder.mCheckView, checkboxTransitionName);
                viewHolder.mCheckView.setChecked(selected);
                holder.mImageView.setContentDescription(getLabel(position));
            } else {
                viewHolder.mTextView.setVisibility(View.VISIBLE);
                ViewCompat.setTransitionName(viewHolder.mCheckView, checkboxTransitionName);
                viewHolder.mCheckView.setChecked(selected);
                holder.mImageView.setContentDescription(getLabel(position));
            }
        } else {
            BucketViewHolder viewHolder = (BucketViewHolder) holder;
            viewHolder.mTextView.setText(getLabel(position));
        }
    }

    /**
     * Binding view holder with payloads is used to handle partial changes in item.
     */
    @Override
    public void onBindViewHolder(GalleryAdapter.ViewHolder holder, int position, List<Object> payloads) {
        if (payloads.isEmpty()) { // If doesn't have any payload then bind the fully item
            super.onBindViewHolder(holder, position, payloads);
        } else {
            for (Object payload : payloads) {
                boolean selected = isSelected(position);
                if (SELECTION_PAYLOAD.equals(payload)) {
                    if (VIEW_TYPE_MEDIA == getItemViewType(position)) {
                        MediaViewHolder viewHolder = (MediaViewHolder) holder;
                        viewHolder.mCheckView.setChecked(selected);
                        if (selected) {
                            AnimationHelper.scaleView(holder.mImageView, SELECTED_SCALE);
                        } else {
                            AnimationHelper.scaleView(holder.mImageView, UNSELECTED_SCALE);
                        }
                    }
                }
            }
        }
    }

    public List<Uri> getSelection() {
        return new LinkedList<>(mSelection);
    }

    public void setSelection(@NonNull List<Uri> selection) {
        if (!mSelection.equals(selection)) {
            mSelectionCount -= mSelection.size();
            mSelection.clear();
            mSelection.addAll(selection);
            mSelectionCount += mSelection.size();
            notifySelectionChanged();
        }
    }

    public void setMinImageResolution(int minWidth, int minHeight) {
        mMinWidth = minWidth;
        mMinHeight = minHeight;
    }

    public void selectAll() {
        if (mData == null) {
            return;
        }
        List<Uri> selectionToAdd = new LinkedList<>();
        int count = mData.getCount();
        for (int position = 0; position < count; position++) {
            if (!isSelected(position)) {
                Uri data = getData(position);
                selectionToAdd.add(data);
            }
        }
        if (mSelection.size() + selectionToAdd.size() > mMaxSelection) {
            if (mCallbacks != null) {
                mCallbacks.onWillExceedMaxSelection();
            }
        } else {
            mSelection.addAll(selectionToAdd);
            notifySelectionChanged();
        }
    }

    public void clearSelection() {
        if (!mSelection.isEmpty()) {
            mSelection.clear();
            notifySelectionChanged();
        }
    }

    private void notifySelectionChanged() {
        if (mCallbacks != null) {
            mCallbacks.onSelectionUpdated(mSelectionCount);
        }
        int from = 0, count = getItemCount();
        // If we have LinearLayoutManager we should just rebind the visible items
        if (mLayoutManager != null) {
            from = mLayoutManager.findFirstVisibleItemPosition();
            count = mLayoutManager.findLastVisibleItemPosition() - from + 1;
        }
        notifyItemRangeChanged(from, count, SELECTION_PAYLOAD);
    }

    private boolean isSelected(int position) {
        Uri data = getData(position);
        return mSelection.contains(data);
    }

    private String getLabel(int position) {
        assert mData != null; // It is supposed not be null here
        mData.moveToPosition(position);
        if (mViewType == VIEW_TYPE_MEDIA) {
            return mData.getString(mData.getColumnIndex(MediaStore.Images.ImageColumns.DISPLAY_NAME));
        } else {
            return mData.getString(mData.getColumnIndex(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME));
        }
    }

    private Uri getData(int position) {
        assert mData != null; // It is supposed not be null here
        mData.moveToPosition(position);
        return Uri.fromFile(new File(mData.getString(mData.getColumnIndex(MediaStore.Images.Media.DATA))));
    }

    private long getBucketId(int position) {
        assert mData != null; // It is supposed not be null here
        mData.moveToPosition(position);
        return mData.getLong(mData.getColumnIndex(MediaStore.Images.ImageColumns.BUCKET_ID));
    }

    private boolean checkMinImageResolution(Uri imageUri) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(new File(imageUri.getPath()).getAbsolutePath(), options);
        int imageWidth = options.outWidth;
        int imageHeight = options.outHeight;

        return imageWidth >= mMinWidth && imageHeight >= mMinHeight;
    }

    abstract class ViewHolder extends RecyclerView.ViewHolder {

        public final ImageView mImageView;

        private ViewHolder(View itemView) {
            super(itemView);
            mImageView = itemView.findViewById(R.id.image);
        }
    }

    private class BucketViewHolder extends ViewHolder implements View.OnClickListener {

        private final TextView mTextView;

        private BucketViewHolder(View itemView) {
            super(itemView);
            mTextView = itemView.findViewById(R.id.text);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            // getAdapterPosition() returns RecyclerView.NO_POSITION if item has been removed from the adapter,
            // RecyclerView.Adapter.notifyDataSetChanged() has been called after the last layout pass
            // or the ViewHolder has already been recycled.
            if (position == RecyclerView.NO_POSITION) {
                return;
            }

            if (mCallbacks != null) {
                mCallbacks.onGalleryBucketClick(getItemId(), getLabel(position), position);
            }
        }

    }

    public class MediaViewHolder extends ViewHolder implements View.OnClickListener {

        public final CheckedTextView mCheckView;
        private final TextView mTextView;

        private MediaViewHolder(View itemView) {
            super(itemView);
            mCheckView = itemView.findViewById(R.id.check);
            mTextView = itemView.findViewById(R.id.text);
            mTextView.setText(R.string.low_resolution);
            mCheckView.setOnClickListener(this);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            // getAdapterPosition() returns RecyclerView.NO_POSITION if item has been removed from the adapter,
            // RecyclerView.Adapter.notifyDataSetChanged() has been called after the last layout pass
            // or the ViewHolder has already been recycled.
            if (position == RecyclerView.NO_POSITION) {
                return;
            }

            if (v == mCheckView) {
                boolean selectionChanged = handleChangeSelection(position);
                if (selectionChanged) {
                    notifyItemChanged(position, SELECTION_PAYLOAD);
                }
                if (mCallbacks != null) {
                    if (selectionChanged) {
                        mCallbacks.onSelectionUpdated(mSelectionCount);
                    } else {
                        mCallbacks.onMaxSelectionReached();
                    }
                }
            } else {
                if (mCallbacks != null) {
                    mCallbacks.onGalleryMediaClick(mImageView, mCheckView, getBucketId(position), position);
                }
            }
        }

    }

    private boolean handleChangeSelection(int position) {
        Uri data = getData(position);
        if (!isSelected(position)) {
            if (mSelectionCount == mMaxSelection) {
                return false;
            }
            if (!checkMinImageResolution(data)) mCallbacks.onLowResImageSelected();
            mSelection.add(data);
            mSelectionCount++;
        } else {
            mSelection.remove(data);
            mSelectionCount--;
        }
        return true;
    }
}
