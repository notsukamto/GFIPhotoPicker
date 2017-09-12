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

public class InstagramProvider extends ContentProvider {

    private static String AUTHORITY = "com.github.potatodealer.gfiphotopicker.data.instagram";
    private static final String BASE_PATH = "instagram";
    public static Uri INSTAGRAM_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH );

    private static final int INSTAGRAM = 1;
    private static final int INSTAGRAM_ID = 2;

    public static void initAuthority(String authority) {
        /*String authority = "com.github.potatodealer.gfiphotopicker.data.instagram";

        try {
            ClassLoader loader = FacebookProvider.class.getClassLoader();
            Class<?> cls = loader.loadClass("com.github.potatodealer.gfiphotopicker.activity.PhotoPickerActivity");
            Field declaredField = cls.getDeclaredField("EXTRA_INSTAGRAM_AUTHORITY");

            authority = declaredField.get(null).toString();
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }*/

        AUTHORITY = authority;

        INSTAGRAM_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH );

        uriMatcher.addURI(AUTHORITY,BASE_PATH, INSTAGRAM);
        uriMatcher.addURI(AUTHORITY,BASE_PATH + "/#", INSTAGRAM_ID);
    }

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    private SQLiteDatabase db;

    @Override
    public boolean onCreate() {
        InstagramDBHelper helper = new InstagramDBHelper(getContext());
        db = helper.getWritableDatabase();
        return db != null;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        Cursor cursor;
        switch (uriMatcher.match(uri)) {
            case INSTAGRAM:
                cursor =  db.query(InstagramDBHelper.TABLE_INSTAGRAM, InstagramDBHelper.ALL_IMAGE_PROJECTION,
                        null,null,null,null,InstagramDBHelper._ID +" DESC");
                break;
            default:
                throw new IllegalArgumentException("This is an Unknown URI " + uri);
        }
        cursor.setNotificationUri(getContext().getContentResolver(),uri);

        return cursor;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {

        switch (uriMatcher.match(uri)) {
            case INSTAGRAM:
                return null;
            default:
                throw new IllegalArgumentException("This is an Unknown URI " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        long id = db.insert(InstagramDBHelper.TABLE_INSTAGRAM, null, contentValues);

        if (id > 0) {
            Uri _uri = ContentUris.withAppendedId(INSTAGRAM_URI, id);
            getContext().getContentResolver().notifyChange(_uri, null);
            return _uri;
        }
        throw new SQLException("Insertion Failed for URI :" + uri);

    }

    @Override
    public int delete(Uri uri, String s, String[] strings) {
        int delCount = 0;
        switch (uriMatcher.match(uri)) {
            case INSTAGRAM:
                delCount =  db.delete(InstagramDBHelper.TABLE_INSTAGRAM,s,strings);
                break;
            default:
                throw new IllegalArgumentException("This is an Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return delCount;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String s, String[] strings) {
        int updCount = 0;
        switch (uriMatcher.match(uri)) {
            case INSTAGRAM:
                updCount =  db.update(InstagramDBHelper.TABLE_INSTAGRAM,contentValues,s,strings);
                break;
            default:
                throw new IllegalArgumentException("This is an Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return updCount;
    }

}