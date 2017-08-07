package com.github.potatodealer.gfiphotopicker.adapter;


import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.IntDef;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.github.potatodealer.gfiphotopicker.R;
import com.github.potatodealer.gfiphotopicker.data.InstagramDBHelper;
import com.github.potatodealer.gfiphotopicker.util.AnimationHelper;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.LinkedList;
import java.util.List;

/**
 * {@link RecyclerView.Adapter} subclass used to bind {@link Cursor} items from {@link InstagramDBHelper} into {@link RecyclerView}
 * <p>
 * We can have two types of {@link View} items: {@link #VIEW_TYPE_BUCKET} or {@link #VIEW_TYPE_MEDIA}
 */
public class InstagramAdapter extends RecyclerView.Adapter<InstagramAdapter.ViewHolder> {

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

        void onInstagramMediaClick(View imageView, View checkView, int position);

        void onSelectionUpdated(int count);

        void onMaxSelectionReached();

        void onWillExceedMaxSelection();
    }

    private final List<Uri> mSelection;
    private int mSelectionCount;

    @Nullable
    private InstagramAdapter.Callbacks mCallbacks;
    private int mMaxSelection;
    @Nullable
    private LinearLayoutManager mLayoutManager;
    private int mViewType = VIEW_TYPE_MEDIA;
    @Nullable
    private Cursor mData;

    public InstagramAdapter() {
        mSelection = new LinkedList<>();
        setHasStableIds(true);
    }

    public void setCallbacks(@Nullable InstagramAdapter.Callbacks callbacks) {
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
                return mData.getLong(mData.getColumnIndex(InstagramDBHelper._ID));
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
    public InstagramAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, @ViewType int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_gallery_media, parent, false);
        return new InstagramAdapter.MediaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InstagramAdapter.ViewHolder holder, int position) {
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

        InstagramAdapter.MediaViewHolder viewHolder = (InstagramAdapter.MediaViewHolder) holder;
        ViewCompat.setTransitionName(viewHolder.mCheckView, checkboxTransitionName);
        viewHolder.mCheckView.setChecked(selected);
        holder.mImageView.setContentDescription(getLabel(position));
    }

    /**
     * Binding view holder with payloads is used to handle partial changes in item.
     */
    @Override
    public void onBindViewHolder(InstagramAdapter.ViewHolder holder, int position, List<Object> payloads) {
        if (payloads.isEmpty()) { // If doesn't have any payload then bind the fully item
            super.onBindViewHolder(holder, position, payloads);
        } else {
            for (Object payload : payloads) {
                boolean selected = isSelected(position);
                if (SELECTION_PAYLOAD.equals(payload)) {
                    if (VIEW_TYPE_MEDIA == getItemViewType(position)) {
                        InstagramAdapter.MediaViewHolder viewHolder = (InstagramAdapter.MediaViewHolder) holder;
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

    public List<Uri> getInstagramSelection() {
        return new LinkedList<>(mSelection);
    }

    public void setInstagramSelection(@NonNull List<Uri> selection) {
        if (!mSelection.equals(selection)) {
            mSelection.clear();
            mSelection.addAll(selection);
            mSelectionCount = mSelectionCount + mSelection.size();
            notifySelectionChanged();
        }
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

    public void clearInstagramSelection() {
        if (!mSelection.isEmpty()) {
            mSelectionCount -= mSelection.size();
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
            return mData.getString(mData.getColumnIndex(InstagramDBHelper.DISPLAY_NAME));
        } else {
            return mData.getString(mData.getColumnIndex(InstagramDBHelper.DISPLAY_NAME));
        }
    }

    private Uri getData(int position) {
        assert mData != null; // It is supposed not be null here
        mData.moveToPosition(position);
        return Uri.parse(mData.getString(mData.getColumnIndex(InstagramDBHelper.DATA)));
    }

    abstract class ViewHolder extends RecyclerView.ViewHolder {

        public final ImageView mImageView;

        private ViewHolder(View itemView) {
            super(itemView);
            mImageView = itemView.findViewById(R.id.image);
        }
    }

    public class MediaViewHolder extends InstagramAdapter.ViewHolder implements View.OnClickListener {

        public final CheckedTextView mCheckView;

        private MediaViewHolder(View itemView) {
            super(itemView);
            mCheckView = itemView.findViewById(R.id.check);
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
                    mCallbacks.onInstagramMediaClick(mImageView, mCheckView, position);
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
            mSelection.add(data);
            mSelectionCount++;
        } else {
            mSelection.remove(data);
            mSelectionCount--;
        }
        return true;
    }
}