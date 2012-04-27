package com.threebars.mailater;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import com.threebars.mailater.database.EmailsDAO;
import com.threebars.mailater.model.DelayedEmail;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.Time;
import android.widget.TextView;

public class MailViewActivity extends Activity {

	EmailsDAO emailsDao;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.email_detail);

		//get the rowId
		Bundle bundle = getIntent().getExtras();
		
		long rowId = bundle.getLong("rowId");
		
		new PopulateFieldsTask().execute(rowId);
		
	}

	@Override
	protected void onPause() {
		if(emailsDao != null) {
			emailsDao.close();
		}
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		if(emailsDao != null) {
			emailsDao.close();
		}
		super.onDestroy();
	}
	
	class PopulateFieldsTask extends AsyncTask<Long, Void, DelayedEmail> {

		@Override
		protected DelayedEmail doInBackground(Long... rowIds) {
			
			long rowId = rowIds[0];
			emailsDao = new EmailsDAO(MailViewActivity.this);
			emailsDao.open();
			DelayedEmail email = emailsDao.getEmail(rowId);
			return email;
		}
		
		@Override
		protected void onPostExecute(DelayedEmail email) {
			TextView timeView = (TextView) findViewById(R.id.when);
			TextView fromView = (TextView) findViewById(R.id.from);
			TextView toView = (TextView) findViewById(R.id.to);
			TextView subjectView = (TextView)findViewById(R.id.subject);
			TextView emailBodyView = (TextView)findViewById(R.id.mailText);
			
			Calendar cal = Calendar.getInstance();
			Time time = email.getTime();
			if(time != null)
			{
				cal.setTimeInMillis(time.toMillis(false));
				SimpleDateFormat df = new SimpleDateFormat("MMM dd, yyyy h:mmaa");
				timeView.setText(df.format(cal.getTime()));
			}
			fromView.setText(email.getSender());
			toView.setText(email.getReceipients());
			subjectView.setText(email.getSubject());
			emailBodyView.setText(email.getBody());
		}
		
	}
}
