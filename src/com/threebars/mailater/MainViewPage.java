package com.threebars.mailater;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;

import com.threebars.mailater.database.EmailsDAO;
import com.viewpagerindicator.TitlePageIndicator;

public class MainViewPage extends Activity {

	ViewPagerAdapter adapter;
	/**
	 * data source
	 */
	EmailsDAO emailsDao;	
	private CursorChangeListener cursorChangeListener;
	
	private static final String TAG = "MainViewPage";
	private Cursor pendingCursor;
	private Cursor outboxCursor;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "calling onCreate");
		setContentView(R.layout.scroll_main);
		
		emailsDao = new EmailsDAO(this);
		emailsDao.open();
		
		ViewPager pager = (ViewPager) findViewById(R.id.viewpager);
		adapter = new ViewPagerAdapter(this, pager);
		
		cursorChangeListener = new CursorChangeListener() {
			
			@Override
			public void startManagingMyCursor(Cursor cursor) {
				startManagingCursor(cursor);
			}

			@Override
			public void stopManagingMyCursor(Cursor cursor) {
				stopManagingCursor(cursor);
				cursor.close();
			}

		};
		
		adapter.setCursorChangeListener(cursorChangeListener);
		
		Cursor pendingCursor = emailsDao.getCursorForAllEmailsWithStatus(EmailsDAO.PENDING);
		Cursor outboxCursor = emailsDao.getCursorForAllEmailsWithStatus(EmailsDAO.SENT);
//		startManagingCursor(pendingCursor);
//		startManagingCursor(outboxCursor);
		
		adapter.setDataSource(emailsDao);
		adapter.setPendingCursor(pendingCursor);
		adapter.setOutboxCursor(outboxCursor);
		
		
		
		
		TitlePageIndicator indicator = (TitlePageIndicator) findViewById(R.id.indicator);
		indicator.setFooterColor(Color.rgb(160,82,45));
		indicator.setOnPageChangeListener(new OnPageChangeListener() {
			
			@Override
			public void onPageSelected(int position) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onPageScrollStateChanged(int state) {
				// TODO Auto-generated method stub
				
			}
		});
		pager.setAdapter(adapter);
		indicator.setViewPager(pager);
		
		// Register to receive messages.
		// We are registering an observer (mMessageReceiver) to receive Intents
		// with actions named "custom-event-name".
		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
		      new IntentFilter("com.threebars.updatelist"));
	}
	
	// Our handler for received Intents. This will be called whenever an Intent
	// with an action named "custom-event-name" is broadcasted.
	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
	  @Override
	  public void onReceive(Context context, Intent intent) {
		  Log.d(TAG, "Received broadcast updating the lists...");
		  if(emailsDao.getDatabase() != null && emailsDao.getDatabase().isOpen())
		  {
	    	if(!adapter.getPendingCursor().isClosed()) {
	    		adapter.getPendingCursor().requery();
	    	}
	    	if(!adapter.getOutboxCursor().isClosed()) {
	    		adapter.getOutboxCursor().requery();
	    	}
	    	adapter.resetAdapters();
	    	adapter.notifyDataSetChanged();
		  }
	  }
	};
	
	@Override
	protected void onPause() {
		Log.d(TAG, "onPause closing emailsDao" + (emailsDao != null));
		if(emailsDao != null) {
			emailsDao.close();
		}
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		Log.d(TAG, "onResume" + (emailsDao != null));
		if(emailsDao != null) {	//first time
			Log.d(TAG, "trying to open emailsDao");
			emailsDao.open();	
		}
		//set the adapters
		adapter.resetAdapters();
		super.onResume();
	}
	
	@Override
	protected void onDestroy() {
		// Unregister since the activity is about to be closed.
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
		super.onDestroy();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		switch (requestCode) {
		case ViewPagerAdapter.PICKFILE_RESULT_CODE:
			if (resultCode == RESULT_OK) {
				String filePath = data.getData().getPath();
				String fileName = data.getData().getLastPathSegment();
				adapter.setFilePath(filePath, fileName);
				Log.v(TAG, fileName);
				Log.v(TAG, filePath);
			}
			break;

		}
	}
}
