package com.github.potatodealer.gfiphotopicker.data;


import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.net.URISyntaxException;

public class InstagramDBHelper extends SQLiteOpenHelper {

    //Constants for db name and version
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "instagramManager";

    //Constants for table and columns
    public static final String TABLE_INSTAGRAM = "instagram";
    public static final String KEY_ID = "id";
    public static final String KEY_NAME = "name";
    public static final String KEY_DATA = "data";

    public InstagramDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static final String[] ALL_COLUMNS = {KEY_ID, KEY_NAME, KEY_DATA};

    private static final String CREATE_INSTAGRAM_TABLE = "CREATE TABLE " + TABLE_INSTAGRAM + "("
            + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + KEY_NAME + " TEXT,"
            + KEY_DATA + " TEXT" + ")";

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
    public void addInstagramPhoto(InstagramPhoto instagramPhoto) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        try {
            String url = instagramPhoto.getFullURL().toURI().toString();
            String name = url.substring(url.lastIndexOf("/") + 1);
            values.put(KEY_NAME, name);
            values.put(KEY_DATA, url);
            Log.v("Insta URL get",name);

            // Inserting Row
            db.insert(TABLE_INSTAGRAM, null, values);
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
