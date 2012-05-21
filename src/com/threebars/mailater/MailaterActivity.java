package com.threebars.mailater;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.threebars.mailater.database.EmailsDAO;
import com.threebars.mailater.model.DelayedEmail;
import com.threebars.mailater.services.SendMailService;
import com.threebars.mailater.util.MailaterUtil;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.InputType;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MailaterActivity extends Activity {
	private static final String TAG = "Mailater";

	private static Level LOGGING_LEVEL = Level.CONFIG; // Level.OFF;

	private static final String PREF = "MyPrefs";
	private static final int DIALOG_ACCOUNTS = 0;
	private static final int GET_PASSWORD = 1;
	private static final int GET_DATETIME = 2;

	private final static int GET_SCHEDULE = 100; // request code for get schedule

	private String accountName;
	private String accountType;
	AccountManager accountManager;
//	private final static String HEX = "A5414CF7A433D";
	// private Account account;
	private Time time;

	private TextView whenView;


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		whenView = (TextView) findViewById(R.id.when);
		whenView.setText("");
		time = null; // reset time

		Bundle extras = getIntent().getExtras();
		accountName = extras.getString("account");
		accountType = extras.getString("accountType");
		ArrayList<String> emailContacts = extras.getStringArrayList("emailContacts");

		if (accountName != null) {
			TextView from = (TextView) findViewById(R.id.from);
			from.setText(accountName);
		}
		// add the contacts to the 'To' field of email
		AutoCompleteTextView recipientFieldTextView = (AutoCompleteTextView) findViewById(R.id.to);

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.list_item, emailContacts);
		recipientFieldTextView.setAdapter(adapter);



		Button sendButton = (Button) findViewById(R.id.sendButtonId);
		sendButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String passwd = MailaterActivity.this.getSharedPreferences(accountName, Context.MODE_PRIVATE).getString(accountName, null);
				if (passwd == null) {
					showDialog(GET_PASSWORD);
				} else {
					// validate required fields are there
					if (validateRequiredFields()) {
						EmailsDAO datasource = new EmailsDAO(MailaterActivity.this);
						datasource.open();

						EditText subject = (EditText) findViewById(R.id.subject);
						EditText recepients = (EditText) findViewById(R.id.to);
						String from = accountName;
						EditText body = (EditText) findViewById(R.id.mailText);

						StringBuilder recepientList = new StringBuilder();
						StringTokenizer tokenizer = new StringTokenizer(recepients.getText().toString(), ",");
						while (tokenizer.hasMoreTokens()) {
							String email = tokenizer.nextToken();
							Log.d(TAG, "before: " + email);
							if(email.indexOf('<') != -1) {
								email = email.substring(email.indexOf('<') + 1, email.lastIndexOf('>'));	
							}
							// TODO - validate email format
							Log.d(TAG, "after: " + email);
							recepientList.append(email);
							recepientList.append(',');
						}

						Log.d(TAG, recepientList.toString());
						DelayedEmail delayedEmail = datasource.createEmail(from, recepientList.toString(), MailaterUtil.getStringIfNotNull(subject), MailaterUtil.getStringIfNotNull(body),time.format("%Y/%m/%d T%H:%M"));
						

						// send intent to start service to send mail later given
						// the ID of the row in the db

						// use Alarm Manager
						AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

						// Create an intent to our broadcast receiver
						Intent intent = new Intent(MailaterActivity.this, SendMailService.class);
						intent.putExtra("row_id", delayedEmail.getId());
						intent.putExtra("accountName", accountName); // use account name to retrieve its password later
						intent.putExtra("accountType", accountType);
															
						PendingIntent senderService = PendingIntent.getService(MailaterActivity.this, 0, intent, 0);
						alarmManager.set(AlarmManager.RTC_WAKEUP, time.toMillis(false), senderService);
//						clear();
						time = null;
						//set the mail status to pending
						datasource.updateEmailStatus(delayedEmail.getId(), EmailsDAO.PENDING);
						datasource.close();
						finish();
					} else {
//						Toast.makeText(MailaterActivity.this, "Please enter all required fields", Toast.LENGTH_LONG).show();
						Intent intent = new Intent(MailaterActivity.this, Outbox.class);
						startActivity(intent);
					}
				}
			}
		});
	}


	// -----------------------------------------------------

	private boolean validateRequiredFields() {
		EditText subject = (EditText) findViewById(R.id.subject);
		if (subject.getText().toString() == null || subject.getText().toString().trim().equals("")) {
			return false;
		}
		if (time == null) {
			return false;
		}
		return true;
	}


	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case GET_PASSWORD:
			// Set an EditText view to get user input
			final EditText input = new EditText(this);
			input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
			new AlertDialog.Builder(this).setTitle("Password").setMessage("Please provide the password to this account").setView(input).setPositiveButton("Submit", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					try {
						String password = input.getText().toString();
						Editor e = MailaterActivity.this.getSharedPreferences(MailaterActivity.this.accountName, Context.MODE_PRIVATE).edit();
						e.putString(accountName, password);// SimpleCrypto.encrypt(HEX,
														// password));//TODO
						e.commit();
					} catch (Exception e1) {
						e1.printStackTrace();
					}

				}
			}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// Do nothing.
				}
			}).show();
		case GET_DATETIME:
			//create custom dialog
			Dialog dialog = new Dialog(this);
			dialog.setContentView(R.layout.time2_layout);
			dialog.setCancelable(true);
			dialog.show();
		}

		return null;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case GET_SCHEDULE:
			Time time = new Time();
			time.setToNow();
			if(data == null) {
				return;
			}
			int year = data.getIntExtra("year", time.year);
			int month = data.getIntExtra("month", time.month);
			int day = data.getIntExtra("day", time.monthDay);
			int hour = data.getIntExtra("hour", time.hour);
			int min = data.getIntExtra("min", time.minute);

			time.set(0, min, hour, day, month, year); // get the scheduled time

			this.time = time;

			whenView.setText(time.format("%Y-%m-%d T%H:%M "));
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		accountName = null;
	}

	// private void handleException(Exception e) {
	// e.printStackTrace();
	// if (e instanceof HttpResponseException) {
	// HttpResponse response = ((HttpResponseException) e).getResponse();
	// int statusCode = response.getStatusCode();
	// try {
	// response.ignore();
	// } catch (IOException e1) {
	// e1.printStackTrace();
	// }
	// if (statusCode == 401 || statusCode == 403) {
	// gotAccount(true);
	// return;
	// }
	// }
	// Log.e(TAG, e.getMessage(), e);
	// }
}