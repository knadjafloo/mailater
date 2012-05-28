package com.threebars.mailater;

import static com.threebars.mailater.Mail.MAIL_CONFIG.GMAIL;
import static com.threebars.mailater.Mail.MAIL_CONFIG.HOTMAIL;
import static com.threebars.mailater.Mail.MAIL_CONFIG.YAHOO;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;


import com.threebars.mailater.database.EmailsDAO;
import com.threebars.mailater.database.MySQLiteHelper.COLUMN_NAMES;
import com.threebars.mailater.model.DelayedEmail;
import com.threebars.mailater.services.SendMailService;
import com.threebars.mailater.util.MailaterUtil;
import com.viewpagerindicator.TitleProvider;

public class ViewPagerAdapter extends PagerAdapter implements TitleProvider {
	private static String[] titles ;//= new String[] { "Main", "Pending", "Outbox" };
	private final Context context;
	/**
	 * ArrayList to hold our list of email Contacts (cached locally)
	 */
	private ArrayList<String> emailContacts;
	
	private static final int GET_PASSWORD = 1;
	private static final int GET_DATETIME = 2;
	
	private final static int PENDING_PAGE = 1;
	private final static int OUTBOX_PAGE = 2;
	
	private static final int DATE_DIALOG_ID = 10;
	private static final int TIME_DIALOG_ID = 11;
	
	public static final int PICKFILE_RESULT_CODE = 20;
	
	private int mYear;
    private int mMonth;
    private int mDay;
	
    private int mHour;
    private int mMinute;
	
	private Cursor pendingCursor;
	private Cursor outboxCursor;
	private SimpleCursorAdapter pendingAdapter;
	private SimpleCursorAdapter outboxAdapter;
	
	private String accountName;
	private String accountType;
	private AccountManager accountManager;
	private ViewPager viewPager;
	
	private Builder editDialog = null;	
	
	private String DELETE_ACTION;
	private String RESEND_ACTION;
	private final String[] editActions;
	
	private final String[] sendAndDeleteActions;
	
	
	private Time time;
	private AutoCompleteTextView recipientFieldTextView ;
	private TextView dateView;
	private TextView timeView;
	private TextView attachmentView;
	
	private String attachmentPath = "";
	
	private SimpleDateFormat dateFormatter;
	private SimpleDateFormat timeFormatter;
	
	
	public ViewPagerAdapter(Context context, ViewPager viewPager) {
		this.context = context;
		this.viewPager = viewPager;
		
		// "Main", "Pending", "Outbox" 
		titles = new String[] { context.getString(R.string.Main), context.getString(R.string.Pending), context.getString(R.string.Outbox)};
		DELETE_ACTION = context.getString(R.string.Delete);
		RESEND_ACTION = context.getString(R.string.SendNow);
		
		editActions = new String[] {DELETE_ACTION};
		sendAndDeleteActions = new String[] {RESEND_ACTION, DELETE_ACTION};
		
		dateFormatter = new SimpleDateFormat(context.getString(R.string.dateFormat));
		timeFormatter = new SimpleDateFormat(context.getString(R.string.timeFormat));
		
		
	}

	@Override
	public String getTitle(int position) {
		return titles[position];
	}

	@Override
	public int getCount() {
		return titles.length;
	}

	@Override
	public Object instantiateItem(View pager, int position) {
		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = null;
		switch (position) {
		case 0:	//main
			layout = inflater.inflate(R.layout.main, null);
			((ViewPager) pager).addView(layout, 0);
			initiateMainAcitivity(layout);
			return layout;
		case 1:	//pending
			layout = inflater.inflate(R.layout.pending_emails, null);
			((ViewPager) pager).addView(layout, 0);
			outboxAdapter = initiateOutboxLayout(layout, EmailsDAO.PENDING);
			return layout;
		case 2: //sent
			layout = inflater.inflate(R.layout.pending_emails, null);
			((ViewPager) pager).addView(layout, 0);
			pendingAdapter = initiateOutboxLayout(layout, EmailsDAO.SENT);
			return layout;
		default:
			TextView v = new TextView(context);
			v.setText(titles[position]);
			((ViewPager) pager).addView(v, 0);
			return v;
		}
	}
	
	@Override
	public void destroyItem(View pager, int position, Object view) {
		((ViewPager) pager).removeView((View) view);
	}

	@Override
	public boolean isViewFromObject(View view, Object object) {
		return view.equals(object);
	}

	@Override
	public void finishUpdate(View view) {
	}

	@Override
	public void restoreState(Parcelable p, ClassLoader c) {
	}

	@Override
	public Parcelable saveState() {
		return null;
	}

	@Override
	public void startUpdate(View view) {
	}
	
	
	public void setPendingCursor(Cursor cursor) {
		this.pendingCursor = cursor;
	}
	
	public void setOutboxCursor(Cursor cursor) {
		this.outboxCursor = cursor;
	}
	
	public Cursor getPendingCursor() {
		if(pendingCursor.isClosed()) {
			pendingCursor = emailsDao.getCursorForAllEmailsWithStatus(EmailsDAO.PENDING);
		}
		return this.pendingCursor;
	}
	
	public Cursor getOutboxCursor() {
		if(outboxCursor.isClosed()) {
			outboxCursor = emailsDao.getCursorForAllEmailsWithStatus(EmailsDAO.SENT);
		}
		return this.outboxCursor;
	}
	
	//------------------------------------------------------ private methods
	private void goToViewPage(int index) {
		this.viewPager.setCurrentItem(index);
		updateCursors(index);
	}
	
	//TODO - do this asynchronously
	public void updateCursors(int index) {
		switch (index) {
		case PENDING_PAGE:
			cursorChangeListener.stopManagingMyCursor(pendingCursor);
			pendingCursor = emailsDao.getCursorForAllEmailsWithStatus(EmailsDAO.PENDING);
			cursorChangeListener.startManagingMyCursor(pendingCursor);
			pendingAdapter.changeCursor(pendingCursor);
			pendingAdapter.notifyDataSetChanged();
			break;
		case OUTBOX_PAGE:
			cursorChangeListener.stopManagingMyCursor(outboxCursor);
			outboxCursor = emailsDao.getCursorForAllEmailsWithStatus(EmailsDAO.SENT);
			cursorChangeListener.startManagingMyCursor(outboxCursor);
			outboxAdapter.changeCursor(outboxCursor);
			outboxAdapter.notifyDataSetChanged();
			break;
		}
	}
	
	private SimpleCursorAdapter initiateOutboxLayout(final View layout, String action) {
		Log.d(TAG, "initiateOutboxLayout");
		//TODO do this using fragments
		if(action.equals(EmailsDAO.PENDING)) {
			pendingCursor = emailsDao.getCursorForAllEmailsWithStatus(action);
			outboxAdapter = new SimpleCursorAdapter(
		            context,
		            R.layout.email_row,
		            pendingCursor,                                              
		            new String[] {COLUMN_NAMES.COLUMN_SUBJECT.getName(), COLUMN_NAMES.COLUMN_BODY.getName(), COLUMN_NAMES.COLUMN_DATE.getName(), COLUMN_NAMES.COLUMN_RECEPIENTS.getName()},           
		            new int[] {R.id.emailSubject, R.id.emailText, R.id.emailDate, R.id.emailAddress},
		            0);
			
			ListView listView = (ListView) layout.findViewById(android.R.id.list);
			listView.setAdapter(outboxAdapter);
			
			listView.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					pendingCursor.moveToPosition(position);
	            	long rowId = pendingCursor.getLong(pendingCursor.getColumnIndex(COLUMN_NAMES.COLUMN_ID.getName()));
	            	Intent mailViewIntent = new Intent(context, MailViewActivity.class);
	            	mailViewIntent.putExtra("rowId", rowId);
	            	context.startActivity(mailViewIntent);
				}
			});
			
			listView.setOnItemLongClickListener(new OnItemLongClickListener() {

		            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		            	
		            	pendingCursor.moveToPosition(position);
		            	long rowId = pendingCursor.getLong(pendingCursor.getColumnIndex(COLUMN_NAMES.COLUMN_ID.getName()));
		            	showSelectionDialog(rowId, PENDING_PAGE);
		                return true;
		            }
		        });
	
		}
		else if(action.equals(EmailsDAO.SENT)) {
			outboxCursor = emailsDao.getCursorForAllEmailsWithStatus(action);
			pendingAdapter = new SimpleCursorAdapter(
		            context,
		            R.layout.email_row,
		            outboxCursor,                                              
		            new String[] {COLUMN_NAMES.COLUMN_SUBJECT.getName(), COLUMN_NAMES.COLUMN_BODY.getName(), COLUMN_NAMES.COLUMN_DATE.getName(), COLUMN_NAMES.COLUMN_RECEPIENTS.getName()},           
		            new int[] {R.id.emailSubject, R.id.emailText, R.id.emailDate, R.id.emailAddress},
		            0);
			
			ListView listView = (ListView) layout.findViewById(android.R.id.list);
			listView.setAdapter(pendingAdapter);
			
			listView.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					outboxCursor.moveToPosition(position);
	            	long rowId = outboxCursor.getLong(outboxCursor.getColumnIndex(COLUMN_NAMES.COLUMN_ID.getName()));
	            	Intent mailViewIntent = new Intent(context, MailViewActivity.class);
	            	mailViewIntent.putExtra("rowId", rowId);
	            	context.startActivity(mailViewIntent);
				}
			});
			
			listView.setOnItemLongClickListener(new OnItemLongClickListener() {

		            public boolean onItemLongClick(AdapterView<?> arg0, View view, int position, long id) {
		            	
		            	outboxCursor.moveToPosition(position);
		            	long rowId = outboxCursor.getLong(outboxCursor.getColumnIndex(COLUMN_NAMES.COLUMN_ID.getName()));
		            	showSelectionDialog(rowId, OUTBOX_PAGE);
//		            	Toast.makeText(context, "row id : " + rowId, Toast.LENGTH_LONG).show();
		                return true;
		            }
		        });
	
		}
		
			
		
//			this.context.startManagingCursor(cursor);
		
		return outboxAdapter;	///TODO remove
	}
	
	
	private void showSelectionDialog(final long rowId, final int PAGE_INDEX)
	{
		final String[] actions;
		
		if(PAGE_INDEX == OUTBOX_PAGE)
		{
			actions = editActions;
		}
		else {
			actions = sendAndDeleteActions;
		}
		if(editDialog == null)
		{
			editDialog = new AlertDialog.Builder(context);
			editDialog.setTitle(context.getString(R.string.selectAction));
			editDialog.setItems(actions,  new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int index) {
				
					String action = actions[index];
					if(action.equals(DELETE_ACTION)) {	//change to enum
						emailsDao.deleteEmail(rowId);
						
						//update the list
						updateCursors(PAGE_INDEX);
					}
					else if(action.equals(RESEND_ACTION)) {
						//ask for password again
						showPasswordDialog(new TextListener() {
							
							@Override
							public void onPositiveResult(String password) {
								Time timeNow = new Time();
								timeNow.setToNow();
								sendEmail(rowId, timeNow, password);
							}
						}, getAccountName(rowId));
						
					}
				}
			});
		}
		
		editDialog.show();
	}
	
	/**
	 * get the account name associated with this row
	 * @param rowId
	 * @return
	 */
	private String getAccountName (long rowId) {
		
		DelayedEmail email = emailsDao.getEmail(rowId);
		if(email != null) {
//			Log.d(TAG, "finding password for " + email.getSender());
			return email.getSender();
		}
		return "";
	}
	
	private boolean accountExists(AccountWrapper account, ArrayList<AccountWrapper> accounts) {
		if(accounts == null) {
			return false;
		}
		
		for(Account acc : accounts) {
			if(acc.name.equalsIgnoreCase(account.name)) {
				return true;
			}
		}
		return false;
	}
	
	private DatePickerDialog datePickerDialog;
	private TimePickerDialog timePickerDialog;

	private void showDialog(int id) {
		switch (id) {
		case DATE_DIALOG_ID:
			if(datePickerDialog == null) {
				datePickerDialog = new DatePickerDialog(context, mDateSetListener, mYear, mMonth, mDay);
			}
			else {
				datePickerDialog.updateDate(mYear, mMonth, mDay);
			}
			datePickerDialog.show();
			return;
		case TIME_DIALOG_ID:
			if(timePickerDialog == null) {
				timePickerDialog = new TimePickerDialog(context, mTimeSetListener, mHour, mMinute, false);
			}
			else {
				timePickerDialog.updateTime(mHour, mMinute);
			}
			timePickerDialog.show();
			return;
		}
	}

	// the callback received when the user "sets" the date in the dialog
	private DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() {

		public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
			mYear = year;
			mMonth = monthOfYear;
			mDay = dayOfMonth;
			updateDisplay();
		}
	};

	// the callback received when the user "sets" the time in the dialog
	private TimePickerDialog.OnTimeSetListener mTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			mHour = hourOfDay;
			mMinute = minute;
			updateDisplay();
		}
	};
	
    // updates the date in the TextView
    private void updateDisplay() {
    	Calendar cal = Calendar.getInstance();
    	cal.set(Calendar.YEAR, mYear);
    	cal.set(Calendar.MONTH, mMonth);
    	cal.set(Calendar.DAY_OF_MONTH, mDay);
    	cal.set(Calendar.HOUR_OF_DAY, mHour);
    	cal.set(Calendar.MINUTE, mMinute);
    	
        dateView.setText(dateFormatter.format(cal.getTime()));
        timeView.setText(timeFormatter.format(cal.getTime()));
    }

	private void initiateMainAcitivity(final View layout) {
		cleanUp();
		
		recipientFieldTextView = (AutoCompleteTextView) layout.findViewById(R.id.to);
		if(emailContacts == null) {	//start task to read all user contacts
			new GetEmailContactsTask().execute((Void)null);
		}
		else {
			// add the contacts to the 'To' field of email
//			TextView listItem = (TextView)layout.findViewById(R.id.listItem);
			
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, R.layout.list_item, emailContacts);
			recipientFieldTextView.setAdapter(adapter);
		}
		Calendar now = Calendar.getInstance();
		dateView = (TextView) layout.findViewById(R.id.date);
		dateView.setText(dateFormatter.format(now.getTime()));
		
		timeView = (TextView) layout.findViewById(R.id.time);
		timeView.setText(timeFormatter.format(now.getTime()));
		
		  // get the current date
        mYear = now.get(Calendar.YEAR);
        mMonth = now.get(Calendar.MONTH);
        mDay = now.get(Calendar.DAY_OF_MONTH);
        
        mHour = now.get(Calendar.HOUR_OF_DAY);
        mMinute = now.get(Calendar.MINUTE);
		
		dateView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showDialog(DATE_DIALOG_ID);
            }
        });
		
		timeView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showDialog(TIME_DIALOG_ID);
            }
        });
		
		TextView timeLabel = (TextView)layout.findViewById(R.id.timeLabel);
		
		
		attachmentView = (TextView) layout.findViewById(R.id.attachment);
		attachmentView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
//            	Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//                intent.setType("file/*");

            	final String MIME_TYPE_ALL = "*/*"; // Filter for all MIME types
            	String title = "";//context.getString(R.string.select_file);		
        		String type = MIME_TYPE_ALL;	
        		
        		// Implicitly allow the user to select a particular kind of data
        		final Intent intent = new Intent(Intent.ACTION_GET_CONTENT); 
        		// Specify the MIME data type filter (Must be lower case)
        		intent.setType(type.toLowerCase()); 
        		// Only return URIs that can be opened with ContentResolver
        		intent.addCategory(Intent.CATEGORY_OPENABLE);
        		// Display intent chooser
        		try {
        			 ((Activity) context).startActivityForResult(intent,PICKFILE_RESULT_CODE);
        		} catch (android.content.ActivityNotFoundException e) {
//        			onFileError(e);
        		}
            	
//                ((Activity) context).startActivityForResult(intent,PICKFILE_RESULT_CODE);
            }
        });
		
		
		// TODO - do in separate thread
		AccountManager accountManager = AccountManager.get(context);
		final Account[] allAccounts = accountManager.getAccounts();
		ArrayList<AccountWrapper> accountList = new ArrayList<AccountWrapper>();
		for(Account account : allAccounts) {
			if(account.type.equals("com.dropbox.android.account")) {
				continue;
			}
			if (account.type.equalsIgnoreCase(GMAIL.getAccountType())) {
				accountList.add(new AccountWrapper(account));
			} else if (account.type.equalsIgnoreCase(HOTMAIL.getAccountType()) || account.name.contains("@hotmail")) {
				AccountWrapper accountWrapper = new AccountWrapper(account);
				if (!accountExists(accountWrapper, accountList)) {
					accountList.add(accountWrapper);
				}
			} else if (account.type.equalsIgnoreCase(YAHOO.getAccountType()) || account.name.contains("@yahoo")) {
				AccountWrapper accountWrapper = new AccountWrapper(account);
				if (!accountExists(accountWrapper,accountList)) {
					accountList.add(accountWrapper);
				}
			}
		}
				
		
		Spinner fromSpinner = (Spinner) layout.findViewById(R.id.fromSpinner);
		SpinnerAdapter spinnerAdapter = new SpinnerAdapter(context, android.R.layout.simple_spinner_item, accountList);
//		spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		fromSpinner.setAdapter(spinnerAdapter);
		
		fromSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent,View view, int pos, long id) {
				AccountWrapper account = (AccountWrapper)parent.getItemAtPosition(pos);
				accountName = account.name;
				accountType = account.type;
			}

			@Override
			public void onNothingSelected(AdapterView parent) {
				//do nothing
			}
		});
/*
		Bundle extras = getIntent().getExtras();
		accountName = extras.getString("account");
		accountType = extras.getString("accountType");
		ArrayList<String> emailContacts = extras.getStringArrayList("emailContacts");

		if (accountName != null) {
			TextView from = (TextView) findViewById(R.id.from);
			from.setText(accountName);
		}
*/		


		Button sendButton = (Button) layout.findViewById(R.id.sendButtonId);
		sendButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
//				String passwd = context.getSharedPreferences(accountName, Context.MODE_PRIVATE).getString(accountName, null);
//				if (passwd == null) {
				if (validateRequiredFields(layout)) {
					showPasswordDialog(new TextListener() {
						
						@Override
						public void onPositiveResult(String password) {
							// validate required fields are there
				

								EditText subject = (EditText) layout.findViewById(R.id.subject);
								EditText recepients = (EditText) layout.findViewById(R.id.to);
								String from = accountName;
								EditText body = (EditText) layout.findViewById(R.id.mailText);

								StringBuilder recepientList = new StringBuilder();
								StringTokenizer tokenizer = new StringTokenizer(recepients.getText().toString(), ",");
								int totalCount = tokenizer.countTokens();
								while (tokenizer.hasMoreTokens()) {
									String email = tokenizer.nextToken();
									Log.d(TAG, "before: " + email);
									if(email.indexOf('<') != -1) {
										email = email.substring(email.indexOf('<') + 1, email.lastIndexOf('>'));	
									}
									// TODO - validate email format
									Log.d(TAG, "after: " + email);
									recepientList.append(email);
									if(totalCount > 1) {
										recepientList.append(',');
									}
								}

								Log.d(TAG, recepientList.toString());
								Calendar cal = Calendar.getInstance();
								cal.setTimeInMillis(time.toMillis(false));
								
								String dateAndTime = dateFormatter.format(cal.getTime()) + " " + timeFormatter.format(cal.getTime());
								
								DelayedEmail delayedEmail = emailsDao.createEmail(from, recepientList.toString(), MailaterUtil.getStringIfNotNull(subject), MailaterUtil.getStringIfNotNull(body), dateAndTime, attachmentPath);
								
								sendEmail(delayedEmail.getId(), null, password);
													
						}
					}, accountName);
				} else {
//					Toast.makeText(context, context.getString(R.string.validationFailedMessage), Toast.LENGTH_LONG).show();
					// Intent intent = new Intent(context, Outbox.class);
					// context.startActivity(intent);
				}	
					
//				} 
			}
		});
	}
	
	// send intent to start service to send mail later given
	// the ID of the row in the db
	private void sendEmail(long rowId, Time timeToSend, String password) {
		
		if(timeToSend != null) {
			time = timeToSend;	//overwrite original selected time if needed
		}
		// use Alarm Manager
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

		// Create an intent to our broadcast receiver
		Intent intent = new Intent(context, SendMailService.class);
		intent.putExtra("row_id", rowId);
		intent.putExtra("accountName", accountName); // use account name to retrieve its password later
		intent.putExtra("accountType", accountType);
		intent.putExtra("password", password);
											
		PendingIntent senderService = PendingIntent.getService(context, 0, intent, 0);
		alarmManager.set(AlarmManager.RTC_WAKEUP, time.toMillis(false), senderService);

		time = null;
		//set the mail status to pending
		emailsDao.updateEmailStatus(rowId, EmailsDAO.PENDING);
//		finish();
		// go to second tab
		goToViewPage(PENDING_PAGE);
	}
	
	class GetEmailContactsTask extends AsyncTask<Void, Void, ArrayList<String>> {

		@Override
		protected ArrayList<String> doInBackground(Void... params) {
			return getEmailAddresses();
		}
		
		@Override
		protected void onPostExecute(ArrayList<String> result) {
			emailContacts = result;
			
			
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, R.layout.list_item, emailContacts);
			recipientFieldTextView.setAdapter(adapter);
		}
	}
	
	private final static String TAG = "viewpageradapter";
	
	private ArrayList<String> getEmailAddresses() {
		if (emailContacts != null && emailContacts.size() > 0) {
			return emailContacts;
		}
		emailContacts = new ArrayList<String>();
		ContentResolver cr = context.getContentResolver();
		Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
		if (cur.getCount() > 0) {
			while (cur.moveToNext()) {
				String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
				String name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
				Cursor emailCur = cr.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null, ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?", new String[] { id }, null);
				while (emailCur.moveToNext()) {
					// This would allow you get several email addresses
					// if the email addresses were stored in an array
					String email = emailCur.getString(emailCur.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
					String emailType = emailCur.getString(emailCur.getColumnIndex(ContactsContract.CommonDataKinds.Email.TYPE));
					Log.v(TAG, name + "<" + email + ">");
					if (isValidEmail(email)) {
						emailContacts.add(name + " <" + email + ">");
					}

				}
				emailCur.close();
			}
			cur.close();
		}
		return emailContacts;
	}
	
	private boolean isValidEmail(String email) {
		return email != null && email.length() > 0 && email.indexOf('@') != -1;
	}
	
	private void showPasswordDialog(final TextListener listener, String account) {
		final EditText input = new EditText(context);
		input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		new AlertDialog.Builder(context).setTitle(context.getString(R.string.password)).setMessage(context.getString(R.string.providerPassword) + " " + account).setView(input).setPositiveButton(context.getString(R.string.submit), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				try {
					String password = input.getText().toString();
					listener.onPositiveResult(password);
					//removing storing of password
//					Editor e = context.getSharedPreferences(accountName, Context.MODE_PRIVATE).edit();
//					e.putString(accountName, password);// SimpleCrypto.encrypt(HEX, password));//TODO
//					e.commit();
				} catch (Exception e1) {
					e1.printStackTrace();
				}

			}
		}).setNegativeButton(context.getString(R.string.cancel), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Do nothing.
			}
		}).show();
	}
	
	

	private EmailsDAO emailsDao;	//data source
	private CursorChangeListener cursorChangeListener;
	

	

	private boolean validateRequiredFields(View layout) {
		EditText subject = (EditText) layout.findViewById(R.id.subject);
		boolean isValid = true;
		StringBuilder errorMessages = new StringBuilder();
		if (subject.getText().toString() == null || subject.getText().toString().trim().equals("")) {
			errorMessages.append(context.getString(R.string.enterSubject));
			isValid = false;
		}
		
		Time now = new Time();
		now.setToNow();
		
		if(time == null)
		{
			time = new Time();
		}
		
		int yearNum = mYear;
		int monthNum = mMonth;
		int dayNum = mDay ; // since Time day starts at 1

		int hourNum = mHour;
		int minNum = mMinute;
		

		time.set(0, minNum, hourNum, dayNum, monthNum, yearNum);
		
		if (time == null || time.before(now)) {
			errorMessages.append("\n" + context.getString(R.string.enterTimeFuture));
			isValid = false;
		}
		
		EditText recepients = (EditText) layout.findViewById(R.id.to);

		if(recepients.getText() == null || recepients.getText().toString().equals("") )
		{
			errorMessages.append("\n" + context.getString(R.string.needReceipient));
			isValid = false;
		}
		//check for valid email address
		Pattern p = Pattern.compile(".+@.+\\.[a-z]+");
		

		StringTokenizer tokenizer = new StringTokenizer(recepients.getText().toString(), ",");
		while (tokenizer.hasMoreTokens()) {
			String email = tokenizer.nextToken();
			Log.d(TAG, "before: " + email);
			if(email.indexOf('<') != -1) {
				email = email.substring(email.indexOf('<') + 1, email.lastIndexOf('>'));	
			}
			// TODO - validate email format
			Log.d(TAG, "after: " + email);
			Matcher m = p.matcher(email);
			boolean matchFound = m.matches();
			if(!matchFound) 	//invalid email
			{
				errorMessages.append("\n" + context.getString(R.string.enterValidEmail));
				isValid = false;
			}
		}
		
		if(!isValid) {
			Toast.makeText(context, errorMessages.toString(), Toast.LENGTH_LONG).show();
		}
				
//		Toast.makeText(context, context.getString(R.string.validationFailedMessage), Toast.LENGTH_LONG).show();
		return isValid;
	}
	
	private void cleanUp() {
		time = null;
		String accountName = null;
		String accountType = null;
		attachmentPath = "";
		Calendar now = Calendar.getInstance();
		mYear = now.get(Calendar.YEAR);
        mMonth = now.get(Calendar.MONTH);
        mDay = now.get(Calendar.DAY_OF_MONTH);
//		if(emailContacts != null) {
//			emailContacts.clear();
//		}
	}

	public void setDataSource(EmailsDAO emailsDao) {
		this.emailsDao = emailsDao;
	}

	public void setCursorChangeListener(CursorChangeListener cursorChangeListener) {
		this.cursorChangeListener = cursorChangeListener;
		
	}

	public void resetAdapters() {
		resetOutboxAdapter();
		resetPendingAdapter();
	}
	
	private void resetOutboxAdapter() {
		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.pending_emails, null);
		outboxAdapter = new SimpleCursorAdapter(
	            context,
	            R.layout.email_row,
	            outboxCursor,                                              
	            new String[] {COLUMN_NAMES.COLUMN_SUBJECT.getName(), COLUMN_NAMES.COLUMN_BODY.getName(), COLUMN_NAMES.COLUMN_DATE.getName(), COLUMN_NAMES.COLUMN_RECEPIENTS.getName()},           
	            new int[] {R.id.emailSubject, R.id.emailText, R.id.emailDate, R.id.emailAddress},
	            0);
		
		ListView listView = (ListView) layout.findViewById(android.R.id.list);
		listView.setAdapter(outboxAdapter);
	}
	
	private void resetPendingAdapter() {
		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.pending_emails, null);
		pendingAdapter = new SimpleCursorAdapter(
	            context,
	            R.layout.email_row,
	            pendingCursor,                                              
	            new String[] {COLUMN_NAMES.COLUMN_SUBJECT.getName(), COLUMN_NAMES.COLUMN_BODY.getName(), COLUMN_NAMES.COLUMN_DATE.getName(),COLUMN_NAMES.COLUMN_RECEPIENTS.getName()},           
	            new int[] {R.id.emailSubject, R.id.emailText, R.id.emailDate, R.id.emailAddress},
	            0);
		
		ListView listView = (ListView) layout.findViewById(android.R.id.list);
		listView.setAdapter(pendingAdapter);
	}

	public void setFilePath(String filePath, String fileName) {
		attachmentView.setText(fileName);
		attachmentPath = filePath ;
	}

}