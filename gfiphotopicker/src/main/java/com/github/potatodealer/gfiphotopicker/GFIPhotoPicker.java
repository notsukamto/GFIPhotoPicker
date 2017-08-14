package com.github.potatodealer.gfiphotopicker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatDelegate;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;

import com.github.potatodealer.gfiphotopicker.activity.PhotoPickerActivity;

import java.util.List;

public class GFIPhotoPicker {

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    private Activity mActivity;
    private Fragment mFragment;
    private String mInstagramClientId;
    private String mInstagramRedirectUri;
    private String mAlertText;
    private int mRequestCode;
    private int mMaxSelection;
    private int mMinHeight;
    private int mMinWidth;
    private List<Uri> mSelection;
    private List<Uri> mInstagramSelection;
    private List<Uri> mFacebookSelection;

    private GFIPhotoPicker(@NonNull Activity activity) {
        mActivity = activity;
        mRequestCode = -1;
        mMinWidth = 0;
        mMinHeight = 0;
    }

    private GFIPhotoPicker(@NonNull Fragment fragment) {
        mFragment = fragment;
        mRequestCode = -1;
        mMinWidth = 0;
        mMinHeight = 0;
    }

    public static GFIPhotoPicker init(@NonNull Activity activity) {
        return new GFIPhotoPicker(activity);
    }

    public static GFIPhotoPicker init(@NonNull Fragment fragment) {
        return new GFIPhotoPicker(fragment);
    }

    /**
     * Set the Client _ID for Instagram API
     */
    public GFIPhotoPicker setInstagramClientId(String instagramClientId) {
        mInstagramClientId = instagramClientId;
        return this;
    }

    /**
     * Set the Redirect URI for Instagram API
     */
    public GFIPhotoPicker setInstagramRedirectUri(String instagramRedirectUri) {
        mInstagramRedirectUri = instagramRedirectUri;
        return this;
    }

    /**
     * Set the request code to return on {@link Activity#onActivityResult(int, int, Intent)}
     */
    public GFIPhotoPicker setRequestCode(int requestCode) {
        mRequestCode = requestCode;
        return this;
    }

    /**
     * Set the max images allowed to pick
     */
    public GFIPhotoPicker setMaxSelection(@IntRange(from = 0) int maxSelection) {
        mMaxSelection = maxSelection;
        return this;
    }

    /**
     * Set the current selected items
     */
    public GFIPhotoPicker setSelection(@NonNull List<Uri> selection) {
        mSelection = selection;
        return this;
    }

    /**
     * Set the current facebook selected items
     */
    public GFIPhotoPicker setFacebookSelection(@NonNull List<Uri> selection) {
        mFacebookSelection = selection;
        return this;
    }

    /**
     * Set the current instagram selected items
     */
    public GFIPhotoPicker setInstagramSelection(@NonNull List<Uri> selection) {
        mInstagramSelection = selection;
        return this;
    }

    public GFIPhotoPicker setMinImageResolution(int minWidth, int minHeight) {
        mMinWidth = minWidth;
        mMinHeight = minHeight;
        return this;
    }

    public GFIPhotoPicker setLowResolutionAlertText(String alertText) {
        mAlertText = alertText;
        return this;
    }

    public void open() {
        if (mRequestCode == -1) {
            throw new IllegalArgumentException("You need to define a request code in setRequestCode(int) method");
        }
        if (mActivity != null) {
            PhotoPickerActivity.startActivity(mActivity, mInstagramClientId, mInstagramRedirectUri, mRequestCode, mMaxSelection, mSelection, mFacebookSelection, mInstagramSelection, mMinWidth, mMinHeight, mAlertText);
        } else {
            PhotoPickerActivity.startActivity(mFragment, mInstagramClientId, mInstagramRedirectUri, mRequestCode, mMaxSelection, mSelection, mFacebookSelection, mInstagramSelection, mMinWidth, mMinHeight, mAlertText);
        }
    }


    /**
     * ---------------------------------------------------------------------------------------------
     * Facebook Photo Picker
     * ---------------------------------------------------------------------------------------------
     */

}
