package com.threebars.mailater;

import android.accounts.Account;

public class AccountWrapper extends Account {
	
	
	public AccountWrapper(Account account) {
		super(account.name, account.type);
	}
	
	@Override
	public String toString() {
		return this.name;
	}

}
