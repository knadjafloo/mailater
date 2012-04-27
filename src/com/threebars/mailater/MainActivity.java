package com.threebars.mailater;

import static com.threebars.mailater.Mail.MAIL_CONFIG.GMAIL;
import static com.threebars.mailater.Mail.MAIL_CONFIG.HOTMAIL;
import static com.threebars.mailater.Mail.MAIL_CONFIG.YAHOO;

import java.util.ArrayList;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends Activity implements OnChildClickListener {

	private final static String TAG = "MainActivity";
	
	private AccountListAdapter listAdapter;	//listadapter used in the listener
	
	/**
	 * ArrayList to hold our list of email Contacts (cached locally)
	 */
	private ArrayList<String> emailContacts;

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.accounts);

		if(savedInstanceState != null) {
			emailContacts = savedInstanceState.getStringArrayList("emailContacts");	
		}
		
		if(emailContacts == null) {	//start task to read all user contacts
			new GetEmailContactsTask().execute((Void)null);
		}
		// TODO - get this list from db
		ExpandableListView accountsExpandableListView = (ExpandableListView) findViewById(R.id.accountsExpandableListView);

		ArrayList<String> groups = new ArrayList<String>();
		groups.add(GMAIL.getName());
		groups.add(HOTMAIL.getName());
		groups.add(YAHOO.getName());

		ArrayList<ArrayList<Account>> children = new ArrayList<ArrayList<Account>>(groups.size());
		for (int i = 0; i < groups.size(); i++) { // initialize the children
			children.add(new ArrayList<Account>());
		}

		// TODO - do in separate thread
		AccountManager accountManager = AccountManager.get(this);
		final Account[] accounts = accountManager.getAccounts();
		for (Account account : accounts) {
			Log.d(TAG, account.name + " type : " + account.type);
			if (account.type.equalsIgnoreCase(GMAIL.getAccountType())) {
				children.get(0).add(account);
			} else if (account.type.equalsIgnoreCase(HOTMAIL.getAccountType()) || account.name.contains("@hotmail")) {
				if (!accountExists(account, children.get(1))) {
					children.get(1).add(account);
				}
			} else if (account.type.equalsIgnoreCase(YAHOO.getAccountType()) || account.name.contains("@yahoo")) {
				if (!accountExists(account, children.get(2))) {
					children.get(2).add(account);
				}
			}
		}

		// get all children for each account type
		listAdapter = new AccountListAdapter(this, groups, children);
		accountsExpandableListView.setAdapter(listAdapter);

		accountsExpandableListView.setOnChildClickListener(this);
	}
	
	private boolean accountExists(Account account, ArrayList<Account> accounts) {
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

	static class ViewHolder {
		public TextView text;
		public ImageView image;
	}

	/**
	 * temporary method to help find out where to put the children until we
	 * rewrite this
	 * 
	 * @param type
	 * @return
	 */
	private int getIndexByType(String type) {
		if (type.equalsIgnoreCase(GMAIL.getAccountType())) {
			return 0;
		} else if (type.equalsIgnoreCase(HOTMAIL.getAccountType())) {
			return 1;
		} else if (type.equalsIgnoreCase(YAHOO.getAccountType())) {
			return 2;
		}
		return 0;
	}

	private class AccountListAdapter extends BaseExpandableListAdapter {

		private final ArrayList<String> groups;
		private final Context context;
		private final ArrayList<ArrayList<Account>> children;

		public AccountListAdapter(Context context, ArrayList<String> groups, ArrayList<ArrayList<Account>> children) {
			this.context = context;
			this.groups = groups;
			this.children = children;
		}

		// /**
		// * A general add method, that allows you to add a Vehicle to this list
		// *
		// * Depending on if the category opf the vehicle is present or not,
		// * the corresponding item will either be added to an existing group if
		// it
		// * exists, else the group will be created and then the item will be
		// added
		// * @param vehicle
		// */
		// public void addItem(Account vehicle) {
		// if (!groups.contains(vehicle.getGroup())) {
		// groups.add(vehicle.getGroup());
		// }
		// int index = groups.indexOf(vehicle.getGroup());
		// if (children.size() < index + 1) {
		// children.add(new ArrayList<Account>());
		// }
		// children.get(index).add(vehicle);
		// }

		@Override
		public Object getChild(int groupPosition, int childPosition) {
			return children.get(groupPosition).get(childPosition);
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}

		// Return a child view. You can load your custom layout here.
		@Override
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
			Account account = (Account) getChild(groupPosition, childPosition);
			if (convertView == null) {
				LayoutInflater infalInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = infalInflater.inflate(R.layout.child_layout, null);
			}
			TextView tv = (TextView) convertView.findViewById(R.id.tvChild);
			tv.setText("   " + account.name);

			return convertView;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			return children.get(groupPosition).size();
		}

		@Override
		public Object getGroup(int groupPosition) {
			return groups.get(groupPosition);
		}

		@Override
		public int getGroupCount() {
			return groups.size();
		}

		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		// Return a group view. You can load your custom layout here.
		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
			String group = (String) getGroup(groupPosition);
			View rowView = convertView;
			if (convertView == null) {
				LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				rowView = inflater.inflate(R.layout.group_layout, null);
				ViewHolder viewHolder = new ViewHolder();
				viewHolder.text = (TextView) rowView.findViewById(R.id.accountName);
				viewHolder.image = (ImageView) rowView.findViewById(R.id.logo);
				rowView.setTag(viewHolder);
			}
			ViewHolder holder = (ViewHolder) rowView.getTag();

			TextView tv = holder.text;
			tv.setText(group);

			// Depending upon the child type, set the imageTextView01
			tv.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
			if (group.equalsIgnoreCase(GMAIL.getName())) {
				holder.image.setImageResource(R.drawable.gmail);
			} else if (group.equalsIgnoreCase(HOTMAIL.getName())) {
				holder.image.setImageResource(R.drawable.hotmail);
			} else if (group.equalsIgnoreCase(YAHOO.getName())) {
				holder.image.setImageResource(R.drawable.yahoo);
			}

			return rowView;
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}
		
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		
		Account account = (Account)listAdapter.getChild(groupPosition, childPosition);
		Log.d(TAG, "selected account : " + account.name);
		
		Intent gmailIntent = new Intent(MainActivity.this, MailaterActivity.class);
		gmailIntent.putStringArrayListExtra("emailContacts", emailContacts);
		gmailIntent.putExtra("account", account.name);
		gmailIntent.putExtra("accountType", account.type);
		startActivity(gmailIntent);
		
		return true;
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		if (emailContacts != null && emailContacts.size() > 0) {
			outState.putStringArrayList("emailContacts", emailContacts);
		}
	}

	
	private ArrayList<String> getEmailAddresses() {
		if (emailContacts != null && emailContacts.size() > 0) {
			return emailContacts;
		}
		emailContacts = new ArrayList<String>();
		ContentResolver cr = getContentResolver();
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
					Log.d(TAG, name + "<" + email + ">");
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
	
	/**
	 * check if email is valid and not empty
	 * 
	 * @param email
	 * @return
	 */
	private boolean isValidEmail(String email) {
		return email != null && email.length() > 0 && email.indexOf('@') != -1;
	}
	
	class GetEmailContactsTask extends AsyncTask<Void, Void, ArrayList<String>> {

		@Override
		protected ArrayList<String> doInBackground(Void... params) {
			return getEmailAddresses();
		}
		
		@Override
		protected void onPostExecute(ArrayList<String> result) {
			emailContacts = result;
		}
	}

}
