package com.github.potatodealer.gfiphotopicker.fragment;

import android.annotation.TargetApi;
import android.app.AlertDialog;
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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;

import com.github.potatodealer.gfiphotopicker.GFIPhotoPicker;
import com.github.potatodealer.gfiphotopicker.InstagramException;
import com.github.potatodealer.gfiphotopicker.R;
import com.github.potatodealer.gfiphotopicker.activity.InstagramLoginActivity;
import com.github.potatodealer.gfiphotopicker.activity.InstagramPreviewActivity;
import com.github.potatodealer.gfiphotopicker.activity.PhotoPickerActivity;
import com.github.potatodealer.gfiphotopicker.adapter.InstagramAdapter;
import com.github.potatodealer.gfiphotopicker.data.InstagramDBHelper;
import com.github.potatodealer.gfiphotopicker.data.InstagramMediaLoader;
import com.github.potatodealer.gfiphotopicker.data.InstagramMediaRequest;
import com.github.potatodealer.gfiphotopicker.data.InstagramPhoto;
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
public class InstagramFragment extends Fragment implements InstagramMediaLoader.Callbacks, InstagramAdapter.Callbacks {

    private static String INSTAGRAM_CLIENT_ID = "com.github.potatodealer.gfiphotopicker.fragment.INSTAGRAM_CLIENT_ID";
    private static String INSTAGRAM_REDIRECT_URI = "com.github.potatodealer.gfiphotopicker.fragment.INSTAGRAM_REDIRECT_URI";
    private static final int REQUEST_CODE_LOGIN = 2301;

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
    }

    private final InstagramMediaLoader mMediaLoader;
    private final InstagramAdapter mAdapter;
    private View mEmptyView;
    private View mLoginView;
    private GridLayoutManager mLayoutManager;
    private RecyclerView mRecyclerView;
    private InstagramFragment.Callbacks mCallbacks;
    private boolean mShouldHandleBackPressed;
    private MenuItem logoutMenu;
    private InstagramMediaRequest nextItemRequest;
    private InstagramDBHelper db;

    private MediaSharedElementCallback mSharedElementCallback;

    public InstagramFragment() {
        mMediaLoader = new InstagramMediaLoader();
        mAdapter = new InstagramAdapter();
        mAdapter.setCallbacks(this);
        setRetainInstance(true);
        setHasOptionsMenu(true);
    }

    public void setClientId(String clientId) {
        INSTAGRAM_CLIENT_ID = clientId;
    }

    public void setRedirectUri(String redirectUri) {
        INSTAGRAM_REDIRECT_URI = redirectUri;
    }

    public void setMaxSelection(@IntRange(from = 0) int maxSelection) {
        mAdapter.setMaxSelection(maxSelection);
    }

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
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_instagram, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        String accessToken = GFIPhotoPicker.getAccessToken(getActivity());
        String clientId = GFIPhotoPicker.getClientId(getActivity());
        boolean isLoggedIn = accessToken != null && clientId != null && clientId.equals(INSTAGRAM_CLIENT_ID);
        logoutMenu = menu.findItem(R.id.item_logout);
        logoutMenu.setVisible(isLoggedIn);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.item_logout) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMediaLoadFinished(@Nullable Cursor data) {
        mAdapter.swapData(InstagramAdapter.VIEW_TYPE_MEDIA, data);
        getActivity().invalidateOptionsMenu();
        updateEmptyState();
    }

    private void logout() {
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
                        // continue with delete
                        GFIPhotoPicker.logout(getActivity());
                        mAdapter.clearSelection();
                        db.deleteAllInstagramPhotos();
                        logoutMenu.setVisible(false);
                        mRecyclerView.setVisibility(View.INVISIBLE);
                        mEmptyView.setVisibility(View.INVISIBLE);
                        mLoginView.setVisibility(View.VISIBLE);
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .show();
    }

    public void updateLoginState() {
        String accessToken = GFIPhotoPicker.getAccessToken(getActivity());
        String clientId = GFIPhotoPicker.getClientId(getActivity());

        if (accessToken != null) {
            if (clientId != null) {
                if (clientId.equals(INSTAGRAM_CLIENT_ID)) {
                    mLoginView.setVisibility(View.INVISIBLE);
                    mMediaLoader.loadMedias();
                } else {
                    logout();
                }
            }
        } else {
            mLoginView.setVisibility(View.VISIBLE);
        }
    }

    private void updateEmptyState() {
        mRecyclerView.setVisibility(mAdapter.getItemCount() > 0 ? View.VISIBLE : View.INVISIBLE);
        mEmptyView.setVisibility(mAdapter.getItemCount() > 0 ? View.INVISIBLE : View.VISIBLE);
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
            mShouldHandleBackPressed = false;
        }
    }

    public void setShouldHandleBackPressed(boolean shouldHandleBackPressed) {
        mShouldHandleBackPressed = shouldHandleBackPressed;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_instagram, container, false);

        mEmptyView = view.findViewById(android.R.id.empty);
        mLoginView = view.findViewById(R.id.instagram_login);

        Button buttonLogin = (Button) view.findViewById(R.id.instagram_login_button);
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                InstagramLoginActivity.startLoginForResult(getActivity(), INSTAGRAM_CLIENT_ID, INSTAGRAM_REDIRECT_URI, REQUEST_CODE_LOGIN);
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

    public void onActivityReenter(int resultCode, Intent data) {

        final int position = InstagramPreviewActivity.getPosition(resultCode, data);
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
                if (holder instanceof InstagramAdapter.MediaViewHolder) {
                    InstagramAdapter.MediaViewHolder mediaViewHolder = (InstagramAdapter.MediaViewHolder) holder;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        mSharedElementCallback.setSharedElementViews(mediaViewHolder.mImageView, mediaViewHolder.mCheckView);
                    }
                }

                getActivity().supportStartPostponedEnterTransition();

                return true;
            }
        });
    }

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

    /**
     * Load the initial data if it handles the back pressed
     *
     * @return If this Fragment handled the back pressed callback
     */
    public boolean onBackPressed() {
        return mShouldHandleBackPressed;
    }

    public void loadMedias() {
        mLoginView.setVisibility(View.INVISIBLE);
        String accessToken = GFIPhotoPicker.getAccessToken(getActivity());
        nextItemRequest = new InstagramMediaRequest();
        nextItemRequest.getMedia(accessToken, new InstagramMediaRequest.InstagramMediaRequestListener() {
            @Override
            public void onMedia(List<InstagramPhoto> media, InstagramMediaRequest nextPageRequest) {
                nextItemRequest = nextPageRequest;
                for (int i = 0; i < media.size(); i++) {
                    InstagramPhoto mPhoto = (InstagramPhoto) media.get(i);
                    db.addInstagramPhoto(mPhoto);
                }
                mMediaLoader.loadMedias();
            }

            @Override
            public void onError(Exception error) {
                if (error instanceof InstagramException) {
                    InstagramException ex = (InstagramException) error;
                    if (ex.getCode() == InstagramException.CODE_INVALID_ACCESS_TOKEN) {
                        logout();
                    } else {
                        showErrorDialog(ex.getLocalizedMessage());
                    }
                } else {
                    showErrorDialog(error.getLocalizedMessage());
                }
            }
        });
    }

    private void showErrorDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Error");
        builder.setMessage(message);
        builder.setPositiveButton("OK", null);
        builder.show();
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
}
