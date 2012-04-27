package com.threebars.mailater;

import android.database.Cursor;

/**
 * this interface will be passed to our vieweradapter so whenever we change a cursor we can call StartManagingCursor on them
 * @author Kamyar
 *
 */
public interface CursorChangeListener {

	public void startManagingMyCursor(Cursor cursor);
	public void stopManagingMyCursor(Cursor cursor);
}
