package com.threebars.mailater;

import java.util.ArrayList;

import android.accounts.Account;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;
import static com.threebars.mailater.Mail.MAIL_CONFIG.GMAIL;
import static com.threebars.mailater.Mail.MAIL_CONFIG.HOTMAIL;
import static com.threebars.mailater.Mail.MAIL_CONFIG.YAHOO;

public class SpinnerAdapter extends ArrayAdapter<AccountWrapper> {

	private Context context;
	private ArrayList<AccountWrapper> values;

	public SpinnerAdapter(Context context, int textViewResourceId, ArrayList<AccountWrapper> values) {
		super(context, textViewResourceId, values);
		this.context = context;
		this.values = values;
	}

	public int getCount() {
		return values.size();
	}

	public AccountWrapper getItem(int position) {
		return values.get(position);
	}

	public long getItemId(int position) {
		return position;
	}
	
	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		return getCustomView(position, convertView, parent);
	}

	public View getCustomView(int position, View convertView, ViewGroup parent) {

		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View row = inflater.inflate(R.layout.spinner_item, parent, false);
		CheckedTextView label = (CheckedTextView) row.findViewById(R.id.accountName);
		AccountWrapper accountWrapper = values.get(position);
		label.setText(accountWrapper.name);

		ImageView icon = (ImageView) row.findViewById(R.id.icon1);
		if(accountWrapper.type.indexOf(GMAIL.getAccountType()) != -1)
			icon.setImageResource(R.drawable.gmail);
		else if(accountWrapper.type.equalsIgnoreCase(HOTMAIL.getAccountType()) || accountWrapper.name.contains("@hotmail")) 
			icon.setImageResource(R.drawable.hotmail);
		else if(accountWrapper.type.equalsIgnoreCase(YAHOO.getAccountType()) || accountWrapper.name.contains("@yahoo"))
			icon.setImageResource(R.drawable.yahoo);
		
		return row;
	}

	// @Override
	// public View getDropDownView(int position, View convertView, ViewGroup
	// parent) {
	// // I created a dynamic TextView here, but you can reference your own
	// // custom layout for each spinner item
	// TextView label = new TextView(context);
	// label.setTextColor(Color.BLACK);
	// Drawable img = getContext().getResources().getDrawable( R.drawable.gmail
	// );
	// img.setBounds(0, 0, 50, 50);
	// label.setCompoundDrawablesWithIntrinsicBounds(null, null, img, null);
	// // Then you can get the current item using the values array (Users
	// // array) and the current position
	// // You can NOW reference each method you has created in your bean object
	// // (User class)
	// label.setText(values.get(position).name);
	//
	// // And finally return your dynamic (or custom) view for each spinner
	// // item
	// return label;
	// }

	// @Override
	// public View getView(int position, View convertView, ViewGroup parent) {
	// // I created a dynamic TextView here, but you can reference your own
	// // custom layout for each spinner item
	// TextView label = new TextView(context);
	// label.setTextColor(Color.BLACK);
	// Drawable img = getContext().getResources().getDrawable( R.drawable.gmail
	// );
	// label.setCompoundDrawablesWithIntrinsicBounds(img, null, null, null);
	// // Then you can get the current item using the values array (Users
	// // array) and the current position
	// // You can NOW reference each method you has created in your bean object
	// // (User class)
	// label.setText(values.get(position).name);
	//
	// // And finally return your dynamic (or custom) view for each spinner
	// // item
	// return label;
	// }

	// // And here is when the "chooser" is popped up
	// // Normally is the same view, but you can customize it if you want
	// @Override
	// public View getDropDownView(int position, View convertView, ViewGroup
	// parent) {
	//
	// TextView label = new TextView(context);
	//
	// label.setTextColor(Color.BLACK);
	// label.setText(values.get(position).name);
	// return label;
	// }
}
