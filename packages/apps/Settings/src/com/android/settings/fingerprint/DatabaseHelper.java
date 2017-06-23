package com.android.settings.fingerprint;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final int  VERSON = 1;

    public DatabaseHelper(Context context, String name, CursorFactory factory, int verson){
        super(context, name, factory, verson);
    }

    public DatabaseHelper(Context context, String name, int verson){
        this(context, name, null, verson);
    }

    public DatabaseHelper(Context context, String name){
        this(context, name, VERSON);
    }


    @Override
    public void onCreate(SQLiteDatabase arg0) {
        // arg0.execSQL("create table fingerprint1(id int, name varchar(20), mode int)");
        // arg0.execSQL("create table fingerprint2(id int, name varchar(20))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {

    }

}
