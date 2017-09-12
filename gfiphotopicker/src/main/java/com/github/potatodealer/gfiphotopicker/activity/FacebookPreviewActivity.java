package com.github.potatodealer.gfiphotopicker.activity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckedTextView;

import com.github.potatodealer.gfiphotopicker.R;
import com.github.potatodealer.gfiphotopicker.adapter.FacebookPreviewAdapter;
import com.github.potatodealer.gfiphotopicker.data.FacebookMediaLoader;
import com.github.potatodealer.gfiphotopicker.util.transition.MediaSharedElementCallback;
import com.github.potatodealer.gfiphotopicker.util.transition.TransitionCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static android.support.v7.widget.RecyclerView.NO_POSITION;

public class FacebookPreviewActivity extends AppCompatActivity implements FacebookMediaLoader.Callbacks, FacebookPreviewAdapter.Callbacks {

    private static final String EXTRA_BUCKET_ID = FacebookPreviewActivity.class.getPackage().getName() + ".extra.BUCKET_ID";
    private static final String EXTRA_POSITION = FacebookPreviewActivity.class.getPackage().getName() + ".extra.POSITION";
    private static final String EXTRA_SELECTION = FacebookPreviewActivity.class.getPackage().getName() + ".extra.SELECTION";
    private static final String EXTRA_MAX_SELECTION = FacebookPreviewActivity.class.getPackage().getName() + ".extra.MAX_SELECTION";
    private static final String EXTRA_MIN_WIDTH = GalleryPreviewActivity.class.getPackage().getName() + ".extra.MIN_WIDTH";
    private static final String EXTRA_MIN_HEIGHT = GalleryPreviewActivity.class.getPackage().getName() + ".extra.MIN_HEIGHT";
    private static final String EXTRA_ALERT_TEXT = GalleryPreviewActivity.class.getPackage().getName() + ".extra.ALERT_TEXT";

    private static final int FACEBOOK_RESULT = 2;

    public static void startActivity(@NonNull Activity activity, int requestCode, @NonNull View imageView, @NonNull View checkView,
                                     @IntRange(from = 0) long bucketId, @IntRange(from = 0) int position,
                                     List<Uri> selection, int maxSelection, int minWidth, int minHeight, String alertText) {

        Intent intent = new Intent(activity, FacebookPreviewActivity.class);
        intent.putExtra(EXTRA_BUCKET_ID, bucketId);
        intent.putExtra(EXTRA_POSITION, position);
        intent.putExtra(EXTRA_SELECTION, new LinkedList<>(selection));
        intent.putExtra(EXTRA_MAX_SELECTION, maxSelection);
        intent.putExtra(EXTRA_MIN_WIDTH, minWidth);
        intent.putExtra(EXTRA_MIN_HEIGHT, minHeight);
        intent.putExtra(EXTRA_ALERT_TEXT, alertText);

        Pair[] sharedElements = concatToSystemSharedElements(activity,
                Pair.create(imageView, ViewCompat.getTransitionName(imageView)),
                Pair.create(checkView, ViewCompat.getTransitionName(checkView)));

        //noinspection unchecked
        ActivityOptionsCompat options = ActivityOptionsCompat
                .makeSceneTransitionAnimation(activity, sharedElements);
        ActivityCompat.startActivityForResult(activity, intent, requestCode, options.toBundle());
    }

    @SafeVarargs
    private static Pair[] concatToSystemSharedElements(@NonNull Activity activity, @NonNull Pair<View, String>... activitySharedElements) {

        List<Pair<View, String>> sharedElements = new ArrayList<>();
        sharedElements.addAll(Arrays.asList(activitySharedElements));

        View decorView = activity.getWindow().getDecorView();
        View statusBackground = decorView.findViewById(android.R.id.statusBarBackground);
        View navigationBarBackground = decorView.findViewById(android.R.id.navigationBarBackground);

        if (statusBackground != null) {
            sharedElements.add(Pair.create(statusBackground, ViewCompat.getTransitionName(statusBackground)));
        }
        if (navigationBarBackground != null) {
            sharedElements.add(Pair.create(navigationBarBackground, ViewCompat.getTransitionName(navigationBarBackground)));
        }

        Pair[] result = new Pair[sharedElements.size()];
        sharedElements.toArray(result);
        return result;
    }

    public static int getPosition(int resultCode, Intent data) {
        if (resultCode == FACEBOOK_RESULT && data != null && data.hasExtra(EXTRA_POSITION)) {
            return data.getIntExtra(EXTRA_POSITION, NO_POSITION);
        }
        return NO_POSITION;
    }

    public static List<Uri> getSelection(Intent data) {
        //noinspection unchecked
        return (List<Uri>) data.getExtras().get(EXTRA_SELECTION);
    }

    private FacebookMediaLoader mMediaLoader;
    private FacebookPreviewAdapter mAdapter;
    private ViewPager mViewPager;
    private CheckedTextView mCheckbox;
    private int mMinWidth;
    private int mMinHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setupTransition();
        }

        setTitle(null);

        // Postpone transition until the image of ViewPager's initial item is loaded
        supportPostponeEnterTransition();

        MediaSharedElementCallback sharedElementCallback = new MediaSharedElementCallback();
        setEnterSharedElementCallback(sharedElementCallback);

        //noinspection unchecked
        List<Uri> selection = (List<Uri>) getIntent().getExtras().get(EXTRA_SELECTION);
        assert selection != null;
        int maxSelection = getIntent().getExtras().getInt(EXTRA_MAX_SELECTION);

        mMinWidth = getIntent().getExtras().getInt(EXTRA_MIN_WIDTH);
        mMinHeight = getIntent().getExtras().getInt(EXTRA_MIN_HEIGHT);

        mCheckbox = (CheckedTextView) findViewById(R.id.check);
        mCheckbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAdapter.selectCurrentItem();
            }
        });

        mAdapter = new FacebookPreviewAdapter(this, mCheckbox, sharedElementCallback, selection);
        mAdapter.setCallbacks(this);
        mAdapter.setMaxSelection(maxSelection);
        mAdapter.setMinImageResolution(mMinWidth, mMinHeight);

        mViewPager = (ViewPager) findViewById(R.id.view_pager);
        mViewPager.setAdapter(mAdapter);

        mMediaLoader = new FacebookMediaLoader();
        mMediaLoader.onAttach(this, this);

        long bucketId = getIntent().getExtras().getLong(EXTRA_BUCKET_ID);
        mMediaLoader.loadByBucket(bucketId);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setupTransition() {
        TransitionInflater inflater = TransitionInflater.from(this);
        Transition sharedElementEnterTransition = inflater.inflateTransition(R.transition.shared_element);
        sharedElementEnterTransition.addListener(new TransitionCallback() {
            @Override
            public void onTransitionEnd(Transition transition) {
                mAdapter.setDontAnimate(false);
            }

            @Override
            public void onTransitionCancel(Transition transition) {
                mAdapter.setDontAnimate(false);
            }
        });
        getWindow().setSharedElementEnterTransition(sharedElementEnterTransition);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Nullable
    @Override
    public Intent getSupportParentActivityIntent() {
        //noinspection ConstantConditions
        return super.getSupportParentActivityIntent().addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    @Override
    public void onBucketLoadFinished(@Nullable Cursor data) {
        swapData(data);
    }

    @Override
    public void onMediaLoadFinished(@Nullable Cursor data) {
        swapData(data);
    }

    @Override
    public void onCheckedUpdated(boolean checked) {
        mCheckbox.setChecked(checked);
    }

    @Override
    public void onMaxSelectionReached() {
        Snackbar.make(mViewPager, R.string.activity_gallery_max_selection_reached, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onLowResImageSelected() {
        String snackbarText = getIntent().getStringExtra(EXTRA_ALERT_TEXT);
        if (snackbarText != null) {
            String lowResText = String.format(snackbarText, mMinWidth, mMinHeight);
            Snackbar.make(mViewPager, lowResText, Snackbar.LENGTH_LONG).show();
        } else {
            String lowResText = String.format(getString(R.string.activity_gallery_low_res_image_selected), mMinWidth, mMinHeight);
            Snackbar.make(mViewPager, lowResText, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void finish() {
        setResult();
        super.finish();
    }

    @Override
    public void finishAfterTransition() {
        setResult();
        super.finishAfterTransition();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMediaLoader.onDetach();
    }

    private void swapData(@Nullable Cursor data) {
        int position = getIntent().getExtras().getInt(EXTRA_POSITION);

        mAdapter.swapData(data);
        mAdapter.setInitialPosition(position);
        mViewPager.setCurrentItem(position, false);

        setCheckboxTransitionName(position);
    }

    private void setResult() {
        int position = mViewPager.getCurrentItem();

        Intent data = new Intent();
        data.putExtra(EXTRA_POSITION, position);
        data.putExtra(EXTRA_SELECTION, new LinkedList<>(mAdapter.getSelection()));
        setResult(FACEBOOK_RESULT, data);

        setCheckboxTransitionName(position);
    }

    private void setCheckboxTransitionName(int position) {
        Uri uri = mAdapter.getData(position);
        if (uri != null) {
            String checkboxTransitionName = getString(R.string.activity_gallery_checkbox_transition, uri.toString());
            ViewCompat.setTransitionName(mCheckbox, checkboxTransitionName);
        }
    }
}