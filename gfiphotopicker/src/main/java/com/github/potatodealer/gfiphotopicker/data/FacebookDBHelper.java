package com.github.potatodealer.gfiphotopicker.data;


import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.github.potatodealer.gfiphotopicker.FacebookAgent;

import java.net.URISyntaxException;
import java.util.List;

public class FacebookDBHelper extends SQLiteOpenHelper {

    //Constants for db name and version
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "facebookManager";

    //Constants for table and columns
    public static final String TABLE_FACEBOOK = "facebook";
    public static final String _ID = "id";
    public static final String BUCKET_ID = "bucket_id";
    public static final String DISPLAY_NAME = "name";
    public static final String BUCKET_DISPLAY_NAME = "bucket_name";
    public static final String DATA = "data";
    public static final String WIDTH = "width";
    public static final String HEIGHT = "height";

    private List<FacebookAgent.Album> mAlbumList;


    public FacebookDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static final String[] IMAGE_PROJECTION = {_ID, BUCKET_ID, DISPLAY_NAME, DATA, WIDTH, HEIGHT};

    public static final String[] ALL_IMAGE_PROJECTION = {_ID, FacebookMediaLoader.ALL_MEDIA_BUCKET_ID + " AS " + BUCKET_ID, DISPLAY_NAME, DATA, WIDTH, HEIGHT};

    public static final String[] BUCKET_PROJECTION = {BUCKET_ID, BUCKET_DISPLAY_NAME, DATA};

    private static final String CREATE_FACEBOOK_TABLE = "CREATE TABLE " + TABLE_FACEBOOK + "("
            + _ID + " INTEGER PRIMARY KEY," + BUCKET_ID + " INTEGER,"
            + DISPLAY_NAME + " TEXT," + BUCKET_DISPLAY_NAME + " TEXT,"
            + DATA + " TEXT," + WIDTH + " INTEGER," + HEIGHT + " INTEGER" + ")";

    public static final String BUCKET_SELECTION = "(1) GROUP BY (1)";

    //Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_FACEBOOK_TABLE);
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FACEBOOK);

        // Create tables again
        onCreate(db);
    }

    /**
     * All CRUD(Create, Read, Update, Delete) Operations
     */

    public void addAlbumInfo(List<FacebookAgent.Album> facebookAlbum) {
        mAlbumList = facebookAlbum;
    }

    //Adding new Facebook photo
    public void addFacebookPhoto(List<FacebookAgent.Photo> facebookPhoto, int albumCount) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        try {
            for (int i = 0; i < facebookPhoto.size(); i++) {
                String id = facebookPhoto.get(i).getId();
                String url = facebookPhoto.get(i).getFullURL().toURI().toString();
                String name = url.substring(url.lastIndexOf("/") + 1, url.lastIndexOf("?"));
                String bucketName = mAlbumList.get(albumCount).getName();
                int width = facebookPhoto.get(i).getFullWidth();
                int height = facebookPhoto.get(i).getFullHeight();
                values.put(_ID, id);
                values.put(BUCKET_ID, albumCount + 1);
                values.put(DISPLAY_NAME, name);
                values.put(BUCKET_DISPLAY_NAME, bucketName);
                values.put(DATA, url);
                values.put(WIDTH, width);
                values.put(HEIGHT, height);

                // Inserting Row
                db.insert(TABLE_FACEBOOK, null, values);
            }
            db.close(); // Closing database connection
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    // Deleting table
    public void deleteAllFacebookPhotos() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_FACEBOOK, null, null);
        db.close();
    }
}
