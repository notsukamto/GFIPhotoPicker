package com.github.potatodealer.gfiphotopicker.fragment;


import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ProgressBar;

import com.github.potatodealer.gfiphotopicker.FacebookAgent;
import com.github.potatodealer.gfiphotopicker.R;
import com.github.potatodealer.gfiphotopicker.activity.FacebookPreviewActivity;
import com.github.potatodealer.gfiphotopicker.activity.PhotoPickerActivity;
import com.github.potatodealer.gfiphotopicker.adapter.FacebookAdapter;
import com.github.potatodealer.gfiphotopicker.data.FacebookDBHelper;
import com.github.potatodealer.gfiphotopicker.data.FacebookMediaLoader;
import com.github.potatodealer.gfiphotopicker.util.ItemOffsetDecoration;
import com.github.potatodealer.gfiphotopicker.util.transition.MediaSharedElementCallback;
import com.github.potatodealer.gfiphotopicker.util.transition.TransitionCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FacebookFragment.Callbacks} interface
 * to handle interaction events.
 */
public class FacebookFragment extends Fragment implements FacebookMediaLoader.Callbacks, FacebookAdapter.Callbacks, FacebookAgent.Callbacks {

    ////////// Static Constant(s) //////////

    @SuppressWarnings( "unused" )
    static private final String  LOG_TAG                           = "FacebookPhotoPicker...";

    static private final boolean DEBUGGING_ENABLED                 = false;

    private static final int     REQUEST_CODE_LOGIN                = 2301;


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

        void onFacebookMediaClick(@NonNull View imageView, View checkView, long bucketId, int position);

        void onSelectionUpdated(int count);

        void onMaxSelectionReached();

        void onWillExceedMaxSelection();

        void onLowResImageSelected();
    }


    ////////// Static Variable(s) //////////

    private static int mBucketPosition;
    private static int mBucketTopView;
    private static ArrayList<Integer> mMediaPosition;
    private static ArrayList<Integer> mMediaTopView;


    ////////// Member Variable(s) //////////

    private final FacebookMediaLoader mMediaLoader;
    private final FacebookAdapter mAdapter;
    private View mEmptyView;
    private View mLoginView;
    private ProgressBar mProgressBar;
    private String mTitle;
    private int mAlbumCount;
    private int mMediaBucketPosition;
    private long mBucketId;
    private GridLayoutManager mLayoutManager;
    private RecyclerView mRecyclerView;
    private FacebookFragment.Callbacks mCallbacks;
    private boolean mShouldHandleBackPressed;
    private boolean mAllAlbumsProcessed;
    private List<FacebookAgent.Album> mAlbumList;
    private MenuItem logoutMenu;
    private FacebookDBHelper db;
    private FacebookAgent mFacebookAgent;


    ////////// Constructor(s) //////////

    public FacebookFragment() {
        mMediaLoader = new FacebookMediaLoader();
        mAdapter = new FacebookAdapter();
        mAdapter.setCallbacks(this);
        setRetainInstance(true);
        setHasOptionsMenu(true);
    }


    ////////// Fragment Method(s) //////////

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d("FacebookFragment", "onAttach");
        mTitle = "Facebook";
        mMediaPosition = new ArrayList<>();
        mMediaTopView = new ArrayList<>();
        if (!(context instanceof FacebookFragment.Callbacks)) {
            throw new IllegalArgumentException(context.getClass().getSimpleName() + " must implement " + FacebookFragment.Callbacks.class.getName());
        }
        mCallbacks = (FacebookFragment.Callbacks) context;
        if (!(context instanceof FragmentActivity)) {
            throw new IllegalArgumentException(context.getClass().getSimpleName() + " must inherit from " + FragmentActivity.class.getName());
        }
        mMediaLoader.onAttach((FragmentActivity) context, this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_social, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        logoutMenu = menu.findItem(R.id.item_logout);
        logoutMenu.setVisible(mFacebookAgent.isLoggedIn());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.item_logout) {
            AlertDialog.Builder builder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder = new AlertDialog.Builder(getActivity(), android.R.style.Theme_Material_Dialog_Alert);
            } else {
                builder = new AlertDialog.Builder(getActivity());
            }
            builder.setTitle("Logout")
                    .setMessage("Are you sure you want to logout from Facebook? All your Facebook selection will be lost.")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            logout();
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothing
                        }
                    })
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d("FacebookFragment", "onDetach");
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
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d("FacebookFragment", "onCreateView");
        mFacebookAgent = FacebookAgent.getInstance(getActivity());

        View view = inflater.inflate(R.layout.fragment_facebook, container, false);

        mEmptyView = view.findViewById(android.R.id.empty);
        mLoginView = view.findViewById(R.id.facebook_login);

        mProgressBar = view.findViewById(R.id.facebook_loading);

        Button buttonLogin = view.findViewById(R.id.facebook_login_button);
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadBuckets();
            }
        });

        mLayoutManager = new GridLayoutManager(getContext(), 1);
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

        db = new FacebookDBHelper(getActivity());

        if (savedInstanceState != null) {
            Log.d("FBonCreateView", "savedInstanceState");
            mMediaPosition = savedInstanceState.getIntegerArrayList("media_position");
            mMediaTopView = savedInstanceState.getIntegerArrayList("media_top_view");
            mLayoutManager.onRestoreInstanceState(savedInstanceState.getParcelable("layout_manager"));
            mRecyclerView.getLayoutManager().onRestoreInstanceState(savedInstanceState.getParcelable("recycler_view"));
            mShouldHandleBackPressed = savedInstanceState.getBoolean("handle_back_press");
            Log.d("FBsavedInstanceState", "mediaPosition = " + mMediaPosition + " & mediaTopView = " + mMediaTopView);
        }

        updateLoginState();

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d("FacebookFragment", "onSaveInstanceState");

        if (mShouldHandleBackPressed) {
            // Remember the scroll position
            mMediaPosition.set(mMediaBucketPosition, mLayoutManager.findFirstVisibleItemPosition());
            View mediaStartView = mRecyclerView.getChildAt(0);
            mMediaTopView.set(mMediaBucketPosition, (mediaStartView == null) ? 0 : (mediaStartView.getTop() - mRecyclerView.getPaddingTop()));
        } else {
            // Remember the scroll position
            mBucketPosition = mLayoutManager.findFirstVisibleItemPosition();
            View bucketStartView = mRecyclerView.getChildAt(0);
            mBucketTopView = (bucketStartView == null) ? 0 : (bucketStartView.getTop() - mRecyclerView.getPaddingTop());
        }

        outState.putParcelable("layout_manager", mLayoutManager.onSaveInstanceState());
        outState.putParcelable("recycler_view", mRecyclerView.getLayoutManager().onSaveInstanceState());
        outState.putBoolean("handle_back_press", mShouldHandleBackPressed);
        outState.putIntegerArrayList("media_position", mMediaPosition);
        outState.putIntegerArrayList("media_top_view", mMediaTopView);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mFacebookAgent.onActivityResult(requestCode, resultCode, data);
        mLoginView.setVisibility(View.INVISIBLE);
        mProgressBar.setVisibility(View.VISIBLE);
    }

    ////////// FacebookMediaLoader.Callbacks Method(s) //////////

    @Override
    public void onBucketLoadFinished(@Nullable Cursor data) {
        mAdapter.swapData(FacebookAdapter.VIEW_TYPE_BUCKET, data);
        getActivity().invalidateOptionsMenu();
        updateEmptyState();
        if (mBucketPosition != -1) mLayoutManager.scrollToPositionWithOffset(mBucketPosition, mBucketTopView);

        if (mMediaPosition.size() == 0) {
            for (int i = 0; i <= mAdapter.getItemCount(); i++) {
                mMediaPosition.add(i, -1);
                mMediaTopView.add(i, -1);
            }
        }
    }

    @Override
    public void onMediaLoadFinished(@Nullable Cursor data) {
        mAdapter.swapData(FacebookAdapter.VIEW_TYPE_MEDIA, data);
        getActivity().invalidateOptionsMenu();
        updateEmptyState();
        if (mMediaPosition.get(mMediaBucketPosition) != -1) {
            mLayoutManager.scrollToPositionWithOffset(mMediaPosition.get(mMediaBucketPosition), mMediaTopView.get(mMediaBucketPosition));
        } else {
            mLayoutManager.scrollToPosition(0);
        }
    }


    ////////// FacebookAdapter.Callbacks Method(s) //////////

    @Override
    public void onFacebookBucketClick(long bucketId, String label, int position) {
        // Remember the scroll position
        mMediaBucketPosition = position;
        mBucketPosition = mLayoutManager.findFirstVisibleItemPosition();
        View bucketStartView = mRecyclerView.getChildAt(0);
        mBucketTopView = (bucketStartView == null) ? 0 : (bucketStartView.getTop() - mRecyclerView.getPaddingTop());

        // Set the title to the bucket label
        mTitle = label;
        ((PhotoPickerActivity) getActivity()).setActionBarTitle(mTitle);

        // load the bucket media
        mBucketId = bucketId;
        mMediaLoader.loadByBucket(mBucketId);

        mShouldHandleBackPressed = true;
    }

    @Override
    public void onFacebookMediaClick(View imageView, View checkView, long bucketId, int position) {
        mCallbacks.onFacebookMediaClick(imageView, checkView, bucketId, position);
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

    @Override
    public void onLowResImageSelected() {
        mCallbacks.onLowResImageSelected();
    }


    ////////// FacebookAgent.Callbacks Method(s) //////////

    /*****************************************************
     *
     * Called when albums were successfully retrieved.
     *
     *****************************************************/
    @Override
    public void facOnAlbumsSuccess( List<FacebookAgent.Album> albumList, boolean moreAlbums ) {
        mAllAlbumsProcessed = false;

        mAlbumList = albumList;
        mAlbumCount = 0;

        db.addAlbumInfo(albumList);

        // Add the albums to our table
        getNextAlbum(mAlbumCount);

        if (moreAlbums) {
            mFacebookAgent.getAlbums(this);
        } else {
            mAllAlbumsProcessed = true;
        }
    }


    /*****************************************************
     *
     * Called when photos were successfully retrieved.
     *
     *****************************************************/
    @Override
    public void facOnPhotosSuccess( List<FacebookAgent.Photo> photoList, boolean morePhotos ) {
        db.addFacebookPhoto(photoList, mAlbumCount);

        if (morePhotos) {
            mFacebookAgent.getPhotos(null, this);
        } else if (mAlbumCount < mAlbumList.size() - 1) {
            mAlbumCount++;
            getNextAlbum(mAlbumCount);
        }

        if (mAllAlbumsProcessed && mAlbumCount == mAlbumList.size() - 1) {
            mShouldHandleBackPressed = false;
            mMediaLoader.loadBuckets();
        }
    }


    /*****************************************************
     *
     * Called when there was an error retrieving photos.
     *
     *****************************************************/
    @Override
    public void facOnError( Exception exception ) {
        Log.e( LOG_TAG, "Facebook error", exception );

        RetryListener  retryListener  = new RetryListener();
        CancelListener cancelListener = new CancelListener();

        new AlertDialog.Builder( getActivity() )
                .setTitle( R.string.title_facebook_alert_dialog )
                .setMessage( getString( R.string.message_facebook_alert_dialog, exception.toString() ) )
                .setPositiveButton( R.string.button_text_retry, retryListener )
                .setNegativeButton( R.string.button_text_cancel, cancelListener )
                .setOnCancelListener( cancelListener )
                .create()
                .show();
    }


    /*****************************************************
     *
     * Called when photo retrieval was cancelled.
     *
     *****************************************************/
    @Override
    public void facOnCancel() {
        mAdapter.clearFacebookSelection();
        mRecyclerView.setVisibility(View.INVISIBLE);
        mEmptyView.setVisibility(View.INVISIBLE);
        mProgressBar.setVisibility(View.INVISIBLE);
        mLoginView.setVisibility(View.VISIBLE);
    }


    ////////// Method(s) //////////

    public void setMaxSelection(@IntRange(from = 0) int maxSelection) {
        mAdapter.setMaxSelection(maxSelection);
    }

    public void updateAllSelectionCount(int count) {
        mAdapter.updateAllSelectionCount(count);
    }

    public List<Uri> getFacebookSelection() {
        return new ArrayList<>(mAdapter.getFacebookSelection());
    }

    public void setFacebookSelection(@NonNull List<Uri> selection) {
        mAdapter.setFacebookSelection(selection);
    }

    public void setMinImageResolution(int minWidth, int minHeight) {
        mAdapter.setMinImageResolution(minWidth, minHeight);
    }

    public void onActivityReenter(int resultCode, Intent data) {

        final int position = FacebookPreviewActivity.getPosition(resultCode, data);
        if (position != RecyclerView.NO_POSITION) {
            mRecyclerView.scrollToPosition(position);
        }

        final MediaSharedElementCallback sharedElementCallback = new MediaSharedElementCallback();
        getActivity().setExitSharedElementCallback(sharedElementCallback);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Listener to reset shared element exit transition callbacks.
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
                if (holder instanceof FacebookAdapter.MediaViewHolder) {
                    FacebookAdapter.MediaViewHolder mediaViewHolder = (FacebookAdapter.MediaViewHolder) holder;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        sharedElementCallback.setSharedElementViews(mediaViewHolder.mImageView, mediaViewHolder.mCheckView);
                    }
                }

                getActivity().supportStartPostponedEnterTransition();

                return true;
            }
        });
    }

    private void getNextAlbum(int albumCount) {
        mFacebookAgent.getPhotos(mAlbumList.get(albumCount), this);
    }

    private void loadBuckets() {
        mFacebookAgent.resetAlbums();
        mFacebookAgent.resetPhotos();
        mFacebookAgent.getAlbums(this);
    }

    private void updateLoginState() {
        if (mFacebookAgent.isLoggedIn()) {
            mLoginView.setVisibility(View.INVISIBLE);

            if (mShouldHandleBackPressed) {
                mMediaLoader.loadByBucket(mBucketId);
            } else {
                mMediaLoader.loadBuckets();
            }
        } else {
            mLoginView.setVisibility(View.VISIBLE);
        }
    }

    private void updateEmptyState() {
        mProgressBar.setVisibility(View.INVISIBLE);
        mRecyclerView.setVisibility(mAdapter.getItemCount() > 0 ? View.VISIBLE : View.INVISIBLE);
        mEmptyView.setVisibility(mAdapter.getItemCount() > 0 ? View.INVISIBLE : View.VISIBLE);
    }

    private void logout() {
        mFacebookAgent.logOut();
        mAdapter.clearFacebookSelection();
        db.deleteAllFacebookPhotos();
        logoutMenu.setVisible(false);
        mRecyclerView.setVisibility(View.INVISIBLE);
        mEmptyView.setVisibility(View.INVISIBLE);
        mLoginView.setVisibility(View.VISIBLE);
    }

    /**
     * Load the initial data if it handles the back pressed
     *
     * @return If this Fragment handled the back pressed callback
     */
    public boolean onBackPressed() {
        if (!mShouldHandleBackPressed) {
            // Remember the scroll position
            mBucketPosition = mLayoutManager.findFirstVisibleItemPosition();
            View bucketStartView = mRecyclerView.getChildAt(0);
            mBucketTopView = (bucketStartView == null) ? 0 : (bucketStartView.getTop() - mRecyclerView.getPaddingTop());
        }

        if (!getUserVisibleHint()) return false;
        if (mShouldHandleBackPressed) {
            // Remember the scroll position
            mMediaPosition.set(mMediaBucketPosition, mLayoutManager.findFirstVisibleItemPosition());
            View mediaStartView = mRecyclerView.getChildAt(0);
            mMediaTopView.set(mMediaBucketPosition, (mediaStartView == null) ? 0 : (mediaStartView.getTop() - mRecyclerView.getPaddingTop()));

            // Set the title back to "Facebook"
            mTitle = "Facebook";
            ((PhotoPickerActivity) getActivity()).setActionBarTitle(mTitle);
            mShouldHandleBackPressed = false;
            mMediaLoader.loadBuckets();
            return false;
        }
        return true;
    }

    private void showErrorDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Error");
        builder.setMessage(message);
        builder.setPositiveButton("OK", null);
        builder.show();
    }


    ////////// Inner Class(es) //////////

    /*****************************************************
     *
     * The alert dialog retry button listener.
     *
     *****************************************************/
    private class RetryListener implements Dialog.OnClickListener
    {
        @Override
        public void onClick( DialogInterface dialog, int which )
        {
            loadBuckets();
        }
    }


    /*****************************************************
     *
     * The alert dialog cancel (button) listener.
     *
     *****************************************************/
    private class CancelListener implements Dialog.OnClickListener, Dialog.OnCancelListener
    {
        @Override
        public void onClick( DialogInterface dialog, int which )
        {
            mAdapter.clearFacebookSelection();
            mRecyclerView.setVisibility(View.INVISIBLE);
            mEmptyView.setVisibility(View.INVISIBLE);
            mProgressBar.setVisibility(View.INVISIBLE);
            mLoginView.setVisibility(View.VISIBLE);
        }

        @Override
        public void onCancel( DialogInterface dialog )
        {
            mAdapter.clearFacebookSelection();
            mRecyclerView.setVisibility(View.INVISIBLE);
            mEmptyView.setVisibility(View.INVISIBLE);
            mProgressBar.setVisibility(View.INVISIBLE);
            mLoginView.setVisibility(View.VISIBLE);
        }
    }

}
