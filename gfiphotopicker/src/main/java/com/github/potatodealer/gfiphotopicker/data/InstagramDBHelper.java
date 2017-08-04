package com.github.potatodealer.gfiphotopicker.data;


import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.github.potatodealer.gfiphotopicker.InstagramAgent;

import java.net.URISyntaxException;
import java.util.List;

public class InstagramDBHelper extends SQLiteOpenHelper {

    //Constants for db name and version
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "instagramManager";

    //Constants for table and columns
    public static final String TABLE_INSTAGRAM = "instagram";
    public static final String _ID = "id";
    public static final String DISPLAY_NAME = "name";
    public static final String DATA = "data";

    public InstagramDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static final String[] ALL_IMAGE_PROJECTION = {_ID, DISPLAY_NAME, DATA};

    private static final String CREATE_INSTAGRAM_TABLE = "CREATE TABLE " + TABLE_INSTAGRAM + "("
            + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + DISPLAY_NAME + " TEXT,"
            + DATA + " TEXT" + ")";

    //Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_INSTAGRAM_TABLE);
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_INSTAGRAM);

        // Create tables again
        onCreate(db);
    }

    /**
     * All CRUD(Create, Read, Update, Delete) Operations
     */

    //Adding new Instagram photo
    public void addInstagramPhoto(List<InstagramAgent.InstagramPhoto> instagramPhoto) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        try {
            for (int i = 0; i < instagramPhoto.size(); i++) {
                String url = instagramPhoto.get(i).getFullURL().toURI().toString();
                String name = url.substring(url.lastIndexOf("/") + 1);
                values.put(DISPLAY_NAME, name);
                values.put(DATA, url);

                // Inserting Row
                db.insert(TABLE_INSTAGRAM, null, values);
            }
            db.close(); // Closing database connection
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    // Deleting table
    public void deleteAllInstagramPhotos() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_INSTAGRAM, null, null);
        db.close();
    }
}
