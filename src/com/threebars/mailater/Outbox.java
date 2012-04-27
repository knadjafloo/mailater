package com.threebars.mailater;

import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.SimpleCursorAdapter;

import com.threebars.mailater.database.EmailsDAO;
import com.threebars.mailater.database.MySQLiteHelper.COLUMN_NAMES;

public class Outbox extends ListActivity  {

	EmailsDAO emailsDao;
	
	protected SimpleCursorAdapter adapter;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pending_emails);
		emailsDao = new EmailsDAO(this);
		emailsDao.open();
		//TODO do this using fragments
		Cursor cursor = emailsDao.getCursorForAllEmailsWithStatus(EmailsDAO.SENT);
		if(cursor != null)
		{
			adapter = new SimpleCursorAdapter(
			            this,
			            R.layout.email_row,
			            cursor,                                              
			            new String[] {COLUMN_NAMES.COLUMN_SUBJECT.getName(), COLUMN_NAMES.COLUMN_BODY.getName(), COLUMN_NAMES.COLUMN_DATE.getName()},           
			            new int[] {R.id.emailSubject, R.id.emailText, R.id.emailDate},
			            0);
			
			setListAdapter(adapter);
			startManagingCursor(cursor);
		}
		emailsDao.close();
	}
	
}
