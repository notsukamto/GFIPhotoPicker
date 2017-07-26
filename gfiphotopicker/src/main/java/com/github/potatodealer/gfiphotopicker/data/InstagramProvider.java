package com.github.potatodealer.gfiphotopicker.data;


import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.Nullable;

public class InstagramProvider extends ContentProvider {

    private static final String AUTHORITY = "com.github.potatodealer.gfiphotopicker.data";
    private static final String BASE_PATH = "instagram";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH );

    private static final int INSTAGRAM = 1;
    private static final int INSTAGRAM_ID = 2;

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        uriMatcher.addURI(AUTHORITY,BASE_PATH, INSTAGRAM);
        uriMatcher.addURI(AUTHORITY,BASE_PATH + "/#", INSTAGRAM_ID);
    }

    private SQLiteDatabase database;

    @Override
    public boolean onCreate() {
        InstagramDBHelper helper = new InstagramDBHelper(getContext());
        database = helper.getWritableDatabase();
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] strings, String s, String[] strings1, String s1) {
        Cursor cursor;
        switch (uriMatcher.match(uri)) {
            case INSTAGRAM:
                cursor =  database.query(InstagramDBHelper.TABLE_INSTAGRAM, InstagramDBHelper.ALL_COLUMNS,
                        s,null,null,null,InstagramDBHelper.KEY_ID +" DESC");
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
                return "vnd.android.cursor.dir/contacts";
            default:
                throw new IllegalArgumentException("This is an Unknown URI " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        long id = database.insert(InstagramDBHelper.TABLE_INSTAGRAM,null,contentValues);

        if (id > 0) {
            Uri _uri = ContentUris.withAppendedId(CONTENT_URI, id);
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
                delCount =  database.delete(InstagramDBHelper.TABLE_INSTAGRAM,s,strings);
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
                updCount =  database.update(InstagramDBHelper.TABLE_INSTAGRAM,contentValues,s,strings);
                break;
            default:
                throw new IllegalArgumentException("This is an Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return updCount;
    }

}
