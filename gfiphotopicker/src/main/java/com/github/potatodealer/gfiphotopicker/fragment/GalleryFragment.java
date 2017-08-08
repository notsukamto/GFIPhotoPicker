package com.github.potatodealer.gfiphotopicker.fragment;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.SharedElementCallback;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.transition.Transition;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import com.github.potatodealer.gfiphotopicker.R;
import com.github.potatodealer.gfiphotopicker.activity.GalleryPreviewActivity;
import com.github.potatodealer.gfiphotopicker.activity.PhotoPickerActivity;
import com.github.potatodealer.gfiphotopicker.adapter.GalleryAdapter;
import com.github.potatodealer.gfiphotopicker.data.GalleryMediaLoader;
import com.github.potatodealer.gfiphotopicker.util.ItemOffsetDecoration;
import com.github.potatodealer.gfiphotopicker.util.transition.MediaSharedElementCallback;
import com.github.potatodealer.gfiphotopicker.util.transition.TransitionCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link GalleryFragment.Callbacks} interface
 * to handle interaction events.
 */
public class GalleryFragment extends Fragment implements GalleryMediaLoader.Callbacks, GalleryAdapter.Callbacks {

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface Callbacks {

        void onGalleryMediaClick(@NonNull View imageView, View checkView, long bucketId, int position);

        void onSelectionUpdated(int count);

        void onMaxSelectionReached();

        void onWillExceedMaxSelection();
    }

    private final GalleryMediaLoader mMediaLoader;
    private final GalleryAdapter mAdapter;
    private View mEmptyView;
    private String mTitle;
    private GridLayoutManager mLayoutManager;
    private RecyclerView mRecyclerView;
    private Callbacks mCallbacks;
    private boolean mShouldHandleBackPressed;

    private MediaSharedElementCallback mSharedElementCallback;


    ////////// Constructor(s) //////////

    public GalleryFragment() {
        mMediaLoader = new GalleryMediaLoader();
        mAdapter = new GalleryAdapter();
        mAdapter.setCallbacks(this);
        setRetainInstance(true);
    }


    ////////// Fragment Method(s) //////////

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mTitle = "Gallery";
        ((PhotoPickerActivity) getActivity()).setActionBarTitle(mTitle);
        if (!(context instanceof Callbacks)) {
            throw new IllegalArgumentException(context.getClass().getSimpleName() + " must implement " + Callbacks.class.getName());
        }
        mCallbacks = (Callbacks) context;
        if (!(context instanceof FragmentActivity)) {
            throw new IllegalArgumentException(context.getClass().getSimpleName() + " must inherit from " + FragmentActivity.class.getName());
        }
        mMediaLoader.onAttach((FragmentActivity) context, this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
        mMediaLoader.onDetach();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && isResumed()) {
            ((PhotoPickerActivity) getActivity()).setActionBarTitle(mTitle);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gallery, container, false);

        mEmptyView = view.findViewById(android.R.id.empty);

        mLayoutManager = new GridLayoutManager(getContext(), 2);
        mAdapter.setLayoutManager(mLayoutManager);

        final int spacing = getResources().getDimensionPixelSize(R.dimen.gallery_item_offset);
        mRecyclerView = view.findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setClipToPadding(false);
        mRecyclerView.addItemDecoration(new ItemOffsetDecoration(spacing));
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                mRecyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
                int size = getResources().getDimensionPixelSize(R.dimen.gallery_item_size);
                int width = mRecyclerView.getMeasuredWidth();
                int columnCount = width / (size + spacing);
                mLayoutManager.setSpanCount(columnCount);
                return false;
            }
        });

        if (savedInstanceState != null) {
            updateEmptyState();
        }

        return view;
    }


    ////////// GalleryMediaLoader.Callbacks Method(s) //////////

    @Override
    public void onBucketLoadFinished(@Nullable Cursor data) {
        mAdapter.swapData(GalleryAdapter.VIEW_TYPE_BUCKET, data);
        getActivity().invalidateOptionsMenu();
        updateEmptyState();
    }

    @Override
    public void onMediaLoadFinished(@Nullable Cursor data) {
        mAdapter.swapData(GalleryAdapter.VIEW_TYPE_MEDIA, data);
        getActivity().invalidateOptionsMenu();
        updateEmptyState();
    }

    private void updateEmptyState() {
        mRecyclerView.setVisibility(mAdapter.getItemCount() > 0 ? View.VISIBLE : View.INVISIBLE);
        mEmptyView.setVisibility(mAdapter.getItemCount() > 0 ? View.INVISIBLE : View.VISIBLE);
    }


    ////////// GalleryAdapter.Callbacks Method(s) //////////

    @Override
    public void onGalleryBucketClick(long bucketId, String label) {
        mTitle = label;
        ((PhotoPickerActivity) getActivity()).setActionBarTitle(mTitle);
        mMediaLoader.loadByBucket(bucketId);
        mShouldHandleBackPressed = true;
    }

    @Override
    public void onGalleryMediaClick(View imageView, View checkView, long bucketId, int position) {
        mCallbacks.onGalleryMediaClick(imageView, checkView, bucketId, position);
    }

    @Override
    public void onSelectionUpdated(int count) {
        mCallbacks.onSelectionUpdated(count);
    }

    @Override
    public void onMaxSelectionReached() {
        mCallbacks.onMaxSelectionReached();
    }

    @Override
    public void onWillExceedMaxSelection() {
        mCallbacks.onWillExceedMaxSelection();
    }


    ////////// Method(s) //////////

    public void setMaxSelection(@IntRange(from = 0) int maxSelection) {
        mAdapter.setMaxSelection(maxSelection);
    }

    public void updateAllSelectionCount(int count) {
        mAdapter.updateAllSelectionCount(count);
    }

    public List<Uri> getSelection() {
        return new ArrayList<>(mAdapter.getSelection());
    }

    public void setSelection(@NonNull List<Uri> selection) {
        mAdapter.setSelection(selection);
    }

    public void onActivityReenter(int resultCode, Intent data) {

        final int position = GalleryPreviewActivity.getPosition(resultCode, data);
        if (position != RecyclerView.NO_POSITION) {
            mRecyclerView.scrollToPosition(position);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mSharedElementCallback = new MediaSharedElementCallback();
            getActivity().setExitSharedElementCallback(mSharedElementCallback);

            // ISelectableItem to reset shared element exit transition callbacks.
            getActivity().getWindow().getSharedElementExitTransition().addListener(new TransitionCallback() {
                @Override
                public void onTransitionEnd(Transition transition) {
                    removeCallback();
                }

                @Override
                public void onTransitionCancel(Transition transition) {
                    removeCallback();
                }

                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                private void removeCallback() {
                    if (getActivity() != null) {
                        getActivity().getWindow().getSharedElementExitTransition().removeListener(this);
                        getActivity().setExitSharedElementCallback((SharedElementCallback) null);
                    }
                }
            });
        }

        //noinspection ConstantConditions
        getActivity().supportPostponeEnterTransition();
        mRecyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                mRecyclerView.getViewTreeObserver().removeOnPreDrawListener(this);

                RecyclerView.ViewHolder holder = mRecyclerView.findViewHolderForAdapterPosition(position);
                if (holder instanceof GalleryAdapter.MediaViewHolder) {
                    GalleryAdapter.MediaViewHolder mediaViewHolder = (GalleryAdapter.MediaViewHolder) holder;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        mSharedElementCallback.setSharedElementViews(mediaViewHolder.mImageView, mediaViewHolder.mCheckView);
                    }
                }

                getActivity().supportStartPostponedEnterTransition();

                return true;
            }
        });
    }

    /**
     * Load the initial data if it handles the back pressed
     *
     * @return If this Fragment handled the back pressed callback
     */
    public boolean onBackPressed() {
        if (!getUserVisibleHint()) return false;
        if (mShouldHandleBackPressed) {
            mTitle = "Gallery";
            ((PhotoPickerActivity) getActivity()).setActionBarTitle(mTitle);
            loadBuckets();
            return false;
        }
        return true;
    }

    public void loadBuckets() {
        mMediaLoader.loadBuckets();
        mShouldHandleBackPressed = false;
    }

}
