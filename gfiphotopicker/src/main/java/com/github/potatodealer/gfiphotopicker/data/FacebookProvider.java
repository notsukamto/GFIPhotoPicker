package com.github.potatodealer.gfiphotopicker.data;


import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.lang.reflect.Field;

public class FacebookProvider extends ContentProvider {

    private static String AUTHORITY = "com.github.potatodealer.gfiphotopicker.data.facebook";
    private static final String PATH_ALL_IMAGE = "all_image";
    private static final String PATH_BUCKET = "bucket";
    private static final String PATH_IMAGE = "image";
    public static Uri FACEBOOK_ALL_IMAGE_URI = Uri.parse("content://" + AUTHORITY + "/" + PATH_ALL_IMAGE);
    public static Uri FACEBOOK_BUCKET_URI = Uri.parse("content://" + AUTHORITY + "/" + PATH_BUCKET);
    public static Uri FACEBOOK_IMAGE_URI = Uri.parse("content://" + AUTHORITY + "/" + PATH_IMAGE);

    private static final int ALL_IMAGE = 1;
    private static final int ALL_IMAGE_ID = 2;
    private static final int BUCKET = 3;
    private static final int BUCKET_ID = 4;
    private static final int IMAGE = 5;
    private static final int IMAGE_ID = 6;

    public static void initAuthority(String authority) {
        /*String authority = "com.github.potatodealer.gfiphotopicker.data.facebook";

        try {
            ClassLoader loader = FacebookProvider.class.getClassLoader();
            Class<?> cls = loader.loadClass("com.github.potatodealer.gfiphotopicker.activity.PhotoPickerActivity");
            Field declaredField = cls.getDeclaredField("EXTRA_FACEBOOK_AUTHORITY");

            authority = declaredField.get(null).toString();
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }*/

        AUTHORITY = authority;

        FACEBOOK_ALL_IMAGE_URI = Uri.parse("content://" + AUTHORITY + "/" + PATH_ALL_IMAGE);
        FACEBOOK_BUCKET_URI = Uri.parse("content://" + AUTHORITY + "/" + PATH_BUCKET);
        FACEBOOK_IMAGE_URI = Uri.parse("content://" + AUTHORITY + "/" + PATH_IMAGE);

        uriMatcher.addURI(AUTHORITY, PATH_ALL_IMAGE, ALL_IMAGE);
        uriMatcher.addURI(AUTHORITY, PATH_ALL_IMAGE + "/#", ALL_IMAGE_ID);
        uriMatcher.addURI(AUTHORITY, PATH_BUCKET, BUCKET);
        uriMatcher.addURI(AUTHORITY, PATH_BUCKET + "/#", BUCKET_ID);
        uriMatcher.addURI(AUTHORITY, PATH_IMAGE, IMAGE);
        uriMatcher.addURI(AUTHORITY, PATH_IMAGE + "/#", IMAGE_ID);
    }

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    private SQLiteDatabase db;

    @Override
    public boolean onCreate() {
        FacebookDBHelper helper = new FacebookDBHelper(getContext());
        db = helper.getWritableDatabase();
        return db != null;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        Cursor cursor;
        Log.d("uri", "" + uriMatcher.match(uri));
        switch (uriMatcher.match(uri)) {
            case ALL_IMAGE:
                cursor =  db.query(FacebookDBHelper.TABLE_FACEBOOK, FacebookDBHelper.ALL_IMAGE_PROJECTION,
                        null, null, null, null, FacebookDBHelper._ID +" DESC");
                break;
            case BUCKET:
                cursor =  db.query(FacebookDBHelper.TABLE_FACEBOOK, FacebookDBHelper.BUCKET_PROJECTION,
                        String.format("%s", FacebookDBHelper.BUCKET_SELECTION), null, null, null, FacebookDBHelper.BUCKET_ID +" ASC");
                break;
            case IMAGE:
                cursor =  db.query(FacebookDBHelper.TABLE_FACEBOOK, FacebookDBHelper.IMAGE_PROJECTION,
                        String.format("%s=%s", FacebookDBHelper.BUCKET_ID, FacebookMediaLoader.bucketId), null, null, null, FacebookDBHelper._ID +" DESC");
                break;
            default:
                throw new IllegalArgumentException("This is an Unknown URI " + uri);
        }
        cursor.setNotificationUri(getContext().getContentResolver(),uri);

        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        Log.d("getType", "Initialized");
        switch (uriMatcher.match(uri)) {
            case BUCKET:
                return null;
            default:
                throw new IllegalArgumentException("This is an Unknown URI " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        Log.d("insert", "Initialized");

        long id = db.insert(FacebookDBHelper.TABLE_FACEBOOK,null,contentValues);

        if (id > 0) {
            Uri _uri = ContentUris.withAppendedId(FACEBOOK_BUCKET_URI, id);
            getContext().getContentResolver().notifyChange(_uri, null);
            return _uri;
        }
        throw new SQLException("Insertion Failed for URI :" + uri);
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String s, @Nullable String[] strings) {
        Log.d("delete", "Initialized");

        int delCount = 0;
        switch (uriMatcher.match(uri)) {
            case BUCKET:
                delCount =  db.delete(FacebookDBHelper.TABLE_FACEBOOK,s,strings);
                break;
            default:
                throw new IllegalArgumentException("This is an Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return delCount;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String s, @Nullable String[] strings) {
        Log.d("update", "Initialized");

        int updCount = 0;
        switch (uriMatcher.match(uri)) {
            case BUCKET:
                updCount =  db.update(FacebookDBHelper.TABLE_FACEBOOK,contentValues,s,strings);
                break;
            default:
                throw new IllegalArgumentException("This is an Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return updCount;
    }
}