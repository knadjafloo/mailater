package com.threebars.mailater.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MySQLiteHelper extends SQLiteOpenHelper {

	public static final String TABLE_EMAILS = "emails";

		
	public static enum COLUMN_NAMES {

		COLUMN_ID("_id", 0),
		COLUMN_SUBJECT( "_subject", 1),
		COLUMN_RECEPIENTS("_recepients", 2),
		COLUMN_FROM("_from", 3),
		COLUMN_BODY("_body", 4),
		COLUMN_STATUS("status", 5),
		COLUMN_DATE("date", 6);
		
		private String columnName;
		private int columnIndex;

		COLUMN_NAMES(String columnName, int columnIndex) {
			this.columnName = columnName;
			this.columnIndex = columnIndex;
		}
		
		public int getIndex() {
			return this.columnIndex;
		}
		
		public String getName() {
			return this.columnName;
		}
		
		public String toString() {
			return this.columnName;
		}

	};
	private static final String TAG = "MySQLiteHelper";
	
	private static final String DATABASE_NAME = "mailater.db";
	private static final int DATABASE_VERSION = 1;

	private final Context context;
	
	// Database creation sql statement
	private static final String DATABASE_CREATE = "create table " + TABLE_EMAILS + "( " + COLUMN_NAMES.COLUMN_ID + " integer primary key autoincrement, " + COLUMN_NAMES.COLUMN_SUBJECT
			+ " text not null,"  + COLUMN_NAMES.COLUMN_RECEPIENTS
			+ " text not null,"  + COLUMN_NAMES.COLUMN_FROM
			+ " text not null,"  + COLUMN_NAMES.COLUMN_BODY
			+ " text not null,"  + COLUMN_NAMES.COLUMN_STATUS
			+ " text not null,"  + COLUMN_NAMES.COLUMN_DATE
			+ " text not null);";

	public MySQLiteHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		this.context = context;
	}

	@Override
	public void onCreate(SQLiteDatabase database) {
		database.execSQL(DATABASE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(MySQLiteHelper.class.getName(), "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
		db.execSQL("DROP TABLE IF EXISTS" + TABLE_EMAILS);
		onCreate(db);
	}

}
