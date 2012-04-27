package com.threebars.mailater.util;

import java.util.StringTokenizer;

import android.widget.EditText;

public class MailaterUtil {

	/**
	 * convert comma separated strings to string array
	 * @param input
	 * @param delimiters
	 * @return
	 */
	public static String[] convertStringToArray(String input, String delimiters) {
		if(input == null) {
			return null;
		}
		
		StringTokenizer tokenizer = new StringTokenizer(input, delimiters);
		String[] array = new String[tokenizer.countTokens()];
		int index = 0;
		while(tokenizer.hasMoreTokens()) {
			array[index++] = tokenizer.nextToken().trim();
		}
		return array;
	}
	
	public static String getStringIfNotNull(EditText input) {
		if(input.getText() == null)
			return null;
		else return input.getText().toString();
				 
	}
}
