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

import com.github.potatodealer.gfiphotopicker.InstagramAgent;
import com.github.potatodealer.gfiphotopicker.R;
import com.github.potatodealer.gfiphotopicker.activity.InstagramLoginActivity;
import com.github.potatodealer.gfiphotopicker.activity.InstagramPreviewActivity;
import com.github.potatodealer.gfiphotopicker.activity.PhotoPickerActivity;
import com.github.potatodealer.gfiphotopicker.adapter.InstagramAdapter;
import com.github.potatodealer.gfiphotopicker.data.InstagramDBHelper;
import com.github.potatodealer.gfiphotopicker.data.InstagramMediaLoader;
import com.github.potatodealer.gfiphotopicker.util.ItemOffsetDecoration;
import com.github.potatodealer.gfiphotopicker.util.transition.MediaSharedElementCallback;
import com.github.potatodealer.gfiphotopicker.util.transition.TransitionCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link InstagramFragment.Callbacks} interface
 * to handle interaction events.
 */
public class InstagramFragment extends Fragment implements InstagramMediaLoader.Callbacks, InstagramAdapter.Callbacks, InstagramAgent.Callbacks {

    ////////// Static Constant(s) //////////

    @SuppressWarnings( "unused" )
    static private final String  LOG_TAG                           = "InstagramPhotoPicker...";

    static private final boolean DEBUGGING_ENABLED                 = false;

    private static final String  INTENT_EXTRA_PREFIX               = "com.github.potatodealer.gfiphotopicker.fragment";
    private static String        INTENT_EXTRA_NAME_CLIENT_ID       = INTENT_EXTRA_PREFIX + ".INTENT_EXTRA_NAME_CLIENT_ID";
    private static String        INTENT_EXTRA_NAME_REDIRECT_URI    = INTENT_EXTRA_PREFIX + ".INTENT_EXTRA_NAME_REDIRECT_URI";

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

        void onInstagramMediaClick(@NonNull View imageView, View checkView, int position);

        void onSelectionUpdated(int count);

        void onMaxSelectionReached();

        void onWillExceedMaxSelection();

        void onLowResImageSelected();
    }


    ////////// Static Variable(s) //////////

    private static int mMediaPosition;
    private static int mMediaTopView;


    ////////// Member Variable(s) //////////

    private final InstagramMediaLoader mMediaLoader;
    private final InstagramAdapter mAdapter;
    private View mEmptyView;
    private View mLoginView;
    private ProgressBar mProgressBar;
    private GridLayoutManager mLayoutManager;
    private RecyclerView mRecyclerView;
    private InstagramFragment.Callbacks mCallbacks;
    private boolean mShouldHandleBackPressed;
    private MenuItem logoutMenu;
    private InstagramDBHelper db;
    private InstagramAgent mInstagramAgent;


    ////////// Constructor(s) //////////

    public InstagramFragment() {
        mMediaLoader = new InstagramMediaLoader();
        mAdapter = new InstagramAdapter();
        mAdapter.setCallbacks(this);
        setRetainInstance(true);
        setHasOptionsMenu(true);
    }


    ////////// Fragment Method(s) //////////

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (!(context instanceof InstagramFragment.Callbacks)) {
            throw new IllegalArgumentException(context.getClass().getSimpleName() + " must implement " + InstagramFragment.Callbacks.class.getName());
        }
        mCallbacks = (InstagramFragment.Callbacks) context;
        if (!(context instanceof FragmentActivity)) {
            throw new IllegalArgumentException(context.getClass().getSimpleName() + " must inherit from " + FragmentActivity.class.getName());
        }
        mMediaLoader.onAttach((FragmentActivity) context, this);
        mInstagramAgent = InstagramAgent.getInstance(getActivity(), INTENT_EXTRA_NAME_CLIENT_ID, INTENT_EXTRA_NAME_REDIRECT_URI, this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_social, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        logoutMenu = menu.findItem(R.id.item_logout);
        logoutMenu.setVisible(mInstagramAgent.haveAccessToken(getActivity()));
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
                    .setMessage("Are you sure you want to logout from Instagram? All your Instagram selection will be lost.")
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
        mCallbacks = null;
        mMediaLoader.onDetach();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && isResumed()) {
            ((PhotoPickerActivity) getActivity()).setActionBarTitle("Instagram");
            mShouldHandleBackPressed = true;
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_instagram, container, false);

        mEmptyView = view.findViewById(android.R.id.empty);
        mLoginView = view.findViewById(R.id.instagram_login);

        mProgressBar = view.findViewById(R.id.instagram_loading);

        Button buttonLogin = view.findViewById(R.id.instagram_login_button);
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                InstagramLoginActivity.startLoginForResult(getActivity(), INTENT_EXTRA_NAME_CLIENT_ID, INTENT_EXTRA_NAME_REDIRECT_URI, REQUEST_CODE_LOGIN);
            }
        });

        mLayoutManager = new GridLayoutManager(getContext(), 1);
        mAdapter.setLayoutManager(mLayoutManager);

        final int spacing = getResources().getDimensionPixelSize(R.dimen.gallery_item_offset);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
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

        db = new InstagramDBHelper(getActivity());

        updateLoginState();

        return view;
    }


    ////////// InstagramMediaLoader.Callbacks Method(s) //////////

    @Override
    public void onMediaLoadFinished(@Nullable Cursor data) {
        mAdapter.swapData(InstagramAdapter.VIEW_TYPE_MEDIA, data);
        getActivity().invalidateOptionsMenu();
        updateEmptyState();
        if (mMediaPosition != -1) mLayoutManager.scrollToPositionWithOffset(mMediaPosition, mMediaTopView);
    }


    ////////// InstagramAdapter.Callbacks Method(s) //////////

    @Override
    public void onInstagramMediaClick(View imageView, View checkView, int position) {
        mCallbacks.onInstagramMediaClick(imageView, checkView, position);
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


    ////////// InstagramAgent.Callbacks Method(s) //////////

    /*****************************************************
     *
     * Called to restart.
     *
     *****************************************************/
    @Override
    public void iaRestart() {
        if ( DEBUGGING_ENABLED ) Log.d( LOG_TAG, "iaRestart()" );

        loadMedias();
    }

    /*****************************************************
     *
     * Called when there was an error retrieving photos.
     *
     *****************************************************/
    @Override
    public void iaOnError(Exception exception) {
        if (DEBUGGING_ENABLED) {
            Log.e(LOG_TAG, "Instagram error", exception);
        }

        RetryListener  retryListener  = new RetryListener();
        CancelListener cancelListener = new CancelListener();

        new AlertDialog.Builder( getActivity() )
                .setTitle( R.string.instagram_alert_dialog_title )
                .setMessage( getString( R.string.instagram_alert_dialog_message, exception.toString() ) )
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
    public void iaOnCancel() {
        if ( DEBUGGING_ENABLED ) Log.d( LOG_TAG, "iaOnCancel()" );

        mAdapter.clearInstagramSelection();
        mRecyclerView.setVisibility(View.INVISIBLE);
        mEmptyView.setVisibility(View.INVISIBLE);
        mLoginView.setVisibility(View.VISIBLE);
    }

    /*****************************************************
     *
     * Called when photos were successfully retrieved.

     *****************************************************/
    @Override
    public void iaOnPhotosSuccess(List<InstagramAgent.InstagramPhoto> photoList, boolean morePhotos) {
        if ( DEBUGGING_ENABLED ) Log.d( LOG_TAG, "iaOnPhotosSuccess( photoList = " + ( photoList != null ? photoList : "null" ) + " ( " + ( photoList != null ? photoList.size() : "0" ) + " ), morePhotos = " + morePhotos + " )" );

        db.addInstagramPhoto(photoList);

        if (morePhotos) {
            mInstagramAgent.getPhotos();
        } else {
            mMediaLoader.loadMedias();
        }
    }


    ////////// Method(s) //////////

    public void setClientId(String clientId) {
        INTENT_EXTRA_NAME_CLIENT_ID = clientId;
    }

    public void setRedirectUri(String redirectUri) {
        INTENT_EXTRA_NAME_REDIRECT_URI = redirectUri;
    }

    public void setMaxSelection(@IntRange(from = 0) int maxSelection) {
        mAdapter.setMaxSelection(maxSelection);
    }

    public void updateAllSelectionCount(int count) {
        mAdapter.updateAllSelectionCount(count);
    }

    public List<Uri> getInstagramSelection() {
        return new ArrayList<>(mAdapter.getInstagramSelection());
    }

    public void setInstagramSelection(@NonNull List<Uri> selection) {
        mAdapter.setInstagramSelection(selection);
    }

    public void setMinImageResolution(int minWidth, int minHeight) {
        mAdapter.setMinImageResolution(minWidth, minHeight);
    }

    public void onActivityReenter(int resultCode, Intent data) {

        final int position = InstagramPreviewActivity.getPosition(resultCode, data);
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
                if (holder instanceof InstagramAdapter.MediaViewHolder) {
                    InstagramAdapter.MediaViewHolder mediaViewHolder = (InstagramAdapter.MediaViewHolder) holder;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        sharedElementCallback.setSharedElementViews(mediaViewHolder.mImageView, mediaViewHolder.mCheckView);
                    }
                }

                getActivity().supportStartPostponedEnterTransition();

                return true;
            }
        });
    }

    public void loadMedias() {
        mLoginView.setVisibility(View.INVISIBLE);
        mProgressBar.setVisibility(View.VISIBLE);
        mInstagramAgent.resetPhotos();
        mInstagramAgent.getPhotos();
    }

    private void showErrorDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Error");
        builder.setMessage(message);
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    public void updateLoginState() {
        if (mInstagramAgent.haveAccessToken(getActivity())) {
            mLoginView.setVisibility(View.INVISIBLE);
            mMediaLoader.loadMedias();
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
        mInstagramAgent.clearAccessToken(getActivity());
        InstagramLoginActivity.logOut(getActivity());
        mAdapter.clearInstagramSelection();
        db.deleteAllInstagramPhotos();
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
        if (getUserVisibleHint()) {
            // Remember the scroll position
            mMediaPosition = mLayoutManager.findFirstVisibleItemPosition();
            View mediaStartView = mRecyclerView.getChildAt(0);
            mMediaTopView = (mediaStartView == null) ? 0 : (mediaStartView.getTop() - mRecyclerView.getPaddingTop());
        }

        return getUserVisibleHint();
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
            loadMedias();
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
            mAdapter.clearInstagramSelection();
            mRecyclerView.setVisibility(View.INVISIBLE);
            mEmptyView.setVisibility(View.INVISIBLE);
            mLoginView.setVisibility(View.VISIBLE);
        }

        @Override
        public void onCancel( DialogInterface dialog )
        {
            mAdapter.clearInstagramSelection();
            mRecyclerView.setVisibility(View.INVISIBLE);
            mEmptyView.setVisibility(View.INVISIBLE);
            mLoginView.setVisibility(View.VISIBLE);
        }
    }

}
