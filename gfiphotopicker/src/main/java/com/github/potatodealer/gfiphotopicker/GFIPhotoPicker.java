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
    private int mRequestCode;
    private int mMaxSelection;
    private List<Uri> mSelection;
    private List<Uri> mInstagramSelection;

    private GFIPhotoPicker(@NonNull Activity activity) {
        mActivity = activity;
        mRequestCode = -1;
    }

    private GFIPhotoPicker(@NonNull Fragment fragment) {
        mFragment = fragment;
        mRequestCode = -1;
    }

    public static GFIPhotoPicker init(@NonNull Activity activity) {
        return new GFIPhotoPicker(activity);
    }

    public static GFIPhotoPicker init(@NonNull Fragment fragment) {
        return new GFIPhotoPicker(fragment);
    }

    /**
     * Set the Client ID for Instagram API
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
     * Set the current instagram selected items
     */
    public GFIPhotoPicker setInstagramSelection(@NonNull List<Uri> selection) {
        mInstagramSelection = selection;
        return this;
    }

    public void open() {
        if (mRequestCode == -1) {
            throw new IllegalArgumentException("You need to define a request code in setRequestCode(int) method");
        }
        if (mActivity != null) {
            PhotoPickerActivity.startActivity(mActivity, mInstagramClientId, mInstagramRedirectUri, mRequestCode, mMaxSelection, mSelection, mInstagramSelection);
        } else {
            PhotoPickerActivity.startActivity(mFragment, mInstagramClientId, mInstagramRedirectUri, mRequestCode, mMaxSelection, mSelection, mInstagramSelection);
        }
    }


    /**
     * ---------------------------------------------------------------------------------------------
     * Instagram Photo Picker
     * ---------------------------------------------------------------------------------------------
     */
    static final String PREFERENCE_FILE = "com.github.potatodealer.gfiphotopicker.PREFERENCE_FILE";
    static final String PREFERENCE_ACCESS_TOKEN = "com.github.potatodealer.gfiphotopicker.PREFERENCE_ACCESS_TOKEN";
    static final String PREFERENCE_CLIENT_ID = "com.github.potatodealer.gfiphotopicker.PREFERENCE_CLIENT_ID";
    static final String PREFERENCE_REDIRECT_URI = "com.github.potatodealer.gfiphotopicker.PREFERENCE_REDIRECT_URI";

    public static String cachedAccessToken = null;
    public static String cachedClientId = null;
    static String cachedRedirectUri = null;

    public static String getAccessToken(Context context) {
        if (cachedAccessToken == null) {
            loadInstagramPreferences(context);
        }
        return cachedAccessToken;
    }

    public static String getClientId(Context context) {
        if (cachedClientId == null) {
            loadInstagramPreferences(context);
        }
        return cachedClientId;
    }

    public static String getRedirectUri(Context context) {
        if (cachedRedirectUri == null) {
            loadInstagramPreferences(context);
        }
        return cachedRedirectUri;
    }

    private static void loadInstagramPreferences(Context context) {
        Context applicationContext = context.getApplicationContext();
        SharedPreferences preferences = applicationContext.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE);
        cachedAccessToken = preferences.getString(PREFERENCE_ACCESS_TOKEN, null);
        cachedClientId = preferences.getString(PREFERENCE_CLIENT_ID, null);
        cachedRedirectUri = preferences.getString(PREFERENCE_REDIRECT_URI, null);
    }

    public static void saveInstagramPreferences(Context context, String accessToken, String clientId, String redirectURI) {
        Context applicationContext = context.getApplicationContext();
        SharedPreferences preferences = applicationContext.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PREFERENCE_ACCESS_TOKEN, accessToken);
        editor.putString(PREFERENCE_CLIENT_ID, clientId);
        editor.putString(PREFERENCE_REDIRECT_URI, redirectURI);
        editor.apply();
        cachedAccessToken = accessToken;
        cachedClientId = clientId;
        cachedRedirectUri = redirectURI;
    }

    public static void logout(Context context) {
        if (context != null) {
            Context applicationContext = context.getApplicationContext();
            SharedPreferences preferences = applicationContext.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.remove(PREFERENCE_ACCESS_TOKEN);
            editor.remove(PREFERENCE_CLIENT_ID);
            editor.remove(PREFERENCE_REDIRECT_URI);
            editor.apply();
            cachedAccessToken = null;
            cachedClientId = null;
            cachedRedirectUri = null;

            CookieSyncManager.createInstance(context);
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.removeAllCookie();
            new WebView(context).clearCache(true);
        }
    }

}
