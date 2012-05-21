package com.threebars.mailater.database;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.text.format.Time;

import com.threebars.mailater.model.DelayedEmail;
import static com.threebars.mailater.database.MySQLiteHelper.COLUMN_NAMES.*;

public class EmailsDAO {
	// Database fields
	private SQLiteDatabase database;
	private MySQLiteHelper dbHelper;
	
	// email statuses in the database
	public final static String SENT = "S";
	public final static String PENDING = "P";
	public final static String CANCELED = "C";
	
	public EmailsDAO(Context context) {
		dbHelper = new MySQLiteHelper(context);
	}

	public void open() throws SQLException {
		database = dbHelper.getWritableDatabase();
	}

	public void close() {
		dbHelper.close();
	}
	
	public SQLiteDatabase getDatabase() {
		return this.database;
	}
	
	public DelayedEmail createEmail(String from, String recepients, String subject, String body, String date, String attachment) {
		ContentValues values = new ContentValues();
		values.put(COLUMN_FROM.getName(), from);
		values.put(COLUMN_BODY.getName(), body);
		values.put(COLUMN_SUBJECT.getName(), subject);
		values.put(COLUMN_RECEPIENTS.getName(), recepients);
		values.put(COLUMN_STATUS.getName(), PENDING);
		values.put(COLUMN_DATE.getName(), date);
		values.put(COLUMN_ATTACHMENT.getName(), attachment);
		long insertId = database.insert(MySQLiteHelper.TABLE_EMAILS, null, values);
		// To show how to query
		Cursor cursor = database.query(MySQLiteHelper.TABLE_EMAILS, getAllColumns(), COLUMN_ID + " = " + insertId, null, null, null, null);
		cursor.moveToFirst();
		return cursorToEmail(cursor);
	}

	public void deleteEmail(DelayedEmail comment) {
		long id = comment.getId();
		System.out.println("DelayedEmail deleted with id: " + id);
		database.delete(MySQLiteHelper.TABLE_EMAILS, MySQLiteHelper.COLUMN_NAMES.COLUMN_ID + " = " + id, null);
	}
	
	public void deleteEmail(long id) {
		database.delete(MySQLiteHelper.TABLE_EMAILS, MySQLiteHelper.COLUMN_NAMES.COLUMN_ID + " = " + id, null);
	}

	public DelayedEmail getEmail(long id) {
		Cursor cursor = database.query(MySQLiteHelper.TABLE_EMAILS, getAllColumns(), COLUMN_ID + " = " + id, null, null, null, null);
		cursor.moveToFirst();
		return cursorToEmail(cursor);
	}
	
	public int updateEmailStatus(long id, String newStatus) {
		ContentValues args = new ContentValues();
	    args.put(COLUMN_STATUS.getName(), newStatus);
	    return database.update(MySQLiteHelper.TABLE_EMAILS, args, COLUMN_ID + " = " + id, null);
	}
	
	public List<DelayedEmail> getAllEmails() {
		List<DelayedEmail> emails = new ArrayList<DelayedEmail>();
		Cursor cursor = database.query(MySQLiteHelper.TABLE_EMAILS, getAllColumns(), null, null, null, null, null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			DelayedEmail comment = cursorToEmail(cursor);
			emails.add(comment);
			cursor.moveToNext();
		}
		// Make sure to close the cursor
		cursor.close();
		return emails;
	}
	
	public List<DelayedEmail> getAllEmailsWithStatus(String status) {
		List<DelayedEmail> emails = new ArrayList<DelayedEmail>();
		Cursor cursor = database.query(MySQLiteHelper.TABLE_EMAILS, getAllColumns(), COLUMN_STATUS + " = ?" , new String[] {status}, null, null, null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			DelayedEmail comment = cursorToEmail(cursor);
			emails.add(comment);
			cursor.moveToNext();
		}
		// Make sure to close the cursor
		cursor.close();
		return emails;
	}
	
	public Cursor getCursorForAllEmailsWithStatus(String status) {
		Cursor cursor = database.query(MySQLiteHelper.TABLE_EMAILS, getAllColumns(), COLUMN_STATUS + " = ?" , new String[] {status}, null, null, null);
		return cursor;
	}

	private DelayedEmail cursorToEmail(Cursor cursor) {
		DelayedEmail delayedEmail = new DelayedEmail();
		delayedEmail.setId(cursor.getLong(COLUMN_ID.getIndex()));
		delayedEmail.setBody(cursor.getString(COLUMN_BODY.getIndex()));
		delayedEmail.setSender(cursor.getString(COLUMN_FROM.getIndex()));
		delayedEmail.setReceipients(cursor.getString(COLUMN_RECEPIENTS.getIndex()));
		delayedEmail.setSubject(cursor.getString(COLUMN_SUBJECT.getIndex()));
		delayedEmail.setStatus(cursor.getString(COLUMN_STATUS.getIndex()));
		delayedEmail.setAttachments(cursor.getString(COLUMN_ATTACHMENT.getIndex()));
		String timeString = cursor.getString(COLUMN_DATE.getIndex());
		DateFormat df = new SimpleDateFormat("MMM dd, yyyy h:mmaa");
		try {
			Date date = df.parse(timeString);
			Time time = new Time();
			time.set(date.getTime());
			delayedEmail.setTime(time);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return delayedEmail;
	}
	
	private String[] getAllColumns() {
		String[] arr = new String[MySQLiteHelper.COLUMN_NAMES.values().length];
		int index = 0;
		for(MySQLiteHelper.COLUMN_NAMES col: MySQLiteHelper.COLUMN_NAMES.values()) {
			arr[index++] = col.getName();
		}
		return arr;
		
	}
}

