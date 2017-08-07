package com.github.potatodealer.gfiphotopicker.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.andremion.counterfab.CounterFab;
import com.github.potatodealer.gfiphotopicker.FacebookAgent;
import com.github.potatodealer.gfiphotopicker.InstagramAgent;
import com.github.potatodealer.gfiphotopicker.R;
import com.github.potatodealer.gfiphotopicker.StoragePermissionActivity;
import com.github.potatodealer.gfiphotopicker.adapter.ViewPagerAdapter;
import com.github.potatodealer.gfiphotopicker.fragment.FacebookFragment;
import com.github.potatodealer.gfiphotopicker.fragment.GalleryFragment;
import com.github.potatodealer.gfiphotopicker.fragment.InstagramFragment;
import com.github.potatodealer.gfiphotopicker.util.transition.TransitionCallback;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class PhotoPickerActivity extends StoragePermissionActivity implements GalleryFragment.Callbacks, FacebookFragment.Callbacks, InstagramFragment.Callbacks, View.OnClickListener {

    private static final String EXTRA_PREFIX = PhotoPickerActivity.class.getPackage().getName();
    private static final String EXTRA_INSTAGRAM_CLIENT_ID = EXTRA_PREFIX + ".extra.INSTAGRAM_CLIENT_ID";
    private static final String EXTRA_INSTAGRAM_REDIRECT_URI = EXTRA_PREFIX + ".extra.INSTAGRAM_REDIRECT_URI";
    private static final String EXTRA_MAX_SELECTION = EXTRA_PREFIX + ".extra.MAX_SELECTION";
    private static final String EXTRA_SELECTION = EXTRA_PREFIX + ".extra.SELECTION";
    private static final String EXTRA_FACEBOOK_SELECTION = EXTRA_PREFIX + ".extra.FACEBOOK_SELECTION";
    private static final String EXTRA_INSTAGRAM_SELECTION = EXTRA_PREFIX + ".extra.INSTAGRAM_SELECTION";
    private static final String EXTRA_SELECTION_PATH = EXTRA_PREFIX + ".extra.SELECTION_PATH";
    private static final int DEFAULT_MAX_SELECTION = 1;
    private static final String TITLE_STATE = "title_state";
    private static final int PREVIEW_REQUEST_CODE = 0;
    private static final int FACEBOOK_PREVIEW_REQUEST_CODE = 50;
    private static final int INSTAGRAM_PREVIEW_REQUEST_CODE = 100;

    /**
     * Start the PhotoPicker Activity with additional launch information.
     *
     * @param activity        Context to launch activity from.
     * @param requestCode     If >= 0, this code will be returned in onActivityResult() when the activity exits.
     * @param maxSelection    The max count of image selection
     * @param selection       The current image selection
     */
    public static void startActivity(@NonNull Activity activity,
                                     String instagramClientId,
                                     String instagramRedirectUri,
                                     int requestCode,
                                     @IntRange(from = 0) int maxSelection,
                                     List<Uri> selection,
                                     List<Uri> facebookSelection,
                                     List<Uri> instagramSelection) {
        Intent intent = buildIntent(activity, instagramClientId, instagramRedirectUri, maxSelection, selection, facebookSelection, instagramSelection);
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * Start the Gallery Activity with additional launch information.
     *
     * @param fragment        Context to launch fragment from.
     * @param requestCode     If >= 0, this code will be returned in onActivityResult() when the fragment exits.
     * @param maxSelection    The max count of image selection
     * @param selection       The current image selection
     */
    public static void startActivity(@NonNull Fragment fragment,
                                     String instagramClientId,
                                     String instagramRedirectUri,
                                     int requestCode,
                                     @IntRange(from = 0) int maxSelection,
                                     List<Uri> selection,
                                     List<Uri> facebookSelection,
                                     List<Uri> instagramSelection) {
        Intent intent = buildIntent(fragment.getContext(), instagramClientId, instagramRedirectUri, maxSelection, selection, facebookSelection, instagramSelection);
        fragment.startActivityForResult(intent, requestCode);
    }

    @NonNull
    private static Intent buildIntent(@NonNull Context context, String instagramClientId, String instagramRedirectUri, @IntRange(from = 0) int maxSelection, List<Uri> selection, List<Uri> facebookSelection, List<Uri> instagramSelection) {
        Intent intent = new Intent(context, PhotoPickerActivity.class);
        if (instagramClientId != null) {
            intent.putExtra(EXTRA_INSTAGRAM_CLIENT_ID, instagramClientId);
        }
        if (instagramRedirectUri != null) {
            intent.putExtra(EXTRA_INSTAGRAM_REDIRECT_URI, instagramRedirectUri);
        }
        if (maxSelection > 0) {
            intent.putExtra(EXTRA_MAX_SELECTION, maxSelection);
        }
        if (selection != null) {
            intent.putExtra(EXTRA_SELECTION, new LinkedList<>(selection));
        }
        if (selection != null) {
            intent.putExtra(EXTRA_FACEBOOK_SELECTION, new LinkedList<>(facebookSelection));
        }
        if (instagramSelection != null) {
            intent.putExtra(EXTRA_INSTAGRAM_SELECTION, new LinkedList<>(instagramSelection));
        }
        return intent;
    }

    public static List<Uri> getSelection(Intent data) {
        return data.getParcelableArrayListExtra(EXTRA_SELECTION);
    }

    public static List<Uri> getFacebookSelection(Intent data) {
        return data.getParcelableArrayListExtra(EXTRA_FACEBOOK_SELECTION);
    }

    public static List<Uri> getInstagramSelection(Intent data) {
        return data.getParcelableArrayListExtra(EXTRA_INSTAGRAM_SELECTION);
    }

    public static List<String> getPathSelection(Intent data) {
        return data.getStringArrayListExtra(EXTRA_SELECTION_PATH);
    }

    private GalleryFragment mGalleryFragment;
    private FacebookFragment mFacebookFragment;
    private InstagramFragment mInstagramFragment;
    private ViewGroup mContentView;
    private CounterFab mFab;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_photo_picker);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setupTransition();

        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPager.setOffscreenPageLimit(10);
        setupViewPager(viewPager);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        mContentView = (ViewGroup) findViewById(R.id.coordinator_layout);

        mFab = (CounterFab) findViewById(R.id.fab_done);
        mFab.setOnClickListener(this);

        FragmentPagerAdapter fa = (FragmentPagerAdapter) viewPager.getAdapter();
        mGalleryFragment = (GalleryFragment) fa.getItem(0);
        mFacebookFragment = (FacebookFragment) fa.getItem(1);
        mInstagramFragment = (InstagramFragment) fa.getItem(2);

        if (savedInstanceState == null) {
            setResult(RESULT_CANCELED);
        } else {
            setActionBarTitle(savedInstanceState.getString(TITLE_STATE));
        }
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        if (fragment == mGalleryFragment) {
            mGalleryFragment.setMaxSelection(getIntent().getIntExtra(EXTRA_MAX_SELECTION, DEFAULT_MAX_SELECTION));
            if (getIntent().hasExtra(EXTRA_SELECTION)) {
                //noinspection unchecked
                mGalleryFragment.setSelection((List<Uri>) getIntent().getSerializableExtra(EXTRA_SELECTION));
            }
            askForPermission();
        } else if (fragment == mFacebookFragment) {
            mFacebookFragment.setMaxSelection(getIntent().getIntExtra(EXTRA_MAX_SELECTION, DEFAULT_MAX_SELECTION));
            if (getIntent().hasExtra(EXTRA_FACEBOOK_SELECTION)) {
                //noinspection unchecked
                mFacebookFragment.setFacebookSelection((List<Uri>) getIntent().getSerializableExtra(EXTRA_FACEBOOK_SELECTION));
            }
        } else if (fragment == mInstagramFragment){
            mInstagramFragment.setMaxSelection(getIntent().getIntExtra(EXTRA_MAX_SELECTION, DEFAULT_MAX_SELECTION));
            if (getIntent().hasExtra(EXTRA_INSTAGRAM_CLIENT_ID)) {
                mInstagramFragment.setClientId(getIntent().getStringExtra(EXTRA_INSTAGRAM_CLIENT_ID));
            }
            if (getIntent().hasExtra(EXTRA_INSTAGRAM_REDIRECT_URI)) {
                mInstagramFragment.setRedirectUri(getIntent().getStringExtra(EXTRA_INSTAGRAM_REDIRECT_URI));
            }
            if (getIntent().hasExtra(EXTRA_INSTAGRAM_SELECTION)) {
                //noinspection unchecked
                mInstagramFragment.setInstagramSelection((List<Uri>) getIntent().getSerializableExtra(EXTRA_INSTAGRAM_SELECTION));
            }
        }
    }

    private void setupTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            TransitionInflater inflater = TransitionInflater.from(this);
            Transition exitTransition = inflater.inflateTransition(R.transition.gallery_exit);
            exitTransition.addListener(new TransitionCallback() {
                @Override
                public void onTransitionStart(Transition transition) {
                    mFab.hide();
                }
            });
            getWindow().setExitTransition(exitTransition);
            Transition reenterTransition = inflater.inflateTransition(R.transition.gallery_reenter);
            reenterTransition.addListener(new TransitionCallback() {
                @Override
                public void onTransitionEnd(Transition transition) {
                    mFab.show();
                }

                @Override
                public void onTransitionCancel(Transition transition) {
                    mFab.show();
                }
            });
            getWindow().setReenterTransition(reenterTransition);
        }
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFrag(new GalleryFragment(), "GALLERY");
        adapter.addFrag(new FacebookFragment(), "FACEBOOK");
        adapter.addFrag(new InstagramFragment(), "INSTAGRAM");

        viewPager.setAdapter(adapter);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharSequence(TITLE_STATE, getSupportActionBar().getTitle());
    }

    @Override
    public void onBackPressed() {
        if (mInstagramFragment.onBackPressed()) {
            super.onBackPressed();
        }
        if (mFacebookFragment.onBackPressed()) {
            super.onBackPressed();
        }
        if (mGalleryFragment.onBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    public void onActivityReenter(int resultCode, Intent data) {
        mGalleryFragment.onActivityReenter(resultCode, data);
        mInstagramFragment.onActivityReenter(resultCode, data);
    }

    @Override
    public void onClick(View v) {
        Intent data = new Intent();
        data.putExtra(EXTRA_SELECTION, (ArrayList<Uri>) mGalleryFragment.getSelection());
        data.putExtra(EXTRA_FACEBOOK_SELECTION, (ArrayList<Uri>) mFacebookFragment.getFacebookSelection());
        data.putExtra(EXTRA_INSTAGRAM_SELECTION, (ArrayList<Uri>) mInstagramFragment.getInstagramSelection());
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    public void onGalleryMediaClick(@NonNull View imageView, @NonNull View checkView, long bucketId, int position) {
        GalleryPreviewActivity.startActivity(this, PREVIEW_REQUEST_CODE, imageView, checkView, bucketId, position, mGalleryFragment.getSelection(),
                getIntent().getIntExtra(EXTRA_MAX_SELECTION, DEFAULT_MAX_SELECTION));
    }

    @Override
    public void onFacebookMediaClick(@NonNull View imageView, View checkView, long bucketId, int position) {
        FacebookPreviewActivity.startActivity(this, FACEBOOK_PREVIEW_REQUEST_CODE, imageView, checkView, bucketId, position, mFacebookFragment.getFacebookSelection(),
                getIntent().getIntExtra(EXTRA_MAX_SELECTION, DEFAULT_MAX_SELECTION));
    }

    @Override
    public void onInstagramMediaClick(@NonNull View imageView, View checkView, int position) {
        InstagramPreviewActivity.startActivity(this, INSTAGRAM_PREVIEW_REQUEST_CODE, imageView, checkView, position, mInstagramFragment.getInstagramSelection(),
                getIntent().getIntExtra(EXTRA_MAX_SELECTION, DEFAULT_MAX_SELECTION));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Before Lollipop we don't have Activity.onActivityReenter() callback,
        // so we have to call GalleryFragment.onActivityReenter() here.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            mGalleryFragment.onActivityReenter(resultCode, data);
            mFacebookFragment.onActivityReenter(resultCode, data);
            mInstagramFragment.onActivityReenter(resultCode, data);
        }

        if (requestCode == PREVIEW_REQUEST_CODE) {
            mGalleryFragment.setSelection(GalleryPreviewActivity.getSelection(data));
        } else if (requestCode == FACEBOOK_PREVIEW_REQUEST_CODE) {
            mFacebookFragment.setFacebookSelection(FacebookPreviewActivity.getSelection(data));
        } else if (requestCode == INSTAGRAM_PREVIEW_REQUEST_CODE) {
            mInstagramFragment.setInstagramSelection(InstagramPreviewActivity.getSelection(data));
        } else if (requestCode == 2301 && data != null) { //Instagram Login Request Code
            String accessToken = InstagramLoginActivity.getAccessToken(data);

            InstagramAgent.saveAccessToken(this, accessToken);

            mInstagramFragment.loadMedias();
        } else if (requestCode == 64206 && data != null){ //Facebook Login Request Code
            mFacebookFragment.onActivityResult(requestCode, resultCode, data);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onPermissionGranted() {
        mGalleryFragment.loadBuckets();
    }

    @Override
    public void onSelectionUpdated(int count) {
        mGalleryFragment.updateAllSelectionCount(count);
        mFacebookFragment.updateAllSelectionCount(count);
        mInstagramFragment.updateAllSelectionCount(count);
        mFab.setCount(count);
    }

    @Override
    public void onMaxSelectionReached() {
        Snackbar.make(mContentView, R.string.activity_gallery_max_selection_reached, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onWillExceedMaxSelection() {
        Snackbar.make(mContentView, R.string.activity_gallery_will_exceed_max_selection, Snackbar.LENGTH_SHORT).show();
    }

    @SuppressWarnings("ConstantConditions")
    public void setActionBarTitle(@Nullable CharSequence title) {
        getSupportActionBar().setTitle(title);
    }

    private void resetActionBarTitle() {
        setActionBarTitle(getTitle());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        onAttachFragment(mGalleryFragment);
        onAttachFragment(mFacebookFragment);
        onAttachFragment(mInstagramFragment);
    }
}
