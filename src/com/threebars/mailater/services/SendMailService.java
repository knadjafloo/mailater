package com.threebars.mailater.services;

import java.util.ArrayList;
import java.util.List;

import javax.mail.AuthenticationFailedException;

import com.threebars.mailater.Mail;
import com.threebars.mailater.MailaterActivity;
import com.threebars.mailater.MainActivity;
import com.threebars.mailater.MainViewPage;
import com.threebars.mailater.R;
import com.threebars.mailater.database.EmailsDAO;
import com.threebars.mailater.database.MySQLiteHelper;
import com.threebars.mailater.model.DelayedEmail;
import com.threebars.mailater.util.MailaterUtil;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import static com.threebars.mailater.Mail.MAIL_CONFIG.*;

public class SendMailService extends Service {

	private final String TAG = "SENDMAILSERVICE";

	private Handler handler = new Handler();

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	
	@Override
	public int onStartCommand(final Intent intent, int flags, int startId) {
		Log.d(TAG, "Starting Send mail service....");

		final String accountName = intent.getExtras().getString("accountName");
		final String accountType = intent.getExtras().getString("accountType");
		final NotificationRunnable updateGuiNotification = new NotificationRunnable(accountName);

		// background thread to actually send the email so we don't block the ui thread
		// and then update the ui using handler
		Runnable sendMailThread = new Runnable() {
			@Override
			public void run() {
				// send Mail TODO

				long rowId = intent.getExtras().getLong("row_id", -1);
				String accountName = intent.getExtras().getString("accountName");
				String password = intent.getExtras().getString("password");
//				SharedPreferences sharedPref = SendMailService.this.getSharedPreferences(accountName, Context.MODE_PRIVATE);
//				String password = sharedPref.getString(accountName, null);
				Log.d(TAG, "userName : " + accountName + " password: " + password);
				EmailsDAO datasource = new EmailsDAO(SendMailService.this);
				datasource.open();

				DelayedEmail delayedEmail = datasource.getEmail(rowId);
//				Log.d(TAG, "sender: " + delayedEmail.getSender());
//				Log.d(TAG, "receip : " + delayedEmail.getReceipients());
//				Log.d(TAG, "subject: " + delayedEmail.getSubject());
//				Log.d(TAG, "body   : " + delayedEmail.getBody());
//				Log.d(TAG, "accName: " + accountName);
				boolean authenticationFailed = false;
				boolean otherErrorOccured = false;
				try {

					Mail m = null;
					Log.d(TAG, "account type : " + accountType + " name : " + accountName);
					// send email
					if(accountType.contains(GMAIL.getAccountType())) {
						m = new Mail(accountName, password);	
					}
					else if(accountType.contains(HOTMAIL.getAccountType())) {
						m = new Mail(accountName, password, HOTMAIL);
					}
					else if(accountType.contains(YAHOO.getAccountType())) {
						m = new Mail(accountName, password, YAHOO);
					}
					else {
						new Mail(accountName, password);
					}
					//use factory for Mail

					String[] toArr =  MailaterUtil.convertStringToArray(delayedEmail.getReceipients(), ",");
					for(String s : toArr) {
						Log.d(TAG, "to : " + s);
					}
					m.setTo(toArr);
					m.setFrom(accountName);
					m.setSubject(delayedEmail.getSubject());
					m.setBody(delayedEmail.getBody());

					try {
						String attachments = delayedEmail.getAttachments();
						if(attachments!= null && attachments.length() > 0) {
							Log.v(TAG, attachments);
							m.addAttachment(attachments);	
						}
						
						if (m.send()) {
							Log.e(TAG, "finished sending email...");
							handler.post(updateGuiNotification);
							datasource.updateEmailStatus(rowId, EmailsDAO.SENT);
							//if no exceptions mark the mail as sent
							
							Intent intent = new Intent("com.threebars.updatelist");
//							sendBroadcast(intent); // finally broadcast
							LocalBroadcastManager.getInstance(SendMailService.this).sendBroadcast(intent);
						} else {
							//TODO handle error
							Log.e(TAG, "Email was not sent.");
						}
					}
					catch(AuthenticationFailedException e) {
						authenticationFailed = true;
					}
					catch (Exception e) {
						otherErrorOccured = true;
						Log.e("MailApp", "Could not send email", e);
					}
					
					
					
				} catch (Exception e) {
					e.printStackTrace();
				}
				finally {
					Log.d(TAG, "send email now closing db");
					
					datasource.close();
					String errorMsg = "";
					//send error notification
					if(authenticationFailed)
						errorMsg = "Authentication Failed";
					else if(otherErrorOccured)
						errorMsg = "An error occured and the mail was not sent.";
					if(authenticationFailed || otherErrorOccured)
					{
						sendErrorNotification(accountName, errorMsg);
					}
				}

			}
		};

		// send email
		Thread thread = new Thread(null, sendMailThread, "SendMailThread");
		thread.start();

		return START_NOT_STICKY;
	}

	/**
	 * Notification that the email was sent
	 */
	private void sendNotification(String accountName) {
		int MY_NOTIFICATION_ID = 1;
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		Notification myNotification = new Notification(R.drawable.blue_email, "Mail Sent", System.currentTimeMillis());
		
		Context context = getApplicationContext();
		String notificationTitle = "Scheduled Email Sent";
		String notificationText = "Email was successfully sent to " + accountName;
		
		Intent notificationIntent = new Intent(this, MainViewPage.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
//		myNotification.defaults |= Notification.DEFAULT_SOUND;
		myNotification.flags |= Notification.FLAG_AUTO_CANCEL;
		myNotification.setLatestEventInfo(context, notificationTitle, notificationText, pendingIntent);
		notificationManager.notify(MY_NOTIFICATION_ID, myNotification);
		Log.d(TAG, "sent notification");
		stopSelf();
	}
	
	
	/**
	 * Notification that the email was sent
	 */
	private void sendErrorNotification(String accountName, String error) {
		int MY_NOTIFICATION_ID = 1;
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		Notification myNotification = new Notification(R.drawable.blue_email, "Mail Not Sent", System.currentTimeMillis());
		
		Context context = getApplicationContext();
		String notificationTitle = "Scheduled Email Failed";
		String notificationText = error;
		
		Intent notificationIntent = new Intent(this, MainViewPage.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
//		myNotification.defaults |= Notification.DEFAULT_SOUND;
		myNotification.flags |= Notification.FLAG_AUTO_CANCEL;
		myNotification.setLatestEventInfo(context, notificationTitle, notificationText, pendingIntent);
		notificationManager.notify(MY_NOTIFICATION_ID, myNotification);

		stopSelf();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
	class NotificationRunnable implements Runnable {

		private String name;
		public NotificationRunnable(String name) {
			this.name = name;
		}
		@Override
		public void run() {
			sendNotification(name);
		}

	}
}


